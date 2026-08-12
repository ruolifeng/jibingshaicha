package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.*;
import cn.luyou.service.*;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.FirstVisitSputumCultureSupport;
import cn.luyou.utils.FollowUpCaseClosureSupport;
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
    private final MedicationPickupService medicationPickupService;
    private final UserService userService;
    private final FirstVisitSputumCultureSupport firstVisitSputumCultureSupport;

    /** 填写领药：仅 pickup 可保存；查看领药记录允许服药管理或填写领药 */
    private static final String[] MEDICATION_PICKUP_WRITE_PERMISSIONS = {
            "patientManagement:pickup"
    };
    private static final String[] MEDICATION_PICKUP_READ_PERMISSIONS = {
            "patientManagement:pickup",
            "patientManagement:medication"
    };

    @Operation(summary = "手动新增在管患者")
    @PostMapping
    @OperationLog(type = "create", module = "patient", action = "手动新增在管患者")
    public ResultResponse<Long> create(@RequestBody Map<String, Object> body) {
        return ResultRes.success(patientService.createManual(body));
    }

    @Operation(summary = "批量导入在管患者（字段与手动新增一致）")
    @PostMapping("/import")
    @OperationLog(type = "import", module = "patient", action = "批量导入在管患者")
    public ResultResponse<ImportResult> importManual(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmSkipInvalid", defaultValue = "false") boolean confirmSkipInvalid,
            @RequestParam(value = "confirmSkipDuplicateInFile", defaultValue = "false") boolean confirmSkipDuplicateInFile) {
        return ResultRes.success(patientService.importManualBatch(file, confirmSkipInvalid, confirmSkipDuplicateInFile));
    }

    @Operation(summary = "表头筛选：某列实际去重值（Excel 式）")
    @GetMapping("/column-distinct")
    public ResultResponse<List<String>> columnDistinct(
            @RequestParam String field,
            @RequestParam(required = false, defaultValue = "0") Integer archived) {
        return ResultRes.success(patientService.listDistinctColumnValues(field, archived));
    }

    @Operation(summary = "患者详情")
    @GetMapping("/{id}")
    public ResultResponse<Patient> detail(@PathVariable Long id) {
        return ResultRes.success(patientService.getDetail(id));
    }

    @Operation(summary = "更新患者基本信息")
    @PutMapping("/{id}")
    @OperationLog(type = "update", module = "patient", action = "更新患者基本信息")
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
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String formatIssue,
            @RequestParam(required = false) String sputumCulture,
            @RequestParam(required = false) String drugResistance) {
        return ResultRes.success(patientService.queryPage(
                page, size, populationType, name, idNumber, phone, currentAddress, diagnosisResult, 0,
                dateFrom, dateTo, dateFilterBy, medicationManagementUnit, crowdCategory,
                creatorUsername, columnFilters, sortField, sortOrder, formatIssue,
                sputumCulture, drugResistance));
    }

    @Operation(summary = "历史患者列表")
    @GetMapping("/history")
    public ResultResponse<IPage<Patient>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String stopTreatmentReason,
            @RequestParam(required = false) String columnFilters) {
        return ResultRes.success(patientService.queryHistoryPage(
                page, size, populationType, name, idNumber, phone, diagnosisResult, startTime, endTime,
                stopTreatmentReason, columnFilters));
    }

    @Operation(summary = "历史患者统计汇总")
    @GetMapping("/history/stats")
    public ResultResponse<Map<String, Long>> historyStats(@RequestParam String populationType) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patient::getArchived, 1)
               .eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
               .and(w -> w.isNull(Patient::getArchiveRemark)
                       .or()
                       .ne(Patient::getArchiveRemark, PatientService.ARCHIVE_REMARK_TRANSFERRED_OUT));
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
    @OperationLog(type = "import", module = "patient", action = "导入大疫情表")
    public ResultResponse<Integer> importEpidemic(
            @RequestParam("file") MultipartFile file,
            @RequestParam String populationType) {
        int count = patientService.importEpidemic(file, populationType);
        return ResultRes.success(count);
    }

    @Operation(summary = "导入专病网/病案信息表（populationType=specialDisease）")
    @PostMapping("/import-special-disease")
    @OperationLog(type = "import", module = "patient", action = "导入专病网病案信息表")
    public ResultResponse<Integer> importSpecialDisease(@RequestParam("file") MultipartFile file) {
        int count = patientService.importSpecialDisease(file);
        return ResultRes.success(count);
    }

    @Operation(summary = "归档患者")
    @PostMapping("/archive/{id}")
    @OperationLog(type = "update", module = "patient", action = "归档患者")
    public ResultResponse<Void> archive(@PathVariable Long id) {
        patientService.archivePatient(id, "手动归档");
        return ResultRes.success(null);
    }

    @Operation(summary = "解锁停止治疗归档的患者（管理员）")
    @PostMapping("/unarchive/{id}")
    @OperationLog(type = "update", module = "patient", action = "解锁停止治疗归档患者")
    public ResultResponse<Void> unarchiveFromStopTreatment(@PathVariable Long id) {
        Integer role = BaseContext.getCurrentRole();
        if (role == null || role == 6) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无权限解锁患者档案");
        }
        patientService.unarchivePatientFromStopTreatment(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除患者（级联删除首次随访/后续随访/服药/通知单）")
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "delete", module = "patient", action = "批量删除患者")
    public ResultResponse<Void> batchDelete(@RequestBody Map<String, Object> body) {
        userService.checkPermissionCode("patientManagement:delete");
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

    @Operation(summary = "按筛选条件删除患者（级联删除）")
    @DeleteMapping("/delete-by-filter")
    @OperationLog(type = "delete", module = "patient", action = "按筛选条件删除患者")
    public ResultResponse<Integer> deleteByFilter(
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue) {
        userService.checkPermissionCode("patientManagement:delete");
        return ResultRes.success(patientService.deleteByFilter(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult, archived,
                dateFrom, dateTo, dateFilterBy, medicationManagementUnit, crowdCategory,
                creatorUsername, columnFilters, formatIssue));
    }

    @Operation(summary = "删除患者（级联删除首次随访/后续随访/服药/通知单）")
    @DeleteMapping("/{id}")
    @OperationLog(type = "delete", module = "patient", action = "删除患者")
    public ResultResponse<Void> deletePatient(@PathVariable Long id) {
        userService.checkPermissionCode("patientManagement:delete");
        patientService.deletePatient(id);
        return ResultRes.success(null);
    }

    // ==================== 首次随访 ====================

    /** 已完成首次随访可随时修改（仍校验 edit 权限） */
    private void assertFirstVisitEditable(FirstVisit existing) {
        if (existing == null || !Integer.valueOf(1).equals(existing.getStatus())) {
            return;
        }
        assertFirstVisitEditPermission(existing);
    }

    /** 修改已完成的首次随访需具备 patientManagement:firstVisit:edit 权限 */
    private void assertFirstVisitEditPermission(FirstVisit existing) {
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            userService.checkPermissionCode("patientManagement:firstVisit:edit");
        }
    }

    @Operation(summary = "保存首次随访草稿（部分填写即可）")
    @PostMapping("/first-visit/draft")
    @OperationLog(type = "update", module = "patient", action = "保存首次随访草稿")
    public ResultResponse<Void> saveFirstVisitDraft(@RequestBody FirstVisit firstVisit) {
        if (firstVisit.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        patientService.assertPatientOperable(firstVisit.getPatientId());
        mergeExistingFirstVisitId(firstVisit);
        FirstVisit existing = firstVisit.getId() != null
                ? firstVisitService.getById(firstVisit.getId())
                : firstVisitService.lambdaQuery().eq(FirstVisit::getPatientId, firstVisit.getPatientId()).one();
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "首次随访已完成，请直接保存正式记录");
        }
        userService.checkPermissionCode("patientManagement:firstVisit:fill");
        firstVisit.setStatus(0);
        firstVisitService.saveOrUpdate(firstVisit);
        return ResultRes.success(null);
    }

    @Operation(summary = "保存首次随访")
    @PostMapping("/first-visit/save")
    @OperationLog(type = "update", module = "patient", action = "保存首次随访")
    public ResultResponse<Void> saveFirstVisit(@RequestBody FirstVisit firstVisit) {
        validateFirstVisitRequired(firstVisit);
        if (firstVisit.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        patientService.assertPatientOperable(firstVisit.getPatientId());
        mergeExistingFirstVisitId(firstVisit);
        FirstVisit existing = firstVisit.getId() != null
                ? firstVisitService.getById(firstVisit.getId())
                : firstVisitService.lambdaQuery().eq(FirstVisit::getPatientId, firstVisit.getPatientId()).one();
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            assertFirstVisitEditable(existing);
        } else {
            userService.checkPermissionCode("patientManagement:firstVisit:fill");
        }
        if (firstVisit.getFilledBy() == null) {
            firstVisit.setFilledBy(BaseContext.getCurrentId());
        } else if (existing != null && existing.getFilledBy() != null) {
            firstVisit.setFilledBy(existing.getFilledBy());
        }
        firstVisit.setStatus(1);
        firstVisitSputumCultureSupport.prepareSupplementStatus(firstVisit, existing);
        firstVisitService.saveOrUpdate(firstVisit);
        firstVisitSputumCultureSupport.syncMessages(firstVisit, existing);
        return ResultRes.success(null);
    }

    private void mergeExistingFirstVisitId(FirstVisit firstVisit) {
        if (firstVisit.getId() != null || firstVisit.getPatientId() == null) return;
        FirstVisit existing = firstVisitService.lambdaQuery()
                .eq(FirstVisit::getPatientId, firstVisit.getPatientId())
                .one();
        if (existing != null) {
            firstVisit.setId(existing.getId());
        }
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
        if (StrUtil.isBlank(fv.getFormNo())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写编号");
        }
        if (!fv.getFormNo().matches("\\d{8}")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "编号须为8位数字");
        }
        if (fv.getVisitDate() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择随访时间");
        }
        if (StrUtil.isBlank(fv.getVisitMethod())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择随访方式");
        }
        if ("其他".equals(fv.getVisitMethod()) && StrUtil.isBlank(fv.getVisitMethodOther())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写随访方式");
        }
        if (!"其他".equals(fv.getVisitMethod())) {
            fv.setVisitMethodOther(null);
        }
        if (StrUtil.isBlank(fv.getPatientType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择患者类型");
        }
        if (StrUtil.isBlank(fv.getSputumStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择痰菌情况");
        }
        if (StrUtil.isBlank(fv.getSputumCulture())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择痰培养情况");
        }
        if (StrUtil.isBlank(fv.getDrugResistance())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择耐药情况");
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

    @Operation(summary = "保存领药记录")
    @PostMapping("/medication-pickup/save")
    @OperationLog(type = "update", module = "patient", action = "保存领药记录")
    public ResultResponse<Void> saveMedicationPickup(@RequestBody MedicationPickup pickup) {
        userService.checkAnyPermissionCode(MEDICATION_PICKUP_WRITE_PERMISSIONS);
        if (pickup.getPatientId() != null) {
            patientService.assertPatientOperable(pickup.getPatientId());
        }
        medicationPickupService.savePickup(pickup);
        return ResultRes.success(null);
    }

    @Operation(summary = "领药记录列表")
    @GetMapping("/medication-pickup/list/{patientId}")
    public ResultResponse<List<MedicationPickup>> listMedicationPickup(@PathVariable Long patientId) {
        userService.checkAnyPermissionCode(MEDICATION_PICKUP_READ_PERMISSIONS);
        return ResultRes.success(medicationPickupService.listByPatientId(patientId));
    }

    // ==================== 后续随访 ====================

    /** 已完成后续随访可随时修改（仍校验 edit 权限） */
    private void assertFollowUpEditable(FollowUpVisit existing) {
        if (existing == null || !Integer.valueOf(1).equals(existing.getStatus())) {
            return;
        }
        assertFollowUpEditPermission(existing);
    }

    /** 修改已提交的后续随访记录需具备 patientManagement:followUp:edit 权限 */
    private void assertFollowUpEditPermission(FollowUpVisit existing) {
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            userService.checkPermissionCode("patientManagement:followUp:edit");
        }
    }

    @Operation(summary = "患者结案全程管理统计（实际访视/服药次数）")
    @GetMapping("/follow-up/case-closure-stats/{patientId}")
    public ResultResponse<Map<String, Integer>> getFollowUpCaseClosureStats(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "true") boolean includeCurrentFollowUp) {
        patientService.assertPatientOperable(patientId);
        Map<String, Integer> stats = new HashMap<>();
        stats.put("actualVisitCount", FollowUpCaseClosureSupport.computeActualVisitCount(
                firstVisitService, followUpVisitService, patientId, includeCurrentFollowUp));
        stats.put("actualDoseCount", FollowUpCaseClosureSupport.computeActualDoseCount(
                medicationManagementService, patientId));
        return ResultRes.success(stats);
    }

    @Operation(summary = "查询后续随访草稿")
    @GetMapping("/follow-up/draft/{patientId}")
    public ResultResponse<FollowUpVisit> getFollowUpDraft(@PathVariable Long patientId) {
        FollowUpVisit draft = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 0)
                .orderByDesc(FollowUpVisit::getUpdateTime)
                .last("LIMIT 1")
                .one();
        return ResultRes.success(draft);
    }

    @Operation(summary = "保存后续随访草稿（部分填写即可）")
    @PostMapping("/follow-up/draft")
    @OperationLog(type = "update", module = "patient", action = "保存后续随访草稿")
    public ResultResponse<Void> saveFollowUpDraft(@RequestBody FollowUpVisit followUpVisit) {
        if (followUpVisit.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        patientService.assertPatientOperable(followUpVisit.getPatientId());
        userService.checkPermissionCode("patientManagement:followUp:fill");
        assertPatientNotArchivedForNewFollowUp(followUpVisit);
        FollowUpVisit existingDraft = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, followUpVisit.getPatientId())
                .eq(FollowUpVisit::getStatus, 0)
                .one();
        if (existingDraft != null) {
            followUpVisit.setId(existingDraft.getId());
        } else {
            followUpVisit.setId(null);
        }
        followUpVisit.setStatus(0);
        followUpVisit.setVisitSeq(null);
        followUpVisitService.saveOrUpdate(followUpVisit);
        return ResultRes.success(null);
    }

    @Operation(summary = "保存后续随访")
    @PostMapping("/follow-up/save")
    @OperationLog(type = "update", module = "patient", action = "保存后续随访")
    public ResultResponse<Void> saveFollowUp(@RequestBody FollowUpVisit followUpVisit) {
        if (followUpVisit.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        if (followUpVisit.getVisitDate() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写随访时间");
        }
        patientService.assertPatientOperable(followUpVisit.getPatientId());
        assertPatientNotArchivedForNewFollowUp(followUpVisit);
        validateFollowUpVisitMethod(followUpVisit);
        validateStopTreatmentOnSave(followUpVisit);
        FollowUpVisit existingForStats = followUpVisit.getId() != null
                ? followUpVisitService.getById(followUpVisit.getId())
                : null;
        FollowUpCaseClosureSupport.applyCaseClosureStats(
                followUpVisit,
                firstVisitService,
                followUpVisitService,
                medicationManagementService,
                FollowUpCaseClosureSupport.shouldIncludeCurrentFollowUp(existingForStats));
        if (followUpVisit.getId() != null) {
            FollowUpVisit existing = existingForStats;
            if (existing != null) {
                if (Integer.valueOf(1).equals(existing.getStatus())) {
                    assertFollowUpEditable(existing);
                } else {
                    userService.checkPermissionCode("patientManagement:followUp:fill");
                }
                followUpVisit.setStatus(1);
                if (Integer.valueOf(0).equals(existing.getStatus())) {
                    followUpVisit.setVisitSeq(nextFollowUpSeq(followUpVisit.getPatientId()));
                } else {
                    followUpVisit.setVisitSeq(existing.getVisitSeq());
                }
                followUpVisitService.updateById(followUpVisit);
                handleStopTreatmentAfterSave(followUpVisit);
                return ResultRes.success(null);
            }
        }
        userService.checkPermissionCode("patientManagement:followUp:fill");
        followUpVisit.setId(null);
        followUpVisit.setStatus(1);
        followUpVisit.setVisitSeq(nextFollowUpSeq(followUpVisit.getPatientId()));
        followUpVisitService.save(followUpVisit);
        handleStopTreatmentAfterSave(followUpVisit);
        return ResultRes.success(null);
    }

    /** 新建后续随访时，已归档患者不可再填写（修改已有记录除外） */
    private void assertPatientNotArchivedForNewFollowUp(FollowUpVisit followUpVisit) {
        if (followUpVisit.getId() != null) {
            FollowUpVisit existing = followUpVisitService.getById(followUpVisit.getId());
            if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
                return;
            }
        }
        Patient patient = patientService.getById(followUpVisit.getPatientId());
        if (patient != null && Integer.valueOf(1).equals(patient.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者已归档，无法填写后续随访");
        }
    }

    private void validateFollowUpVisitMethod(FollowUpVisit followUpVisit) {
        if ("4".equals(followUpVisit.getVisitMethod())
                && StrUtil.isBlank(followUpVisit.getVisitMethodOther())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写随访方式");
        }
        if (!"4".equals(followUpVisit.getVisitMethod())) {
            followUpVisit.setVisitMethodOther(null);
        }
    }

    private void validateStopTreatmentOnSave(FollowUpVisit followUpVisit) {
        if (!"是".equals(followUpVisit.getStopTreatment())) {
            return;
        }
        if (followUpVisit.getStopTreatmentDate() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择停止治疗时间");
        }
        if (StrUtil.isBlank(followUpVisit.getStopTreatmentReason())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择停止治疗原因");
        }
        if ("其它".equals(followUpVisit.getStopTreatmentReason())
                && StrUtil.isBlank(followUpVisit.getStopTreatmentReasonOther())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写停止治疗原因");
        }
    }

    /** 停止治疗：前四项（完成疗程/死亡/丢失/其它）归档；转入耐多药治疗可继续随访 */
    private void handleStopTreatmentAfterSave(FollowUpVisit followUpVisit) {
        if (!"是".equals(followUpVisit.getStopTreatment())) {
            return;
        }
        String reason = followUpVisit.getStopTreatmentReason();
        if (StrUtil.isBlank(reason)) {
            return;
        }
        Patient patient = patientService.getById(followUpVisit.getPatientId());
        if (patient == null) {
            return;
        }
        if (PatientService.STOP_TREATMENT_REASON_MDR.equals(reason)) {
            if (Integer.valueOf(1).equals(patient.getArchived())
                    && PatientService.isStopTreatmentArchiveRemark(patient.getArchiveRemark())) {
                patientService.unarchivePatientFromStopTreatment(followUpVisit.getPatientId());
            }
            return;
        }
        if (!PatientService.shouldArchiveOnStopTreatment(followUpVisit.getStopTreatment(), reason)) {
            return;
        }
        String displayReason = reason;
        if ("其它".equals(reason) && StrUtil.isNotBlank(followUpVisit.getStopTreatmentReasonOther())) {
            displayReason = followUpVisit.getStopTreatmentReasonOther();
        }
        String archiveRemark = PatientService.ARCHIVE_REMARK_STOP_TREATMENT_PREFIX + displayReason;
        if (!Integer.valueOf(1).equals(patient.getArchived())) {
            patientService.archivePatient(followUpVisit.getPatientId(), archiveRemark);
        } else if (PatientService.isStopTreatmentArchiveRemark(patient.getArchiveRemark())) {
            patient.setArchiveRemark(archiveRemark);
            patientService.updateById(patient);
        }
    }

    private int nextFollowUpSeq(Long patientId) {
        long count = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .count();
        return (int) count + 1;
    }

    @Operation(summary = "后续随访列表")
    @GetMapping("/follow-up/list/{patientId}")
    public ResultResponse<List<FollowUpVisit>> listFollowUp(@PathVariable Long patientId) {
        LambdaQueryWrapper<FollowUpVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .orderByAsc(FollowUpVisit::getCreateTime);
        List<FollowUpVisit> list = followUpVisitService.list(wrapper);
        list.forEach(v -> v.setEditable(true));
        return ResultRes.success(list);
    }

    @Operation(summary = "删除单条后续随访记录")
    @DeleteMapping("/follow-up/{id}")
    @OperationLog(type = "delete", module = "patient", action = "删除后续随访记录")
    public ResultResponse<Void> deleteFollowUp(@PathVariable Long id) {
        FollowUpVisit existing = followUpVisitService.getById(id);
        if (existing == null || !Integer.valueOf(1).equals(existing.getStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "后续随访记录不存在");
        }
        patientService.assertPatientOperable(existing.getPatientId());
        assertFollowUpEditable(existing);
        assertFollowUpEditPermission(existing);
        Long patientId = existing.getPatientId();
        boolean shouldTryUnarchive = "是".equals(existing.getStopTreatment())
                && PatientService.shouldArchiveOnStopTreatment(existing.getStopTreatment(), existing.getStopTreatmentReason());
        followUpVisitService.removeById(id);
        renumberFollowUpSeq(patientId);
        if (shouldTryUnarchive) {
            restorePatientArchiveIfNoStopTreatmentFollowUp(patientId);
        }
        return ResultRes.success(null);
    }

    private void renumberFollowUpSeq(Long patientId) {
        List<FollowUpVisit> list = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .orderByAsc(FollowUpVisit::getCreateTime)
                .list();
        for (int i = 0; i < list.size(); i++) {
            int newSeq = i + 1;
            FollowUpVisit visit = list.get(i);
            if (!Integer.valueOf(newSeq).equals(visit.getVisitSeq())) {
                visit.setVisitSeq(newSeq);
                followUpVisitService.updateById(visit);
            }
        }
    }

    /** 删除停止治疗随访后，若已无其它需归档的停止治疗记录，则解除停止治疗归档 */
    private void restorePatientArchiveIfNoStopTreatmentFollowUp(Long patientId) {
        Patient patient = patientService.getById(patientId);
        if (patient == null || !Integer.valueOf(1).equals(patient.getArchived())) {
            return;
        }
        if (!PatientService.isStopTreatmentArchiveRemark(patient.getArchiveRemark())) {
            return;
        }
        boolean hasArchivingStopTreatment = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .eq(FollowUpVisit::getStopTreatment, "是")
                .list()
                .stream()
                .anyMatch(v -> PatientService.shouldArchiveOnStopTreatment(v.getStopTreatment(), v.getStopTreatmentReason()));
        if (!hasArchivingStopTreatment) {
            patientService.unarchivePatientFromStopTreatment(patientId);
        }
    }

    // ==================== 服药管理 ====================

    @Operation(summary = "保存服药管理")
    @PostMapping("/medication/save")
    @OperationLog(type = "update", module = "patient", action = "保存服药管理")
    public ResultResponse<Void> saveMedication(@RequestBody MedicationManagement medication) {
        preparePatientMedication(medication);
        medicationManagementService.saveOrUpdate(medication);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询服药管理")
    @GetMapping("/medication/{patientId}")
    public ResultResponse<MedicationManagement> getMedication(@PathVariable Long patientId) {
        LambdaQueryWrapper<MedicationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationManagement::getPatientId, patientId)
                .orderByDesc(MedicationManagement::getCreateTime)
                .last("LIMIT 1");
        return ResultRes.success(medicationManagementService.getOne(wrapper));
    }

    @Operation(summary = "完成服药管理（归档患者）")
    @PostMapping("/medication/complete")
    @OperationLog(type = "update", module = "patient", action = "完成服药管理")
    public ResultResponse<Void> completeMedication(@RequestBody MedicationManagement medication) {
        preparePatientMedication(medication);
        medicationManagementService.saveOrUpdate(medication);
        if (medication.getStopDate() != null) {
            patientService.archivePatient(medication.getPatientId());
        }
        return ResultRes.success(null);
    }

    /** 校验归属、补齐人群类型，并确保不会误绑潜伏感染记录 */
    private void preparePatientMedication(MedicationManagement medication) {
        if (medication.getPatientId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少患者ID");
        }
        patientService.assertPatientOperable(medication.getPatientId());
        if (medication.getId() != null) {
            MedicationManagement existing = medicationManagementService.getById(medication.getId());
            if (existing == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "服药管理记录不存在");
            }
            if (existing.getPatientId() == null
                    || !existing.getPatientId().equals(medication.getPatientId())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "患者与服药管理记录不匹配");
            }
        }
        if (StrUtil.isBlank(medication.getPopulationType())) {
            Patient patient = patientService.getById(medication.getPatientId());
            if (patient != null) {
                medication.setPopulationType(patient.getPopulationType());
            }
        }
        if (StrUtil.isBlank(medication.getPopulationType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少人群类型");
        }
        medication.setLatentInfectionId(null);
    }
}
