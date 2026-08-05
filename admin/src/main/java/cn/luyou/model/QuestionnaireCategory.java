package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("questionnaire_category")
public class QuestionnaireCategory extends BaseEntity {
    /** 分类编码（写入 questionnaire.category） */
    private String code;
    /** 分类名称 */
    private String name;
    /** 排序，越小越靠前 */
    private Integer sort;
}
