package cn.luyou.model;

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

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
