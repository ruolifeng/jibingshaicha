package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 潜伏感染管理表（V4）
 * 新增字段：追踪到位后的胸片检查信息、首次诊断结果、密接阳性轮次
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("latent_infection")
public class LatentInfection extends BaseEntity {

    private Long screeningId;
    private String populationType;
    /** 人群分类（手动新增/导入持久化；有筛查关联时由筛查表回填展示） */
    private String crowdCategory;
    private String name;
    private String idNumber;
    private String gender;
    private Integer age;
    private String phone;
    /** 户籍地址（手动新增/导入持久化；有筛查关联时由筛查表回填展示） */
    private String householdAddress;
    /** 现住地址 */
    private String currentAddress;
    /** 联系电话与联系人关系 */
    private String phoneContactRelation;
    /** 感染筛查日期（手动新增/导入） */
    private LocalDate infectionScreenDate;
    private String infectionResult;
    /** 追踪状态：0待追踪 1到位 2未到位 3其他 4强制结束 */
    private Integer trackingStatus;
    private Integer notInPlaceCount;
    /** 追踪情况 */
    private String trackingRemark;
    /** 追踪历史 JSON（每次追踪的状态、时间、备注） */
    private String trackingHistoryJson;
    /** 真实到位时间（手动录入，日期精度） */
    private LocalDate actualArrivalDate;
    /** 备注 */
    private String remark;

    // ===== V4 新增：追踪到位后录入胸片与诊断 =====
    /** 是否进行胸片检查（是/否） */
    private String hasChestXray;
    private LocalDate chestXrayDate;
    /** 胸片检查结果：正常/异常/未查 */
    private String chestXrayResult;
    /** 首次诊断结果：排除/疑似肺结核/潜伏感染者/确诊患者/其他 */
    private String diagnosisFirst;
    /** 筛查表中的待确认诊断（非持久化，仅待诊断列表展示/预填） */
    @TableField(exist = false)
    private String screeningDiagnosisFirst;
    /** 密接阳性轮次：1首次 2半年后 3一年后（仅密接人群使用） */
    private Integer activeRound;

    /** 转诊结果：excluded/other/confirmed/suspected/latent */
    private String referralResult;
    private String referralRemark;
    /** 真实转诊时间（手动录入，日期精度） */
    private LocalDate actualReferralDate;
    private String diagnosisResult;
    /** 治疗阶段：0未开始 1预防治疗中 2已结案 */
    private Integer treatmentPhase;
    /** 服药状态：1按要求服药 2不服药 */
    private Integer medicationStatus;
    private Integer archived;
    private LocalDateTime archivedTime;
    /** 归档备注（如：转出待确认、已转出） */
    private String archiveRemark;
    /** 所属部门ID */
    private Long departmentId;
    /** 录入人用户ID（手动新增/导入时写入，五级数据权限） */
    private Long creatorId;
    /** Excel 导入行号（与模板物理行号一致，用于列表按原 Excel 顺序展示） */
    private Integer importRowNo;
    /** 录入用户名（非数据库字段，查询时按 creatorId 填充） */
    @TableField(exist = false)
    private String creatorUsername;
    /** 转出复制来源潜伏感染ID */
    private Long sourceLatentId;

    /** 是否已发送潜伏者通知单（非持久化字段） */
    @TableField(exist = false)
    private Boolean noticeSent;

    /** 通知单状态（非持久化）：0草稿 1已发送 2已确认 null无通知单 */
    @TableField(exist = false)
    private Integer noticeStatus;

    /** 通知单ID（非持久化） */
    @TableField(exist = false)
    private Long noticeId;

    /** 督导表是否已完成（status=2，非持久化字段） */
    @TableField(exist = false)
    private Boolean supervisionCompleted;

    /** 督导表状态：0未填写 1已提交 2已归档（非持久化字段） */
    @TableField(exist = false)
    private Integer supervisionStatus;

    /** 治疗完成情况（来自督导表，非持久化） */
    @TableField(exist = false)
    private String treatmentCompletionStatus;

    // ===== 通知单自动回填字段（非持久化）=====
    @TableField(exist = false)
    private LocalDate birthDate;
    @TableField(exist = false)
    private String ethnicity;
    /** 感染检测时间（通知单展示，优先筛查表，否则取 infectionScreenDate） */
    @TableField(exist = false)
    private LocalDate screenDate;
    /** 感染检查方法 */
    @TableField(exist = false)
    private String screenMethod;
    /** 感染检查结果 */
    @TableField(exist = false)
    private String screenResult;
    /** 预防性治疗方案（通知单展示，来自筛查表） */
    @TableField(exist = false)
    private String preventivePlan;
}
