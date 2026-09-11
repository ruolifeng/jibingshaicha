package cn.luyou.utils;

import cn.luyou.model.CloseContactCase;
import cn.luyou.model.SupervisionForm;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseContactCaseSupervisionSyncSupportTest {

    @Test
    void mapsSupervisionYesToCaseStarted() {
        SupervisionForm form = SupervisionForm.builder()
                .hasPreventiveTreatment("是")
                .treatmentPlan("3HP")
                .treatmentCompletionStatus("完成治疗")
                .build();
        assertEquals("开展", CloseContactCaseSupervisionSyncSupport.resolveHasPreventiveTreatment(form));
        assertEquals("是", CloseContactCaseSupervisionSyncSupport.resolveTreatmentCompleted(form));
    }

    @Test
    void infersHasTreatmentFromPlanWhenBlank() {
        SupervisionForm noPlan = SupervisionForm.builder().treatmentPlan("不服药").build();
        SupervisionForm withPlan = SupervisionForm.builder().treatmentPlan("3HP").build();
        assertEquals("未开展", CloseContactCaseSupervisionSyncSupport.resolveHasPreventiveTreatment(noPlan));
        assertEquals("开展", CloseContactCaseSupervisionSyncSupport.resolveHasPreventiveTreatment(withPlan));
    }

    @Test
    void mapsNormativeTreatmentAsCompleted() {
        SupervisionForm form = SupervisionForm.builder()
                .treatmentCompletionStatus("规范治疗")
                .build();
        assertEquals("是", CloseContactCaseSupervisionSyncSupport.resolveTreatmentCompleted(form));
    }

    @Test
    void infersCompletedWhenNoInterruptAndEndDate() {
        SupervisionForm form = SupervisionForm.builder()
                .interruptMedication("无")
                .treatmentEndDate(LocalDate.of(2026, 8, 1))
                .build();
        assertEquals("是", CloseContactCaseSupervisionSyncSupport.resolveTreatmentCompleted(form));
    }

    @Test
    void selectsLatestUploadedFormByCreateTime() {
        // id/createTime 在 BaseEntity，@Builder 不含父类字段，需 set 赋值
        SupervisionForm first = SupervisionForm.builder().status(1).build();
        first.setId(1L);
        first.setCreateTime(LocalDateTime.of(2026, 8, 1, 10, 0));
        SupervisionForm last = SupervisionForm.builder().status(2).build();
        last.setId(2L);
        last.setCreateTime(LocalDateTime.of(2026, 9, 1, 18, 44));
        SupervisionForm draft = SupervisionForm.builder().status(0).build();
        draft.setId(3L);
        draft.setCreateTime(LocalDateTime.of(2026, 9, 11, 12, 0));
        assertEquals(2L, CloseContactCaseSupervisionSyncSupport.selectLastUploaded(List.of(first, last, draft)).getId());
    }

    @Test
    void applyOverwritesCasePreventiveFields() {
        CloseContactCase caze = CloseContactCase.builder()
                .hasPreventiveTreatment("开展")
                .preventivePlan("")
                .treatmentCompleted("")
                .build();
        SupervisionForm form = SupervisionForm.builder()
                .hasPreventiveTreatment("是")
                .treatmentPlan("3HP")
                .treatmentCompletionStatus("规范治疗")
                .build();
        assertTrue(CloseContactCaseSupervisionSyncSupport.applyFromSupervisionForm(caze, form));
        assertEquals("开展", caze.getHasPreventiveTreatment());
        assertEquals("3HP", caze.getPreventivePlan());
        assertEquals("是", caze.getTreatmentCompleted());
        assertTrue(Boolean.TRUE.equals(caze.getPreventiveSyncedFromSupervision()));
    }
}
