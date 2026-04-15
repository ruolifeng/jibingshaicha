package cn.luyou.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入结果
 */
@Data
public class ImportResult {

    /** 成功导入的条数 */
    private int successCount;

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
