package cn.luyou.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolStatisticsVO {

    @ExcelProperty("区县")
    private String district;

    @ExcelProperty("学校名称")
    private String schoolName;

    @ExcelProperty("应筛查人数")
    private Long shouldScreenCount;

    @ExcelProperty("实际筛查人数")
    private Long actualScreenCount;

    @ExcelProperty("与肺结核患者密切接触的人数")
    private Long closeContactCount;

    @ExcelProperty("有肺结核可疑症状者人数")
    private Long suspiciousSymptomCount;

    @ExcelProperty("胸片检查人数")
    private Long chestXrayCount;

    @ExcelProperty("胸片异常人数")
    private Long chestXrayAbnormalCount;

    @ExcelProperty("结核菌素试验检测人数")
    private Long ppdTestCount;

    @ExcelProperty("PPD+人数")
    private Long ppdPositive1;

    @ExcelProperty("PPD++人数")
    private Long ppdPositive2;

    @ExcelProperty("PPD+++人数")
    private Long ppdPositive3;

    @ExcelProperty("PPD阳性总人数（+、++、+++合计）")
    private Long ppdPositiveTotal;

    @ExcelProperty("EC阴性人数")
    private Long ecNegative;

    @ExcelProperty("EC阳性人数")
    private Long ecPositive;

    @ExcelProperty("IGRA阳性人数")
    private Long igraPositive;

    @ExcelProperty("IGRA阴性人数")
    private Long igraNegative;

    @ExcelProperty("肺结核/疑似肺结核患者人数")
    private Long tbPatientCount;

    @ExcelProperty("备注")
    private String remark;
}
