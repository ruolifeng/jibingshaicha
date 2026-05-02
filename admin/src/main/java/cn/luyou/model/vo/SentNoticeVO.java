package cn.luyou.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 已发送通知单列表 VO */
@Data
public class SentNoticeVO {

    private Long id;

    /** 通知单类型：latent / patient */
    private String noticeType;

    /** 人群类型 */
    private String populationType;

    /** 患者姓名（通知单内容摘要） */
    private String patientName;

    /** 发送者ID */
    private Long senderId;

    /** 发送者姓名 */
    private String senderName;

    /** 发送者机构 */
    private String senderOrgName;

    /** 接收者ID */
    private Long receiverOrgId;

    /** 接收者姓名 */
    private String receiverName;

    /** 接收者机构 */
    private String receiverOrgName;

    /** 状态：1已发送 2已确认 */
    private Integer status;

    /** 发送时间 */
    private LocalDateTime sentTime;

    /** 确认接收时间 */
    private LocalDateTime confirmedTime;
}
