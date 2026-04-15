package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("follow_up_visit")
public class FollowUpVisit extends BaseEntity {

    private Long patientId;
    private String populationType;
    /** 随访次数（第几次，由后端按 patient_id 自动累加写入） */
    private Integer visitSeq;
    /** 随访时间 */
    private LocalDate visitDate;
    /** 随访方式：门诊/家庭 */
    private String visitMethod;
    /** 随访情况 */
    private String visitSituation;
    /** 备注 */
    private String remarks;
    /** 附件图片URL */
    private String attachmentUrl;
    private Long filledBy;
}
