package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("notice")
public class Notice extends BaseEntity {

    /** 通知单类型：latent / patient */
    private String noticeType;
    private String populationType;
    /** 关联业务ID */
    private Long bizId;

    // ===== 基本信息（两类通知单共用）=====
    private String patientName;
    private String idNumber;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String phone;
    private String crowdCategory;
    /** 民族 */
    private String ethnicity;
    /** 现居住地址 */
    private String currentAddress;
    /** 户籍地址 */
    private String householdAddress;
    /** 登记号（潜伏感染者通知单填写，同步至潜伏感染主表） */
    private String registrationNo;

    // ===== 检查信息（两类共用）=====
    private LocalDate chestXrayDate;
    /** 胸片检查结果：正常/异常/未查 */
    private String chestXrayResult;
    /** 治疗机构 */
    private String treatmentInstitution;
    /** 下发时间 */
    private LocalDate issuedTime;

    // ===== 潜伏感染者通知单专用字段 =====
    /** 感染检测时间 */
    private LocalDate infectionDate;
    /** 感染检查方法：PPD/EC/IGRA */
    private String infectionMethod;
    /** 感染检查结果 */
    private String infectionResultValue;
    /** 治疗方案（潜伏）：免费药品/生物制剂/未治疗 */
    private String latentTreatmentOption;

    // ===== 患者通知单专用字段 =====
    /** 患者类型：初治/复治 */
    private String patientType;
    /** 管理方式：全程督导/强化督导/全程管理/未管理 */
    private String managementMethod;
    /** 治疗方案（患者，FDC-2HRZE/4HR 等7个方案） */
    private String treatmentPlan;
    /** 耐药情况：耐药/非耐药/未检测（患者通知单） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String drugResistance;
    /** 个体化方案详情 */
    private String customPlanDetail;
    /** 痰涂片：未出结果/阴性/阳性/未做/未知 */
    private String sputumSmear;
    /** 痰培养 */
    private String sputumCulture;
    /** 分子检查 */
    private String molecularTest;
    /** 病理学检查 */
    private String pathologyTest;
    /** 其他注意事项 */
    private String otherNotes;
    /** 服药管理单位（来自病案信息，患者通知单） */
    private String medicationManagementUnit;
    /** 备注（手动填写，患者通知单） */
    private String remark;

    // ===== 流转字段 =====
    private Long senderId;
    private Long receiverOrgId;

    /** 非数据库字段：下发人姓名（接口查询时填充） */
    @TableField(exist = false)
    private String senderName;
    /** 非数据库字段：下发人机构名称 */
    @TableField(exist = false)
    private String senderOrgName;
    /** 非数据库字段：接收人姓名 */
    @TableField(exist = false)
    private String receiverName;
    /** 非数据库字段：接收人机构名称 */
    @TableField(exist = false)
    private String receiverOrgName;
    /** 状态：1已发送 2已确认 */
    private Integer status;
    private LocalDateTime sentTime;
    private LocalDateTime confirmedTime;
    /** 是否已发送通知单48h超时提醒（0否 1是） */
    private Integer timeoutNotified;
    /** 是否已发送督导表72h超时提醒（0否 1是） */
    private Integer supervisionTimeoutNotified;
    /** 是否已发送首次随访72h超时提醒（0否 1是） */
    private Integer visitTimeoutNotified;
}
