package cn.luyou.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 密接人群筛查数据（V4 模板 — 三轮独立筛查）
 *
 * V4 结构（A-BC 共52列）：
 *   A-P  (0-15)  : 密接者基本信息
 *   Q-S  (16-18) : 原患者信息
 *   T-AB (19-27) : 首次感染筛查+胸片+诊断
 *   AC-AK(28-36) : 半年后感染筛查+胸片+诊断
 *   AL-AT(37-45) : 一年后感染筛查+胸片+诊断
 *   AU-AZ(46-51) : 潜伏感染者管理情况（是否治疗/方案/开始/完成/结果/管理人员）
 *   BA   (52)     : 惠民方式
 *   BB   (53)     : 备注
 *
 * 业务规则：
 *   - 感染筛查结果阳性 → 进行胸片 → 得出诊断 → 进入潜伏/患者管理 → 停止后续轮
 *   - 感染筛查结果阴性 → 跳过胸片 → 继续下一轮
 *   - 三轮均阴 → 直接归档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_close_contact")
public class ScreeningCloseContact extends BaseEntity {

    // ===== 密接者基本信息（A-P，index 0-15）=====
    @ExcelProperty(index = 1)
    private String year;
    @ExcelProperty(index = 2)
    private String city;
    @ExcelProperty(index = 3)
    private String district;
    @ExcelProperty(index = 4)
    private String name;
    @ExcelProperty(index = 5)
    private String gender;
    @ExcelProperty(index = 6)
    private LocalDate birthDate;
    @ExcelProperty(index = 7)
    private Integer age;
    @ExcelProperty(index = 8)
    private String idType;
    @ExcelProperty(index = 9)
    private String idNumber;
    @ExcelProperty(index = 10)
    private String ethnicity;
    @ExcelProperty(index = 11)
    private String occupation;
    @ExcelProperty(index = 12)
    private String phone;
    @ExcelProperty(index = 13)
    private String householdAddress;
    @ExcelProperty(index = 14)
    private String currentAddress;
    @ExcelProperty(index = 15)
    private String contactType;

    // ===== 原患者信息（Q-S，index 16-18）=====
    @ExcelProperty(index = 16)
    private String sourcePatientName;
    @ExcelProperty(index = 17)
    private LocalDate sourcePatientConfirmDate;
    @ExcelProperty(index = 18)
    private String sourcePatientIdNumber;

    // ===== 首次筛查（T-AB，index 19-27）=====
    @ExcelProperty(index = 19)
    private LocalDate firstScreenDate;
    @ExcelProperty(index = 20)
    private String firstSymptomResult;
    @ExcelProperty(index = 21)
    private String firstInfectionMethod;
    @ExcelProperty(index = 22)
    private String firstScreenResult;
    @ExcelProperty(index = 23)
    private String firstInfectionResult;
    @ExcelProperty(index = 24)
    private String firstHasChestXray;
    @ExcelProperty(index = 25)
    private LocalDate firstChestXrayDate;
    @ExcelProperty(index = 26)
    private String firstChestXrayResult;
    @ExcelProperty(index = 27)
    private String firstDiagnosis;

    // ===== 半年后筛查（AC-AK，index 28-36）=====
    @ExcelProperty(index = 28)
    private LocalDate halfYearScreenDate;
    @ExcelProperty(index = 29)
    private String halfYearSymptomResult;
    @ExcelProperty(index = 30)
    private String halfYearInfectionMethod;
    @ExcelProperty(index = 31)
    private String halfYearScreenResult;
    @ExcelProperty(index = 32)
    private String halfYearInfectionResult;
    @ExcelProperty(index = 33)
    private String halfYearHasChestXray;
    @ExcelProperty(index = 34)
    private LocalDate halfYearChestXrayDate;
    @ExcelProperty(index = 35)
    private String halfYearChestXrayResult;
    @ExcelProperty(index = 36)
    private String halfYearDiagnosis;

    // ===== 一年后筛查（AL-AT，index 37-45）=====
    @ExcelProperty(index = 37)
    private LocalDate oneYearScreenDate;
    @ExcelProperty(index = 38)
    private String oneYearSymptomResult;
    @ExcelProperty(index = 39)
    private String oneYearInfectionMethod;
    @ExcelProperty(index = 40)
    private String oneYearScreenResult;
    @ExcelProperty(index = 41)
    private String oneYearInfectionResult;
    @ExcelProperty(index = 42)
    private String oneYearHasChestXray;
    @ExcelProperty(index = 43)
    private LocalDate oneYearChestXrayDate;
    @ExcelProperty(index = 44)
    private String oneYearChestXrayResult;
    @ExcelProperty(index = 45)
    private String oneYearDiagnosis;

    // ===== 潜伏感染者管理情况（AU-AZ，index 46-51）=====
    @ExcelProperty(index = 46)
    private String hasPreventiveTreatment;
    @ExcelProperty(index = 47)
    private String preventivePlan;
    @ExcelProperty(index = 48)
    private LocalDate preventiveStartDate;
    @ExcelProperty(index = 49)
    private LocalDate preventiveEndDate;
    @ExcelProperty(index = 50)
    private String preventiveResult;
    @ExcelProperty(index = 51)
    private String preventiveManager;

    // ===== 惠民方式 + 备注（BA-BB，index 52-53）=====
    @ExcelProperty(index = 52)
    private String benefitMethod;
    @ExcelProperty(index = 53)
    private String remark;

    /** 是否潜伏管理者：0否 1是（系统自动判定） */
    private Integer isLatent;

    /** 阳性轮次：1首次 2半年后 3一年后（系统自动判定） */
    private Integer activeRound;

    private String uploadBatch;
}
