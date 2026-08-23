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
    /** 距计划日剩余天数：0~7 */
    private Integer leadDays;
    /** 管理人对应机构（通知单接收人/填写人所属机构，或管理单位） */
    private String managerOrgName;
}
