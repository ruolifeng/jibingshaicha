package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 随访/督导到期提醒发送记录，防止同一计划日同一提前天数重复发送 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("visit_supervision_reminder_log")
public class VisitSupervisionReminderLog extends BaseEntity {

    /** follow_up / supervision */
    private String bizType;
    /** 患者 ID 或潜伏感染 ID */
    private Long bizId;
    /** 产生下次日期的随访/督导记录 ID */
    private Long sourceId;
    private LocalDate dueDate;
    /** 7 / 3 / 1 */
    private Integer leadDays;
}
