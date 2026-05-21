package cn.luyou.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionnaireFieldDTO {

    private String key;

    private String label;

    /** input / select / date / number / textarea */
    private String type;

    private Boolean required;

    private List<String> options;

    /** 条件显示：当指定字段等于指定值时展示 */
    private ShowWhenDTO showWhen;
}
