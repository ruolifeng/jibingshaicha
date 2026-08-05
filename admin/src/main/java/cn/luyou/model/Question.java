package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionnaireId;
    private Integer sortOrder;
    private String type;
    private String title;
    private String description;
    private Integer required;
    /** [{label, value}] */
    private String options;
    /** {min,max,pattern,message} */
    private String validationRules;
    /** [{condition,targetQuestionId}] */
    private String logicRules;
    private Integer pageNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
