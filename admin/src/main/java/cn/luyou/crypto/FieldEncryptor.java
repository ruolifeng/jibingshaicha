package cn.luyou.crypto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感字段 AES-256-GCM。相同明文得到相同密文，便于等值查询；密文以 {@code encv1:} 开头。
 */
public final class FieldEncryptor {

    public static final String PREFIX = "encv1:";

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptor.class);
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final Pattern ID_CARD = Pattern.compile("^[0-9]{17}[0-9Xx]$");
    private static final Pattern MOBILE = Pattern.compile("^1[3-9][0-9]{9}$");
    private static final Set<String> PLAIN_PLACEHOLDERS = Set.of(
            "无", "无证件号", "无身份证", "无身份证号", "无电话", "暂无", "-", "/", "——", "—"
    );

    private final byte[] aesKey;
    private final byte[] macKey;

    public FieldEncryptor(String secret) {
        if (StrUtil.isBlank(secret)) {
            throw new IllegalArgumentException("数据加密密钥不能为空");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            this.aesKey = sha.digest((secret.trim() + ":aes").getBytes(StandardCharsets.UTF_8));
            this.macKey = sha.digest((secret.trim() + ":mac").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("初始化数据加密密钥失败", e);
        }
    }

    public static FieldEncryptor fromConfiguredKey(String configured, Path keyFile) {
        String key = configured == null ? "" : configured.trim();
        try {
            if (key.isEmpty()) {
                if (keyFile != null && Files.exists(keyFile)) {
                    key = Files.readString(keyFile).trim();
                    log.info("使用数据加密密钥文件: {}", keyFile.toAbsolutePath());
                } else if (keyFile != null) {
                    byte[] raw = new byte[32];
                    new SecureRandom().nextBytes(raw);
                    key = Base64.getEncoder().encodeToString(raw);
                    Path parent = keyFile.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(keyFile, key);
                    log.warn("未配置 DATA_ENCRYPT_KEY，已生成密钥文件 {}。请立即备份，丢失后已加密数据无法恢复。",
                            keyFile.toAbsolutePath());
                }
            } else {
                log.info("使用环境变量 DATA_ENCRYPT_KEY 作为数据加密密钥");
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取或生成数据加密密钥失败: " + keyFile, e);
        }
        return new FieldEncryptor(key);
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank() || isEncrypted(plain) || PLAIN_PLACEHOLDERS.contains(plain.trim())) {
            return plain;
        }
        String normalized = canonicalize(plain);
        try {
            byte[] plainBytes = normalized.getBytes(StandardCharsets.UTF_8);
            byte[] iv = deriveIv(plainBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] packed = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (packed.length <= IV_LENGTH) {
                return stored;
            }
            byte[] iv = Arrays.copyOfRange(packed, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(packed, IV_LENGTH, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("敏感字段解密失败，已保留原值");
            return stored;
        }
    }

    /**
     * 只加密 JSON 中的证件、电话、住址类字段，登记日期、诊断结果等筛选字段保持明文。
     */
    public String encryptJson(String json) {
        return transformJson(json, true);
    }

    public String decryptJson(String json) {
        return transformJson(json, false);
    }

    /** 1 格式正确，0 格式异常，null 表示空值或「无」等占位。 */
    public static Integer idNumberFlag(String plain) {
        if (isBlankOrPlaceholder(plain)) {
            return null;
        }
        return ID_CARD.matcher(plain.trim()).matches() ? 1 : 0;
    }

    public static Integer phoneFlag(String plain) {
        if (isBlankOrPlaceholder(plain)) {
            return null;
        }
        return MOBILE.matcher(plain.trim()).matches() ? 1 : 0;
    }

    /**
     * 合法证件号统一成大写 X 并去掉首尾空白，避免库排序规则忽略大小写、加密后却匹配失败。
     * 合法手机号去掉首尾空白。住址等其它内容保持原样。
     */
    static String canonicalize(String plain) {
        String trimmed = plain.trim();
        if (ID_CARD.matcher(trimmed).matches()) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        if (MOBILE.matcher(trimmed).matches()) {
            return trimmed;
        }
        return plain;
    }

    public static boolean isBlankOrPlaceholder(String plain) {
        return plain == null || plain.isBlank() || PLAIN_PLACEHOLDERS.contains(plain.trim());
    }

    private String transformJson(String json, boolean encrypt) {
        if (StrUtil.isBlank(json) || isEncrypted(json)) {
            return encrypt ? encrypt(json) : decrypt(json);
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return json;
        }
        try {
            if (trimmed.startsWith("[")) {
                JSONArray array = JSONUtil.parseArray(json);
                boolean[] changed = {false};
                transformNode(array, encrypt, changed);
                return changed[0] ? array.toString() : json;
            }
            JSONObject object = JSONUtil.parseObj(json);
            boolean[] changed = {false};
            transformNode(object, encrypt, changed);
            return changed[0] ? object.toString() : json;
        } catch (Exception e) {
            log.warn("敏感 JSON 字段处理失败，已跳过: {}", e.getMessage());
            return json;
        }
    }

    private void transformNode(Object node, boolean encrypt, boolean[] changed) {
        if (node instanceof JSONObject object) {
            for (String key : new java.util.ArrayList<>(object.keySet())) {
                Object value = object.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    transformNode(value, encrypt, changed);
                    continue;
                }
                if (!(value instanceof CharSequence)) {
                    continue;
                }
                String text = value.toString();
                if (encrypt) {
                    if (!sensitiveJsonKey(key)) {
                        continue;
                    }
                    String next = encrypt(text);
                    if (!text.equals(next)) {
                        object.set(key, next);
                        changed[0] = true;
                    }
                } else if (isEncrypted(text)) {
                    object.set(key, decrypt(text));
                    changed[0] = true;
                }
            }
            return;
        }
        if (node instanceof JSONArray array) {
            for (Object item : array) {
                transformNode(item, encrypt, changed);
            }
        }
    }

    static boolean sensitiveJsonKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        if (key.contains("登记") || key.contains("诊断") || key.contains("病原")
                || key.contains("管理单位") || key.contains("人群") || key.contains("民族")
                || key.contains("性别") || key.contains("日期") || key.contains("时间")
                || key.contains("关系")) {
            return false;
        }
        return key.contains("身份证") || key.contains("证件") || key.contains("电话")
                || key.contains("手机") || key.contains("住址") || key.contains("地址") || key.contains("户籍");
    }

    private byte[] deriveIv(byte[] plain) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(macKey, "HmacSHA256"));
        return Arrays.copyOf(mac.doFinal(plain), IV_LENGTH);
    }
}
