package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.common.cuenum.StatusEnum;
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
import java.util.Set;

@Tag(name = "患者管理")
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;

    @Operation(summary = "手动新增在管患者")
    @PostMapping
    public ResultResponse<Long> create(@RequestBody Map<String, Object> body) {
        return ResultRes.success(patientService.createManual(body));
    }

    @Operation(summary = "患者详情")
    @GetMapping("/{id}")
    public ResultResponse<Patient> detail(@PathVariable Long id) {
        return ResultRes.success(patientService.getDetail(id));
    }

    @Operation(summary = "更新患者基本信息")
    @PutMapping("/{id}")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        patientService.updateBasicInfo(id, body);
        return ResultRes.success(null);
    }

    @Operation(summary = "患者列表")
    @GetMapping("/list")
    public ResultResponse<IPage<Patient>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress) {
        return ResultRes.success(patientService.queryPage(page, size, populationType, name, idNumber, phone, currentAddress, 0));
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

    @Operation(summary = "批量删除患者（级联删除首次随访/后续随访/服药/通知单）")
    @DeleteMapping("/batch-delete")
    public ResultResponse<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new cn.luyou.common.customError.ServiceException(
                    cn.luyou.common.cuenum.StatusEnum.PARAM_INVALID, "请选择要删除的记录");
        }
        List<Long> ids = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        patientService.batchDeletePatients(ids);
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
        validateFirstVisitRequired(firstVisit);
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

    private static final List<String> FIRST_VISIT_EDUCATION_ITEMS = List.of(
            "服药记录卡的填写",
            "服药方法及药品存放",
            "肺结核治疗疗程",
            "不规律服药危害",
            "服药后不良反应及处理",
            "治疗期间复诊查痰",
            "外出期间如何坚持服药",
            "生活习惯及注意事项",
            "密切接触者检查"
    );

    private void validateFirstVisitRequired(FirstVisit fv) {
        if (fv.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        if (fv.getVisitDate() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择随访时间");
        }
        if (StrUtil.isBlank(fv.getVisitMethod())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择随访方式");
        }
        if (StrUtil.isBlank(fv.getPatientType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择患者类型");
        }
        if (StrUtil.isBlank(fv.getSputumStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择痰菌情况");
        }
        if (StrUtil.isBlank(fv.getDrugResistance())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择耐药情况");
        }
        if (StrUtil.isBlank(fv.getOtherSymptoms())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写其他症状");
        }
        if (StrUtil.isBlank(fv.getSymptoms())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请至少选择一项症状及体征");
        }
        if (StrUtil.isBlank(fv.getChemotherapy())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写化疗方案");
        }
        if (StrUtil.isBlank(fv.getMedicationUsage())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择用法");
        }
        if (StrUtil.isBlank(fv.getSupervisor())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择督导人员");
        }
        if (StrUtil.isBlank(fv.getDrugForm())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请至少选择一种药品剂型");
        }
        if (StrUtil.isBlank(fv.getMedicationLocation())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写取药地点");
        }
        if (StrUtil.isBlank(fv.getMedicationPickTime())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择取药时间");
        }
        validateFirstVisitEducationItems(fv.getEducationItems());
        if (fv.getNextVisitDate() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择下次随访时间");
        }
        if (StrUtil.isBlank(fv.getDoctorSignature())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写评估医生签名");
        }
    }

    private void validateFirstVisitEducationItems(String educationItemsJson) {
        if (StrUtil.isBlank(educationItemsJson)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请完成健康教育培训各项选择");
        }
        JSONObject map;
        try {
            map = JSONUtil.parseObj(educationItemsJson);
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "健康教育培训数据格式有误");
        }
        Set<String> validValues = Set.of("掌握", "未掌握");
        for (String item : FIRST_VISIT_EDUCATION_ITEMS) {
            String value = map.getStr(item);
            if (StrUtil.isBlank(value) || !validValues.contains(value)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择「" + item + "」的掌握情况");
            }
        }
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
