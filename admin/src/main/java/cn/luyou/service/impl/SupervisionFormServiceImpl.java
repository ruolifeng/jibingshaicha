package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupervisionFormServiceImpl extends ServiceImpl<SupervisionFormMapper, SupervisionForm>
        implements SupervisionFormService {

    private final LatentInfectionService latentInfectionService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAndArchive(SupervisionForm form) {
        form.setStatus(2);
        form.setArchivedTime(LocalDateTime.now());

        // V5 兼容：将新字段回填到旧字段，保持筛查表回写兼容
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

        saveOrUpdate(form);

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
                    s.setPreventivePlan(form.getTreatmentPlan());
                    s.setPreventiveStartDate(form.getTreatmentStartDate());
                    s.setPreventiveEndDate(form.getTreatmentEndDate());
                    s.setPreventiveResult(form.getPreventiveResult());
                    s.setPreventiveManager(form.getPreventiveManager());
                    screeningSchoolMapper.updateById(s);
                }
            }
            case "keyPopulation" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(screeningId);
                if (k != null) {
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
                    // 密接模型中仅有 hasPreventiveTreatment 与 preventivePlan 两个预防治疗字段
                    // 治疗开始/结束日期、管理人员等详情保存在 supervision_form 表，无需回写
                    c.setHasPreventiveTreatment("是");
                    c.setPreventivePlan(form.getTreatmentPlan());
                    screeningCloseContactMapper.updateById(c);
                }
            }
            default -> { /* 未知类型不处理 */ }
        }
    }
}
