package cn.luyou.service;

import cn.luyou.model.QuestionnaireCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface QuestionnaireCategoryService extends IService<QuestionnaireCategory> {

    List<QuestionnaireCategory> listAll();

    QuestionnaireCategory create(QuestionnaireCategory category);

    QuestionnaireCategory update(Long id, QuestionnaireCategory category);

    void delete(Long id);
}
