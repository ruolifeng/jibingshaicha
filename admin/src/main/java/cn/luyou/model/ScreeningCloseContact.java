package cn.luyou.model;

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
 * 密接人群筛查数据（新模板 — 73列，含6/12/24月随访）
 *
 * 列映射（0-based index）：
 *   0   A  : 市/州
 *   1   B  : 区/县
 *   2   C  : 患者姓名（原患者）
 *   3   D  : 病案号
 *   4   E  : 病原学结果
 *   5   F  : 患者电话
 *   6   G  : 患者身份证号
 *   7   H  : 填表日期
 *   8   I  : 密切接触者登记日期（6/12/24月随访到期基准）
 *   9   J  : 报表填报季度（自动生成，忽略）
 *   10  K  : 时间间隔（自动生成，忽略）
 *   11  L  : 接触者姓名
 *   12  M  : 接触者身份证号
 *   13  N  : 年龄
 *   14  O  : 年龄组（自动，忽略）
 *   15  P  : 接触者电话
 *   16  Q  : 接触类型
 *   17  R  : 接触场所
 *   18  S  : 首次筛查日期
 *   19  T  : 结核症状1
 *   20  U  : 结核症状2
 *   21  V  : 感染检测日期
 *   22  W  : 感染检测方法
 *   23  X  : 结果判定
 *   24  Y  : 影像检查日期
 *   25  Z  : 影像方法
 *   26  AA : 影像结果
 *   27  AB : 痰检留标日期
 *   28  AC : 痰检方法
 *   29  AD : 痰检结果
 *   30  AE : 最终筛查结果 ← 核心分类字段
 *   31  AF : 有无禁忌症
 *   32  AG : 不接受预防治疗的原因
 *   33  AH : 禁忌症备注
 *   34  AI : 是否开展预防治疗
 *   35  AJ : 预防性治疗方案
 *   36  AK : 其他方案备注
 *   37  AL : 是否完成治疗
 *   38  AM : 未完成原因
 *   39  AN : 6月随访日期
 *   40  AO : 6月-症状筛查日期
 *   41  AP : 6月-症状1
 *   42  AQ : 6月-症状2
 *   43  AR : 6月-影像检查日期
 *   44  AS : 6月-影像方法
 *   45  AT : 6月-影像结果
 *   46  AU : 6月-痰检日期
 *   47  AV : 6月-病原学方法
 *   48  AW : 6月-病原学结果
 *   49  AX : 6月-筛查结果
 *   50  AY : 12月随访日期
 *   51  AZ : 12月-症状筛查日期
 *   52  BA : 12月-症状1
 *   53  BB : 12月-症状2
 *   54  BC : 12月-影像检查日期
 *   55  BD : 12月-影像方法
 *   56  BE : 12月-影像结果
 *   57  BF : 12月-痰检日期
 *   58  BG : 12月-病原学方法
 *   59  BH : 12月-病原学结果
 *   60  BI : 12月-筛查结果
 *   61  BJ : 24月随访日期
 *   62  BK : 24月-症状筛查日期
 *   63  BL : 24月-症状1
 *   64  BM : 24月-症状2
 *   65  BN : 24月-影像检查日期
 *   66  BO : 24月-影像方法
 *   67  BP : 24月-影像结果
 *   68  BQ : 24月-痰检日期
 *   69  BR : 24月-病原学方法
 *   70  BS : 24月-病原学结果
 *   71  BT : 24月-筛查结果
 *   72  BU : 备注
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

    /** 密切接触者登记日期，是 6/12/24 月随访到期的计算基准 */
    @ExcelProperty(index = 8, converter = FlexibleLocalDateConverter.class)
    private LocalDate registrationDate;

    // index 9(季度)、10(时间间隔) 自动生成，跳过

    // ===== 接触者基本信息（L-R，index 11-17）=====
    @ExcelProperty(index = 11)
    private String name;

    @ExcelProperty(index = 12)
    private String idNumber;

    @ExcelProperty(index = 13)
    private Integer age;

    // index 14(年龄组) 自动生成，跳过

    @ExcelProperty(index = 15)
    private String phone;

    @ExcelProperty(index = 16)
    private String contactType;

    @ExcelProperty(index = 17)
    private String contactPlace;

    // ===== 初次筛查信息（S-AE，index 18-30）=====
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

    /** 最终筛查结果（AE列，核心分类字段）：活动性肺结核/潜伏感染者/未做/未发现异常 */
    @ExcelProperty(index = 30)
    private String finalScreeningResult;

    // ===== 预防性治疗信息（AF-AM，index 31-38）=====
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

    // ===== 6月随访（AN-AX，index 39-49）=====
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

    /** 6月随访筛查结果：活动性肺结核/潜伏感染者/其它/未发现异常/未做 */
    @ExcelProperty(index = 49)
    private String followup6Result;

    // ===== 12月随访（AY-BI，index 50-60）=====
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

    /** 12月随访筛查结果 */
    @ExcelProperty(index = 60)
    private String followup12Result;

    // ===== 24月随访（BJ-BT，index 61-71）=====
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

    /** 24月随访筛查结果 */
    @ExcelProperty(index = 71)
    private String followup24Result;

    @ExcelProperty(index = 72)
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
}
