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
 * 推介追踪记录表（V17）
 * biz_mode=recommend 为推介流程，biz_mode=track 为直接追踪流程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("referral_tracking")
public class ReferralTracking extends BaseEntity {

    /** 业务模式：recommend=推介 / track=追踪 */
    private String bizMode;

    /** 数据来源：manual=手动录入 epidemic=大疫情导入 */
    private String sourceType;

    // ===== 基本信息（手动录入）=====
    private String name;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String idType;
    private String idNumber;
    private String ethnicity;
    private String phone;
    private String householdAddress;
    private String currentAddress;
    private String crowdCategory;
    /** 推介原因（bizMode=recommend 时手动录入） */
    private String recommendReason;
    /** 推介单位名称（创建时快照，默认取填报用户所属机构名称） */
    private String recommendUnitName;
    /** 填写用户名称（创建时快照，默认取填报用户姓名） */
    private String fillUserName;
    /** 追踪原因（bizMode=track 时手动录入） */
    private String trackReason;

    // ===== 大疫情导入字段（sourceType=epidemic 时使用）=====
    /** 卡片ID */
    private String cardId;
    /** 患儿家长姓名 */
    private String parentName;
    /** 患者工作单位 */
    private String workplace;
    /** 乡镇（筛选/展示） */
    private String township;
    /** 病例分类 */
    private String caseCategory;
    /** 疾病名称 */
    private String diseaseName;
    /** 报告单位 */
    private String reportUnit;
    /** 报告卡录入时间 */
    private LocalDateTime reportCardTime;
    /** 大疫情备注 */
    private String epidemicRemark;
    /** 导入批次号 */
    private String uploadBatch;
    /** Excel 导入行号（与模板物理行号一致，用于列表按原 Excel 顺序展示） */
    private Integer importRowNo;

    // ===== 推介专用字段（bizMode=recommend 时使用）=====
    /** 接收推介的一至五级用户ID */
    private Long receiverUserId;
    /** 接收推介的用户所在部门ID（自动派生） */
    private Long receiverDeptId;
    /** 推介状态：0未发送 1已发送 2已接受 3已拒绝 */
    private Integer recommendStatus;
    private String rejectedReason;
    private LocalDateTime recommendSentTime;
    private LocalDateTime recommendConfirmTime;
    /** 是否共同追踪：0否 1是（接收方开启后，发起方与接收方均可追踪，次数合并计算） */
    private Integer jointTracking;
    /** 开启共同追踪时间 */
    private LocalDateTime jointTrackingTime;
    /**
     * 大疫情跨镇导入确认：0无需 1待区县三级确认 2已确认 3已拒绝。
     * 独立于 recommendStatus，避免确认后被剔出追踪列表。
     */
    private Integer crossTownConfirmStatus;
    /** 跨镇确认/拒绝时间 */
    private LocalDateTime crossTownConfirmTime;

    // ===== 追踪 =====
    /** 追踪状态：0待追踪 1到位 2未到位 3其他 4强制结束 */
    private Integer trackingStatus;
    private Integer notInPlaceCount;
    private String trackingRemark;
    /** 到位时间（追踪状态变为到位时记录，系统时间） */
    private LocalDateTime arrivalTime;
    /** 真实到位时间（手动录入，日期精度） */
    private LocalDate actualArrivalDate;
    /** 追踪过程记录 JSON：[{attempt,status,trackTime,reason}] */
    private String trackingHistoryJson;

    // ===== 到位后补录 =====
    private String hasInfectionScreen;
    private LocalDate screenDate;
    private String screenMethod;
    private String screenResult;
    private String infectionResult;
    private String hasChestXray;
    private LocalDate chestXrayDate;
    private String chestXrayResult;
    /** 症状筛查 JSON（键值对形式存储多个症状及结果） */
    private String symptomsJson;

    // ===== 诊断 =====
    /** 诊断结果：排除 / 确诊患者 / 潜伏感染者 / 其他 */
    private String diagnosisResult;
    /** 诊断结果选择其他时的补充说明 */
    private String diagnosisRemark;
    private LocalDateTime diagnosisTime;

    // ===== 归集去向 =====
    private Integer archived;
    /** 确诊患者时对应的 patient.id */
    private Long targetPatientId;
    /** 潜伏感染者时对应的 latent_infection.id */
    private Long targetLatentId;

    private Long departmentId;
    private Long creatorId;

    // ===== 非持久化字段（查询时填充）=====
    @TableField(exist = false)
    private String receiverUserName;
    @TableField(exist = false)
    private String creatorUserName;
    @TableField(exist = false)
    private String entryUnitName;
}
