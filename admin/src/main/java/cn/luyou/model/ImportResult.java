package cn.luyou.model;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入结果
 */
@Data
public class ImportResult {

    /** 成功导入的条数（新增 + 覆盖更新） */
    private int successCount;

    /** 新增条数 */
    private int insertCount;

    /** 覆盖更新条数 */
    private int updateCount;

    /** 跳过条数（重复且未选择覆盖） */
    private int skippedCount;

    /** 与系统已有记录重复的条数（预览或导入前检测） */
    private int duplicateCount;

    /** 缺少姓名的行数（无法作为有效人员记录） */
    private int invalidIdentityCount;

    /** 有姓名但未填写证件号（或填「无」）的行数；这类行仍会导入 */
    private int missingIdCount;

    /** 是否需要用户确认后跳过无效行再继续导入 */
    private boolean requireIdentityConfirm;

    /** 同一 Excel 文件内重复身份证涉及的额外行数（不含每组首行） */
    private int duplicateInFileCount;

    /** 文件内重复身份证摘要，用于导入前提醒 */
    private List<String> duplicateInFileSummaries = new ArrayList<>();

    /** 是否需要用户确认后继续导入（文件内存在重复身份证） */
    private boolean requireDuplicateInFileConfirm;

    /** 错误行描述列表，格式：第N行：原因 - 姓名 */
    private List<String> errors = new ArrayList<>();

    public static ImportResult of(int successCount) {
        ImportResult r = new ImportResult();
        r.successCount = successCount;
        return r;
    }

    public void addError(int rowNum, String name, String reason) {
        errors.add(String.format("第%d行：%s - %s", rowNum, reason, name == null ? "未知" : name));
    }

    public void addInvalidIdentityError(int rowNum, String name, String idNumber) {
        invalidIdentityCount++;
        String label = StrUtil.isNotBlank(name) ? name : (StrUtil.isNotBlank(idNumber) ? idNumber : "未知");
        errors.add(String.format("第%d行：无人员基本信息（缺少姓名） - %s", rowNum, label));
    }

    public void addMissingIdWarning(int rowNum, String name) {
        missingIdCount++;
        errors.add(String.format("第%d行：未填写身份证号（已导入） - %s",
                rowNum, StrUtil.isNotBlank(name) ? name : "未知"));
    }

    public void addDuplicateInFileWarning(int rowNum, String name, String idNumber, String reason) {
        String label = StrUtil.isNotBlank(name) ? name : (StrUtil.isNotBlank(idNumber) ? idNumber : "未知");
        errors.add(String.format("第%d行：%s - %s", rowNum, reason, label));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
