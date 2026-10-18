package cn.luyou.config;

import cn.luyou.crypto.FieldEncryptor;
import cn.luyou.crypto.SensitiveDataRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 启动时扩列，并把仍是明文的存量敏感字段加密。新写入由 MyBatis 拦截器处理。
 */
@Component
public class SensitiveDataMigrationRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataMigrationRunner.class);
    private static final String PLACEHOLDERS =
            "'无','无证件号','无身份证','无身份证号','无电话','暂无','-','/','——','—'";

    private final JdbcTemplate jdbc;
    private final FieldEncryptor encryptor;
    private final boolean migrateExisting;
    private final Path jsonMarker;

    public SensitiveDataMigrationRunner(JdbcTemplate jdbc,
                                        FieldEncryptor encryptor,
                                        @Value("${app.crypto.migrate-existing:true}") boolean migrateExisting,
                                        @Value("${app.crypto.key-file:./data/field-encrypt.key}") String keyFile) {
        this.jdbc = jdbc;
        this.encryptor = encryptor;
        this.migrateExisting = migrateExisting;
        Path keyPath = Path.of(keyFile);
        Path parent = keyPath.getParent() == null ? Path.of("./data") : keyPath.getParent();
        this.jsonMarker = parent.resolve("sensitive-json.migrated");
    }

    @Override
    public void afterSingletonsInstantiated() {
        widenColumns();
        addFormatFlags();
        if (!migrateExisting) {
            log.info("已跳过存量敏感数据加密（app.crypto.migrate-existing=false）");
            return;
        }
        encryptPlainColumns();
        encryptJsonColumns();
    }

    private void widenColumns() {
        for (SensitiveDataRegistry.Column column : SensitiveDataRegistry.COLUMNS) {
            if (column.varcharLength() <= 0 || column.kind() == SensitiveDataRegistry.Kind.JSON) {
                continue;
            }
            Integer length = columnLength(column.table(), column.column());
            if (length == null || length >= column.varcharLength()) {
                continue;
            }
            String ddl = "ALTER TABLE `" + column.table() + "` MODIFY COLUMN `" + column.column()
                    + "` VARCHAR(" + column.varcharLength() + ") NULL COMMENT '敏感字段，AES加密存储'";
            jdbc.execute(ddl);
            log.info("已扩展 {}.{} 至 VARCHAR({})", column.table(), column.column(), column.varcharLength());
        }
    }

    private void addFormatFlags() {
        for (String table : SensitiveDataRegistry.FORMAT_FLAG_TABLES) {
            addFlagColumn(table, "id_number_fmt", "证件号格式：1正确 0异常");
            addFlagColumn(table, "phone_fmt", "手机号格式：1正确 0异常");
        }
    }

    private void addFlagColumn(String table, String column, String comment) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` TINYINT NULL COMMENT '" + comment + "'");
        log.info("已新增 {}.{}", table, column);
    }

    private void encryptPlainColumns() {
        for (SensitiveDataRegistry.Column column : SensitiveDataRegistry.COLUMNS) {
            if (column.kind() == SensitiveDataRegistry.Kind.JSON || columnLength(column.table(), column.column()) == null) {
                continue;
            }
            boolean withFlag = hasFormatFlag(column);
            int updated = 0;
            while (true) {
                String flagSelect = withFlag ? ", " + flagColumn(column) + " AS fmt" : ", NULL AS fmt";
                List<RowValue> rows = jdbc.query(
                        "SELECT id, `" + column.column() + "` AS v" + flagSelect
                                + " FROM `" + column.table() + "` WHERE `" + column.column()
                                + "` IS NOT NULL AND TRIM(`" + column.column() + "`) <> '' AND `"
                                + column.column() + "` NOT LIKE 'encv1:%' AND `" + column.column()
                                + "` NOT IN (" + PLACEHOLDERS + ") LIMIT 400",
                        (rs, rowNum) -> new RowValue(rs.getLong("id"), rs.getString("v")));
                if (rows.isEmpty()) {
                    break;
                }
                int batchUpdated = 0;
                for (RowValue row : rows) {
                    String encrypted = encryptor.encrypt(row.value());
                    if (row.value().equals(encrypted)) {
                        continue;
                    }
                    if (withFlag) {
                        Integer flag = column.kind() == SensitiveDataRegistry.Kind.PHONE
                                ? FieldEncryptor.phoneFlag(row.value())
                                : FieldEncryptor.idNumberFlag(row.value());
                        jdbc.update("UPDATE `" + column.table() + "` SET `" + column.column()
                                        + "` = ?, `" + flagColumn(column) + "` = ? WHERE id = ?",
                                encrypted, flag, row.id());
                    } else {
                        jdbc.update("UPDATE `" + column.table() + "` SET `" + column.column()
                                + "` = ? WHERE id = ?", encrypted, row.id());
                    }
                    updated++;
                    batchUpdated++;
                }
                if (batchUpdated == 0) {
                    log.warn("表 {} 列 {} 仍有明文未能加密，已停止该列迁移", column.table(), column.column());
                    break;
                }
                if (rows.size() < 400) {
                    break;
                }
            }
            if (updated > 0) {
                log.info("已加密存量数据 {}.{} {} 行", column.table(), column.column(), updated);
            }
        }
    }

    private void encryptJsonColumns() {
        if (Files.exists(jsonMarker)) {
            return;
        }
        for (SensitiveDataRegistry.Column column : SensitiveDataRegistry.COLUMNS) {
            if (column.kind() != SensitiveDataRegistry.Kind.JSON || columnLength(column.table(), column.column()) == null) {
                continue;
            }
            long lastId = 0;
            int updated = 0;
            while (true) {
                long cursor = lastId;
                List<RowValue> rows = jdbc.query(
                        "SELECT id, CAST(`" + column.column() + "` AS CHAR) AS v FROM `" + column.table()
                                + "` WHERE id > ? AND `" + column.column() + "` IS NOT NULL ORDER BY id LIMIT 200",
                        (rs, rowNum) -> new RowValue(rs.getLong("id"), rs.getString("v")), cursor);
                if (rows.isEmpty()) {
                    break;
                }
                for (RowValue row : rows) {
                    lastId = row.id();
                    String encrypted = encryptor.encryptJson(row.value());
                    if (row.value() != null && row.value().equals(encrypted)) {
                        continue;
                    }
                    jdbc.update("UPDATE `" + column.table() + "` SET `" + column.column()
                            + "` = ? WHERE id = ?", encrypted, row.id());
                    updated++;
                }
            }
            if (updated > 0) {
                log.info("已加密存量 JSON {}.{} {} 行", column.table(), column.column(), updated);
            }
        }
        try {
            Path parent = jsonMarker.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(jsonMarker, "ok");
        } catch (Exception e) {
            log.warn("写入 JSON 加密完成标记失败: {}", e.getMessage());
        }
    }

    private boolean hasFormatFlag(SensitiveDataRegistry.Column column) {
        if (!"id_number".equals(column.column()) && !"phone".equals(column.column())) {
            return false;
        }
        return SensitiveDataRegistry.FORMAT_FLAG_TABLES.contains(column.table())
                && columnLength(column.table(), flagColumn(column)) != null;
    }

    private static String flagColumn(SensitiveDataRegistry.Column column) {
        return "phone".equals(column.column()) ? "phone_fmt" : "id_number_fmt";
    }

    /** JSON 列没有字符长度，用 DATA_TYPE 是否存在代替。返回 0 表示列存在但非字符类型。 */
    private Integer columnLength(String table, String column) {
        return jdbc.query(
                "SELECT CHARACTER_MAXIMUM_LENGTH AS len, DATA_TYPE AS dataType FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    int len = rs.getInt("len");
                    if (rs.wasNull()) {
                        return 0;
                    }
                    return len;
                }, table, column);
    }

    private record RowValue(long id, String value) {
    }
}
