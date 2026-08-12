package cn.luyou.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新生入学体检结核病检查情况（学生报表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportStatisticsVO {

    @ExcelProperty("学校分类")
    private String schoolCategory;

    @ExcelProperty("入学新生人数")
    private Long enrollmentCount;

    @ExcelProperty("接受检查人数")
    private Long acceptedExamCount;

    @ExcelProperty("接受规范检查人数")
    private Long standardizedExamCount;

    @ExcelProperty("发现肺结核患者例数")
    private Long tbPatientCount;
}
