package cn.luyou.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 转诊详情 VO（含发送方信息） */
@Data
public class ReferralDetailVO {

    private Long id;
    private String bizType;
    private String populationType;
    private String moduleType;
    private String subjectName;

    /** 业务摘要（JSON 字符串，前端自行解析展示） */
    private String summary;

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
