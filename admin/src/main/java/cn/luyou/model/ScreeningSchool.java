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
@TableName("screening_school")
public class ScreeningSchool extends BaseEntity {

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

    @ExcelProperty("现地址")
    private String currentAddress;

    @ExcelProperty("学校类型")
    private String schoolType;

    @ExcelProperty("学校名称")
    private String schoolName;

    @ExcelProperty("班级（院系）")
    private String className;

    @ExcelProperty("既往结核病史")
    private String tbHistory;

    @ExcelProperty("密切接触史")
    private String closeContactHistory;

    @ExcelProperty("结核病可疑症状")
    private String suspiciousSymptoms;

    @ExcelProperty("是否进行感染筛")
    private String hasInfectionScreen;

    @ExcelProperty("感染筛查日期")
    private LocalDate screenDate;

    @ExcelProperty("方法")
    private String screenMethod;

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

    @ExcelProperty("痰涂片")
    private String sputumSmear;

    @ExcelProperty("分子生物学")
    private String molecularBiology;

    @ExcelProperty("诊断结果")
    private String diagnosisResult;

    @ExcelProperty("预防性治疗")
    private String preventiveTreatment;

    @ExcelProperty("备注")
    private String remark;

    /** 是否潜伏管理者：0否 1是（系统自动判定） */
    private Integer isLatent;

    private String uploadBatch;
}
