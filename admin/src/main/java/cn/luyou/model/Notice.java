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
@TableName("notice")
public class Notice extends BaseEntity {

    /** 通知单类型：latent / patient */
    private String noticeType;
    private String populationType;
    /** 关联业务ID */
    private Long bizId;
    private String patientName;
    private String currentAddress;
    private String householdAddress;
    private String idNumber;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String ethnicity;
    private String crowdCategory;
    private String treatmentPlan;
    private String customPlanDetail;
    private Long senderId;
    private Long receiverOrgId;
    /** 状态：1已发送 2已确认 */
    private Integer status;
    private LocalDateTime sentTime;
    private LocalDateTime confirmedTime;
    /** 是否已发送通知单48h超时提醒（0否 1是） */
    private Integer timeoutNotified;
    /** 是否已发送督导表72h超时提醒（0否 1是） */
    private Integer supervisionTimeoutNotified;
    /** 是否已发送首次随访72h超时提醒（0否 1是） */
    private Integer visitTimeoutNotified;
}
