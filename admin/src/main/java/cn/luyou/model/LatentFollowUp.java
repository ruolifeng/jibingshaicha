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
@TableName("latent_follow_up")
public class LatentFollowUp extends BaseEntity {

    private Long latentInfectionId;
    private LocalDate followUpDate;
    private String followUpType;
    private String content;
    private String result;
    private String operator;
}
