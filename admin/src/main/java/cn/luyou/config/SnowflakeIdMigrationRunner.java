package cn.luyou.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 停机一次性：将存量自增主键与逻辑外键重写为雪花 ID。
 * <p>
 * 启用方式：app.migrate-snowflake-ids=true，启动一次后务必改回 false。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.migrate-snowflake-ids", havingValue = "true")
public class SnowflakeIdMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdMigrationRunner.class);

    /** 需要去掉 AUTO_INCREMENT 并迁移主键的业务表 */
    private static final String[] TABLES = {
            "user", "sys_sms_config", "sys_message",
            "screening_school", "screening_key_population", "screening_close_contact", "close_contact_case",
            "latent_infection", "notice", "supervision_form", "latent_follow_up", "latent_check",
            "patient", "first_visit", "follow_up_visit", "medication_management", "medication_pickup",
            "epidemic_report", "permission", "role_permission", "department", "referral",
            "user_permission", "operation_log", "referral_tracking", "epidemic_import", "questionnaire_config"
    };

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final IdentifierGenerator identifierGenerator;
    private final ConfigurableApplicationContext applicationContext;

    public SnowflakeIdMigrationRunner(JdbcTemplate jdbc,
                                      PlatformTransactionManager transactionManager,
                                      IdentifierGenerator identifierGenerator,
                                      ConfigurableApplicationContext applicationContext) {
        this.jdbc = jdbc;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.identifierGenerator = identifierGenerator;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("========== 雪花 ID 存量迁移开始（app.migrate-snowflake-ids=true） ==========");
        final int[] exitCode = {0};
        try {
            if (isAlreadyMigrated()) {
                log.warn("检测到主键已无 AUTO_INCREMENT，跳过迁移。请将 app.migrate-snowflake-ids 改回 false。");
            } else {
                transactionTemplate.executeWithoutResult(status -> doMigrate());
                dropAutoIncrement();
                validate();
                log.warn("========== 雪花 ID 存量迁移完成，请将 app.migrate-snowflake-ids 改回 false 并重启 ==========");
            }
        } catch (Exception e) {
            exitCode[0] = 1;
            log.error("雪花 ID 存量迁移失败", e);
        }
        System.exit(SpringApplication.exit(applicationContext, () -> exitCode[0]));
    }

    private boolean isAlreadyMigrated() {
        Integer cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user'
                  AND COLUMN_NAME = 'id'
                  AND EXTRA LIKE '%auto_increment%'
                """, Integer.class);
        return cnt == null || cnt == 0;
    }

    private void doMigrate() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS `_id_map` (
                  `table_name` VARCHAR(64)  NOT NULL,
                  `old_id`     BIGINT       NOT NULL,
                  `new_id`     BIGINT       NOT NULL,
                  PRIMARY KEY (`table_name`, `old_id`),
                  UNIQUE KEY `uk_new` (`table_name`, `new_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.update("DELETE FROM `_id_map`");

        for (String table : TABLES) {
            if (!tableExists(table)) {
                log.warn("表不存在，跳过: {}", table);
                continue;
            }
            buildMapForTable(table);
        }

        remapForeignKeys();
        remapPrimaryKeys();
    }

    private boolean tableExists(String table) {
        Integer cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """, Integer.class, table);
        return cnt != null && cnt > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer cnt = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return cnt != null && cnt > 0;
    }

    private void buildMapForTable(String table) {
        List<Long> oldIds = jdbc.queryForList("SELECT `id` FROM `" + table + "`", Long.class);
        if (oldIds.isEmpty()) {
            log.info("映射生成: {} (0 行)", table);
            return;
        }
        List<Object[]> batch = new ArrayList<>(oldIds.size());
        for (Long oldId : oldIds) {
            long newId = identifierGenerator.nextId(null).longValue();
            batch.add(new Object[]{table, oldId, newId});
        }
        jdbc.batchUpdate("INSERT INTO `_id_map`(`table_name`,`old_id`,`new_id`) VALUES (?,?,?)", batch);
        log.info("映射生成: {} ({} 行)", table, oldIds.size());
    }

    private void remapForeignKeys() {
        // department 自引用
        updateFk("department", "parent_id", "department", true);

        // user
        updateFk("user", "department_id", "department", false);

        // permission
        updateFk("permission", "parent_id", "permission", true);
        updateFk("role_permission", "permission_id", "permission", false);
        updateFk("user_permission", "user_id", "user", false);
        updateFk("user_permission", "permission_id", "permission", false);

        // screening
        for (String t : List.of("screening_school", "screening_key_population", "screening_close_contact", "close_contact_case")) {
            updateFk(t, "department_id", "department", false);
            updateFk(t, "creator_id", "user", false);
        }

        // latent_infection
        updateScreeningIdByPopulation("latent_infection");
        updateFk("latent_infection", "department_id", "department", false);
        updateFk("latent_infection", "creator_id", "user", false);
        updateFk("latent_infection", "source_latent_id", "latent_infection", false);
        updateFk("latent_infection", "notice_id", "notice", false);

        // notice
        updatePolymorphicBizId("notice", "biz_id", "notice_type", Map.of(
                "patient", "patient",
                "latent", "latent_infection"
        ));
        updateFk("notice", "sender_id", "user", false);
        updateFk("notice", "receiver_org_id", "user", false);

        // latent children
        updateFk("supervision_form", "latent_infection_id", "latent_infection", false);
        updateFk("latent_follow_up", "latent_infection_id", "latent_infection", false);
        updateFk("latent_check", "latent_infection_id", "latent_infection", false);

        // patient
        updateScreeningIdByPopulation("patient");
        updateFk("patient", "latent_infection_id", "latent_infection", false);
        updateFk("patient", "department_id", "department", false);
        updateFk("patient", "creator_id", "user", false);
        updateFk("patient", "source_patient_id", "patient", false);
        updateFk("patient", "notice_id", "notice", false);

        for (String t : List.of("first_visit", "follow_up_visit", "medication_management",
                "medication_pickup", "epidemic_report")) {
            updateFk(t, "patient_id", "patient", false);
        }

        // referral
        updateReferralBizIds();
        updateFk("referral", "sender_id", "user", false);
        updateFk("referral", "receiver_org_id", "user", false);

        // messages
        updateSysMessageBizIds();
        updateFk("sys_message", "sender_id", "user", false);
        updateFk("sys_message", "receiver_id", "user", false);

        // operation_log（尽力按 biz_type / op_module）
        updateOperationLogBizIds();
        updateFk("operation_log", "user_id", "user", false);
        updateFk("operation_log", "department_id", "department", false);

        // referral_tracking / epidemic_import
        updateFk("referral_tracking", "receiver_user_id", "user", false);
        updateFk("referral_tracking", "receiver_dept_id", "department", false);
        updateFk("referral_tracking", "target_patient_id", "patient", false);
        updateFk("referral_tracking", "target_latent_id", "latent_infection", false);
        updateFk("referral_tracking", "department_id", "department", false);
        updateFk("referral_tracking", "creator_id", "user", false);

        updateFk("epidemic_import", "target_patient_id", "patient", false);
        updateFk("epidemic_import", "target_latent_id", "latent_infection", false);
        updateFk("epidemic_import", "department_id", "department", false);
        updateFk("epidemic_import", "creator_id", "user", false);
    }

    private void updateFk(String table, String column, String parentTable, boolean skipZero) {
        if (!tableExists(table) || !columnExists(table, column)) {
            return;
        }
        String skip = skipZero ? " AND t.`" + column + "` <> 0" : "";
        int n = jdbc.update("""
                UPDATE `%s` t
                INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`%s`
                SET t.`%s` = m.`new_id`
                WHERE t.`%s` IS NOT NULL%s
                """.formatted(table, column, column, column, skip), parentTable);
        log.info("外键重写: {}.{} -> {} ({} 行)", table, column, parentTable, n);
    }

    private void updateScreeningIdByPopulation(String table) {
        if (!tableExists(table) || !columnExists(table, "screening_id") || !columnExists(table, "population_type")) {
            return;
        }
        Map<String, String> typeToTable = new LinkedHashMap<>();
        typeToTable.put("school", "screening_school");
        typeToTable.put("keyPopulation", "screening_key_population");
        typeToTable.put("regular", "screening_key_population");
        typeToTable.put("closeContact", "screening_close_contact");

        for (Map.Entry<String, String> e : typeToTable.entrySet()) {
            int n = jdbc.update("""
                    UPDATE `%s` t
                    INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`screening_id`
                    SET t.`screening_id` = m.`new_id`
                    WHERE t.`screening_id` IS NOT NULL AND t.`population_type` = ?
                    """.formatted(table), e.getValue(), e.getKey());
            if (n > 0) {
                log.info("外键重写: {}.screening_id [{}] -> {} ({} 行)", table, e.getKey(), e.getValue(), n);
            }
        }
    }

    private void updatePolymorphicBizId(String table, String column, String typeColumn,
                                        Map<String, String> typeToParent) {
        if (!tableExists(table) || !columnExists(table, column) || !columnExists(table, typeColumn)) {
            return;
        }
        for (Map.Entry<String, String> e : typeToParent.entrySet()) {
            int n = jdbc.update("""
                    UPDATE `%s` t
                    INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`%s`
                    SET t.`%s` = m.`new_id`
                    WHERE t.`%s` IS NOT NULL AND t.`%s` = ?
                    """.formatted(table, column, column, column, typeColumn), e.getValue(), e.getKey());
            log.info("多态外键: {}.{} type={} -> {} ({} 行)", table, column, e.getKey(), e.getValue(), n);
        }
    }

    private void updateReferralBizIds() {
        if (!tableExists("referral")) {
            return;
        }
        // biz_type 形如 module_population：screening_school / latent_key / patient_aggregate ...
        record Rule(String likePattern, String parentTable) {}
        List<Rule> rules = List.of(
                new Rule("screening_school%", "screening_school"),
                new Rule("screening_key%", "screening_key_population"),
                new Rule("screening_close%", "screening_close_contact"),
                new Rule("suspected_%", "latent_infection"),
                new Rule("latent_%", "latent_infection"),
                new Rule("patient_%", "patient")
        );
        for (String col : List.of("biz_id", "target_biz_id")) {
            if (!columnExists("referral", col)) {
                continue;
            }
            for (Rule rule : rules) {
                int n = jdbc.update("""
                        UPDATE `referral` t
                        INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`%s`
                        SET t.`%s` = m.`new_id`
                        WHERE t.`%s` IS NOT NULL AND t.`biz_type` LIKE ?
                        """.formatted(col, col, col), rule.parentTable(), rule.likePattern());
                if (n > 0) {
                    log.info("外键重写: referral.{} LIKE {} -> {} ({} 行)", col, rule.likePattern(), rule.parentTable(), n);
                }
            }
        }
    }

    private void updateSysMessageBizIds() {
        if (!tableExists("sys_message") || !columnExists("sys_message", "biz_id")) {
            return;
        }
        record Rule(String typeLike, String parentTable) {}
        List<Rule> rules = List.of(
                new Rule("referral_tracking%", "referral_tracking"),
                new Rule("referral%", "referral"),
                new Rule("notice%", "notice"),
                new Rule("review_reminder", "screening_close_contact"),
                new Rule("supervision%", "latent_infection"),
                new Rule("visit%", "patient")
        );
        for (Rule rule : rules) {
            int n = jdbc.update("""
                    UPDATE `sys_message` t
                    INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`biz_id`
                    SET t.`biz_id` = m.`new_id`
                    WHERE t.`biz_id` IS NOT NULL AND t.`type` LIKE ?
                    """, rule.parentTable(), rule.typeLike());
            if (n > 0) {
                log.info("外键重写: sys_message.biz_id type LIKE {} -> {} ({} 行)", rule.typeLike(), rule.parentTable(), n);
            }
        }
    }

    private void updateOperationLogBizIds() {
        if (!tableExists("operation_log") || !columnExists("operation_log", "biz_id")) {
            return;
        }
        // 尽力匹配：优先 biz_type，其次 op_module
        record Rule(String condSql, Object[] args, String parentTable) {}
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"screening_school%"}, "screening_school"));
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"screening_key%"}, "screening_key_population"));
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"screening_close%"}, "screening_close_contact"));
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"latent%"}, "latent_infection"));
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"patient%"}, "patient"));
        rules.add(new Rule("t.`biz_type` LIKE ?", new Object[]{"referral%"}, "referral"));
        rules.add(new Rule("t.`op_module` = ?", new Object[]{"screening"}, "screening_school"));
        rules.add(new Rule("t.`op_module` = ?", new Object[]{"latent"}, "latent_infection"));
        rules.add(new Rule("t.`op_module` = ?", new Object[]{"patient"}, "patient"));
        rules.add(new Rule("t.`op_module` = ?", new Object[]{"referral"}, "referral_tracking"));

        for (Rule rule : rules) {
            Object[] params = new Object[rule.args().length + 1];
            params[0] = rule.parentTable();
            System.arraycopy(rule.args(), 0, params, 1, rule.args().length);
            int n = jdbc.update("""
                    UPDATE `operation_log` t
                    INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`biz_id`
                    SET t.`biz_id` = m.`new_id`
                    WHERE t.`biz_id` IS NOT NULL AND t.`biz_id` < 100000000000000000
                      AND %s
                    """.formatted(rule.condSql()), params);
            if (n > 0) {
                log.info("外键重写: operation_log.biz_id -> {} ({} 行)", rule.parentTable(), n);
            }
        }
    }

    private void remapPrimaryKeys() {
        for (String table : TABLES) {
            if (!tableExists(table)) {
                continue;
            }
            int n = jdbc.update("""
                    UPDATE `%s` t
                    INNER JOIN `_id_map` m ON m.`table_name` = ? AND m.`old_id` = t.`id`
                    SET t.`id` = m.`new_id`
                    """.formatted(table), table);
            log.info("主键重写: {} ({} 行)", table, n);
        }
    }

    private void dropAutoIncrement() {
        for (String table : TABLES) {
            if (!tableExists(table)) {
                continue;
            }
            try {
                jdbc.execute("ALTER TABLE `" + table + "` MODIFY COLUMN `id` BIGINT NOT NULL");
                log.info("去掉 AUTO_INCREMENT: {}", table);
            } catch (Exception e) {
                log.warn("去掉 AUTO_INCREMENT 失败 {}: {}", table, e.getMessage());
            }
        }
    }

    private void validate() {
        // 仍小于雪花量级的主键（可能未迁移）
        for (String table : TABLES) {
            if (!tableExists(table)) {
                continue;
            }
            Integer small = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM `" + table + "` WHERE `id` < 100000000000000000", Integer.class);
            if (small != null && small > 0) {
                log.warn("校验警告: {} 仍有 {} 行 id 过小（可能未迁移或种子手动 ID）", table, small);
            }
        }
        Integer mapCount = jdbc.queryForObject("SELECT COUNT(*) FROM `_id_map`", Integer.class);
        log.info("校验完成: _id_map 共 {} 条映射", mapCount);
    }
}
