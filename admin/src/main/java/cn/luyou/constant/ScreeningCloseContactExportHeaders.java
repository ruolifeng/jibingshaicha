package cn.luyou.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 密接筛查列表导出列（不含原患者身份证号）。
 */
public final class ScreeningCloseContactExportHeaders {

    private ScreeningCloseContactExportHeaders() {
    }

    public static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "市/州",
            "区/县",
            "原患者姓名",
            "原患者病案号",
            "接触者姓名",
            "接触者身份证号",
            "年龄",
            "接触者电话",
            "联系电话与接触者关系",
            "接触类型",
            "接触场所",
            "密接登记日期",
            "首次筛查日期",
            "感染检测方法",
            "感染检测结果",
            "影像方法",
            "影像结果",
            "痰检方法",
            "痰检结果",
            "最终筛查结果",
            "是否开展预防治疗",
            "预防性治疗方案",
            "是否完成治疗",
            "6月随访结果",
            "12月随访结果",
            "24月随访结果",
            "流程状态",
            "备注"
    ));

    public static List<List<String>> asEasyExcelHead() {
        return COLUMNS.stream().map(List::of).toList();
    }
}
