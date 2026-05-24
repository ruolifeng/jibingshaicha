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
 * 重点人群筛查数据（V4 模板）
 * Excel 导入字段范围：A-AN（序号~首次诊断结果），预防治疗列由系统回写
 * 胸片检查与诊断结果可由 Excel 直接导入，导入后同步进入疑似结核管理
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
    @ExcelProperty(index = 6, converter = FlexibleLocalDateConverter.class)
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
    private String townshipCommunity;
    @ExcelProperty(index = 14)
    private String currentAddress;

    // 人群分类（各列独立，列15-22）
    @ExcelProperty(index = 15)
    private String crowdCategoryClose;
    @ExcelProperty(index = 16)
    private String crowdCategoryStudent;
    @ExcelProperty(index = 17)
    private String crowdCategoryTeacher;
    @ExcelProperty(index = 18)
    private String crowdCategoryElder;
    @ExcelProperty(index = 19)
    private String crowdCategoryDiabetes;
    @ExcelProperty(index = 20)
    private String crowdCategoryDual;
    @ExcelProperty(index = 21)
    private String crowdCategoryTbHist;
    @ExcelProperty(index = 22)
    private String crowdCategoryNormal;

    // 症状筛查（列23-31）
    @ExcelProperty(index = 23)
    private String hasSuspiciousSymptoms;
    @ExcelProperty(index = 24)
    private String cough;
    @ExcelProperty(index = 25)
    private String hemoptysis;
    @ExcelProperty(index = 26)
    private String fever;
    @ExcelProperty(index = 27)
    private String chestPain;
    @ExcelProperty(index = 28)
    private String nightSweats;
    @ExcelProperty(index = 29)
    private String appetiteLoss;
    @ExcelProperty(index = 30)
    private String fatigue;
    @ExcelProperty(index = 31)
    private String weightLoss;

    // 感染筛查（列32-36）
    @ExcelProperty(index = 32)
    private String hasInfectionScreen;
    @ExcelProperty(index = 33, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;
    @ExcelProperty(index = 34)
    private String screenMethod;
    @ExcelProperty(index = 35)
    private String screenResult;
    @ExcelProperty(index = 36)
    private String infectionResult;

    // ===== 胸片与诊断（列37-40）：支持 Excel 直接导入 =====
    @ExcelProperty(index = 37)
    private String hasChestXray;
    @ExcelProperty(index = 38, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;
    @ExcelProperty(index = 39)
    private String chestXrayResult;
    @ExcelProperty(index = 40)
    private String diagnosisFirst;

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    @ExcelIgnore
    private String diagnosisHalfYear;
    @ExcelIgnore
    private String diagnosisOneYear;

    // V4 AQ-AV（index 42-47）：预防性治疗情况，由督导表归档后系统写入
    @ExcelIgnore
    private String hasPreventiveTreatment;
    @ExcelIgnore
    private String preventivePlan;
    @ExcelIgnore
    private LocalDate preventiveStartDate;
    @ExcelIgnore
    private LocalDate preventiveEndDate;
    @ExcelIgnore
    private String preventiveResult;
    @ExcelIgnore
    private String preventiveManager;

    @ExcelIgnore
    private String remark;

    /** 是否潜伏管理者：0否 1是 */
    @ExcelIgnore
    private Integer isLatent;
    @ExcelIgnore
    private String uploadBatch;
    /** 所属部门ID */
    @ExcelIgnore
    private Long departmentId;
    /**
     * V16 数据来源类型：keyPopulation（重点人群）/ regular（疫情筛查）
     * 用于区分同一张表中的两类数据，影响 latent_infection.population_type 的赋值。
     */
    @ExcelIgnore
    private String sourceType;
}
