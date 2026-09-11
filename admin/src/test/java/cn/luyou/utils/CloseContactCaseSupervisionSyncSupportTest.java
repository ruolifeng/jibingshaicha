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
        SupervisionForm first = SupervisionForm.builder()
                .id(1L)
                .status(1)
                .createTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();
        SupervisionForm last = SupervisionForm.builder()
                .id(2L)
                .status(2)
                .createTime(LocalDateTime.of(2026, 9, 1, 18, 44))
                .build();
        SupervisionForm draft = SupervisionForm.builder()
                .id(3L)
                .status(0)
                .createTime(LocalDateTime.of(2026, 9, 11, 12, 0))
                .build();
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
