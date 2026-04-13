package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDate treatmentStartDate;
    private String treatmentPlan;
    /** 督导内容（JSON） */
    private String supervisionContent;
    private Long filledBy;
    /** 状态：0未填写 1已填写 2已归档 */
    private Integer status;
    private LocalDateTime archivedTime;
}
