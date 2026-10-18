package cn.luyou.crypto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldEncryptorTest {

    private final FieldEncryptor encryptor = new FieldEncryptor("unit-test-key");

    @Test
    void roundTripAndDeterministic() {
        String cipher1 = encryptor.encrypt("510101199001011234");
        String cipher2 = encryptor.encrypt("510101199001011234");
        assertTrue(cipher1.startsWith(FieldEncryptor.PREFIX));
        assertEquals(cipher1, cipher2);
        assertEquals("510101199001011234", encryptor.decrypt(cipher1));
        assertEquals(encryptor.encrypt("51010119900101123X"), encryptor.encrypt("51010119900101123x"));
        assertEquals("51010119900101123X", encryptor.decrypt(encryptor.encrypt("  51010119900101123x  ")));
        assertFalse(cipher1.equals(encryptor.encrypt("510101199001011235")));
    }

    @Test
    void keepsPlaceholderAndBlank() {
        assertEquals("无", encryptor.encrypt("无"));
        assertNull(encryptor.encrypt(null));
        assertEquals("", encryptor.encrypt(""));
        assertNull(FieldEncryptor.idNumberFlag("无"));
        assertEquals(1, FieldEncryptor.idNumberFlag("510101199001011234"));
        assertEquals(0, FieldEncryptor.phoneFlag("12345"));
        assertEquals(1, FieldEncryptor.phoneFlag("13800138000"));
    }

    @Test
    void encryptsOnlySensitiveJsonKeys() {
        String json = "{\"证件号\":\"510101199001011234\",\"登记日期\":\"2024-01-02\",\"联系电话\":\"13800138000\"}";
        String encrypted = encryptor.encryptJson(json);
        assertTrue(encrypted.contains(FieldEncryptor.PREFIX));
        assertTrue(encrypted.contains("2024-01-02"));
        assertFalse(encrypted.contains("13800138000"));
        String decrypted = encryptor.decryptJson(encrypted);
        assertTrue(decrypted.contains("13800138000"));
        assertTrue(decrypted.contains("510101199001011234"));
    }

    @Test
    void rewritesLikeToEncryptedEquality() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("MPGENVAL1", "%510101199001011234%");
        Map<String, Object> ew = new HashMap<>();
        ew.put("paramNameValuePairs", pairs);
        Map<String, Object> param = new HashMap<>();
        param.put("ew", ew);

        String sql = "SELECT id FROM patient WHERE id_number LIKE #{ew.paramNameValuePairs.MPGENVAL1}";
        String rewritten = SensitiveSqlRewriter.rewrite(sql, new MapAccess(param), encryptor);
        assertEquals("SELECT id FROM patient WHERE id_number = #{ew.paramNameValuePairs.MPGENVAL1}", rewritten);
        assertEquals(encryptor.encrypt("510101199001011234"), pairs.get("MPGENVAL1"));
    }

    @Test
    void updateSetsFormatFlagOnlyOnBusinessTables() {
        Map<String, Object> pairs = new HashMap<>();
        pairs.put("MPGENVAL1", "13800138000");
        pairs.put("MPGENVAL2", "1");
        Map<String, Object> ew = new HashMap<>();
        ew.put("paramNameValuePairs", pairs);
        Map<String, Object> param = new HashMap<>();
        param.put("ew", ew);

        String sql = "UPDATE patient SET phone=#{ew.paramNameValuePairs.MPGENVAL1} WHERE id=#{ew.paramNameValuePairs.MPGENVAL2}";
        String rewritten = SensitiveSqlRewriter.rewrite(sql, new MapAccess(param), encryptor);
        assertEquals("UPDATE patient SET phone=#{ew.paramNameValuePairs.MPGENVAL1}, phone_fmt=1 WHERE id=#{ew.paramNameValuePairs.MPGENVAL2}",
                rewritten);
        assertEquals(encryptor.encrypt("13800138000"), pairs.get("MPGENVAL1"));
        assertEquals("1", pairs.get("MPGENVAL2"));

        pairs.put("MPGENVAL1", "13800138000");
        String userSql = "UPDATE user SET phone=#{ew.paramNameValuePairs.MPGENVAL1} WHERE id=#{ew.paramNameValuePairs.MPGENVAL2}";
        String userRewritten = SensitiveSqlRewriter.rewrite(userSql, new MapAccess(param), encryptor);
        assertFalse(userRewritten.contains("phone_fmt"));
        assertTrue(userRewritten.startsWith("UPDATE user SET phone="));
    }

    private static final class MapAccess implements SensitiveSqlRewriter.ValueAccess {
        private final Map<String, Object> root;

        private MapAccess(Map<String, Object> root) {
            this.root = root;
        }

        @Override
        public Object get(String path) {
            Object current = root;
            for (String part : path.split("\\.")) {
                if (!(current instanceof Map<?, ?> map)) {
                    return null;
                }
                current = map.get(part);
            }
            return current;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void set(String path, Object value) {
            Object current = root;
            String[] parts = path.split("\\.");
            for (int i = 0; i < parts.length - 1; i++) {
                current = ((Map<String, Object>) current).get(parts[i]);
            }
            ((Map<String, Object>) current).put(parts[parts.length - 1], value);
        }
    }
}
