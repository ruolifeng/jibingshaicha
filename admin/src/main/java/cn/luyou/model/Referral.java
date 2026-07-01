package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("referral")
public class Referral implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联业务记录ID */
    private Long bizId;

    /**
     * 业务类型，格式：{module}_{populationType}
     * 如 screening_school / suspected_key / latent_close / patient_school
     */
    private String bizType;

    /** 人群类型：school / key / close */
    private String populationType;

    /** 模块类型：screening / suspected / latent / patient */
    private String moduleType;

    /** 对象姓名（用于展示） */
    private String subjectName;

    /** 推送的业务摘要（JSON 格式，前端自行序列化关键字段） */
    private String summary;

    /** 发送方用户ID */
    private Long senderId;

    /** 接收方用户/部门ID */
    private Long receiverOrgId;

    /** 接收确认后在接收方生成的业务记录ID（如患者复制后的 patient.id） */
    private Long targetBizId;

    /**
     * 状态：1=待确认  2=已接收  3=已拒绝
     */
    private Integer status;

    private LocalDateTime sentTime;
    private LocalDateTime confirmedTime;
    /** 真实转诊时间（手动录入，日期精度） */
    private LocalDate actualReferralDate;
    private LocalDateTime rejectedTime;

    /** 拒绝原因 */
    private String rejectReason;

    /** 转诊原因（发送方填写） */
    private String referralReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
