package cn.luyou.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 大疫情报告卡导入（推介追踪-追踪模块）需提取的表头字段。
 */
public final class EpidemicTrackImportHeaders {

    private EpidemicTrackImportHeaders() {
    }

    public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
            "卡片ID",
            "患者姓名",
            "患儿家长姓名",
            "有效证件号",
            "性别",
            "出生日期",
            "年龄",
            "患者工作单位",
            "联系电话",
            "现住详细地址",
            "人群分类",
            "病例分类",
            "疾病名称",
            "报告单位",
            "报告卡录入时间",
            "报告卡录入日期",
            "备注"
    ));
}
