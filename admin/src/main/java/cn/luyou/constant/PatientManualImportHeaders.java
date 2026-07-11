package cn.luyou.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 在管患者手动批量导入 Excel 表头（与新增表单字段一致）。
 */
public final class PatientManualImportHeaders {

    private PatientManualImportHeaders() {
    }

    public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
            "数据来源",
            "姓名",
            "性别",
            "出生日期",
            "年龄",
            "证件类型",
            "证件号",
            "民族",
            "联系电话",
            "户籍地址",
            "现住址",
            "登记号",
            "联系人姓名",
            "联系人监护人与本人关系",
            "联系人监护人电话号码",
            "诊断结果",
            "合并症",
            "治疗分类",
            "服药管理单位",
            "备注",
            "首次治疗方案",
            "药敏结果：利福平（R）",
            "药敏结果：异烟肼（H）",
            "培养结果"
    ));
}
