package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.QuestionnaireCategoryMapper;
import cn.luyou.mapper.QuestionnaireMapper;
import cn.luyou.model.Questionnaire;
import cn.luyou.model.QuestionnaireCategory;
import cn.luyou.service.QuestionnaireCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuestionnaireCategoryServiceImpl
        extends ServiceImpl<QuestionnaireCategoryMapper, QuestionnaireCategory>
        implements QuestionnaireCategoryService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,49}$");

    private final QuestionnaireMapper questionnaireMapper;

    @Override
    public List<QuestionnaireCategory> listAll() {
        return lambdaQuery()
                .orderByAsc(QuestionnaireCategory::getSort)
                .orderByAsc(QuestionnaireCategory::getCreateTime)
                .list();
    }

    @Override
    public QuestionnaireCategory create(QuestionnaireCategory category) {
        String name = StrUtil.trim(category.getName());
        String code = StrUtil.trim(category.getCode());
        if (StrUtil.isBlank(name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类名称不能为空");
        }
        if (StrUtil.isBlank(code)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类编码不能为空");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类编码需以字母开头，仅含字母数字下划线，最长50");
        }
        long exists = lambdaQuery().eq(QuestionnaireCategory::getCode, code).count();
        if (exists > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类编码已存在");
        }
        QuestionnaireCategory entity = new QuestionnaireCategory();
        entity.setName(name);
        entity.setCode(code);
        entity.setSort(category.getSort() != null ? category.getSort() : 0);
        save(entity);
        return entity;
    }

    @Override
    public QuestionnaireCategory update(Long id, QuestionnaireCategory category) {
        QuestionnaireCategory existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类不存在");
        }
        String name = StrUtil.trim(category.getName());
        if (StrUtil.isBlank(name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类名称不能为空");
        }
        existing.setName(name);
        if (category.getSort() != null) {
            existing.setSort(category.getSort());
        }
        // 编码创建后不可改，避免已有问卷引用失效
        updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        QuestionnaireCategory existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分类不存在");
        }
        Long used = questionnaireMapper.selectCount(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getCategory, existing.getCode()));
        if (used != null && used > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该分类仍被问卷使用，无法删除");
        }
        // 释放唯一编码：用固定短前缀 + id，避免截断后碰撞，且软删除后可重建同编码
        existing.setCode("d" + id);
        updateById(existing);
        removeById(id);
    }
}
