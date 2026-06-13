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
 * 学校人群筛查数据（学生筛查模板）
 * Excel 导入字段范围：A-AE（序号~诊断结果），预防治疗列由系统回写
 * 胸片检查与诊断结果可由 Excel 直接导入，导入后同步进入疑似结核管理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_school")
public class ScreeningSchool extends BaseEntity {

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

    @ExcelProperty(value = "证件号", index = 9)
    private String idNumber;

    @ExcelProperty(value = "民族", index = 10)
    private String ethnicity;

    @ExcelProperty(value = "联系电话", index = 11)
    private String phone;

    @ExcelProperty(value = "户籍地址", index = 12)
    private String householdAddress;

    @ExcelProperty(value = "现住址", index = 13)
    private String currentAddress;

    @ExcelProperty(value = "学校类型", index = 14)
    private String schoolType;

    @ExcelProperty(value = "学校名称", index = 15)
    private String schoolName;

    @ExcelProperty(value = "班级", index = 16)
    private String className;

    @ExcelProperty(value = "既往结核病史", index = 17)
    private String tbHistory;

    @ExcelProperty(value = "密切接触史", index = 18)
    private String closeContactHistory;

    @ExcelProperty(value = "有无可疑症状", index = 19)
    private String suspiciousSymptoms;

    /** 是否进行感染筛（列20） */
    @ExcelProperty(value = "是否感染筛查", index = 20)
    private String hasInfectionScreen;

    /** 感染筛查日期（列21） */
    @ExcelProperty(value = "感染筛查日期", index = 21, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;

    /** 方法（PPD/EC/IGRA，列22） */
    @ExcelProperty(value = "感染筛查方法", index = 22)
    private String screenMethod;

    /** 结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性，列23） */
    @ExcelProperty(value = "筛查结果", index = 23)
    private String screenResult;

    /** 感染筛查结果（V4：PPD阴性/PPD+/PPD++/PPD+++/EC阴性/EC阳性/IGRA阴性/IGRA阳性，列24） */
    @ExcelProperty(value = "感染筛查结果", index = 24)
    private String infectionResult;

    // ===== 胸片、病原学与诊断（Z-AE，index 25-30）：支持 Excel 直接导入 =====
    @ExcelProperty(value = "是否胸片检查", index = 25)
    private String hasChestXray;
    @ExcelProperty(value = "胸片检查日期", index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;
    @ExcelProperty(value = "胸片检查结果", index = 27)
    private String chestXrayResult;
    @ExcelProperty(value = "痰涂片结果", index = 28)
    private String sputumSmearResult;
    @ExcelProperty(value = "分子生物学结果", index = 29)
    private String molecularBiologyResult;
    @ExcelProperty(value = "诊断结果", index = 30)
    private String diagnosisFirst;

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    @ExcelIgnore
    private String diagnosisHalfYear;
    @ExcelIgnore
    private String diagnosisOneYear;

    // V4 AF-AK（index 31-36）：预防性治疗情况，由督导表归档后系统写入
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

    /** 是否潜伏管理者：0否 1是（系统自动判定） */
    @ExcelIgnore
    private Integer isLatent;
    @ExcelIgnore
    private String uploadBatch;
    /** 所属部门ID */
    @ExcelIgnore
    private Long departmentId;
}
