package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 列表「格式问题」筛选：证件号 / 手机号格式异常（与导入校验规则一致）。
 * <ul>
 *   <li>证件号：有值且不符合 18 位 {@code \d{17}[\dXx]}</li>
 *   <li>手机号：有值且不符合 {@code ^1[3-9]\d{9}$}</li>
 *   <li>空值不算格式异常</li>
 * </ul>
 * 查询参数：{@code formatIssue=idNumber|phone|any}
 */
public final class IdentityFormatFilterSupport {

    public static final String ISSUE_ID_NUMBER = "idNumber";
    public static final String ISSUE_PHONE = "phone";
    public static final String ISSUE_ANY = "any";

    /** 与导入 isValidIdCard 一致（MySQL REGEXP） */
    private static final String ID_NUMBER_VALID_REGEXP = "^[0-9]{17}[0-9Xx]$";
    /** 与导入 isValidPhone 一致 */
    private static final String PHONE_VALID_REGEXP = "^1[3-9][0-9]{9}$";

    private IdentityFormatFilterSupport() {
    }

    public static boolean isSupported(String formatIssue) {
        if (StrUtil.isBlank(formatIssue)) {
            return false;
        }
        String v = formatIssue.trim();
        return ISSUE_ID_NUMBER.equals(v) || ISSUE_PHONE.equals(v) || ISSUE_ANY.equals(v);
    }

    /**
     * 按 formatIssue 追加 SQL 条件。
     *
     * @param idNumberColumn 证件号列名，如 id_number
     * @param phoneColumn    手机号列名，如 phone
     */
    public static <T> void apply(LambdaQueryWrapper<T> wrapper,
                                 String formatIssue,
                                 String idNumberColumn,
                                 String phoneColumn) {
        if (wrapper == null || !isSupported(formatIssue)) {
            return;
        }
        String issue = formatIssue.trim();
        String idSql = "(" + idNumberColumn + " IS NOT NULL AND TRIM(" + idNumberColumn + ") <> ''"
                + " AND " + idNumberColumn + " NOT REGEXP {0})";
        String phoneSql = "(" + phoneColumn + " IS NOT NULL AND TRIM(" + phoneColumn + ") <> ''"
                + " AND " + phoneColumn + " NOT REGEXP {0})";
        switch (issue) {
            case ISSUE_ID_NUMBER -> wrapper.apply(idSql, ID_NUMBER_VALID_REGEXP);
            case ISSUE_PHONE -> wrapper.apply(phoneSql, PHONE_VALID_REGEXP);
            case ISSUE_ANY -> wrapper.and(w -> w
                    .apply(idSql, ID_NUMBER_VALID_REGEXP)
                    .or()
                    .apply(phoneSql, PHONE_VALID_REGEXP));
            default -> {
            }
        }
    }
}
