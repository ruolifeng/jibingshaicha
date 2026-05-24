package cn.luyou.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 潜伏感染者手动批量导入 Excel 表头（与新增表单字段一致）。
 */
public final class LatentImportHeaders {

    private LatentImportHeaders() {
    }

    public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
            "数据来源",
            "姓名",
            "性别",
            "年龄",
            "证件号",
            "联系电话",
            "感染筛查结果",
            "是否胸片检查",
            "胸片检查日期",
            "胸片检查结果",
            "首次诊断",
            "追踪备注"
    ));
}
