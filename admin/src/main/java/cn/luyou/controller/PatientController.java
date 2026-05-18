package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.*;
import cn.luyou.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "患者管理")
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;

    @Operation(summary = "患者列表")
    @GetMapping("/list")
    public ResultResponse<IPage<Patient>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber) {
        return ResultRes.success(patientService.queryPage(page, size, populationType, name, idNumber, 0));
    }

    @Operation(summary = "历史患者列表")
    @GetMapping("/history")
    public ResultResponse<IPage<Patient>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ResultRes.success(patientService.queryHistoryPage(page, size, populationType, name, idNumber, startTime, endTime));
    }

    @Operation(summary = "历史患者统计汇总")
    @GetMapping("/history/stats")
    public ResultResponse<Map<String, Long>> historyStats(@RequestParam String populationType) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patient::getArchived, 1)
               .eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType);
        List<Patient> all = patientService.list(wrapper);
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalCount", (long) all.size());
        stats.put("confirmedCount", all.stream().filter(p -> "confirmed".equals(p.getSource())).count());
        stats.put("epidemicCount", all.stream().filter(p -> "epidemic".equals(p.getSource())).count());
        stats.put("maleCount", all.stream().filter(p -> "男".equals(p.getGender())).count());
        stats.put("femaleCount", all.stream().filter(p -> "女".equals(p.getGender())).count());
        return ResultRes.success(stats);
    }

    @Operation(summary = "导入大疫情表")
    @PostMapping("/import-epidemic")
    public ResultResponse<Integer> importEpidemic(
            @RequestParam("file") MultipartFile file,
            @RequestParam String populationType) {
        int count = patientService.importEpidemic(file, populationType);
        return ResultRes.success(count);
    }

    @Operation(summary = "导入专病网/病案信息表（populationType=specialDisease）")
    @PostMapping("/import-special-disease")
    public ResultResponse<Integer> importSpecialDisease(@RequestParam("file") MultipartFile file) {
        int count = patientService.importSpecialDisease(file);
        return ResultRes.success(count);
    }

    @Operation(summary = "归档患者")
    @PostMapping("/archive/{id}")
    public ResultResponse<Void> archive(@PathVariable Long id) {
        patientService.archivePatient(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除患者（级联删除首次随访/后续随访/服药/通知单）")
    @DeleteMapping("/{id}")
    public ResultResponse<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResultRes.success(null);
    }

    // ==================== 首次随访 ====================

    @Operation(summary = "保存首次随访")
    @PostMapping("/first-visit/save")
    public ResultResponse<Void> saveFirstVisit(@RequestBody FirstVisit firstVisit) {
        // 若已存在首次随访记录则更新，避免唯一键冲突
        if (firstVisit.getId() == null && firstVisit.getPatientId() != null) {
            FirstVisit existing = firstVisitService.lambdaQuery()
                    .eq(FirstVisit::getPatientId, firstVisit.getPatientId())
                    .one();
            if (existing != null) {
                firstVisit.setId(existing.getId());
            }
        }
        firstVisitService.saveOrUpdate(firstVisit);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询首次随访")
    @GetMapping("/first-visit/{patientId}")
    public ResultResponse<FirstVisit> getFirstVisit(@PathVariable Long patientId) {
        LambdaQueryWrapper<FirstVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstVisit::getPatientId, patientId).last("LIMIT 1");
        return ResultRes.success(firstVisitService.getOne(wrapper));
    }

    // ==================== 后续随访 ====================

    @Operation(summary = "保存后续随访")
    @PostMapping("/follow-up/save")
    public ResultResponse<Void> saveFollowUp(@RequestBody FollowUpVisit followUpVisit) {
        followUpVisit.setId(null);
        // 自动计算本次随访是第几次（visitSeq = 已有记录数 + 1）
        long count = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, followUpVisit.getPatientId())
                .count();
        followUpVisit.setVisitSeq((int) count + 1);
        followUpVisitService.save(followUpVisit);
        return ResultRes.success(null);
    }

    @Operation(summary = "后续随访列表")
    @GetMapping("/follow-up/list/{patientId}")
    public ResultResponse<List<FollowUpVisit>> listFollowUp(@PathVariable Long patientId) {
        LambdaQueryWrapper<FollowUpVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowUpVisit::getPatientId, patientId).orderByAsc(FollowUpVisit::getCreateTime);
        return ResultRes.success(followUpVisitService.list(wrapper));
    }

    // ==================== 服药管理 ====================

    @Operation(summary = "保存服药管理")
    @PostMapping("/medication/save")
    public ResultResponse<Void> saveMedication(@RequestBody MedicationManagement medication) {
        medicationManagementService.saveOrUpdate(medication);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询服药管理")
    @GetMapping("/medication/{patientId}")
    public ResultResponse<MedicationManagement> getMedication(@PathVariable Long patientId) {
        LambdaQueryWrapper<MedicationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationManagement::getPatientId, patientId).last("LIMIT 1");
        return ResultRes.success(medicationManagementService.getOne(wrapper));
    }

    @Operation(summary = "完成服药管理（归档患者）")
    @PostMapping("/medication/complete")
    public ResultResponse<Void> completeMedication(@RequestBody MedicationManagement medication) {
        medicationManagementService.saveOrUpdate(medication);
        if (medication.getStopDate() != null) {
            patientService.archivePatient(medication.getPatientId());
        }
        return ResultRes.success(null);
    }
}
