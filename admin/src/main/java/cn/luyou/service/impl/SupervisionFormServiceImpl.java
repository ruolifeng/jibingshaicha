package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SupervisionForm;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupervisionFormServiceImpl extends ServiceImpl<SupervisionFormMapper, SupervisionForm>
        implements SupervisionFormService {

    private static final int EDIT_DAYS_LEVEL5 = 10;

    private final LatentInfectionService latentInfectionService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;

    public SupervisionFormServiceImpl(
            @Lazy LatentInfectionService latentInfectionService,
            ScreeningSchoolMapper screeningSchoolMapper,
            ScreeningKeyPopulationMapper screeningKeyPopulationMapper,
            ScreeningCloseContactMapper screeningCloseContactMapper) {
        this.latentInfectionService = latentInfectionService;
        this.screeningSchoolMapper = screeningSchoolMapper;
        this.screeningKeyPopulationMapper = screeningKeyPopulationMapper;
        this.screeningCloseContactMapper = screeningCloseContactMapper;
    }

    @Override
    public SupervisionForm getDraft(Long latentInfectionId) {
        return lambdaQuery()
                .eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .eq(SupervisionForm::getStatus, 0)
                .orderByDesc(SupervisionForm::getUpdateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(SupervisionForm form) {
        latentInfectionService.assertLatentOperable(form.getLatentInfectionId());
        SupervisionForm existingDraft = getDraft(form.getLatentInfectionId());
        if (existingDraft != null) {
            form.setId(existingDraft.getId());
        } else {
            form.setId(null);
        }
        form.setStatus(0);
        form.setFormSeq(null);
        form.setArchivedTime(null);
        ensureFilledBy(form);
        saveOrUpdate(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSubmit(SupervisionForm form) {
        latentInfectionService.assertLatentOperable(form.getLatentInfectionId());
        form.setStatus(1);
        form.setArchivedTime(null);
        ensureFilledBy(form);
        if (form.getId() != null) {
            SupervisionForm existing = getById(form.getId());
            if (existing != null) {
                assertSubmittable(existing);
                if (Integer.valueOf(0).equals(existing.getStatus())) {
                    form.setFormSeq(nextFormSeq(form.getLatentInfectionId()));
                } else {
                    form.setFormSeq(existing.getFormSeq());
                }
                updateById(form);
                return;
            }
        }
        form.setId(null);
        form.setFormSeq(nextFormSeq(form.getLatentInfectionId()));
        save(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAndArchive(SupervisionForm form) {
        latentInfectionService.assertLatentOperable(form.getLatentInfectionId());
        form.setStatus(2);
        form.setArchivedTime(LocalDateTime.now());
        ensureFilledBy(form);

        // V5：根据治疗方案推断是否进行预防性治疗，供筛查表回写
        if (StrUtil.isBlank(form.getHasPreventiveTreatment()) && StrUtil.isNotBlank(form.getTreatmentPlan())) {
            form.setHasPreventiveTreatment("不服药".equals(form.getTreatmentPlan()) ? "否" : "是");
        }

        // V5 兼容：将新字段回填到旧字段，保持筛查表回写兼容
        if (StrUtil.isBlank(form.getPreventiveManager()) && StrUtil.isNotBlank(form.getManagingUnit())) {
            form.setPreventiveManager(form.getManagingUnit());
        }
        if (StrUtil.isBlank(form.getPreventiveManager()) && (StrUtil.isNotBlank(form.getManagerType()) || StrUtil.isNotBlank(form.getManagerName()))) {
            StringBuilder manager = new StringBuilder();
            if (StrUtil.isNotBlank(form.getManagerType())) manager.append(form.getManagerType());
            if (StrUtil.isNotBlank(form.getManagerName())) {
                if (manager.length() > 0) manager.append(" - ");
                manager.append(form.getManagerName());
            }
            form.setPreventiveManager(manager.toString());
        }
        // 若中断用药=无且完成时间存在，默认标记为规范完成
        if (StrUtil.isBlank(form.getPreventiveResult()) && "无".equals(form.getInterruptMedication()) && form.getTreatmentEndDate() != null) {
            form.setPreventiveResult("规范完成");
        }

        if (form.getId() != null) {
            SupervisionForm existing = getById(form.getId());
            if (existing != null) {
                if (Integer.valueOf(2).equals(existing.getStatus())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "该督导表已归档，不可重复归档");
                }
                if (Integer.valueOf(1).equals(existing.getStatus())) {
                    assertEditable(existing);
                }
                if (Integer.valueOf(0).equals(existing.getStatus())) {
                    form.setFormSeq(nextFormSeq(form.getLatentInfectionId()));
                } else {
                    form.setFormSeq(existing.getFormSeq());
                }
                updateById(form);
                triggerArchiveSideEffects(form);
                return;
            }
        }
        form.setId(null);
        form.setFormSeq(nextFormSeq(form.getLatentInfectionId()));
        save(form);
        triggerArchiveSideEffects(form);
    }

    @Override
    public List<SupervisionForm> listCompleted(Long latentInfectionId, Integer role) {
        LambdaQueryWrapper<SupervisionForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .ge(SupervisionForm::getStatus, 1)
                .orderByAsc(SupervisionForm::getCreateTime);
        List<SupervisionForm> list = list(wrapper);
        list.forEach(form -> form.setEditable(isEditable(role, form)));
        return list;
    }

    private int nextFormSeq(Long latentInfectionId) {
        long count = lambdaQuery()
                .eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .ge(SupervisionForm::getStatus, 1)
                .count();
        return (int) count + 1;
    }

    private boolean isEditable(Integer role, SupervisionForm form) {
        if (form == null || !Integer.valueOf(1).equals(form.getStatus())) {
            return false;
        }
        if (role == null || role != 6) {
            return true;
        }
        if (form.getCreateTime() == null) {
            return true;
        }
        return !form.getCreateTime().plusDays(EDIT_DAYS_LEVEL5).isBefore(LocalDateTime.now());
    }

    private void assertSubmittable(SupervisionForm existing) {
        if (Integer.valueOf(2).equals(existing.getStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已归档记录不可修改");
        }
        if (Integer.valueOf(1).equals(existing.getStatus())) {
            assertEditable(existing);
        }
    }

    private void assertEditable(SupervisionForm existing) {
        if (!isEditable(BaseContext.getCurrentRole(), existing)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "督导表已超过10天修改期限，请联系上级管理员");
        }
    }

    private void triggerArchiveSideEffects(SupervisionForm form) {
        if (form.getLatentInfectionId() == null) return;

        LatentInfection latent = latentInfectionService.getById(form.getLatentInfectionId());
        if (latent == null) return;

        // 督导表归档 → 潜伏感染进入"预防治疗中"阶段
        if (Integer.valueOf(0).equals(latent.getTreatmentPhase())) {
            latent.setTreatmentPhase(1);
            latentInfectionService.updateById(latent);
        }

        // V4/V5 sheet2：将预防性治疗数据回写到对应筛查表
        writeBackPreventiveToScreening(latent, form);
    }

    /**
     * 将督导表中的预防性治疗字段回写到对应的筛查管理表。
     * 学校 → screening_school (AF-AK)
     * 重点人群 → screening_key_population (AQ-AV)
     * 密接 → screening_close_contact (AU-AZ)
     */
    private void writeBackPreventiveToScreening(LatentInfection latent, SupervisionForm form) {
        Long screeningId = latent.getScreeningId();
        if (screeningId == null) return;
        String type = latent.getPopulationType();
        if (StrUtil.isBlank(type)) return;

        switch (type) {
            case "school" -> {
                ScreeningSchool s = screeningSchoolMapper.selectById(screeningId);
                if (s != null) {
                    s.setHasPreventiveTreatment(form.getHasPreventiveTreatment());
                    s.setPreventivePlan(form.getTreatmentPlan());
                    s.setPreventiveStartDate(form.getTreatmentStartDate());
                    s.setPreventiveEndDate(form.getTreatmentEndDate());
                    s.setPreventiveResult(form.getPreventiveResult());
                    s.setPreventiveManager(form.getPreventiveManager());
                    screeningSchoolMapper.updateById(s);
                }
            }
            case "keyPopulation", "regular" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(screeningId);
                if (k != null) {
                    k.setHasPreventiveTreatment(form.getHasPreventiveTreatment());
                    k.setPreventivePlan(form.getTreatmentPlan());
                    k.setPreventiveStartDate(form.getTreatmentStartDate());
                    k.setPreventiveEndDate(form.getTreatmentEndDate());
                    k.setPreventiveResult(form.getPreventiveResult());
                    k.setPreventiveManager(form.getPreventiveManager());
                    screeningKeyPopulationMapper.updateById(k);
                }
            }
            case "closeContact" -> {
                ScreeningCloseContact c = screeningCloseContactMapper.selectById(screeningId);
                if (c != null) {
                    c.setHasPreventiveTreatment("是");
                    c.setPreventivePlan(form.getTreatmentPlan());
                    screeningCloseContactMapper.updateById(c);
                }
            }
            default -> { /* 未知类型不处理 */ }
        }
    }

    /** 保存/提交/归档时补全填写人，便于列表按录入者检索（编辑时保留原填写人） */
    private void ensureFilledBy(SupervisionForm form) {
        if (form == null || form.getFilledBy() != null) {
            return;
        }
        if (form.getId() != null) {
            SupervisionForm existing = getById(form.getId());
            if (existing != null && existing.getFilledBy() != null) {
                form.setFilledBy(existing.getFilledBy());
                return;
            }
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            form.setFilledBy(currentId);
        }
    }
}
