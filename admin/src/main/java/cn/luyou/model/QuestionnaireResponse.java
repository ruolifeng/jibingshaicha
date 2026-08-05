package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("questionnaire_response")
public class QuestionnaireResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionnaireId;
    private String accessToken;
    private String respondentIp;
    /** 0-进行中 1-已提交 2-不良样本 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
}
