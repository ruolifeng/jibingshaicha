package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingVisitSupervisionVO {
    /** follow_up / supervision */
    private String type;
    private Long bizId;
    private String name;
    private LocalDate dueDate;
    /** 距计划日剩余天数：7 / 3 / 1 */
    private Integer leadDays;
}
