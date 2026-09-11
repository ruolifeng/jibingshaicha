package cn.luyou.service.impl;

import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.Patient;
import cn.luyou.service.MedicationManagementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationManagementServiceImpl extends ServiceImpl<MedicationManagementMapper, MedicationManagement>
        implements MedicationManagementService {

    private final FollowUpVisitMapper followUpVisitMapper;
    private final PatientMapper patientMapper;

    @Override
    public void syncStopDateFromFollowUp(Long patientId) {
        if (patientId == null) {
            return;
        }
        MedicationManagement medication = lambdaQuery()
                .eq(MedicationManagement::getPatientId, patientId)
                .orderByDesc(MedicationManagement::getCreateTime)
                .last("LIMIT 1")
                .one();
        fillStopDateFromFollowUp(medication);
    }

    @Override
    public void fillStopDateFromFollowUp(MedicationManagement medication) {
        if (medication == null || medication.getPatientId() == null) {
            return;
        }
        Patient patient = patientMapper.selectById(medication.getPatientId());
        if (patient == null || !Integer.valueOf(1).equals(patient.getArchived())) {
            return;
        }
        LocalDate stopDate = resolveLastFollowUpStopDate(medication.getPatientId());
        if (stopDate == null || stopDate.equals(medication.getStopDate())) {
            return;
        }
        medication.setStopDate(stopDate);
        if (medication.getId() == null) {
            return;
        }
        update(new LambdaUpdateWrapper<MedicationManagement>()
                .eq(MedicationManagement::getId, medication.getId())
                .set(MedicationManagement::getStopDate, stopDate));
    }

    private LocalDate resolveLastFollowUpStopDate(Long patientId) {
        List<FollowUpVisit> visits = followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .eq(FollowUpVisit::getStopTreatment, "是")
                .isNotNull(FollowUpVisit::getStopTreatmentDate));
        if (visits == null || visits.isEmpty()) {
            return null;
        }
        return visits.stream()
                .max(Comparator
                        .comparing((FollowUpVisit v) -> v.getCreateTime() == null
                                ? LocalDateTime.MIN : v.getCreateTime())
                        .thenComparing(v -> v.getId() == null ? 0L : v.getId()))
                .map(FollowUpVisit::getStopTreatmentDate)
                .orElse(null);
    }
}
