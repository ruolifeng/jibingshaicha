package cn.luyou.crypto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 需要加密存储的列。姓名、诊断结果仍明文，以便模糊查询和统计筛选。
 */
public final class SensitiveDataRegistry {

    public enum Kind {
        /** 证件号，并写入 id_number_fmt */
        ID,
        /** 手机号，并写入 phone_fmt */
        PHONE,
        /** 住址等长文本 */
        TEXT,
        /** 短信密钥 */
        SECRET,
        /** JSON 内的证件、电话、住址 */
        JSON
    }

    public record Column(String table, String column, String property, Kind kind, int varcharLength) {
    }

    public static final List<Column> COLUMNS = List.of(
            col("patient", "id_number", "idNumber", Kind.ID, 512),
            col("patient", "phone", "phone", Kind.PHONE, 512),
            col("patient", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("patient", "current_address", "currentAddress", Kind.TEXT, 1024),
            col("patient", "epidemic_data", "epidemicData", Kind.JSON, 0),

            col("latent_infection", "id_number", "idNumber", Kind.ID, 512),
            col("latent_infection", "phone", "phone", Kind.PHONE, 512),
            col("latent_infection", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("latent_infection", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("screening_school", "id_number", "idNumber", Kind.ID, 512),
            col("screening_school", "phone", "phone", Kind.PHONE, 512),
            col("screening_school", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("screening_school", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("screening_key_population", "id_number", "idNumber", Kind.ID, 512),
            col("screening_key_population", "phone", "phone", Kind.PHONE, 512),
            col("screening_key_population", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("screening_key_population", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("screening_close_contact", "id_number", "idNumber", Kind.ID, 512),
            col("screening_close_contact", "phone", "phone", Kind.PHONE, 512),
            col("screening_close_contact", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("screening_close_contact", "current_address", "currentAddress", Kind.TEXT, 1024),
            col("screening_close_contact", "source_patient_id_number", "sourcePatientIdNumber", Kind.ID, 512),
            col("screening_close_contact", "source_patient_phone", "sourcePatientPhone", Kind.PHONE, 512),

            col("close_contact_case", "id_number", "idNumber", Kind.ID, 512),
            col("close_contact_case", "phone", "phone", Kind.PHONE, 512),
            col("close_contact_case", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("close_contact_case", "current_address", "currentAddress", Kind.TEXT, 1024),
            col("close_contact_case", "source_patient_id_number", "sourcePatientIdNumber", Kind.ID, 512),
            col("close_contact_case", "source_patient_phone", "sourcePatientPhone", Kind.PHONE, 512),

            col("supervision_form", "id_number", "idNumber", Kind.ID, 512),
            col("supervision_form", "phone", "phone", Kind.PHONE, 512),
            col("supervision_form", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("supervision_form", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("notice", "id_number", "idNumber", Kind.ID, 512),
            col("notice", "phone", "phone", Kind.PHONE, 512),
            col("notice", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("notice", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("referral_tracking", "id_number", "idNumber", Kind.ID, 512),
            col("referral_tracking", "phone", "phone", Kind.PHONE, 512),
            col("referral_tracking", "household_address", "householdAddress", Kind.TEXT, 1024),
            col("referral_tracking", "current_address", "currentAddress", Kind.TEXT, 1024),

            col("epidemic_import", "id_number", "idNumber", Kind.ID, 512),
            col("epidemic_import", "phone", "phone", Kind.PHONE, 512),
            col("epidemic_import", "current_address", "currentAddress", Kind.TEXT, 1024),
            col("epidemic_report", "raw_data", "rawData", Kind.JSON, 0),

            col("user", "phone", "phone", Kind.PHONE, 512),
            col("sys_sms_config", "secret_id", "secretId", Kind.SECRET, 512),
            col("sys_sms_config", "secret_key", "secretKey", Kind.SECRET, 512)
    );

    /** 列表「格式问题」筛选使用的表，额外维护 *_fmt 标记。 */
    public static final List<String> FORMAT_FLAG_TABLES = List.of(
            "patient", "latent_infection", "screening_school", "screening_key_population",
            "screening_close_contact", "close_contact_case", "supervision_form"
    );

    private static final Map<String, List<Column>> BY_TABLE = COLUMNS.stream()
            .collect(Collectors.groupingBy(Column::table));

    private SensitiveDataRegistry() {
    }

    public static List<Column> columnsOf(String table) {
        return BY_TABLE.getOrDefault(table, List.of());
    }

    private static Column col(String table, String column, String property, Kind kind, int varcharLength) {
        return new Column(table, column, property, kind, varcharLength);
    }
}
