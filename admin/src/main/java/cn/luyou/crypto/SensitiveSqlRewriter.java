package cn.luyou.crypto;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把敏感列上的查询参数改写成密文。模糊条件改为等值，因此证件号、电话、住址需输入完整内容才能命中。
 */
public final class SensitiveSqlRewriter {

    private static final String COLUMN = "(?:`)?(?:source_patient_id_number|source_patient_phone|"
            + "household_address|current_address|id_number|secret_key|secret_id|phone)(?:`)?";

    private static final Pattern LIKE = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s+like\\s+#\\{([^}]+)}");
    private static final Pattern COMPARE = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s*(=|<>|!=)\\s*#\\{([^}]+)}");
    private static final Pattern IN_LIST = Pattern.compile(
            "(?i)(?<![\\w])(" + COLUMN + ")(?![\\w])\\s+in\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
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
        if (sql == null || sql.isBlank() || access == null || encryptor == null) {
            return sql;
        }
        Map<String, Integer> setFlags = new HashMap<>();
        String rewritten = replaceLike(sql, access, encryptor, setFlags);
        encryptMatches(COMPARE, rewritten, access, encryptor, false, setFlags);
        Matcher inMatcher = IN_LIST.matcher(rewritten);
        while (inMatcher.find()) {
            if (inMatcher.start() < whereIndex(rewritten)) {
                continue;
            }
            Matcher placeholders = PLACEHOLDER.matcher(inMatcher.group(2));
            while (placeholders.find()) {
                encryptParameter(access, encryptor, placeholders.group(1), false);
            }
        }
        return appendFormatFlags(rewritten, setFlags);
    }

    private static String replaceLike(String sql, ValueAccess access, FieldEncryptor encryptor,
                                      Map<String, Integer> setFlags) {
        Matcher matcher = LIKE.matcher(sql);
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

    private static void encryptMatches(Pattern pattern, String sql, ValueAccess access,
                                       FieldEncryptor encryptor, boolean like,
                                       Map<String, Integer> setFlags) {
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String plain = encryptParameter(access, encryptor, matcher.group(3), like);
            if (plain == null) {
                continue;
            }
            rememberSetFlag(sql, matcher.start(), matcher.group(1), plain, setFlags);
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
