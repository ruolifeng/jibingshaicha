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
 * 学校人群筛查数据（V4 模板）
 * 初次导入字段范围：A-Y（序号~感染筛查结果）
 * 胸片检查与诊断结果由潜伏感染追踪阶段录入，不在本表存储
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
    @ExcelProperty(index = 21)
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

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    // V4 Z-AE（index 25-30）：胸片与诊断，在潜伏感染追踪到位后由系统回写
    private String hasChestXray;
    private LocalDate chestXrayDate;
    private String chestXrayResult;
    private String diagnosisFirst;
    private String diagnosisHalfYear;
    private String diagnosisOneYear;

    // V4 AF-AK（index 31-36）：预防性治疗情况，由督导表归档后系统写入
    private String preventivePlan;
    private LocalDate preventiveStartDate;
    private LocalDate preventiveEndDate;
    private String preventiveResult;
    private String preventiveManager;

    // V4 学生模板无备注列（模板最后一列 AK=index36 为随访管理人员）
    private String remark;

    /** 是否潜伏管理者：0否 1是（系统自动判定） */
    private Integer isLatent;
    private String uploadBatch;
}
