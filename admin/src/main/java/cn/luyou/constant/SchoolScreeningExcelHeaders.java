package cn.luyou.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 学生/学校人群筛查 Excel 表头（与系统列表、导入模板一致：2 行表头）。
 * <p>导出在「序号」后追加「录入用户」；导入模板不含该系统字段。
 */
public final class SchoolScreeningExcelHeaders {

    private SchoolScreeningExcelHeaders() {
    }

    public static final String SHEET_NAME = "筛查数据";

    private static final String G_INFECTION = "学校人群感染筛查情况";
    private static final String G_XRAY = "学校人群胸片检查";
    private static final String G_PREVENT = "潜伏感染者管理情况";

    private static List<String> head(String... parts) {
        return new ArrayList<>(Arrays.asList(parts));
    }

    /** 导入模板表头（不含录入用户） */
    public static List<List<String>> asTemplateHead() {
        List<List<String>> heads = new ArrayList<>(37);
        heads.add(head("序号"));
        appendBizHeads(heads);
        return heads;
    }

    /** 导出表头：序号 + 录入用户 + 业务列（与系统字段对齐） */
    public static List<List<String>> asExportHead() {
        List<List<String>> heads = new ArrayList<>(38);
        heads.add(head("序号"));
        heads.add(head("录入用户"));
        appendBizHeads(heads);
        return heads;
    }

    private static void appendBizHeads(List<List<String>> heads) {
        heads.add(head("年份"));
        heads.add(head("市（州）"));
        heads.add(head("县（市、区）"));
        heads.add(head("姓名"));
        heads.add(head("性别"));
        heads.add(head("出生日期"));
        heads.add(head("年龄"));
        heads.add(head("证件类型"));
        heads.add(head("证件号"));
        heads.add(head("民族"));
        heads.add(head("联系电话"));
        heads.add(head("户籍所在地（XX市XX县、区）"));
        heads.add(head("现地址"));
        heads.add(head("学校类型"));
        heads.add(head("学校名称"));
        heads.add(head("班级（院系）"));
        heads.add(head("既往结核病史"));
        heads.add(head("密切接触史"));
        heads.add(head("结核病可疑症状"));
        heads.add(head(G_INFECTION, "是否进行感染筛"));
        heads.add(head(G_INFECTION, "感染筛查日期"));
        heads.add(head(G_INFECTION, "方法"));
        heads.add(head(G_INFECTION, "结果（PPD：mmXmm；EC及IGRA：阳性/阴性）"));
        heads.add(head(G_INFECTION, "感染筛查结果"));
        heads.add(head(G_XRAY, "是否进行胸片检查"));
        heads.add(head(G_XRAY, "胸片检查日期"));
        heads.add(head(G_XRAY, "胸片结果"));
        heads.add(head("痰涂片结果"));
        heads.add(head("分子生物学结果"));
        heads.add(head("诊断结果"));
        heads.add(head(G_PREVENT, "是否进行预防者治疗"));
        heads.add(head(G_PREVENT, "预防性治疗方案"));
        heads.add(head(G_PREVENT, "预防性治疗开始时间（年月日）"));
        heads.add(head(G_PREVENT, "预防性治疗完成时间（年月日）"));
        heads.add(head(G_PREVENT, "预防性治疗结果"));
        heads.add(head(G_PREVENT, "预防性治疗期间随访管理人员"));
    }
}
