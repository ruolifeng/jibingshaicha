package cn.luyou.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 已发送分级诊疗列表 VO */
@Data
public class SentReferralVO {

    private Long id;
    private String bizType;
    private String populationType;
    private String moduleType;
    private String subjectName;

    private Long senderId;
    private String senderName;
    private String senderOrgName;

    private Long receiverOrgId;
    private String receiverName;
    private String receiverOrgName;

    /** 1=待确认  2=已接收  3=已拒绝 */
    private Integer status;

    private LocalDateTime sentTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime rejectedTime;
    private String rejectReason;
}
