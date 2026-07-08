package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ImportResult;

/**
 * 导入 Excel 时的人员基本信息校验：姓名 + 证件号（以身份证为标识）均必填。
 */
public final class ImportIdentitySupport {

    private ImportIdentitySupport() {
    }

    public static boolean hasValidBasicIdentity(String name, String idNumber) {
        return StrUtil.isNotBlank(name) && StrUtil.isNotBlank(idNumber);
    }

    public static boolean isMissingBasicIdentity(String name, String idNumber) {
        return !hasValidBasicIdentity(name, idNumber);
    }

    /**
     * 记录缺少基本信息的行；若尚未确认跳过无效行，则返回 true 表示应阻断本次导入。
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

    /** 存在无效身份行且用户尚未确认跳过时，标记结果并阻断导入。 */
    public static boolean shouldBlockImport(ImportResult result, boolean confirmSkipInvalid) {
        if (result.getInvalidIdentityCount() > 0 && !confirmSkipInvalid) {
            result.setRequireIdentityConfirm(true);
            return true;
        }
        return false;
    }
}
