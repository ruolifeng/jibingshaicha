package cn.luyou.service;

import cn.luyou.model.QuestionnaireAnswer;
import cn.luyou.model.QuestionnaireResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

public interface QuestionnaireResponseService {

    String generateQrUrl(Long questionnaireId);

    Map<String, Object> initFill(Long questionnaireId);

    void submitAnswers(Long responseId, List<QuestionnaireAnswer> answers, String ip, String token);

    Page<QuestionnaireResponse> responsePage(Long questionnaireId, int pageNum, int pageSize, Integer status, Boolean submitted);

    Map<String, Object> responseDetail(Long responseId);

    void exportResponses(Long questionnaireId, Integer status, Boolean submitted, String format, HttpServletResponse response);

    List<Map<String, Object>> listExportData(Long questionnaireId, Integer status, Boolean submitted);

    Map<String, Object> statistics(Long questionnaireId);

    void updateResponseStatus(Long responseId, Integer status);
}
