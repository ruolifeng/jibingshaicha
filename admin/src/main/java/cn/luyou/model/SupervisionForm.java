package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预防性治疗督导表（V4）
 * 新增字段：治疗完成时间、预防性治疗结果、随访管理人员
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("supervision_form")
public class SupervisionForm extends BaseEntity {

    private Long latentInfectionId;
    private String populationType;
    private String patientName;
    /** 预防性治疗开始日期 */
    private LocalDate treatmentStartDate;
    /** 预防性治疗完成时间（V4新增） */
    private LocalDate treatmentEndDate;
    private String treatmentPlan;
    /** 督导内容（JSON） */
    private String supervisionContent;
    /** 预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4新增） */
    private String preventiveResult;
    /** 预防性治疗期间随访管理人员（V4新增） */
    private String preventiveManager;
    private Long filledBy;
    /** 状态：0未填写 1已填写 2已归档 */
    private Integer status;
    private LocalDateTime archivedTime;
}
