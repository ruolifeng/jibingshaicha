package cn.luyou.service;

import cn.luyou.model.Question;
import cn.luyou.model.Questionnaire;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface QuestionnaireService {

    Page<Questionnaire> page(int pageNum, int pageSize, String keyword, Integer status);

    Questionnaire getById(Long id);

    Questionnaire create(Questionnaire q);

    Questionnaire update(Long id, Questionnaire q);

    void delete(Long id);

    void updateStatus(Long id, int status);

    List<Question> listQuestions(Long questionnaireId);

    void saveQuestions(Long questionnaireId, List<Question> questions);

    Questionnaire saveAsTemplate(Long questionnaireId, String templateTitle, String templateType);

    Questionnaire createFromTemplate(Long templateId, String newTitle);

    Page<Questionnaire> templateList(int pageNum, int pageSize, String templateType);

    void deleteTemplate(Long templateId);
}
