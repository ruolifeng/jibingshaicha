package cn.luyou.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 潜伏感染者手动批量导入 Excel 表头（与新增表单字段一致）。
 */
public final class LatentImportHeaders {

    private LatentImportHeaders() {
    }

    public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
            "数据来源",
            "人群分类",
            "姓名",
            "性别",
            "年龄",
            "证件号",
            "联系电话",
            "联系电话与联系人关系",
            "户籍地址",
            "现住地址",
            "感染筛查日期",
            "感染筛查方法",
            "感染筛查结果",
            "是否胸片检查",
            "胸片检查日期",
            "胸片检查结果",
            "首次诊断",
            "追踪情况",
            "备注"
    ));

    /** 兼容旧模板表头别名 → 标准列名 */
    public static final Map<String, String> HEADER_ALIASES = Map.of(
            "感染筛查时间", "感染筛查日期",
            "筛查方法", "感染筛查方法",
            "感染检测方法", "感染筛查方法",
            "结果判定", "感染筛查结果",
            "现住址", "现住地址",
            "追踪备注", "追踪情况"
    );
}
