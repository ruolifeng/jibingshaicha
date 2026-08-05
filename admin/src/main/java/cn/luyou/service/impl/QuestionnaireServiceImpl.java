package cn.luyou.service.impl;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.QuestionMapper;
import cn.luyou.mapper.QuestionnaireMapper;
import cn.luyou.model.Question;
import cn.luyou.model.Questionnaire;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.QuestionnaireService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final QuestionnaireMapper questionnaireMapper;
    private final QuestionMapper questionMapper;
    private final DepartmentService departmentService;

    @Override
    public Page<Questionnaire> page(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Questionnaire> query = new LambdaQueryWrapper<Questionnaire>()
                .isNull(Questionnaire::getTemplateType)
                .orderByDesc(Questionnaire::getCreatedAt);
        applyDepartmentScope(query);
        if (keyword != null && !keyword.isEmpty()) {
            query.like(Questionnaire::getTitle, keyword);
        }
        if (status != null) {
            query.eq(Questionnaire::getStatus, status);
        }
        return questionnaireMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public Questionnaire getById(Long id) {
        Questionnaire q = questionnaireMapper.selectById(id);
        if (q == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "问卷不存在");
        }
        // 未登录（公开填写）跳过部门校验
        if (BaseContext.getCurrentId() != null) {
            assertAccessible(q);
        }
        return q;
    }

    @Override
    public Questionnaire create(Questionnaire q) {
        q.setId(null);
        q.setDepartmentId(BaseContext.getCurrentDepartmentId());
        q.setStatus(0);
        q.setTotalVisits(0);
        q.setTotalResponses(0);
        q.setCreatedBy(BaseContext.getCurrentId());
        q.setIsDeleted(0);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        q.setTemplateType(null);
        questionnaireMapper.insert(q);
        return q;
    }

    @Override
    public Questionnaire update(Long id, Questionnaire q) {
        Questionnaire existing = getById(id);
        existing.setTitle(q.getTitle());
        existing.setDescription(q.getDescription());
        existing.setCategory(q.getCategory());
        existing.setStartTime(q.getStartTime());
        existing.setEndTime(q.getEndTime());
        existing.setUpdatedAt(LocalDateTime.now());
        questionnaireMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        questionnaireMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, int status) {
        Questionnaire q = getById(id);
        q.setStatus(status);
        q.setUpdatedAt(LocalDateTime.now());
        questionnaireMapper.updateById(q);
    }

    @Override
    public List<Question> listQuestions(Long questionnaireId) {
        if (BaseContext.getCurrentId() != null) {
            getById(questionnaireId);
        }
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuestionnaireId, questionnaireId)
                .orderByAsc(Question::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuestions(Long questionnaireId, List<Question> questions) {
        getById(questionnaireId);
        List<Question> existing = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuestionnaireId, questionnaireId));
        Set<Long> existingIds = existing.stream().map(Question::getId).collect(Collectors.toSet());
        Set<Long> keptIds = new HashSet<>();

        int order = 1;
        LocalDateTime now = LocalDateTime.now();
        for (Question q : questions) {
            q.setQuestionnaireId(questionnaireId);
            q.setSortOrder(order++);
            q.setUpdatedAt(now);
            if (q.getId() != null && existingIds.contains(q.getId())) {
                questionMapper.updateById(q);
                keptIds.add(q.getId());
            } else {
                q.setId(null);
                q.setCreatedAt(now);
                questionMapper.insert(q);
            }
        }

        for (Long id : existingIds) {
            if (!keptIds.contains(id)) {
                questionMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Questionnaire saveAsTemplate(Long questionnaireId, String templateTitle, String templateType) {
        Questionnaire src = getById(questionnaireId);
        if (!"public".equals(templateType) && !"private".equals(templateType)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "模板类型不合法，请传 public 或 private");
        }
        Questionnaire tpl = new Questionnaire();
        // 公用/专属均挂当前部门，列表按部门树隔离（不再全系统共享）
        tpl.setDepartmentId(BaseContext.getCurrentDepartmentId());
        tpl.setTitle(templateTitle != null ? templateTitle : src.getTitle());
        tpl.setDescription(src.getDescription());
        tpl.setCategory(src.getCategory());
        tpl.setTemplateType(templateType);
        tpl.setStatus(0);
        tpl.setTotalVisits(0);
        tpl.setTotalResponses(0);
        tpl.setCreatedBy(BaseContext.getCurrentId());
        tpl.setIsDeleted(0);
        tpl.setCreatedAt(LocalDateTime.now());
        tpl.setUpdatedAt(LocalDateTime.now());
        questionnaireMapper.insert(tpl);

        copyQuestions(questionnaireId, tpl.getId());
        return tpl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Questionnaire createFromTemplate(Long templateId, String newTitle) {
        Questionnaire tpl = getById(templateId);
        checkTemplateVisible(tpl);
        Questionnaire q = new Questionnaire();
        q.setDepartmentId(BaseContext.getCurrentDepartmentId());
        q.setTitle(newTitle != null && !newTitle.isEmpty() ? newTitle : tpl.getTitle());
        q.setDescription(tpl.getDescription());
        q.setCategory(tpl.getCategory() != null ? tpl.getCategory() : "custom");
        q.setTemplateType(null);
        q.setStatus(0);
        q.setTotalVisits(0);
        q.setTotalResponses(0);
        q.setCreatedBy(BaseContext.getCurrentId());
        q.setIsDeleted(0);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        questionnaireMapper.insert(q);

        copyQuestions(templateId, q.getId());
        return q;
    }

    @Override
    public Page<Questionnaire> templateList(int pageNum, int pageSize, String templateType) {
        LambdaQueryWrapper<Questionnaire> query = new LambdaQueryWrapper<Questionnaire>()
                .isNotNull(Questionnaire::getTemplateType)
                .orderByDesc(Questionnaire::getCreatedAt);

        if ("public".equals(templateType) || "private".equals(templateType)) {
            query.eq(Questionnaire::getTemplateType, templateType);
        }
        // 公用/专属均按部门树隔离
        applyDepartmentScope(query);

        return questionnaireMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public void deleteTemplate(Long templateId) {
        Questionnaire tpl = questionnaireMapper.selectById(templateId);
        if (tpl == null || tpl.getTemplateType() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "模板不存在");
        }
        assertAccessible(tpl);
        questionnaireMapper.deleteById(templateId);
    }

    private void copyQuestions(Long fromQuestionnaireId, Long toQuestionnaireId) {
        List<Question> srcQuestions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuestionnaireId, fromQuestionnaireId)
                .orderByAsc(Question::getSortOrder));
        int order = 1;
        LocalDateTime now = LocalDateTime.now();
        for (Question q : srcQuestions) {
            Question nq = new Question();
            nq.setQuestionnaireId(toQuestionnaireId);
            nq.setSortOrder(order++);
            nq.setType(q.getType());
            nq.setTitle(q.getTitle());
            nq.setDescription(q.getDescription());
            nq.setRequired(q.getRequired());
            nq.setOptions(q.getOptions());
            nq.setValidationRules(q.getValidationRules());
            nq.setLogicRules(q.getLogicRules());
            nq.setPageNumber(q.getPageNumber());
            nq.setCreatedAt(now);
            nq.setUpdatedAt(now);
            questionMapper.insert(nq);
        }
    }

    private void checkTemplateVisible(Questionnaire tpl) {
        if (tpl.getTemplateType() == null) {
            return;
        }
        assertAccessible(tpl);
    }

    private void applyDepartmentScope(LambdaQueryWrapper<Questionnaire> query) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long deptId = BaseContext.getCurrentDepartmentId();
        List<Long> deptIds = departmentService.getDescendantIds(deptId);
        if (deptIds == null || deptIds.isEmpty()) {
            if (deptId != null) {
                query.eq(Questionnaire::getDepartmentId, deptId);
            } else {
                query.apply("1 = 0");
            }
            return;
        }
        query.in(Questionnaire::getDepartmentId, deptIds);
    }

    private void assertAccessible(Questionnaire q) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long deptId = BaseContext.getCurrentDepartmentId();
        List<Long> deptIds = departmentService.getDescendantIds(deptId);
        if (deptIds == null || deptIds.isEmpty()) {
            if (deptId == null || !deptId.equals(q.getDepartmentId())) {
                throw new ServiceException(StatusEnum.FORBIDDEN, "无权访问该问卷");
            }
            return;
        }
        if (q.getDepartmentId() == null || !deptIds.contains(q.getDepartmentId())) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权访问该问卷");
        }
    }
}
