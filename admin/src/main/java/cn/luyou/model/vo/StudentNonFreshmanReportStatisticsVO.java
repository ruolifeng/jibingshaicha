package cn.luyou.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学校人群 — 学生报表统计（非新生统计）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentNonFreshmanReportStatisticsVO {

    @ExcelProperty("年级分类")
    private String gradeCategory;

    @ExcelProperty("应检测人数")
    private Long shouldDetectCount;

    @ExcelProperty("接受检查人数")
    private Long acceptedExamCount;

    @ExcelProperty("接受规范检查人数")
    private Long standardizedExamCount;

    @ExcelProperty("发现肺结核患者例数")
    private Long tbPatientCount;

    @ExcelProperty("发现潜伏感染者人数")
    private Long latentInfectionCount;
}
