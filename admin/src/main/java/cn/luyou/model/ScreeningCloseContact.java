package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
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
 * 密接人群筛查数据（官方 72 列模板，含 6/12/24 月随访）
 *
 * 列映射（0-based index，与 {@link cn.luyou.constant.CloseContactCaseExcelHeaders} 一致）：
 *   0-9   : 原患者及登记信息
 *   10-16 : 接触者基本信息
 *   17-29 : 初次筛查（29 = 最终筛查结果，核心分类字段）
 *   30-37 : 禁忌症与预防治疗
 *   38-48 : 6 月随访
 *   49-59 : 12 月随访
 *   60-70 : 24 月随访
 *   71    : 备注
 *
 * 业务分类规则（基于 final_screening_result）：
 *   - 活动性肺结核   → 直接进入患者管理
 *   - 潜伏感染者     → 密接潜伏感染管理（特有流程）
 *   - 未做           → 6/12/24月随访管理
 *   - 未发现异常     → 3月复查，再按结果分流
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_close_contact")
public class ScreeningCloseContact extends BaseEntity {

    // ===== 原患者信息（A-I，index 0-8）=====
    @ExcelProperty(index = 0)
    private String city;

    @ExcelProperty(index = 1)
    private String district;

    @ExcelProperty(index = 2)
    private String sourcePatientName;

    @ExcelProperty(index = 3, converter = ExcelTextStringConverter.class)
    private String sourcePatientCaseNo;

    @ExcelProperty(index = 4)
    private String sourcePatientBacteriologyResult;

    @ExcelProperty(index = 5, converter = ExcelTextStringConverter.class)
    private String sourcePatientPhone;

    /** 原患者身份证号（已从 Excel 模板移除，仅数据库历史字段） */
    @ExcelIgnore
    private String sourcePatientIdNumber;

    @ExcelProperty(index = 6, converter = FlexibleLocalDateConverter.class)
    private LocalDate reportDate;

    /** 密切接触者登记日期，是 6/12/24 月随访到期的计算基准 */
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

    // ===== 接触者基本信息（index 10-16）=====
    @ExcelProperty(index = 10)
    private String name;

    @ExcelProperty(index = 11, converter = ExcelTextStringConverter.class)
    private String idNumber;

    @ExcelProperty(index = 12, converter = FlexibleIntegerConverter.class)
    private Integer age;

    /** 年龄组（Excel 自动判断列，仅导入/导出占位） */
    @TableField(exist = false)
    @ExcelProperty(index = 13)
    private String ageGroup;

    @ExcelProperty(index = 14, converter = ExcelTextStringConverter.class)
    private String phone;

    /** 联系电话与接触者关系（手动录入，Excel 模板无此列） */
    @ExcelIgnore
    private String phoneContactRelation;

    @ExcelProperty(index = 15)
    private String contactType;

    @ExcelProperty(index = 16)
    private String contactPlace;

    /** 接触场所选「其他（需手工录入）」时的补充说明 */
    @ExcelIgnore
    private String contactPlaceOther;

    // ===== 初次筛查信息（index 17-29）=====
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

    @ExcelIgnore
    private String imagingMethodOther;

    @ExcelProperty(index = 25)
    private String imagingResult;

    @ExcelIgnore
    private String imagingResultOther;

    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate sputumCheckDate;

    @ExcelProperty(index = 27)
    private String sputumCheckMethod;

    @ExcelIgnore
    private String sputumCheckMethodOther;

    @ExcelProperty(index = 28)
    private String sputumCheckResult;

    @ExcelIgnore
    private String sputumCheckResultOther;

    /** 最终筛查结果（核心分类字段）：活动性肺结核/潜伏感染者/未做/未发现异常 */
    @ExcelProperty(index = 29)
    private String finalScreeningResult;

    @ExcelIgnore
    private String finalScreeningResultOther;

    // ===== 预防性治疗信息（index 30-37）=====
    @ExcelProperty(index = 30)
    private String hasContraindication;

    @ExcelProperty(index = 31)
    private String noTreatmentReason;

    @ExcelProperty(index = 32)
    private String contraindicationRemark;

    @ExcelProperty(index = 33)
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 34)
    private String preventivePlan;

    @ExcelProperty(index = 35)
    private String preventivePlanRemark;

    @ExcelProperty(index = 36)
    private String treatmentCompleted;

    @ExcelProperty(index = 37)
    private String incompleteReason;

    // ===== 6月随访（index 38-48）=====
    @ExcelProperty(index = 38, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6DueDate;

    @ExcelProperty(index = 39, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ScreenDate;

    @ExcelProperty(index = 40)
    private String followup6Symptom1;

    @ExcelProperty(index = 41)
    private String followup6Symptom2;

    @ExcelProperty(index = 42, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6ImagingDate;

    @ExcelProperty(index = 43)
    private String followup6ImagingMethod;

    @ExcelProperty(index = 44)
    private String followup6ImagingResult;

    @ExcelProperty(index = 45, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup6SputumDate;

    @ExcelProperty(index = 46)
    private String followup6SputumMethod;

    @ExcelProperty(index = 47)
    private String followup6SputumResult;

    /** 6月随访筛查结果：活动性肺结核/潜伏感染者/其它/未发现异常/未做 */
    @ExcelProperty(index = 48)
    private String followup6Result;

    // ===== 12月随访（index 49-59）=====
    @ExcelProperty(index = 49, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12DueDate;

    @ExcelProperty(index = 50, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ScreenDate;

    @ExcelProperty(index = 51)
    private String followup12Symptom1;

    @ExcelProperty(index = 52)
    private String followup12Symptom2;

    @ExcelProperty(index = 53, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12ImagingDate;

    @ExcelProperty(index = 54)
    private String followup12ImagingMethod;

    @ExcelProperty(index = 55)
    private String followup12ImagingResult;

    @ExcelProperty(index = 56, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup12SputumDate;

    @ExcelProperty(index = 57)
    private String followup12SputumMethod;

    @ExcelProperty(index = 58)
    private String followup12SputumResult;

    /** 12月随访筛查结果 */
    @ExcelProperty(index = 59)
    private String followup12Result;

    // ===== 24月随访（index 60-70）=====
    @ExcelProperty(index = 60, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24DueDate;

    @ExcelProperty(index = 61, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ScreenDate;

    @ExcelProperty(index = 62)
    private String followup24Symptom1;

    @ExcelProperty(index = 63)
    private String followup24Symptom2;

    @ExcelProperty(index = 64, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24ImagingDate;

    @ExcelProperty(index = 65)
    private String followup24ImagingMethod;

    @ExcelProperty(index = 66)
    private String followup24ImagingResult;

    @ExcelProperty(index = 67, converter = FlexibleLocalDateConverter.class)
    private LocalDate followup24SputumDate;

    @ExcelProperty(index = 68)
    private String followup24SputumMethod;

    @ExcelProperty(index = 69)
    private String followup24SputumResult;

    /** 24月随访筛查结果 */
    @ExcelProperty(index = 70)
    private String followup24Result;

    @ExcelProperty(index = 71)
    private String remark;

    // ===== 系统字段（不从 Excel 读取）=====

    /**
     * 密接流程状态：
     *   0 - 待处理
     *   1 - 活动性肺结核（已进患者管理）
     *   2 - 潜伏感染者（管理中）
     *   3 - 潜伏感染者（已归档）
     *   4 - 随访监测中（未做/否/未完成治疗）
     *   5 - 随访监测归档
     *   6 - 未发现异常（待3月复查）
     *   7 - 未发现异常（3月复查阴性，结束）
     *   8 - 未发现异常（3月复查阳性，转②流程）
     *   9 - 疑似肺结核（结案）
     */
    @ExcelIgnore
    private Integer ccStatus;

    /** 预计完成治疗时间（系统设置，用于到期提醒） */
    @ExcelIgnore
    private LocalDate expectedTreatmentEndDate;

    /** 3月复查感染检测日期（未发现异常流程，系统录入） */
    @ExcelIgnore
    private LocalDate threeMonthCheckDate;

    /** 3月复查感染检测结果 */
    @ExcelIgnore
    private String threeMonthCheckResult;

    /** 3月复查最终判定：阴性/阳性 */
    @ExcelIgnore
    private String threeMonthFinalResult;

    @ExcelIgnore
    private String uploadBatch;

    /** 年份（从 registrationDate 提取，方便查询统计） */
    @ExcelIgnore
    private String year;

    /** 性别（接触者，Excel 中无单独列，可手动录入或从其他源导入） */
    @ExcelIgnore
    private String gender;

    /** 民族 */
    @ExcelIgnore
    private String ethnicity;

    /** 户籍地址 */
    @ExcelIgnore
    private String householdAddress;

    /** 现住址 */
    @ExcelIgnore
    private String currentAddress;
    /** 所属部门ID */
    @ExcelIgnore
    private Long departmentId;

    /** 是否已发送通知单（非数据库字段，查询时动态填充） */
    @ExcelIgnore
    @TableField(exist = false)
    private Boolean noticeSent;
}
