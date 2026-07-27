package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 密接个案表（电子表格，72列官方模板与密接筛查表一致，独立存储不含流程状态）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("close_contact_case")
public class CloseContactCase extends BaseEntity {

    @ExcelProperty(index = 0)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String city;

    @ExcelProperty(index = 1)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    @ExcelProperty(index = 2)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourcePatientName;

    @ExcelProperty(index = 3, converter = ExcelTextStringConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourcePatientCaseNo;

    @ExcelProperty(index = 4)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourcePatientBacteriologyResult;

    @ExcelProperty(index = 5, converter = ExcelTextStringConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourcePatientPhone;

    /** 原患者身份证号（已从 Excel 模板移除，仅数据库历史字段） */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourcePatientIdNumber;

    @ExcelProperty(index = 6, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate reportDate;

    @ExcelProperty(index = 7, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String name;

    @ExcelProperty(index = 11, converter = ExcelTextStringConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String idNumber;

    @ExcelProperty(index = 12, converter = FlexibleIntegerConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer age;

    /** 年龄组（Excel 自动判断列，仅导入/导出占位） */
    @TableField(exist = false)
    @ExcelProperty(index = 13)
    private String ageGroup;

    @ExcelProperty(index = 14, converter = ExcelTextStringConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String phone;

    @ExcelProperty(index = 15)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contactType;

    @ExcelProperty(index = 16)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contactPlace;

    @ExcelProperty(index = 17, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate firstScreenDate;

    @ExcelProperty(index = 18)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symptom1;

    @ExcelProperty(index = 19)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symptom2;

    @ExcelProperty(index = 20, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate infectionCheckDate;

    @ExcelProperty(index = 21)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String infectionCheckMethod;

    @ExcelProperty(index = 22)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String infectionCheckResult;

    @ExcelProperty(index = 23, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate imagingDate;

    @ExcelProperty(index = 24)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String imagingMethod;

    @ExcelProperty(index = 25)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String imagingResult;

    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate sputumCheckDate;

    @ExcelProperty(index = 27)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sputumCheckMethod;

    @ExcelProperty(index = 28)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sputumCheckResult;

    /** 诊断结果（AE列）：活动性肺结核/潜伏感染者/未做/未发现异常 */
    @ExcelProperty(index = 29)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String finalScreeningResult;

    @ExcelProperty(index = 30)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String hasContraindication;

    @ExcelProperty(index = 31)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String noTreatmentReason;

    @ExcelProperty(index = 32)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contraindicationRemark;

    @ExcelProperty(index = 33)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 34)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preventivePlan;

    @ExcelProperty(index = 35)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preventivePlanRemark;

    @ExcelProperty(index = 36)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String treatmentCompleted;

    @ExcelProperty(index = 37)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String incompleteReason;

    @ExcelProperty(index = 38, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup6DueDate;

    @ExcelProperty(index = 39, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup6ScreenDate;

    @ExcelProperty(index = 40)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6Symptom1;

    @ExcelProperty(index = 41)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6Symptom2;

    @ExcelProperty(index = 42, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup6ImagingDate;

    @ExcelProperty(index = 43)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6ImagingMethod;

    @ExcelProperty(index = 44)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6ImagingResult;

    @ExcelProperty(index = 45, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup6SputumDate;

    @ExcelProperty(index = 46)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6SputumMethod;

    @ExcelProperty(index = 47)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6SputumResult;

    @ExcelProperty(index = 48)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup6Result;

    @ExcelProperty(index = 49, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup12DueDate;

    @ExcelProperty(index = 50, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup12ScreenDate;

    @ExcelProperty(index = 51)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12Symptom1;

    @ExcelProperty(index = 52)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12Symptom2;

    @ExcelProperty(index = 53, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup12ImagingDate;

    @ExcelProperty(index = 54)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12ImagingMethod;

    @ExcelProperty(index = 55)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12ImagingResult;

    @ExcelProperty(index = 56, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup12SputumDate;

    @ExcelProperty(index = 57)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12SputumMethod;

    @ExcelProperty(index = 58)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12SputumResult;

    @ExcelProperty(index = 59)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup12Result;

    @ExcelProperty(index = 60, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup24DueDate;

    @ExcelProperty(index = 61, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup24ScreenDate;

    @ExcelProperty(index = 62)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24Symptom1;

    @ExcelProperty(index = 63)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24Symptom2;

    @ExcelProperty(index = 64, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup24ImagingDate;

    @ExcelProperty(index = 65)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24ImagingMethod;

    @ExcelProperty(index = 66)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24ImagingResult;

    @ExcelProperty(index = 67, converter = FlexibleLocalDateConverter.class)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate followup24SputumDate;

    @ExcelProperty(index = 68)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24SputumMethod;

    @ExcelProperty(index = 69)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24SputumResult;

    @ExcelProperty(index = 70)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String followup24Result;

    @ExcelProperty(index = 71)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String uploadBatch;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String year;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gender;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ethnicity;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String householdAddress;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentAddress;

    @ExcelIgnore
    private Long departmentId;

    /** Excel 导入行号（与模板物理行号一致，用于列表按原 Excel 顺序展示） */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer importRowNo;

    /** 录入用户名（系统账号统一命名格式；导出末尾追加列，导入模板不含） */
    @ExcelProperty(index = 72)
    private String creatorUsername;
}
