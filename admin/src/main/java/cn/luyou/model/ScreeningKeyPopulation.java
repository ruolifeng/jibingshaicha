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
 * 重点人群筛查数据（V4 模板）
 * Excel 导入字段范围：A-AK（序号~感染筛查结果），不含胸片/诊断/预防治疗列
 * 胸片检查与诊断结果由潜伏感染追踪阶段录入后系统回写到本表对应列
 * 预防性治疗字段由督导表归档后系统回写
 * V4 变更：人群分类由合并列改为各列独立 是/否，筛查方法由两列改为一列
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_key_population")
public class ScreeningKeyPopulation extends BaseEntity {

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
    private String phone;
    @ExcelProperty(index = 12)
    private String householdAddress;
    @ExcelProperty(index = 13)
    private String currentAddress;

    // 人群分类（V4：各列独立，列14-21）
    @ExcelProperty(index = 14)
    private String crowdCategoryClose;
    @ExcelProperty(index = 15)
    private String crowdCategoryStudent;
    @ExcelProperty(index = 16)
    private String crowdCategoryTeacher;
    @ExcelProperty(index = 17)
    private String crowdCategoryElder;
    @ExcelProperty(index = 18)
    private String crowdCategoryDiabetes;
    @ExcelProperty(index = 19)
    private String crowdCategoryDual;
    @ExcelProperty(index = 20)
    private String crowdCategoryTbHist;
    @ExcelProperty(index = 21)
    private String crowdCategoryNormal;

    // 症状筛查（列22-30）
    @ExcelProperty(index = 22)
    private String hasSuspiciousSymptoms;
    @ExcelProperty(index = 23)
    private String cough;
    @ExcelProperty(index = 24)
    private String hemoptysis;
    @ExcelProperty(index = 25)
    private String fever;
    @ExcelProperty(index = 26)
    private String chestPain;
    @ExcelProperty(index = 27)
    private String nightSweats;
    @ExcelProperty(index = 28)
    private String appetiteLoss;
    @ExcelProperty(index = 29)
    private String fatigue;
    @ExcelProperty(index = 30)
    private String weightLoss;

    // 感染筛查（列31-35，V4方法改为单列）
    @ExcelProperty(index = 31)
    private String hasInfectionScreen;
    @ExcelProperty(index = 32)
    private LocalDate screenDate;
    @ExcelProperty(index = 33)
    private String screenMethod;
    @ExcelProperty(index = 34)
    private String screenResult;
    @ExcelProperty(index = 35)
    private String infectionResult;

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    // V4 AK-AP（index 36-41）：胸片与诊断，在潜伏感染追踪到位后由系统回写
    private String hasChestXray;
    private LocalDate chestXrayDate;
    private String chestXrayResult;
    private String diagnosisFirst;
    private String diagnosisHalfYear;
    private String diagnosisOneYear;

    // V4 AQ-AV（index 42-47）：预防性治疗情况，由督导表归档后系统写入
    private String preventivePlan;
    private LocalDate preventiveStartDate;
    private LocalDate preventiveEndDate;
    private String preventiveResult;
    private String preventiveManager;

    // V4 重点人群模板共 48 列（A-AV），最后一列 AV(index47) 为随访管理人员，无 remark 列
    private String remark;

    /** 是否潜伏管理者：0否 1是 */
    private Integer isLatent;
    private String uploadBatch;
}
