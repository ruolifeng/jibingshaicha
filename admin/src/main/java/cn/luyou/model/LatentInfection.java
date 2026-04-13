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
@TableName("latent_infection")
public class LatentInfection extends BaseEntity {

    private Long screeningId;
    private String populationType;
    private String name;
    private String idNumber;
    private String gender;
    private Integer age;
    private String phone;
    private String infectionResult;
    /** 追踪状态：0待追踪 1到位 2未到位 3其他 4强制结束 */
    private Integer trackingStatus;
    private Integer notInPlaceCount;
    private String trackingRemark;
    /** 转诊结果：excluded/other/confirmed/latent */
    private String referralResult;
    private String referralRemark;
    private String diagnosisResult;
    private Integer archived;
}
