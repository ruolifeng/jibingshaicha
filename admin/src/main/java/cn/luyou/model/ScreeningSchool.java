package cn.luyou.model;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 学校人群筛查数据（学生筛查模板）
 * 对齐《2026年秋季新生入学结核病筛查记录表》；预防治疗列由系统回写
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("screening_school")
public class ScreeningSchool extends BaseEntity {

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String year;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reportingOrg;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String city;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String township;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String name;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gender;

    /** 历史字段：新模板不再导入，库表保留 */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate birthDate;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer age;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String idType;

    @ExcelIgnore
    private String idNumber;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String ethnicity;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String participatedScreening;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String phone;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String householdAddress;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentAddress;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String schoolType;

    /** 是否寄宿制：寄宿制/非寄宿制/大学/其他 */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String boardingType;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String schoolName;

    /** 年级 */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gradeName;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String className;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tbHistory;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String closeContactHistory;

    /** 汇总：症状三列任一为「有」则为有 */
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String suspiciousSymptoms;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symptomCough;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symptomHemoptysis;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symptomOther;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String hasInfectionScreen;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate screenDate;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String screenMethod;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String screenResult;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String infectionResult;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String hasChestXray;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String chestXrayMethod;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate chestXrayDate;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String chestXrayResult;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sputumSmearResult;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String molecularBiologyResult;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sputumCultureResult;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String diagnosisFirst;

    // ===== 以下字段由系统回写，不参与 Excel 导入 =====
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String diagnosisHalfYear;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String diagnosisOneYear;

    // V4 AF-AK（index 31-36）：预防性治疗情况，由潜伏感染者结案进入历史患者后系统写入
    @ExcelIgnore
    private String hasPreventiveTreatment;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preventivePlan;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate preventiveStartDate;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate preventiveEndDate;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preventiveResult;
    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String preventiveManager;

    @ExcelIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /** 是否潜伏管理者：0否 1是（系统自动判定） */
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
    /** 录入人用户ID */
    @ExcelIgnore
    private Long creatorId;
    /** 录入用户名 */
    @ExcelIgnore
    private String creatorUsername;
}
