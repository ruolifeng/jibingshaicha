package cn.luyou.model.vo;

import cn.luyou.model.dto.QuestionnaireFieldGroupDTO;
import lombok.Data;

import java.util.List;

@Data
public class QuestionnaireConfigVO {

    private String code;

    private String title;

    private String subtitle;

    private Boolean enabled;

    private String populationType;

    private List<QuestionnaireFieldGroupDTO> groups;
}
