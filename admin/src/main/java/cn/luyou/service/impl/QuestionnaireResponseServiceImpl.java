package cn.luyou.service.impl;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.QuestionMapper;
import cn.luyou.mapper.QuestionnaireAnswerMapper;
import cn.luyou.mapper.QuestionnaireMapper;
import cn.luyou.mapper.QuestionnaireResponseMapper;
import cn.luyou.model.Question;
import cn.luyou.model.Questionnaire;
import cn.luyou.model.QuestionnaireAnswer;
import cn.luyou.model.QuestionnaireResponse;
import cn.luyou.service.QuestionnaireResponseService;
import cn.luyou.service.QuestionnaireService;
import cn.luyou.service.export.QuestionnaireSpssExportHelper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionnaireResponseServiceImpl implements QuestionnaireResponseService {

    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionMapper questionMapper;
    private final QuestionnaireResponseMapper responseMapper;
    private final QuestionnaireAnswerMapper answerMapper;
    private final QuestionnaireService questionnaireService;
    private final ObjectMapper objectMapper;
    private final QuestionnaireSpssExportHelper spssExportHelper;

    @Value("${app.base-url:http://localhost:3333}")
    private String appBaseUrl;

    @Override
    public String generateQrUrl(Long questionnaireId) {
        questionnaireService.getById(questionnaireId);
        return appBaseUrl + "/#/fill/" + questionnaireId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initFill(Long questionnaireId) {
        Questionnaire q = questionnaireService.getById(questionnaireId);
        if (q.getStatus() == null || q.getStatus() != 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "问卷未发布或已关闭");
        }

        q.setTotalVisits((q.getTotalVisits() == null ? 0 : q.getTotalVisits()) + 1);
        q.setUpdatedAt(LocalDateTime.now());
        questionnaireMapper.updateById(q);

        List<Question> questions = questionnaireService.listQuestions(questionnaireId);

        String token = UUID.randomUUID().toString().replace("-", "");
        QuestionnaireResponse resp = new QuestionnaireResponse();
        resp.setQuestionnaireId(questionnaireId);
        resp.setAccessToken(token);
        resp.setStatus(0);
        resp.setStartTime(LocalDateTime.now());
        resp.setCreatedAt(LocalDateTime.now());
        responseMapper.insert(resp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("responseId", resp.getId());
        result.put("accessToken", token);
        result.put("questionnaire", q);
        result.put("questions", questions);
        result.put("prefillData", Collections.emptyMap());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAnswers(Long responseId, List<QuestionnaireAnswer> answers, String ip, String token) {
        QuestionnaireResponse resp = responseMapper.selectById(responseId);
        if (resp == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "回收记录不存在");
        }
        if (resp.getStatus() != null && resp.getStatus() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "问卷已提交，不可重复提交");
        }
        if (resp.getAccessToken() != null && !resp.getAccessToken().equals(token)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "提交令牌无效");
        }

        for (QuestionnaireAnswer a : answers) {
            a.setId(null);
            a.setResponseId(responseId);
            answerMapper.insert(a);
        }

        resp.setStatus(1);
        resp.setSubmitTime(LocalDateTime.now());
        resp.setRespondentIp(ip);
        if (resp.getStartTime() != null) {
            resp.setDurationSeconds((int) ChronoUnit.SECONDS.between(resp.getStartTime(), resp.getSubmitTime()));
        }
        // 时长规则：<10分钟标记为不良样本
        if (resp.getDurationSeconds() != null && resp.getDurationSeconds() < 600) {
            resp.setStatus(2);
        }
        responseMapper.updateById(resp);

        Questionnaire q = questionnaireMapper.selectById(resp.getQuestionnaireId());
        if (q != null) {
            q.setTotalResponses((q.getTotalResponses() == null ? 0 : q.getTotalResponses()) + 1);
            q.setUpdatedAt(LocalDateTime.now());
            questionnaireMapper.updateById(q);
        }
    }

    @Override
    public Page<QuestionnaireResponse> responsePage(Long questionnaireId, int pageNum, int pageSize, Integer status, Boolean submitted) {
        questionnaireService.getById(questionnaireId);
        LambdaQueryWrapper<QuestionnaireResponse> query = new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .orderByDesc(QuestionnaireResponse::getCreatedAt);
        if (Boolean.TRUE.equals(submitted)) {
            query.in(QuestionnaireResponse::getStatus, 1, 2);
        } else if (status != null) {
            query.eq(QuestionnaireResponse::getStatus, status);
        }
        return responseMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public Map<String, Object> responseDetail(Long responseId) {
        QuestionnaireResponse resp = responseMapper.selectById(responseId);
        if (resp == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "回收记录不存在");
        }
        questionnaireService.getById(resp.getQuestionnaireId());

        List<QuestionnaireAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireAnswer>()
                        .eq(QuestionnaireAnswer::getResponseId, responseId));
        List<Question> questions = questionnaireService.listQuestions(resp.getQuestionnaireId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("response", resp);
        result.put("answers", answers);
        result.put("questions", questions);
        return result;
    }

    @Override
    public List<Map<String, Object>> listExportData(Long questionnaireId, Integer status, Boolean submitted) {
        questionnaireService.getById(questionnaireId);
        LambdaQueryWrapper<QuestionnaireResponse> query = new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .orderByAsc(QuestionnaireResponse::getSubmitTime)
                .orderByAsc(QuestionnaireResponse::getId);
        if (Boolean.TRUE.equals(submitted)) {
            query.in(QuestionnaireResponse::getStatus, 1, 2);
        } else if (status != null) {
            query.eq(QuestionnaireResponse::getStatus, status);
        }

        List<QuestionnaireResponse> responses = responseMapper.selectList(query);
        if (responses.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> responseIds = responses.stream().map(QuestionnaireResponse::getId).collect(Collectors.toSet());
        List<QuestionnaireAnswer> allAnswers = new ArrayList<>();
        List<Long> idList = new ArrayList<>(responseIds);
        final int batchSize = 500;
        for (int i = 0; i < idList.size(); i += batchSize) {
            List<Long> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
            allAnswers.addAll(answerMapper.selectList(
                    new LambdaQueryWrapper<QuestionnaireAnswer>()
                            .in(QuestionnaireAnswer::getResponseId, batch)));
        }

        Map<Long, List<QuestionnaireAnswer>> answersByResponse = allAnswers.stream()
                .collect(Collectors.groupingBy(QuestionnaireAnswer::getResponseId));

        List<Map<String, Object>> result = new ArrayList<>(responses.size());
        for (QuestionnaireResponse resp : responses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("response", resp);
            item.put("answers", answersByResponse.getOrDefault(resp.getId(), Collections.emptyList()));
            result.add(item);
        }
        return result;
    }

    @Override
    public void exportResponses(
            Long questionnaireId, Integer status, Boolean submitted, String format, HttpServletResponse response) {
        questionnaireService.getById(questionnaireId);
        List<Question> questions = questionnaireService.listQuestions(questionnaireId);
        QuestionnaireSpssExportHelper.ExportValueFormat valueFormat =
                QuestionnaireSpssExportHelper.ExportValueFormat.parse(format);

        LambdaQueryWrapper<QuestionnaireResponse> query = new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .orderByAsc(QuestionnaireResponse::getSubmitTime)
                .orderByAsc(QuestionnaireResponse::getId);
        if (Boolean.TRUE.equals(submitted)) {
            query.in(QuestionnaireResponse::getStatus, 1, 2);
        } else if (status != null) {
            query.eq(QuestionnaireResponse::getStatus, status);
        }
        List<QuestionnaireResponse> responses = responseMapper.selectList(query);

        Map<Long, List<QuestionnaireAnswer>> answersByResponse = Collections.emptyMap();
        if (!responses.isEmpty()) {
            List<Long> idList = responses.stream().map(QuestionnaireResponse::getId).toList();
            List<QuestionnaireAnswer> allAnswers = new ArrayList<>();
            final int batchSize = 500;
            for (int i = 0; i < idList.size(); i += batchSize) {
                List<Long> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
                allAnswers.addAll(answerMapper.selectList(
                        new LambdaQueryWrapper<QuestionnaireAnswer>()
                                .in(QuestionnaireAnswer::getResponseId, batch)));
            }
            answersByResponse = allAnswers.stream()
                    .collect(Collectors.groupingBy(QuestionnaireAnswer::getResponseId));
        }

        QuestionnaireSpssExportHelper.ExportSchema schema =
                spssExportHelper.buildSchema(questions, answersByResponse, valueFormat);
        List<List<Object>> dataList = spssExportHelper.buildDataRows(schema, questions, responses, answersByResponse);
        List<List<String>> headList = schema.buildHeadRows();

        String fileName = "问卷数据_" + questionnaireId + ".xlsx";
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ExcelWriter writer = EasyExcel.write(baos).build()) {
                WriteSheet sheet = EasyExcel.writerSheet("问卷数据").head(headList).build();
                writer.write(dataList, sheet);
            }
            byte[] content = baos.toByteArray();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            response.flushBuffer();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.SERVICE_ERROR, "导出失败");
        }
    }

    @Override
    public void updateResponseStatus(Long responseId, Integer status) {
        QuestionnaireResponse resp = responseMapper.selectById(responseId);
        if (resp == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "回收记录不存在");
        }
        questionnaireService.getById(resp.getQuestionnaireId());
        resp.setStatus(status);
        responseMapper.updateById(resp);
    }

    @Override
    public Map<String, Object> statistics(Long questionnaireId) {
        Questionnaire q = questionnaireService.getById(questionnaireId);

        long totalResp = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId));
        long submitted = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .eq(QuestionnaireResponse::getStatus, 1));
        long badSample = responseMapper.selectCount(new LambdaQueryWrapper<QuestionnaireResponse>()
                .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                .eq(QuestionnaireResponse::getStatus, 2));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVisits", q.getTotalVisits());
        stats.put("totalResponses", totalResp);
        stats.put("submitted", submitted);
        stats.put("badSample", badSample);
        stats.put("completionRate", totalResp > 0 ? Math.round((double) (submitted + badSample) / totalResp * 100) : 0);
        stats.put("choiceQuestionStats", buildChoiceQuestionStats(questionnaireId));
        return stats;
    }

    private List<Map<String, String>> parseOptionDefs(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
            List<Map<String, String>> out = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Object v = m.get("value");
                if (v == null) {
                    continue;
                }
                String value = String.valueOf(v);
                Object l = m.get("label");
                String label = l != null ? String.valueOf(l) : value;
                out.add(Map.of("value", value, "label", label));
            }
            return out;
        } catch (Exception e) {
            log.warn("解析题目选项失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> parseCheckboxAnswerValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String s = raw.trim();
        try {
            return objectMapper.readValue(s, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
        }
    }

    private static double roundPercent(long count, long base) {
        if (base <= 0) {
            return 0;
        }
        return Math.round(count * 1000.0 / base) / 10.0;
    }

    private List<Map<String, Object>> buildChoiceQuestionStats(Long questionnaireId) {
        List<QuestionnaireResponse> submittedList = responseMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireResponse>()
                        .eq(QuestionnaireResponse::getQuestionnaireId, questionnaireId)
                        .in(QuestionnaireResponse::getStatus, 1, 2)
                        .select(QuestionnaireResponse::getId));
        Set<Long> responseIds = submittedList.stream().map(QuestionnaireResponse::getId).collect(Collectors.toSet());
        long baseCount = responseIds.size();
        if (baseCount == 0) {
            return Collections.emptyList();
        }

        List<QuestionnaireAnswer> allAnswers = answerMapper.selectList(
                new LambdaQueryWrapper<QuestionnaireAnswer>()
                        .in(QuestionnaireAnswer::getResponseId, responseIds));
        Map<String, QuestionnaireAnswer> ansByRespQ = new HashMap<>(allAnswers.size());
        for (QuestionnaireAnswer a : allAnswers) {
            ansByRespQ.put(a.getResponseId() + "_" + a.getQuestionId(), a);
        }

        List<Question> choiceQs = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getQuestionnaireId, questionnaireId)
                        .in(Question::getType, "radio", "checkbox")
                        .orderByAsc(Question::getSortOrder));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : choiceQs) {
            List<Map<String, String>> optDefs = parseOptionDefs(q.getOptions());
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Map<String, String> od : optDefs) {
                counts.put(od.get("value"), 0L);
            }
            long blankCount = 0;
            long otherCount = 0;

            for (Long rid : responseIds) {
                QuestionnaireAnswer ans = ansByRespQ.get(rid + "_" + q.getId());
                String val = ans == null ? null : ans.getAnswerValue();

                if ("radio".equals(q.getType())) {
                    if (val == null || val.isBlank()) {
                        blankCount++;
                        continue;
                    }
                    String v = val.trim();
                    if (counts.containsKey(v)) {
                        counts.merge(v, 1L, Long::sum);
                    } else {
                        otherCount++;
                    }
                } else {
                    List<String> picked = parseCheckboxAnswerValues(val);
                    if (picked.isEmpty()) {
                        blankCount++;
                        continue;
                    }
                    boolean anyInvalid = false;
                    for (String p : picked) {
                        if (counts.containsKey(p)) {
                            counts.merge(p, 1L, Long::sum);
                        } else {
                            anyInvalid = true;
                        }
                    }
                    if (anyInvalid) {
                        otherCount++;
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, String> od : optDefs) {
                String value = od.get("value");
                long c = counts.getOrDefault(value, 0L);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", od.get("label"));
                row.put("value", value);
                row.put("count", c);
                row.put("percent", roundPercent(c, baseCount));
                rows.add(row);
            }
            if (blankCount > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", "未作答");
                row.put("value", "__blank__");
                row.put("count", blankCount);
                row.put("percent", roundPercent(blankCount, baseCount));
                rows.add(row);
            }
            if (otherCount > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", "其他（非预设选项）");
                row.put("value", "__other__");
                row.put("count", otherCount);
                row.put("percent", roundPercent(otherCount, baseCount));
                rows.add(row);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q.getId());
            item.put("sortOrder", q.getSortOrder());
            item.put("type", q.getType());
            item.put("title", q.getTitle() != null ? q.getTitle() : "");
            item.put("baseCount", baseCount);
            item.put("blankCount", blankCount);
            item.put("otherCount", otherCount);
            item.put("optionRows", rows);
            result.add(item);
        }
        return result;
    }
}
