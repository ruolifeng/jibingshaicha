package cn.luyou.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_key_population")
public class ScreeningKeyPopulation extends BaseEntity {

    @ExcelProperty("年份")
    private String year;
    @ExcelProperty("市（州）")
    private String city;
    @ExcelProperty("县（市、区）")
    private String district;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("性别")
    private String gender;
    @ExcelProperty("出生日期")
    private LocalDate birthDate;
    @ExcelProperty("年龄")
    private Integer age;
    @ExcelProperty("证件类型")
    private String idType;
    @ExcelProperty("证件号")
    private String idNumber;
    @ExcelProperty("民族")
    private String ethnicity;
    @ExcelProperty("联系电话")
    private String phone;
    @ExcelProperty("户籍所在地")
    private String householdAddress;
    @ExcelProperty("现住址")
    private String currentAddress;
    @ExcelProperty("人群分类")
    private String crowdCategory;
    @ExcelProperty("是否有可疑症状")
    private String hasSuspiciousSymptoms;
    @ExcelProperty("咳嗽咳痰")
    private String cough;
    @ExcelProperty("咯血或血痰")
    private String hemoptysis;
    @ExcelProperty("发热")
    private String fever;
    @ExcelProperty("胸痛")
    private String chestPain;
    @ExcelProperty("夜间盗汗")
    private String nightSweats;
    @ExcelProperty("食欲不振")
    private String appetiteLoss;
    @ExcelProperty("乏力")
    private String fatigue;
    @ExcelProperty("体重减轻")
    private String weightLoss;
    @ExcelProperty("是否进行感染筛")
    private String hasInfectionScreen;
    @ExcelProperty("感染筛查日期")
    private LocalDate screenDate;
    @ExcelProperty("方法1")
    private String screenMethod1;
    @ExcelProperty("方法2")
    private String screenMethod2;
    @ExcelProperty("结果")
    private String screenResult;
    @ExcelProperty("感染筛查结果")
    private String infectionResult;
    @ExcelProperty("是否进行胸片检查")
    private String hasChestXray;
    @ExcelProperty("胸片检查日期")
    private LocalDate chestXrayDate;
    @ExcelProperty("胸片结果")
    private String chestXrayResult;
    @ExcelProperty("结果判定")
    private String resultJudgment;
    @ExcelProperty("是否转诊到定点医疗机构")
    private String isReferred;
    @ExcelProperty("诊断结果")
    private String diagnosisResult;
    @ExcelProperty("是否符合预防性治疗")
    private String isEligiblePreventive;
    @ExcelProperty("是否进行预防性治疗")
    private String hasPreventiveTreatment;
    @ExcelProperty("是否规范完成预防性治疗")
    private String completedPreventive;
    @ExcelProperty("备注")
    private String remark;

    /** 是否潜伏管理者：0否 1是 */
    private Integer isLatent;
    private String uploadBatch;
}
