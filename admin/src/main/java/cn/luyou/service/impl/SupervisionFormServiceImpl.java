package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.SupervisionForm;
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

    private final LatentInfectionService latentInfectionService;

    public SupervisionFormServiceImpl(@Lazy LatentInfectionService latentInfectionService) {
        this.latentInfectionService = latentInfectionService;
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
        list.forEach(form -> form.setEditable(Integer.valueOf(1).equals(form.getStatus())));
        return list;
    }

    private int nextFormSeq(Long latentInfectionId) {
        long count = lambdaQuery()
                .eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .ge(SupervisionForm::getStatus, 1)
                .count();
        return (int) count + 1;
    }

    private void assertSubmittable(SupervisionForm existing) {
        if (Integer.valueOf(2).equals(existing.getStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已归档记录不可修改");
        }
    }

    private void triggerArchiveSideEffects(SupervisionForm form) {
        if (form.getLatentInfectionId() == null) return;

        LatentInfection latent = latentInfectionService.getById(form.getLatentInfectionId());
        if (latent == null) return;

        // 督导表归档 → 潜伏感染进入"预防治疗中"阶段
        // 筛查表预防性治疗字段：待潜伏感染者结案进入历史患者后再回写
        if (Integer.valueOf(0).equals(latent.getTreatmentPhase())) {
            latent.setTreatmentPhase(1);
            latentInfectionService.updateById(latent);
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
