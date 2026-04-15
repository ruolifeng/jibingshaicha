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
        saveOrUpdate(form);

        if (form.getLatentInfectionId() == null) return;

        LatentInfection latent = latentInfectionService.getById(form.getLatentInfectionId());
        if (latent == null) return;

        // 督导表归档 → 潜伏感染进入"预防治疗中"阶段
        if (Integer.valueOf(0).equals(latent.getTreatmentPhase())) {
            latent.setTreatmentPhase(1);
            latentInfectionService.updateById(latent);
        }

        // V4 sheet2：将预防性治疗数据回写到对应筛查表
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
                    c.setPreventivePlan(form.getTreatmentPlan());
                    c.setPreventiveStartDate(form.getTreatmentStartDate());
                    c.setPreventiveEndDate(form.getTreatmentEndDate());
                    c.setPreventiveResult(form.getPreventiveResult());
                    c.setPreventiveManager(form.getPreventiveManager());
                    screeningCloseContactMapper.updateById(c);
                }
            }
            default -> { /* 未知类型不处理 */ }
        }
    }
}
