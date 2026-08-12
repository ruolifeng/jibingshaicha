package cn.luyou.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 学生筛查 Excel 表头（对齐《2026年秋季新生入学结核病筛查记录表新》）。
 * <p>无最左「年份」列；姓名后的「年份」为业务 year。
 * 含「是否寄宿制」「年级」；导出在末尾追加「录入用户」「录入时间」。
 */
public final class SchoolScreeningExcelHeaders {

    private SchoolScreeningExcelHeaders() {
    }

    public static final String SHEET_NAME = "筛查数据";

    private static final String G_SYMPTOM = "结核病可疑症状";
    private static final String G_INFECTION = "感染筛查";
    private static final String G_CHEST = "胸部影像学";

    private static List<String> head(String... parts) {
        return new ArrayList<>(Arrays.asList(parts));
    }

    /** 导入模板表头（不含录入用户） */
    public static List<List<String>> asTemplateHead() {
        List<List<String>> heads = new ArrayList<>(33);
        appendBizHeads(heads);
        return heads;
    }

    /** 导出表头：业务列 + 录入用户 + 录入时间 */
    public static List<List<String>> asExportHead() {
        List<List<String>> heads = new ArrayList<>(35);
        appendBizHeads(heads);
        heads.add(head("录入用户"));
        heads.add(head("录入时间"));
        return heads;
    }

    private static void appendBizHeads(List<List<String>> heads) {
        heads.add(head("填报机构"));
        heads.add(head("市州"));
        heads.add(head("县区"));
        heads.add(head("乡镇/街道"));
        heads.add(head("类型"));
        heads.add(head("是否寄宿制"));
        heads.add(head("学校名称（全称）"));
        heads.add(head("姓名"));
        heads.add(head("年份"));
        heads.add(head("性别"));
        heads.add(head("身份证号"));
        heads.add(head("年龄"));
        heads.add(head("户籍所在地"));
        heads.add(head("年级"));
        heads.add(head("班级"));
        heads.add(head("民族"));
        heads.add(head("是否参加筛查"));
        heads.add(head("有无既往结核病史"));
        heads.add(head("有无肺结核接触史"));
        heads.add(head(G_SYMPTOM, "咳嗽，咳痰≥两周"));
        heads.add(head(G_SYMPTOM, "咯血或血痰"));
        heads.add(head(G_SYMPTOM, "其他"));
        heads.add(head(G_INFECTION, "感染筛查时间"));
        heads.add(head(G_INFECTION, "方法"));
        heads.add(head(G_INFECTION, "结果"));
        heads.add(head(G_INFECTION, "判定结果"));
        heads.add(head(G_CHEST, "胸片检查时间"));
        heads.add(head(G_CHEST, "方法"));
        heads.add(head(G_CHEST, "结果"));
        heads.add(head("分子生物学结果"));
        heads.add(head("痰培养结果"));
        heads.add(head("筛查结果"));
        heads.add(head("备注"));
    }
}
