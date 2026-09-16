package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统定时消息提醒开关配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_reminder_config")
public class SysMessageReminderConfig extends BaseEntity {

    /** 提醒编码（唯一） */
    private String code;

    /** 展示名称 */
    private String name;

    /** 说明 */
    private String description;

    /** 触发说明（如每天 08:00） */
    private String scheduleHint;

    /** 是否开启：0否 1是 */
    private Integer enabled;

    /** 排序 */
    private Integer sort;
}
