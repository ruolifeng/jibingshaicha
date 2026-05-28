package cn.luyou.model;

import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 密接个案表（电子表格，73列模板与密接筛查表一致，独立存储不含流程状态）
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

    @ExcelProperty(index = 6)
    private String sourcePatientIdNumber;

    @ExcelProperty(index = 7, converter = FlexibleLocalDateConverter.class)
    private LocalDate reportDate;

    @ExcelProperty(index = 8, converter = FlexibleLocalDateConverter.class)
    private LocalDate registrationDate;

    @ExcelProperty(index = 11)
    private String name;

    @ExcelProperty(index = 12)
    private String idNumber;

    @ExcelProperty(index = 13, converter = FlexibleIntegerConverter.class)
    private Integer age;

    @ExcelProperty(index = 15)
    private String phone;

    @ExcelProperty(index = 16)
    private String contactType;

    @ExcelProperty(index = 17)
    private String contactPlace;

    @ExcelProperty(index = 18, converter = FlexibleLocalDateConverter.class)
    private LocalDate firstScreenDate;

    @ExcelProperty(index = 19)
    private String symptom1;

    @ExcelProperty(index = 20)
    private String symptom2;

    @ExcelProperty(index = 21, converter = FlexibleLocalDateConverter.class)
    private LocalDate infectionCheckDate;

    @ExcelProperty(index = 22)
    private String infectionCheckMethod;

    @ExcelProperty(index = 23)
    private String infectionCheckResult;

    @ExcelProperty(index = 24, converter = FlexibleLocalDateConverter.class)
    private LocalDate imagingDate;

    @ExcelProperty(index = 25)
    private String imagingMethod;

    @ExcelProperty(index = 26)
    private String imagingResult;

    @ExcelProperty(index = 27, converter = FlexibleLocalDateConverter.class)
    private LocalDate sputumCheckDate;

    @ExcelProperty(index = 28)
    private String sputumCheckMethod;

    @ExcelProperty(index = 29)
    private String sputumCheckResult;

    /** 诊断结果（AE列）：活动性肺结核/潜伏感染者/未做/未发现异常 */
    @ExcelProperty(index = 30)
    private String finalScreeningResult;

    @ExcelProperty(index = 31)
    private String hasContraindication;

    @ExcelProperty(index = 32)
    private String noTreatmentReason;

    @ExcelProperty(index = 33)
    private String contraindicationRemark;

    @ExcelProperty(index = 34)
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 35)
    private String preventivePlan;

    @ExcelProperty(index = 36)
    private String preventivePlanRemark;

    @ExcelProperty(index = 37)
    private String treatmentCompleted;

    @ExcelProperty(index = 38)
    private String incompleteReason;

    @ExcelProperty(index = 39, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6DueDate;

    @ExcelProperty(index = 40, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ScreenDate;

    @ExcelProperty(index = 41)
    private String followup6Symptom1;

    @ExcelProperty(index = 42)
    private String followup6Symptom2;

    @ExcelProperty(index = 43, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ImagingDate;

    @ExcelProperty(index = 44)
    private String followup6ImagingMethod;

    @ExcelProperty(index = 45)
    private String followup6ImagingResult;

    @ExcelProperty(index = 46, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6SputumDate;

    @ExcelProperty(index = 47)
    private String followup6SputumMethod;

    @ExcelProperty(index = 48)
    private String followup6SputumResult;

    @ExcelProperty(index = 49)
    private String followup6Result;

    @ExcelProperty(index = 50, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12DueDate;

    @ExcelProperty(index = 51, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ScreenDate;

    @ExcelProperty(index = 52)
    private String followup12Symptom1;

    @ExcelProperty(index = 53)
    private String followup12Symptom2;

    @ExcelProperty(index = 54, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ImagingDate;

    @ExcelProperty(index = 55)
    private String followup12ImagingMethod;

    @ExcelProperty(index = 56)
    private String followup12ImagingResult;

    @ExcelProperty(index = 57, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12SputumDate;

    @ExcelProperty(index = 58)
    private String followup12SputumMethod;

    @ExcelProperty(index = 59)
    private String followup12SputumResult;

    @ExcelProperty(index = 60)
    private String followup12Result;

    @ExcelProperty(index = 61, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24DueDate;

    @ExcelProperty(index = 62, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ScreenDate;

    @ExcelProperty(index = 63)
    private String followup24Symptom1;

    @ExcelProperty(index = 64)
    private String followup24Symptom2;

    @ExcelProperty(index = 65, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ImagingDate;

    @ExcelProperty(index = 66)
    private String followup24ImagingMethod;

    @ExcelProperty(index = 67)
    private String followup24ImagingResult;

    @ExcelProperty(index = 68, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24SputumDate;

    @ExcelProperty(index = 69)
    private String followup24SputumMethod;

    @ExcelProperty(index = 70)
    private String followup24SputumResult;

    @ExcelProperty(index = 71)
    private String followup24Result;

    @ExcelProperty(index = 72)
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
