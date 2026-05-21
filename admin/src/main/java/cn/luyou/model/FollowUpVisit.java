package cn.luyou.model;

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
@TableName("follow_up_visit")
public class FollowUpVisit extends BaseEntity {

    private Long patientId;
    private String populationType;
    /** 随访次数（第几次，由后端按 patient_id 自动累加写入） */
    private Integer visitSeq;
    /** 随访时间 */
    private LocalDate visitDate;
    /** 治疗月序（第X月）—— V15 */
    private Integer treatmentMonth;
    /** 督导人员（1医生/2家属/3自服药/4其他）—— V15 */
    private String supervisor;
    /** 督导人员-其他 —— V15 */
    private String supervisorOther;
    /** 随访方式：1门诊/2家庭/3电话 */
    private String visitMethod;
    /** 症状及体征（多选 0-11 编号，逗号分隔）—— V15 */
    private String symptoms;
    /** 症状-其它 —— V15 */
    private String symptomsOther;
    /** 吸烟量（支/天）—— V15 */
    private String smokingAmount;
    /** 饮酒量（两/天）—— V15 */
    private String drinkingAmount;
    /** 化疗方案 —— V15 */
    private String chemotherapyPlan;
    /** 用法（1每日/2间歇）—— V15 */
    private String medicationUsage;
    /** 药品剂型（1固定剂量/2散装/3板式/4注射）—— V15 */
    private String drugForm;
    /** 漏服药次数 —— V15 */
    private Integer missedDoses;
    /** 药物不良反应（1无/2有）—— V15 */
    private String adverseReaction;
    /** 不良反应详情 —— V15 */
    private String adverseReactionDetail;
    /** 并发症或合并症（1无/2有）—— V15 */
    private String complication;
    /** 并发症详情 —— V15 */
    private String complicationDetail;
    /** 转诊-科别 —— V15 */
    private String referralDepartment;
    /** 转诊-原因 —— V15 */
    private String referralReason;
    /** 2周内随访结果 —— V15 */
    private String referralTwoWeekResult;
    /** 处理意见 —— V15 */
    private String handlingOpinion;
    /** 下次随访时间 —— V15 */
    private LocalDate nextVisitDate;
    /** 随访医生签名 —— V15 */
    private String doctorSignature;
    /** 停止治疗时间 —— V15 */
    private LocalDate stopTreatmentDate;
    /** 停止治疗原因（完成疗程/死亡/丢失/转入耐多药）—— V15 */
    private String stopTreatmentReason;
    /** 全程管理-应访视次数 —— V15 */
    private Integer shouldVisitCount;
    /** 全程管理-实际访视次数 —— V15 */
    private Integer actualVisitCount;
    /** 全程管理-应服药次数 —— V15 */
    private Integer shouldDoseCount;
    /** 全程管理-实际服药次数 —— V15 */
    private Integer actualDoseCount;
    /** 服药率（%）—— V15 */
    private String medicationRate;
    /** 评估医生签名 —— V15 */
    private String evaluatorSignature;
    /** 备注 */
    private String remarks;
    /** 附件图片URL（JSON数组字符串，2~6 张）—— V15 */
    private String attachmentUrls;
    /** 状态：0草稿 1已完成 */
    private Integer status;

    /** @deprecated V15 起按新模板字段化；本字段保留兼容历史数据。 */
    @Deprecated
    private String visitSituation;
    /** @deprecated V15 起改用 attachmentUrls；本字段保留兼容历史数据。 */
    @Deprecated
    private String attachmentUrl;
    private Long filledBy;
}
