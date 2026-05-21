package cn.luyou.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionnaireFieldGroupDTO {

    private String group;

    private List<QuestionnaireFieldDTO> fields;
}
