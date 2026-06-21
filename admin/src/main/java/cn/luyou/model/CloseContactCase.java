package cn.luyou.model;

import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 密接个案表（电子表格，71列官方模板与密接筛查表一致，独立存储不含流程状态）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("close_contact_case")
public class CloseContactCase extends BaseEntity {

    @ExcelProperty(index = 0)
    private String city;

    @ExcelProperty(index = 1)
    private String district;

    @ExcelProperty(index = 2)
    private String sourcePatientName;

    @ExcelProperty(index = 3)
    private String sourcePatientCaseNo;

    @ExcelProperty(index = 4)
    private String sourcePatientBacteriologyResult;

    @ExcelProperty(index = 5)
    private String sourcePatientPhone;

    /** 原患者身份证号（已从 Excel 模板移除，仅数据库历史字段） */
    @ExcelIgnore
    private String sourcePatientIdNumber;

    @ExcelProperty(index = 6, converter = FlexibleLocalDateConverter.class)
    private LocalDate reportDate;

    @ExcelProperty(index = 7, converter = FlexibleLocalDateConverter.class)
    private LocalDate registrationDate;

    /** 报表填报季度（Excel 自动生成列，仅导入/导出占位） */
    @TableField(exist = false)
    @ExcelProperty(index = 8)
    private String reportQuarter;

    /** 登记日期至当前日期间隔提示（Excel 自动生成列，仅导入/导出占位） */
    @TableField(exist = false)
    @ExcelProperty(index = 9)
    private String registrationIntervalHint;

    @ExcelProperty(index = 10)
    private String name;

    @ExcelProperty(index = 11)
    private String idNumber;

    @ExcelProperty(index = 12, converter = FlexibleIntegerConverter.class)
    private Integer age;

    /** 年龄组（Excel 自动判断列，仅导入/导出占位） */
    @TableField(exist = false)
    @ExcelProperty(index = 13)
    private String ageGroup;

    @ExcelProperty(index = 14)
    private String phone;

    @ExcelProperty(index = 15)
    private String contactType;

    @ExcelProperty(index = 16)
    private String contactPlace;

    @ExcelProperty(index = 17, converter = FlexibleLocalDateConverter.class)
    private LocalDate firstScreenDate;

    @ExcelProperty(index = 18)
    private String symptom1;

    @ExcelProperty(index = 19)
    private String symptom2;

    @ExcelProperty(index = 20, converter = FlexibleLocalDateConverter.class)
    private LocalDate infectionCheckDate;

    @ExcelProperty(index = 21)
    private String infectionCheckMethod;

    @ExcelProperty(index = 22)
    private String infectionCheckResult;

    @ExcelProperty(index = 23, converter = FlexibleLocalDateConverter.class)
    private LocalDate imagingDate;

    @ExcelProperty(index = 24)
    private String imagingMethod;

    @ExcelProperty(index = 25)
    private String imagingResult;

    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate sputumCheckDate;

    @ExcelProperty(index = 27)
    private String sputumCheckMethod;

    @ExcelProperty(index = 28)
    private String sputumCheckResult;

    /** 诊断结果（AE列）：活动性肺结核/潜伏感染者/未做/未发现异常 */
    @ExcelProperty(index = 29)
    private String finalScreeningResult;

    @ExcelProperty(index = 30)
    private String hasContraindication;

    @ExcelProperty(index = 31)
    private String noTreatmentReason;

    @ExcelProperty(index = 32)
    private String contraindicationRemark;

    /** 是否开展预防治疗（已从 Excel 模板移除，仅数据库/手动录入） */
    @ExcelIgnore
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 33)
    private String preventivePlan;

    @ExcelProperty(index = 34)
    private String preventivePlanRemark;

    @ExcelProperty(index = 35)
    private String treatmentCompleted;

    @ExcelProperty(index = 36)
    private String incompleteReason;

    @ExcelProperty(index = 37, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6DueDate;

    @ExcelProperty(index = 38, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ScreenDate;

    @ExcelProperty(index = 39)
    private String followup6Symptom1;

    @ExcelProperty(index = 40)
    private String followup6Symptom2;

    @ExcelProperty(index = 41, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ImagingDate;

    @ExcelProperty(index = 42)
    private String followup6ImagingMethod;

    @ExcelProperty(index = 43)
    private String followup6ImagingResult;

    @ExcelProperty(index = 44, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6SputumDate;

    @ExcelProperty(index = 45)
    private String followup6SputumMethod;

    @ExcelProperty(index = 46)
    private String followup6SputumResult;

    @ExcelProperty(index = 47)
    private String followup6Result;

    @ExcelProperty(index = 48, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12DueDate;

    @ExcelProperty(index = 49, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ScreenDate;

    @ExcelProperty(index = 50)
    private String followup12Symptom1;

    @ExcelProperty(index = 51)
    private String followup12Symptom2;

    @ExcelProperty(index = 52, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ImagingDate;

    @ExcelProperty(index = 53)
    private String followup12ImagingMethod;

    @ExcelProperty(index = 54)
    private String followup12ImagingResult;

    @ExcelProperty(index = 55, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12SputumDate;

    @ExcelProperty(index = 56)
    private String followup12SputumMethod;

    @ExcelProperty(index = 57)
    private String followup12SputumResult;

    @ExcelProperty(index = 58)
    private String followup12Result;

    @ExcelProperty(index = 59, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24DueDate;

    @ExcelProperty(index = 60, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ScreenDate;

    @ExcelProperty(index = 61)
    private String followup24Symptom1;

    @ExcelProperty(index = 62)
    private String followup24Symptom2;

    @ExcelProperty(index = 63, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ImagingDate;

    @ExcelProperty(index = 64)
    private String followup24ImagingMethod;

    @ExcelProperty(index = 65)
    private String followup24ImagingResult;

    @ExcelProperty(index = 66, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24SputumDate;

    @ExcelProperty(index = 67)
    private String followup24SputumMethod;

    @ExcelProperty(index = 68)
    private String followup24SputumResult;

    @ExcelProperty(index = 69)
    private String followup24Result;

    @ExcelProperty(index = 70)
    private String remark;

    @ExcelIgnore
    private String uploadBatch;

    @ExcelIgnore
    private String year;

    @ExcelIgnore
    private String gender;

    @ExcelIgnore
    private String ethnicity;

    @ExcelIgnore
    private String householdAddress;

    @ExcelIgnore
    private String currentAddress;

    @ExcelIgnore
    private Long departmentId;

    /** 录入用户名（系统账号统一命名格式） */
    @ExcelIgnore
    private String creatorUsername;
}
