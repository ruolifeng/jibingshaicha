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

    public static final String TITLE = "2026年秋季新生入学结核病筛查记录表";

    /** 官方业务列数（不含录入用户/录入时间） */
    public static final int BIZ_COLUMN_COUNT = 33;

    private static final String G_SYMPTOM = "结核病可疑症状";
    private static final String G_INFECTION = "感染筛查";
    private static final String G_CHEST = "胸部影像学";

    /**
     * 第 2 行分组标题（与官方表一致；无分组列填叶子名，导出时与第 3 行纵向合并）。
     */
    public static final String[] TOP_HEADERS = {
            "填报机构", "市州", "县区", "乡镇/街道", "类型", "是否寄宿制", "学校名称（全称）", "姓名", "年份", "性别",
            "身份证号", "年龄", "户籍所在地", "年级", "班级", "民族", "是否参加筛查", "有无既往结核病史", "有无肺结核接触史",
            G_SYMPTOM, G_SYMPTOM, G_SYMPTOM,
            G_INFECTION, G_INFECTION, G_INFECTION, G_INFECTION,
            G_CHEST, G_CHEST, G_CHEST,
            "分子生物学结果", "痰培养结果", "筛查结果", "备注"
    };

    /** 第 3 行子表头（无子列时为空，与 TOP 纵向合并） */
    public static final String[] SUB_HEADERS = {
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "",
            "咳嗽，咳痰≥两周", "咯血或血痰", "其他",
            "感染筛查时间", "方法", "结果", "判定结果",
            "胸片检查时间", "方法", "结果",
            "", "", "", ""
    };

    /** 第 5 行填写说明（对齐官方 Excel；无说明的列为空） */
    public static final String[] INSTRUCTION_ROW = {
            "", "", "", "",
            "（填写数字，1=托幼机构，2=小学，3=初中，4=高中阶段教育学校，5=高等教育学校，6=教职工,7=其他（培训学校、特殊教育学校和专门学校等））",
            "(填写数字1-寄宿制，2-非寄宿制，3=大学，4=其他)",
            "", "", "", "",
            "", "", "", "", "", "", "", "", "",
            "", "", "",
            "",
            "（填写数字，1=结核菌素纯蛋白衍生物（PPD），2=重组结核分枝杆菌融合蛋白（EC），3=γ-干扰素释放试验（IGRA），4=未查）",
            "（PPD填写横径×纵径（mm）及有无双圈、水泡、坏死、淋巴管炎等；EC和IGRA填写阳性/阴性；未查填写“无”）",
            "（填写数字，0=未感染，1=感染，2=无法判读，3=未查）",
            "",
            "（填写数字，1=胸部X线，2=胸部CT，3=其他（需注明），4=未查）",
            "（填写数字，0=未见异常，1=异常（疑似活动性结核病变），2=异常（非活动性结核病变），3=其他（需注明），4=未查）",
            "（填写数字，0=阴性，1=阳性，2=无法判读，3=未查）",
            "（0=阴性，1=阳性，2=无法判读，3=未查）",
            "（填写数字，0=未发现异常，1=活动性肺结核，2=疑似肺结核，3=潜伏感染者，4=其他（需注明））",
            ""
    };

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
