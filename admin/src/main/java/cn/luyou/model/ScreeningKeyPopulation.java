package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
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

    @ExcelProperty(value = "年度", index = 1)
    private String year;
    @ExcelProperty(value = "市/州", index = 2)
    private String city;
    @ExcelProperty(value = "区/县", index = 3)
    private String district;
    @ExcelProperty(value = "姓名", index = 4)
    private String name;
    @ExcelProperty(value = "性别", index = 5)
    private String gender;
    @ExcelProperty(value = "出生日期", index = 6, converter = FlexibleLocalDateConverter.class)
    private LocalDate birthDate;
    @ExcelProperty(value = "年龄", index = 7)
    private Integer age;
    @ExcelProperty(value = "证件类型", index = 8)
    private String idType;
    @ExcelProperty(value = "证件号", index = 9, converter = ExcelTextStringConverter.class)
    private String idNumber;
    @ExcelProperty(value = "民族", index = 10)
    private String ethnicity;
    @ExcelProperty(value = "联系电话", index = 11, converter = ExcelTextStringConverter.class)
    private String phone;
    @ExcelProperty(value = "户籍地址", index = 12)
    private String householdAddress;
    @ExcelProperty(value = "乡镇/社区", index = 13)
    private String townshipCommunity;
    @ExcelProperty(value = "现住址", index = 14)
    private String currentAddress;

    // 人群分类（各列独立，列15-22）
    @ExcelProperty(value = "密切接触者", index = 15)
    private String crowdCategoryClose;
    @ExcelProperty(value = "学生", index = 16)
    private String crowdCategoryStudent;
    @ExcelProperty(value = "教师", index = 17)
    private String crowdCategoryTeacher;
    @ExcelProperty(value = "老年人", index = 18)
    private String crowdCategoryElder;
    @ExcelProperty(value = "糖尿病患者", index = 19)
    private String crowdCategoryDiabetes;
    @ExcelProperty(value = "双重感染者", index = 20)
    private String crowdCategoryDual;
    @ExcelProperty(value = "既往结核病史", index = 21)
    private String crowdCategoryTbHist;
    @ExcelProperty(value = "普通人群", index = 22)
    private String crowdCategoryNormal;

    // 症状筛查（列23-31）
    @ExcelProperty(value = "有无可疑症状", index = 23)
    private String hasSuspiciousSymptoms;
    @ExcelProperty(value = "咳嗽", index = 24)
    private String cough;
    @ExcelProperty(value = "咯血", index = 25)
    private String hemoptysis;
    @ExcelProperty(value = "发热", index = 26)
    private String fever;
    @ExcelProperty(value = "胸痛", index = 27)
    private String chestPain;
    @ExcelProperty(value = "盗汗", index = 28)
    private String nightSweats;
    @ExcelProperty(value = "食欲减退", index = 29)
    private String appetiteLoss;
    @ExcelProperty(value = "乏力", index = 30)
    private String fatigue;
    @ExcelProperty(value = "体重减轻", index = 31)
    private String weightLoss;

    // 感染筛查（列32-36）
    @ExcelProperty(value = "是否感染筛查", index = 32)
    private String hasInfectionScreen;
    @ExcelProperty(value = "感染筛查日期", index = 33, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;
    @ExcelProperty(value = "感染筛查方法", index = 34)
    private String screenMethod;
    @ExcelProperty(value = "筛查结果", index = 35)
    private String screenResult;
    @ExcelProperty(value = "感染筛查结果", index = 36)
    private String infectionResult;

    // ===== 胸片与诊断（列37-40）：支持 Excel 直接导入 =====
    @ExcelProperty(value = "是否胸片检查", index = 37)
    private String hasChestXray;
    @ExcelProperty(value = "胸片检查日期", index = 38, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;
    @ExcelProperty(value = "胸片检查结果", index = 39)
    private String chestXrayResult;
    @ExcelProperty(value = "首次诊断结果", index = 40)
    private String diagnosisFirst;

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    @ExcelProperty(index = 41)
    private String diagnosisHalfYear;
    @ExcelProperty(index = 42)
    private String diagnosisOneYear;

    @ExcelProperty(index = 43)
    private String hasPreventiveTreatment;
    @ExcelProperty(index = 44)
    private String preventivePlan;
    @ExcelProperty(index = 45, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveStartDate;
    @ExcelProperty(index = 46, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveEndDate;
    @ExcelProperty(index = 47)
    private String preventiveResult;
    @ExcelProperty(index = 48)
    private String preventiveManager;

    @ExcelIgnore
    private String remark;

    /** 是否潜伏管理者：0否 1是 */
    @ExcelIgnore
    private Integer isLatent;
    @ExcelIgnore
    private String uploadBatch;
    /** Excel 导入行号（与模板物理行号一致，用于列表按原 Excel 顺序展示） */
    @ExcelIgnore
    private Integer importRowNo;
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
