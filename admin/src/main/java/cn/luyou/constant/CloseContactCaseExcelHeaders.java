package cn.luyou.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 密接个案表 / 密接筛查 72 列 Excel 表头（官方模板，已移除原患者身份证号列）。
 * <p>导入模板 72 列；导出在末尾追加「录入用户」共 73 列（官方列序不变，避免再导入列错位）。
 */
public final class CloseContactCaseExcelHeaders {

    private CloseContactCaseExcelHeaders() {
    }

    public static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "市/州（**市或**州）",
            "区/县（**区/县/市）",
            "患者姓名",
            "传报卡号",
            "病原学结果",
            "患者电话",
            "填表日期",
            "密切接触者登记日期（填写yyyy/mm/dd格式）",
            "报表填报季度",
            "计算登记日期到当前日期的时间间隔，提示随访期限",
            "接触者姓名",
            "身份证号",
            "年龄（岁）",
            "年龄组",
            "接触者电话",
            "接触类型",
            "接触场所",
            "首次筛查日期",
            "结核症状1",
            "结核症状2（自行补充）",
            "感染检测日期",
            "感染检测方法",
            "结果判定",
            "影像检查日期（填写yyyy/mm/dd格式）",
            "影像方法",
            "影像结果",
            "痰检留标日期（填写yyyy/mm/dd格式）",
            "痰检方法",
            "痰检结果",
            "最终筛查结果",
            "有无禁忌症",
            "不接受预防性治疗的原因",
            "备注：其他原因和具体的禁忌症（自行填写）",
            "是否开展预防治疗",
            "预防性治疗方案",
            "其他方案，请备注",
            "是否完成治疗",
            "若未完成预防性治疗请选择原因",
            "6月随访日期",
            "6月随访-症状筛查日期",
            "6月随访-症状1",
            "6月随访-症状2（自行填写）",
            "6月随访-影像检查日期",
            "6月随访-影像检查方法",
            "6月随访-影像结果",
            "6月随访-留标本时间",
            "6月随访-病原学检查方法",
            "6月随访-病原学检查结果",
            "6月随访-筛查结果",
            "12月随访日期",
            "12月随访-症状筛查日期",
            "12月随访-症状",
            "12月随访-症状2（自行填写）",
            "12月随访-影像检查日期",
            "12月随访-影像检查方法",
            "12月随访-影像结果",
            "12月随访-留标本时间",
            "12月随访-病原学检查方法",
            "12月随访-病原学检查结果",
            "12月随访-筛查结果",
            "24月随访日期",
            "24月随访-症状筛查日期",
            "24月随访-症状",
            "24月随访-症状2（自行填写）",
            "24月随访-影像检查日期",
            "24月随访-影像检查方法",
            "24月随访-影像结果",
            "24月随访-留标本时间",
            "24月随访-病原学检查方法",
            "24月随访-病原学检查结果",
            "24月随访-筛查结果",
            "备注"
    ));

    /** 导入模板表头（不含录入用户） */
    public static List<List<String>> asTemplateHead() {
        return asEasyExcelHead();
    }

    /** 导出表头：官方 72 列不变，末尾追加录入用户 */
    public static List<List<String>> asExportHead() {
        List<List<String>> heads = new ArrayList<>(asEasyExcelHead());
        heads.add(List.of("录入用户"));
        return heads;
    }

    public static List<List<String>> asEasyExcelHead() {
        return COLUMNS.stream().map(List::of).toList();
    }
}
