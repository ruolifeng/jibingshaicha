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
@TableName("latent_check")
public class LatentCheck extends BaseEntity {

    private Long latentInfectionId;
    private LocalDate checkDate;
    /** 检查周期：3个月/6个月/12个月 */
    private String checkPeriod;
    /** 检查结果：未发病/发病/其他 */
    private String checkResult;
    private String content;
    private String operator;
}
