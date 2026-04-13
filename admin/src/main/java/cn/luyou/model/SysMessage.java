package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    private Long senderId;
    private Long receiverId;
    private String title;
    private String content;
    /** 消息类型：notice_timeout / supervision_timeout / visit_timeout */
    private String type;
    private Long bizId;
    private Integer isRead;
}
