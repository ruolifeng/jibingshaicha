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
@TableName("screening_close_contact")
public class ScreeningCloseContact extends BaseEntity {

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
    @ExcelProperty("职业")
    private String occupation;
    @ExcelProperty("联系电话")
    private String phone;
    @ExcelProperty("户籍所在地")
    private String householdAddress;
    @ExcelProperty("现住址")
    private String currentAddress;
    @ExcelProperty("接触类型")
    private String contactType;
    @ExcelProperty("原患者姓名")
    private String sourcePatientName;
    @ExcelProperty("原患者确诊日期")
    private LocalDate sourcePatientConfirmDate;
    @ExcelProperty("原患者身份证号")
    private String sourcePatientIdNumber;
    @ExcelProperty("首次筛查日期")
    private LocalDate firstScreenDate;
    @ExcelProperty("首次症状筛查结果")
    private String firstSymptomResult;
    @ExcelProperty("半年后筛查日期")
    private LocalDate halfYearScreenDate;
    @ExcelProperty("半年后症状筛查结果")
    private String halfYearSymptomResult;
    @ExcelProperty("一年后筛查日期")
    private LocalDate oneYearScreenDate;
    @ExcelProperty("一年后症状筛查结果")
    private String oneYearSymptomResult;
    @ExcelProperty("感染检查筛查日期")
    private LocalDate infectionScreenDate;
    @ExcelProperty("感染检查方法")
    private String infectionMethod;
    @ExcelProperty("结果")
    private String screenResult;
    @ExcelProperty("感染筛查结果")
    private String infectionResult;
    @ExcelProperty("是否进行胸片检查")
    private String hasChestXray;
    @ExcelProperty("胸片检查日期")
    private LocalDate chestXrayDate;
    @ExcelProperty("胸片检查结果")
    private String chestXrayResult;
    @ExcelProperty("筛查结果-首次")
    private String firstScreeningResult;
    @ExcelProperty("筛查结果-半年后")
    private String halfYearScreeningResult;
    @ExcelProperty("筛查结果-一年后")
    private String oneYearScreeningResult;
    @ExcelProperty("诊断结果")
    private String diagnosisResult;
    @ExcelProperty("是否进行预防性治疗")
    private String hasPreventiveTreatment;
    @ExcelProperty("预防性治疗方案")
    private String preventivePlan;
    @ExcelProperty("预防性治疗开始时间")
    private LocalDate preventiveStartDate;
    @ExcelProperty("预防性治疗完成时间")
    private LocalDate preventiveEndDate;
    @ExcelProperty("预防性治疗结果")
    private String preventiveResult;
    @ExcelProperty("随访管理人员")
    private String preventiveManager;
    @ExcelProperty("惠民方式")
    private String benefitMethod;
    @ExcelProperty("备注")
    private String remark;

    /** 是否潜伏管理者：0否 1是 */
    private Integer isLatent;
    private String uploadBatch;
}
