package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ImportResult;

/**
 * 导入 Excel 时的人员基本信息校验：姓名必填；证件号可空（可填「无」等占位）。
 */
public final class ImportIdentitySupport {

    private ImportIdentitySupport() {
    }

    /** 姓名必填即可作为有效人员记录；证件号允许为空或占位「无」。 */
    public static boolean hasValidBasicIdentity(String name, String idNumber) {
        return StrUtil.isNotBlank(name);
    }

    public static boolean isMissingBasicIdentity(String name, String idNumber) {
        return !hasValidBasicIdentity(name, idNumber);
    }

    /** 证件号为空，或填写了「无」等占位符。 */
    public static boolean isBlankOrPlaceholder(String idNumber) {
        if (StrUtil.isBlank(idNumber)) {
            return true;
        }
        String v = idNumber.trim();
        return "无".equals(v)
                || "无证件号".equals(v)
                || "无身份证".equals(v)
                || "无身份证号".equals(v)
                || "-".equals(v)
                || "/".equals(v)
                || "——".equals(v)
                || "—".equals(v);
    }

    /** 规范化证件号：去空白、Excel 文本前缀引号；占位符转为空字符串，便于入库与去重。 */
    public static String normalizeIdNumber(String idNumber) {
        if (isBlankOrPlaceholder(idNumber)) {
            return "";
        }
        return stripExcelTextMarker(idNumber.trim());
    }

    /**
     * 去掉大疫情网导出常见的文本前缀（如 {@code '5103...}、{@code "5103..."}）。
     */
    public static String stripExcelTextMarker(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        while (text.startsWith("'") || text.startsWith("\"") || text.startsWith("`")) {
            text = text.substring(1).trim();
        }
        while (text.endsWith("'") || text.endsWith("\"") || text.endsWith("`")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return text;
    }

    /**
     * 记录缺少姓名的行；若尚未确认跳过无效行，则返回 true 表示应阻断本次导入。
     */
    public static boolean registerInvalidIdentity(ImportResult result, int rowNum,
                                                  String name, String idNumber,
                                                  boolean confirmSkipInvalid) {
        if (hasValidBasicIdentity(name, idNumber)) {
            return false;
        }
        result.addInvalidIdentityError(rowNum, name, idNumber);
        return !confirmSkipInvalid;
    }

    /** 有姓名但无证件号时记入提醒（不阻断、不跳过，仍可导入）。 */
    public static void registerMissingIdWarning(ImportResult result, int rowNum, String name) {
        if (result == null) {
            return;
        }
        result.addMissingIdWarning(rowNum, name);
    }

    /**
     * 规范化证件号；若原值为空/占位则记入「未填写身份证」提醒。
     *
     * @return 规范化后的证件号（占位为空串）
     */
    public static String normalizeAndWarnMissingId(ImportResult result, int rowNum, String name, String idNumber) {
        boolean missing = isBlankOrPlaceholder(idNumber);
        String normalized = normalizeIdNumber(idNumber);
        if (missing && StrUtil.isNotBlank(name)) {
            registerMissingIdWarning(result, rowNum, name);
        }
        return normalized;
    }

    /** 存在无效身份行（缺姓名）且用户尚未确认跳过时，标记结果并阻断导入。 */
    public static boolean shouldBlockImport(ImportResult result, boolean confirmSkipInvalid) {
        if (result.getInvalidIdentityCount() > 0 && !confirmSkipInvalid) {
            result.setRequireIdentityConfirm(true);
            return true;
        }
        return false;
    }
}
