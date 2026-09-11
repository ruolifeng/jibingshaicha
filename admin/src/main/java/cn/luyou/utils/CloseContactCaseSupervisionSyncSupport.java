package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.CloseContactCaseMapper;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.LatentInfectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 密接个案表预防性治疗三字段：从潜伏感染者「历史患者」最后一次已上传督导表同步。
 * <p>
 * 字段对应：是否预防性治疗 ← 是否开始预防性治疗；预防性治疗方案 ← 治疗方案；
 * 是否完成治疗 ← 治疗完成情况。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseContactCaseSupervisionSyncSupport {

    private final LatentInfectionMapper latentInfectionMapper;
    private final SupervisionFormMapper supervisionFormMapper;
    private final CloseContactCaseMapper closeContactCaseMapper;

    /** 列表/详情/导出：按证件号抓取并回写（有督导表时覆盖个案原值） */
    public void overlayAndPersist(List<CloseContactCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return;
        }
        Map<String, SupervisionForm> formById = loadLastFormByIdNumber(collectIdNumbers(cases));
        if (formById.isEmpty()) {
            return;
        }
        for (CloseContactCase caze : cases) {
            SupervisionForm form = findForm(formById, caze.getIdNumber());
            if (form == null) {
                continue;
            }
            applyAndPersist(caze, form);
        }
    }

    /** 结案进入历史患者后，或历史患者再次上传督导表时，按该潜伏记录证件号回写个案表 */
    public void syncCasesFromArchivedLatent(LatentInfection latent) {
        if (latent == null || !Integer.valueOf(1).equals(latent.getArchived())) {
            return;
        }
        if (StrUtil.isNotBlank(latent.getArchiveRemark())
                && LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT.equals(latent.getArchiveRemark())) {
            return;
        }
        String idNumber = ImportIdentitySupport.normalizeIdNumber(latent.getIdNumber());
        if (StrUtil.isBlank(idNumber)) {
            return;
        }
        SupervisionForm form = selectLastUploaded(listUploadedForms(List.of(latent.getId())));
        if (form == null) {
            return;
        }
        List<CloseContactCase> cases = closeContactCaseMapper.selectList(new LambdaQueryWrapper<CloseContactCase>()
                .and(w -> w.eq(CloseContactCase::getIdNumber, idNumber)
                        .or()
                        .eq(CloseContactCase::getIdNumber, latent.getIdNumber())));
        if (cases == null || cases.isEmpty()) {
            return;
        }
        for (CloseContactCase caze : cases) {
            applyAndPersist(caze, form);
        }
    }

    static SupervisionForm selectLastUploaded(List<SupervisionForm> forms) {
        if (forms == null || forms.isEmpty()) {
            return null;
        }
        return forms.stream()
                .filter(f -> f != null && f.getStatus() != null && f.getStatus() >= 1)
                .max(Comparator
                        .comparing((SupervisionForm f) -> f.getCreateTime() == null
                                ? LocalDateTime.MIN : f.getCreateTime())
                        .thenComparing(f -> f.getId() == null ? 0L : f.getId()))
                .orElse(null);
    }

    static String resolveHasPreventiveTreatment(SupervisionForm form) {
        if (form == null) {
            return null;
        }
        String raw = StrUtil.trim(form.getHasPreventiveTreatment());
        if (StrUtil.isBlank(raw) && StrUtil.isNotBlank(form.getTreatmentPlan())) {
            raw = "不服药".equals(form.getTreatmentPlan().trim()) ? "否" : "是";
        }
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        if ("是".equals(raw) || "开展".equals(raw)) {
            return "开展";
        }
        if ("否".equals(raw) || "未开展".equals(raw) || "不服药".equals(raw)) {
            return "未开展";
        }
        return raw;
    }

    static String resolveTreatmentCompleted(SupervisionForm form) {
        if (form == null) {
            return null;
        }
        String status = StrUtil.trim(form.getTreatmentCompletionStatus());
        if (StrUtil.isBlank(status)) {
            status = StrUtil.trim(form.getPreventiveResult());
        }
        if (StrUtil.isBlank(status)
                && "无".equals(form.getInterruptMedication())
                && form.getTreatmentEndDate() != null) {
            status = "规范完成";
        }
        if (StrUtil.isBlank(status)) {
            return null;
        }
        if (isCompletedYes(status)) {
            return "是";
        }
        if (isCompletedNo(status)) {
            return "否";
        }
        return status;
    }

    static boolean applyFromSupervisionForm(CloseContactCase caze, SupervisionForm form) {
        if (caze == null || form == null) {
            return false;
        }
        boolean changed = false;
        String has = resolveHasPreventiveTreatment(form);
        if (StrUtil.isNotBlank(has) && !Objects.equals(has, caze.getHasPreventiveTreatment())) {
            caze.setHasPreventiveTreatment(has);
            changed = true;
        }
        String plan = StrUtil.trim(form.getTreatmentPlan());
        if (StrUtil.isNotBlank(plan) && !Objects.equals(plan, caze.getPreventivePlan())) {
            caze.setPreventivePlan(plan);
            changed = true;
        }
        String completed = resolveTreatmentCompleted(form);
        if (StrUtil.isNotBlank(completed) && !Objects.equals(completed, caze.getTreatmentCompleted())) {
            caze.setTreatmentCompleted(completed);
            changed = true;
        }
        caze.setPreventiveSyncedFromSupervision(true);
        return changed;
    }

    private void applyAndPersist(CloseContactCase caze, SupervisionForm form) {
        boolean changed = applyFromSupervisionForm(caze, form);
        if (!changed || caze.getId() == null) {
            return;
        }
        closeContactCaseMapper.update(null, new LambdaUpdateWrapper<CloseContactCase>()
                .eq(CloseContactCase::getId, caze.getId())
                .set(CloseContactCase::getHasPreventiveTreatment, caze.getHasPreventiveTreatment())
                .set(CloseContactCase::getPreventivePlan, caze.getPreventivePlan())
                .set(CloseContactCase::getTreatmentCompleted, caze.getTreatmentCompleted()));
        log.debug("密接个案预防治疗已从督导表同步 caseId={} latentFormId={}", caze.getId(), form.getId());
    }

    private Map<String, SupervisionForm> loadLastFormByIdNumber(Set<String> idNumbers) {
        Map<String, SupervisionForm> result = new HashMap<>();
        if (idNumbers.isEmpty()) {
            return result;
        }
        List<LatentInfection> latents = latentInfectionMapper.selectList(new LambdaQueryWrapper<LatentInfection>()
                .in(LatentInfection::getIdNumber, idNumbers)
                .eq(LatentInfection::getArchived, 1)
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT)));
        if (latents == null || latents.isEmpty()) {
            return result;
        }
        List<Long> latentIds = latents.stream().map(LatentInfection::getId).filter(Objects::nonNull).toList();
        if (latentIds.isEmpty()) {
            return result;
        }
        Map<Long, SupervisionForm> lastByLatent = new HashMap<>();
        for (SupervisionForm form : listUploadedForms(latentIds)) {
            SupervisionForm current = lastByLatent.get(form.getLatentInfectionId());
            if (current == null || compareUploadTime(form, current) > 0) {
                lastByLatent.put(form.getLatentInfectionId(), form);
            }
        }
        if (lastByLatent.isEmpty()) {
            return result;
        }
        for (LatentInfection latent : latents) {
            SupervisionForm form = lastByLatent.get(latent.getId());
            if (form == null) {
                continue;
            }
            putIfNewer(result, latent.getIdNumber(), form);
            String normalized = ImportIdentitySupport.normalizeIdNumber(latent.getIdNumber());
            if (StrUtil.isNotBlank(normalized)) {
                putIfNewer(result, normalized, form);
            }
        }
        return result;
    }

    private List<SupervisionForm> listUploadedForms(Collection<Long> latentIds) {
        if (latentIds == null || latentIds.isEmpty()) {
            return List.of();
        }
        List<SupervisionForm> forms = supervisionFormMapper.selectList(new LambdaQueryWrapper<SupervisionForm>()
                .in(SupervisionForm::getLatentInfectionId, latentIds)
                .ge(SupervisionForm::getStatus, 1));
        return forms == null ? List.of() : forms;
    }

    private static Set<String> collectIdNumbers(List<CloseContactCase> cases) {
        Set<String> ids = new HashSet<>();
        for (CloseContactCase caze : cases) {
            if (caze == null || StrUtil.isBlank(caze.getIdNumber())) {
                continue;
            }
            ids.add(caze.getIdNumber().trim());
            String normalized = ImportIdentitySupport.normalizeIdNumber(caze.getIdNumber());
            if (StrUtil.isNotBlank(normalized)) {
                ids.add(normalized);
            }
        }
        return ids;
    }

    private static SupervisionForm findForm(Map<String, SupervisionForm> formById, String idNumber) {
        if (StrUtil.isBlank(idNumber) || formById.isEmpty()) {
            return null;
        }
        SupervisionForm form = formById.get(idNumber.trim());
        if (form != null) {
            return form;
        }
        return formById.get(ImportIdentitySupport.normalizeIdNumber(idNumber));
    }

    private static void putIfNewer(Map<String, SupervisionForm> map, String idNumber, SupervisionForm form) {
        if (StrUtil.isBlank(idNumber) || form == null) {
            return;
        }
        SupervisionForm current = map.get(idNumber);
        if (current == null || compareUploadTime(form, current) > 0) {
            map.put(idNumber, form);
        }
    }

    private static int compareUploadTime(SupervisionForm a, SupervisionForm b) {
        LocalDateTime ta = a.getCreateTime() == null ? LocalDateTime.MIN : a.getCreateTime();
        LocalDateTime tb = b.getCreateTime() == null ? LocalDateTime.MIN : b.getCreateTime();
        int cmp = ta.compareTo(tb);
        if (cmp != 0) {
            return cmp;
        }
        long ida = a.getId() == null ? 0L : a.getId();
        long idb = b.getId() == null ? 0L : b.getId();
        return Long.compare(ida, idb);
    }

    private static boolean isCompletedYes(String status) {
        return "是".equals(status)
                || "完成".equals(status)
                || "完成治疗".equals(status)
                || "规范完成".equals(status)
                || "规范治疗".equals(status);
    }

    private static boolean isCompletedNo(String status) {
        return "否".equals(status)
                || "未完成".equals(status)
                || "失败".equals(status)
                || "死亡".equals(status)
                || "失访".equals(status)
                || "不良反应停药".equals(status)
                || "未评估".equals(status)
                || "自行中断治疗".equals(status)
                || "确诊肺结核".equals(status);
    }
}
