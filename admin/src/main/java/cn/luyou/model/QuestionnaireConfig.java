package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 筛查问卷配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("questionnaire_config")
public class QuestionnaireConfig extends BaseEntity {

    /** 问卷编码，如 school */
    private String code;

    /** 问卷标题 */
    private String title;

    /** 问卷副标题/说明 */
    private String subtitle;

    /** 是否开启填写：0否 1是 */
    private Integer enabled;

    /** 关联人群类型，写入筛查表时使用 */
    private String populationType;

    /** 字段分组配置 JSON */
    private String fieldsJson;
}
