package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预防性治疗督导表（V5）
 * V5 重大改造：按照 Excel 模板《潜伏感染预防性治疗督导表》字段完整重构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("supervision_form")
public class SupervisionForm extends BaseEntity {

    private Long latentInfectionId;
    private String populationType;
    private String patientName;
    /** 第几次督导表（status>=1 时由后端自动累加） */
    private Integer formSeq;

    /** 类别：密接/新生筛查/65岁以上老年人/糖尿病人/双感/其他 */
    private String category;
    /** 性别 */
    private String gender;
    /** 年龄 */
    private Integer age;
    /** 电话号码 */
    private String phone;
    /** 电话备注（非本人电话时说明） */
    private String phoneRemark;
    /** 现住址 */
    private String currentAddress;
    /** 户籍地址（V18新增） */
    private String householdAddress;
    /** 身份证号（V18新增） */
    private String idNumber;
    /** 出生日期（V18新增） */
    private String birthDate;
    /** 民族（V18新增） */
    private String ethnicity;
    /** 管理单位（V18新增） */
    private String managingUnit;
    /** 是否进行预防性治疗：是/否（V22新增） */
    private String hasPreventiveTreatment;
    /** 督导医生（V18新增） */
    private String supervisingDoctor;

    /** 预防性治疗开始日期 */
    private LocalDate treatmentStartDate;
    /** 治疗方案（含新增"不服药"） */
    private String treatmentPlan;

    /** 督导内容（V4旧字段，兼容保留） */
    private String supervisionContent;

    /**
     * 督导记录（JSON 数组，V5新增）
     * 例：[{"time":"2024-01-01","content":"...","method":"电话","remark":"..."}]
     */
    private String supervisionRecords;

    /** 治疗完成情况：完成治疗/失败/死亡/失访/不良反应停药/未评估 */
    private String treatmentCompletionStatus;
    /** 全疗程规律治疗评价 — 中断用药：有/无 */
    private String interruptMedication;
    /** 中断次数（interruptMedication=有时填写） */
    private Integer interruptCount;
    /** 全程应用药次数 */
    private Integer totalDoses;
    /** 实际用药次数 */
    private Integer actualDoses;
    /** 用药率（%） */
    private String medicationRate;
    /** 预防性治疗完成（结束疗程）时间 */
    private LocalDate treatmentEndDate;

    /** 预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4旧字段，兼容保留） */
    private String preventiveResult;
    /** 预防性治疗期间随访管理人员（V4旧字段，兼容保留） */
    private String preventiveManager;

    /** 督导管理人员类型（V5新增） */
    private String managerType;
    /** 督导管理人员姓名（V5新增） */
    private String managerName;

    /** 备注（V5新增） */
    private String remark;
    /** 附件（JSON 数组，存储图片/文件 URL） */
    private String attachmentUrls;

    /** 填写人ID */
    private Long filledBy;
    /** 状态：0草稿 1已提交 2已归档 */
    private Integer status;
    private LocalDateTime archivedTime;

    /** 非数据库字段：当前用户是否可修改（列表接口填充） */
    @TableField(exist = false)
    private Boolean editable;
}
