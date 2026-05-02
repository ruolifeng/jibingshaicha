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
 * 学校人群筛查数据（V4 模板）
 * Excel 导入字段范围：A-AC（序号~首次诊断结果），预防治疗列由系统回写
 * 胸片检查与诊断结果可由 Excel 直接导入，导入后同步进入疑似结核管理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_school")
public class ScreeningSchool extends BaseEntity {

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
    private String currentAddress;

    @ExcelProperty(index = 14)
    private String schoolType;

    @ExcelProperty(index = 15)
    private String schoolName;

    @ExcelProperty(index = 16)
    private String className;

    @ExcelProperty(index = 17)
    private String tbHistory;

    @ExcelProperty(index = 18)
    private String closeContactHistory;

    @ExcelProperty(index = 19)
    private String suspiciousSymptoms;

    /** 是否进行感染筛（列20） */
    @ExcelProperty(index = 20)
    private String hasInfectionScreen;

    /** 感染筛查日期（列21） */
    @ExcelProperty(index = 21, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;

    /** 方法（PPD/EC/IGRA，列22） */
    @ExcelProperty(index = 22)
    private String screenMethod;

    /** 结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性，列23） */
    @ExcelProperty(index = 23)
    private String screenResult;

    /** 感染筛查结果（V4：PPD阴性/PPD+/PPD++/PPD+++/EC阴性/EC阳性/IGRA阴性/IGRA阳性，列24） */
    @ExcelProperty(index = 24)
    private String infectionResult;

    // ===== 胸片与诊断（Z-AC，index 25-28）：支持 Excel 直接导入 =====
    @ExcelProperty(index = 25)
    private String hasChestXray;
    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;
    @ExcelProperty(index = 27)
    private String chestXrayResult;
    @ExcelProperty(index = 28)
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
