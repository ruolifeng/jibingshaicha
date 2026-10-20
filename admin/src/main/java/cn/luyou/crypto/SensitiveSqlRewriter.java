package cn.luyou.crypto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把敏感列上的查询参数改写成密文。模糊条件改为等值，因此证件号、电话、住址需输入完整内容才能命中。
 * <p>
 * MyBatis {@code BoundSql} 中占位符已是 {@code ?}，需配合 parameterMappings 属性路径加密；
 * 同时兼容测试或未解析 SQL 中的 {@code #{...}} 写法。
 */
public final class SensitiveSqlRewriter {

    private static final String COLUMN = "(?:`)?(?:source_patient_id_number|source_patient_phone|"
            + "household_address|current_address|id_number|secret_key|secret_id|phone)(?:`)?";

    private static final Pattern LIKE_NAMED = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s+like\\s+#\\{([^}]+)}");
    private static final Pattern COMPARE_NAMED = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s*(=|<>|!=)\\s*#\\{([^}]+)}");
    private static final Pattern LIKE_JDBC = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s+like\\s+\\?");
    private static final Pattern COMPARE_JDBC = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s*(=|<>|!=)\\s*\\?");
    private static final Pattern IN_LIST = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s+in\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern NAMED_PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
    private static final Pattern JDBC_PLACEHOLDER = Pattern.compile("\\?");
    private static final Pattern UPDATE_TABLE = Pattern.compile("(?i)^\\s*update\\s+`?([a-zA-Z0-9_]+)`?");
    private static final Pattern WHERE_KEYWORD = Pattern.compile("(?i)\\swhere\\s");
    private static final Set<String> FORMAT_FLAG_TABLES = Set.copyOf(SensitiveDataRegistry.FORMAT_FLAG_TABLES);

    private SensitiveSqlRewriter() {
    }

    public interface ValueAccess {
        Object get(String path);

        void set(String path, Object value);
    }

    public static String rewrite(String sql, ValueAccess access, FieldEncryptor encryptor) {
        return rewrite(sql, Collections.emptyList(), access, encryptor);
    }

    /**
     * @param jdbcProperties BoundSql.parameterMappings 中与 {@code ?} 一一对应的属性路径；可为空
     */
    public static String rewrite(String sql, List<String> jdbcProperties, ValueAccess access,
                                 FieldEncryptor encryptor) {
        if (sql == null || sql.isBlank() || access == null || encryptor == null) {
            return sql;
        }
        List<String> properties = jdbcProperties == null ? Collections.emptyList() : jdbcProperties;
        Map<String, Integer> setFlags = new HashMap<>();
        String rewritten = replaceLikeNamed(sql, access, encryptor, setFlags);
        rewritten = replaceLikeJdbc(rewritten, properties, access, encryptor, setFlags);
        encryptNamedMatches(COMPARE_NAMED, rewritten, access, encryptor, setFlags);
        encryptJdbcMatches(COMPARE_JDBC, rewritten, properties, access, encryptor, false, setFlags);
        encryptInList(rewritten, properties, access, encryptor);
        return appendFormatFlags(rewritten, setFlags);
    }

    private static String replaceLikeNamed(String sql, ValueAccess access, FieldEncryptor encryptor,
                                           Map<String, Integer> setFlags) {
        Matcher matcher = LIKE_NAMED.matcher(sql);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String plain = encryptParameter(access, encryptor, matcher.group(2), true);
            if (plain == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            rememberSetFlag(sql, matcher.start(), matcher.group(1), plain, setFlags);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + " = #{" + matcher.group(2) + "}"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String replaceLikeJdbc(String sql, List<String> jdbcProperties, ValueAccess access,
                                          FieldEncryptor encryptor, Map<String, Integer> setFlags) {
        if (jdbcProperties.isEmpty() && !sql.contains("?")) {
            return sql;
        }
        Matcher matcher = LIKE_JDBC.matcher(sql);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int questionIndex = countJdbcPlaceholders(sql, matcher.end() - 1);
            String property = propertyAt(jdbcProperties, questionIndex);
            String plain = property == null ? null : encryptParameter(access, encryptor, property, true);
            if (plain == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            rememberSetFlag(sql, matcher.start(), matcher.group(1), plain, setFlags);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + " = ?"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void encryptNamedMatches(Pattern pattern, String sql, ValueAccess access,
                                            FieldEncryptor encryptor, Map<String, Integer> setFlags) {
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String plain = encryptParameter(access, encryptor, matcher.group(3), false);
            if (plain == null) {
                continue;
            }
            rememberSetFlag(sql, matcher.start(), matcher.group(1), plain, setFlags);
        }
    }

    private static void encryptJdbcMatches(Pattern pattern, String sql, List<String> jdbcProperties,
                                           ValueAccess access, FieldEncryptor encryptor, boolean like,
                                           Map<String, Integer> setFlags) {
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            int questionIndex = countJdbcPlaceholders(sql, matcher.end() - 1);
            String property = propertyAt(jdbcProperties, questionIndex);
            if (property == null) {
                continue;
            }
            String plain = encryptParameter(access, encryptor, property, like);
            if (plain == null) {
                continue;
            }
            rememberSetFlag(sql, matcher.start(), matcher.group(1), plain, setFlags);
        }
    }

    private static void encryptInList(String sql, List<String> jdbcProperties, ValueAccess access,
                                      FieldEncryptor encryptor) {
        Matcher inMatcher = IN_LIST.matcher(sql);
        while (inMatcher.find()) {
            if (inMatcher.start() < whereIndex(sql)) {
                continue;
            }
            String list = inMatcher.group(2);
            Matcher named = NAMED_PLACEHOLDER.matcher(list);
            while (named.find()) {
                encryptParameter(access, encryptor, named.group(1), false);
            }
            if (jdbcProperties.isEmpty()) {
                continue;
            }
            int baseIndex = countJdbcPlaceholders(sql, inMatcher.start());
            Matcher jdbc = JDBC_PLACEHOLDER.matcher(list);
            int offset = 0;
            while (jdbc.find()) {
                String property = propertyAt(jdbcProperties, baseIndex + offset);
                if (property != null) {
                    encryptParameter(access, encryptor, property, false);
                }
                offset++;
            }
        }
    }

    /**
     * @return 加密前的明文；参数不是字符串或路径不存在时返回 null，调用方必须保留原 SQL。
     */
    private static String encryptParameter(ValueAccess access, FieldEncryptor encryptor, String token, boolean like) {
        String path = propertyPath(token);
        if (path.isEmpty()) {
            return null;
        }
        try {
            Object current = access.get(path);
            if (!(current instanceof String text)) {
                return null;
            }
            String plain = like ? unwrapLike(text) : text;
            if (encryptor.isEncrypted(plain)) {
                if (!plain.equals(text)) {
                    access.set(path, plain);
                }
                return plain;
            }
            String encrypted = encryptor.encrypt(plain);
            if (!text.equals(encrypted)) {
                access.set(path, encrypted);
            }
            return plain;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void rememberSetFlag(String sql, int matchStart, String columnExpr, String plain,
                                        Map<String, Integer> setFlags) {
        if (plain == null || plain.startsWith(FieldEncryptor.PREFIX) || matchStart >= whereIndex(sql)) {
            return;
        }
        String column = columnExpr.replace("`", "").toLowerCase(Locale.ROOT);
        if (!"id_number".equals(column) && !"phone".equals(column)) {
            return;
        }
        setFlags.put(column, "phone".equals(column)
                ? FieldEncryptor.phoneFlag(plain)
                : FieldEncryptor.idNumberFlag(plain));
    }

    private static String appendFormatFlags(String sql, Map<String, Integer> setFlags) {
        if (setFlags.isEmpty()) {
            return sql;
        }
        Matcher tableMatcher = UPDATE_TABLE.matcher(sql);
        if (!tableMatcher.find() || !FORMAT_FLAG_TABLES.contains(tableMatcher.group(1).toLowerCase(Locale.ROOT))) {
            return sql;
        }
        int where = whereIndex(sql);
        String head = sql.substring(0, where);
        String tail = sql.substring(where);
        String lowerHead = head.toLowerCase(Locale.ROOT);
        if (setFlags.containsKey("id_number") && !lowerHead.contains("id_number_fmt")) {
            head = head.stripTrailing() + ", id_number_fmt=" + sqlLiteral(setFlags.get("id_number"));
            lowerHead = head.toLowerCase(Locale.ROOT);
        }
        if (setFlags.containsKey("phone") && !lowerHead.contains("phone_fmt")) {
            head = head.stripTrailing() + ", phone_fmt=" + sqlLiteral(setFlags.get("phone"));
        }
        return head + tail;
    }

    private static String sqlLiteral(Integer flag) {
        return flag == null ? "NULL" : Integer.toString(flag);
    }

    private static int whereIndex(String sql) {
        Matcher matcher = WHERE_KEYWORD.matcher(sql);
        return matcher.find() ? matcher.start() : sql.length();
    }

    /** 统计 sql[0, endExclusive) 内的 {@code ?} 个数，用于定位当前占位符在 parameterMappings 中的下标。 */
    static int countJdbcPlaceholders(String sql, int endExclusive) {
        if (sql == null || endExclusive <= 0) {
            return 0;
        }
        int end = Math.min(endExclusive, sql.length());
        int count = 0;
        for (int i = 0; i < end; i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    static String propertyAt(List<String> jdbcProperties, int index) {
        if (jdbcProperties == null || index < 0 || index >= jdbcProperties.size()) {
            return null;
        }
        return jdbcProperties.get(index);
    }

    static String propertyPath(String token) {
        if (token == null) {
            return "";
        }
        String path = token.trim();
        int comma = path.indexOf(',');
        if (comma >= 0) {
            path = path.substring(0, comma).trim();
        }
        return path;
    }

    static String unwrapLike(String value) {
        String text = value;
        if (text.startsWith("%")) {
            text = text.substring(1);
        }
        if (text.endsWith("%")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
