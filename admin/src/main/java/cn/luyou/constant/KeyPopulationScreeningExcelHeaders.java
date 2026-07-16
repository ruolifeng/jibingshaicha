package cn.luyou.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 重点人群 / 疫情筛查 Excel 表头（与官方模板「重点人群测试V1」一致：3 行表头）。
 * <p>导入模板 49 列；导出在末尾追加「录入用户」「录入时间」共 51 列（避免再导入列错位）。
 */
public final class KeyPopulationScreeningExcelHeaders {

    private KeyPopulationScreeningExcelHeaders() {
    }

    public static final String SHEET_NAME = "重点人群第导入阶段";

    private static final String G_BASIC = "个人基本信息";
    private static final String G_SYMPTOM = "症状筛查";
    private static final String G_INFECTION = "重点人群感染筛查情况";
    private static final String G_XRAY = "重点人群胸片检查";
    private static final String G_DIAG = "诊断结果";
    private static final String G_PREVENT = "潜伏感染者管理情况";
    private static final String CROWD = "人群分类（可多选）";
    private static final String SUSP = "可疑症状";

    /** 两级表头：末行重复列名，便于 EasyExcel 纵向合并，避免出现空白行。 */
    private static List<String> head2(String group, String leaf) {
        return new ArrayList<>(Arrays.asList(group, leaf, leaf));
    }

    /** 三级表头：分组 + 子分组 + 叶子列名。 */
    private static List<String> head3(String group, String sub, String leaf) {
        return new ArrayList<>(Arrays.asList(group, sub, leaf));
    }

    /** 导入模板表头（不含录入用户/录入时间） */
    public static List<List<String>> asTemplateHead() {
        return asEasyExcelHead();
    }

    /** 导出表头：官方列序不变，末尾追加录入用户、录入时间 */
    public static List<List<String>> asExportHead() {
        List<List<String>> heads = asEasyExcelHead();
        heads.add(head2(G_BASIC, "录入用户"));
        heads.add(head2(G_BASIC, "录入时间"));
        return heads;
    }

    public static List<List<String>> asEasyExcelHead() {
        List<List<String>> heads = new ArrayList<>(49);
        // 0-14 个人基本信息
        heads.add(head2(G_BASIC, "序号"));
        heads.add(head2(G_BASIC, "年份"));
        heads.add(head2(G_BASIC, "市（州）"));
        heads.add(head2(G_BASIC, "县（市、区）"));
        heads.add(head2(G_BASIC, "姓名"));
        heads.add(head2(G_BASIC, "性别"));
        heads.add(head2(G_BASIC, "出生日期"));
        heads.add(head2(G_BASIC, "年龄"));
        heads.add(head2(G_BASIC, "证件类型"));
        heads.add(head2(G_BASIC, "证件号"));
        heads.add(head2(G_BASIC, "民族"));
        heads.add(head2(G_BASIC, "联系电话"));
        heads.add(head2(G_BASIC, "户籍所在地（XX市XX县、区）"));
        heads.add(head2(G_BASIC, "乡镇/社区"));
        heads.add(head2(G_BASIC, "现住址"));
        // 15-22 人群分类
        heads.add(head3(G_BASIC, CROWD, "密接"));
        heads.add(head3(G_BASIC, CROWD, "学生"));
        heads.add(head3(G_BASIC, CROWD, "教职工"));
        heads.add(head3(G_BASIC, CROWD, "老年人"));
        heads.add(head3(G_BASIC, CROWD, "糖尿病"));
        heads.add(head3(G_BASIC, CROWD, "双感"));
        heads.add(head3(G_BASIC, CROWD, "既往结核史"));
        heads.add(head3(G_BASIC, CROWD, "非重点人群"));
        // 23-31 症状筛查
        heads.add(head3(G_SYMPTOM, SUSP, "是否有可疑症状"));
        heads.add(head3(G_SYMPTOM, SUSP, "咳嗽咳痰"));
        heads.add(head3(G_SYMPTOM, SUSP, "咯血或血痰"));
        heads.add(head3(G_SYMPTOM, SUSP, "发热"));
        heads.add(head3(G_SYMPTOM, SUSP, "胸痛"));
        heads.add(head3(G_SYMPTOM, SUSP, "夜间盗汗"));
        heads.add(head3(G_SYMPTOM, SUSP, "食欲不振"));
        heads.add(head3(G_SYMPTOM, SUSP, "乏力"));
        heads.add(head3(G_SYMPTOM, SUSP, "体重减轻"));
        // 32-36 感染筛查
        heads.add(head2(G_INFECTION, "是否进行感染筛"));
        heads.add(head2(G_INFECTION, "感染筛查日期"));
        heads.add(head2(G_INFECTION, "感染筛查方法"));
        heads.add(head2(G_INFECTION, "结果（PPD：mmXmm；EC及IGRA：阳性/阴性）"));
        heads.add(head2(G_INFECTION, "感染筛查 结果"));
        // 37-39 胸片
        heads.add(head2(G_XRAY, "是否进行  胸片检查"));
        heads.add(head2(G_XRAY, "胸片检查日期"));
        heads.add(head2(G_XRAY, "胸片结果"));
        // 40-42 诊断
        heads.add(head2(G_DIAG, "首次"));
        heads.add(head2(G_DIAG, "半年后"));
        heads.add(head2(G_DIAG, "一年后"));
        // 43-48 预防性治疗
        heads.add(head2(G_PREVENT, "是否进预防性治疗"));
        heads.add(head2(G_PREVENT, "预防性治疗方案"));
        heads.add(head2(G_PREVENT, "预防性治疗完成时间（年月日）"));
        heads.add(head2(G_PREVENT, "预防性治疗完成时间（年月日）"));
        heads.add(head2(G_PREVENT, "预防性治疗结果"));
        heads.add(head2(G_PREVENT, "预防性治疗期间随访管理人员"));
        return heads;
    }
}
