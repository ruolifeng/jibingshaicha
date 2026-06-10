package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.MedicationPickupService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.service.ScreeningSchoolService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningScopeHelper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningSchoolServiceImpl extends ServiceImpl<ScreeningSchoolMapper, ScreeningSchool>
        implements ScreeningSchoolService {

    private final DepartmentService departmentService;
    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final MedicationPickupService medicationPickupService;
    private final EpidemicReportService epidemicReportService;
    private final SysMessageService sysMessageService;
    private final ReferralService referralService;
    private final ScreeningScopeHelper screeningScopeHelper;

    /** V4 阳性关键字（感染筛查结果列） */
    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "PPD+", "PPD++", "PPD+++", "EC阳性", "IGRA阳性"
    );

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        List<ScreeningSchool> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        AtomicInteger rowNum = new AtomicInteger(3); // 数据从第3行开始

        try {
            // V4 学校模板：第1行为大分组标题，第2行为字段名，数据从第3行开始
            EasyExcel.read(file.getInputStream(), ScreeningSchool.class, new ReadListener<ScreeningSchool>() {
                @Override
                public void invoke(ScreeningSchool data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    // 身份证校验
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "身份证号格式不正确");
                    }
                    // 手机号校验
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "手机号格式不正确");
                    }
                    data.setUploadBatch(batchId);
                    boolean directXray = hasDirectXrayAndDiagnosis(data);
                    data.setIsLatent((isPositive(data.getInfectionResult()) || directXray) ? 1 : 0);
                    data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
                    dataList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("学校人群筛查数据解析完成，共 {} 条", dataList.size());
                }
            }).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        // 增量导入：按身份证号去重，同一人多次导入时更新而非重复插入
        List<ScreeningSchool> toInsert = new ArrayList<>();
        List<ScreeningSchool> toUpdate = new ArrayList<>();

        for (ScreeningSchool d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            ScreeningSchool existing = lambdaQuery()
                    .eq(ScreeningSchool::getIdNumber, d.getIdNumber())
                    .last("LIMIT 1")
                    .one();
            if (existing != null) {
                // 合并基本信息，以最新导入为准
                if (StrUtil.isNotBlank(d.getName())) existing.setName(d.getName());
                if (StrUtil.isNotBlank(d.getPhone())) existing.setPhone(d.getPhone());
                if (StrUtil.isNotBlank(d.getCurrentAddress())) existing.setCurrentAddress(d.getCurrentAddress());
                if (StrUtil.isNotBlank(d.getInfectionResult())) existing.setInfectionResult(d.getInfectionResult());
                if (StrUtil.isNotBlank(d.getHasChestXray())) existing.setHasChestXray(d.getHasChestXray());
                if (d.getChestXrayDate() != null) existing.setChestXrayDate(d.getChestXrayDate());
                if (StrUtil.isNotBlank(d.getChestXrayResult())) existing.setChestXrayResult(d.getChestXrayResult());
                if (StrUtil.isNotBlank(d.getDiagnosisFirst())) existing.setDiagnosisFirst(d.getDiagnosisFirst());
                if (StrUtil.isNotBlank(d.getRemark())) existing.setRemark(d.getRemark());
                boolean directXray = hasDirectXrayAndDiagnosis(existing);
                existing.setIsLatent((isPositive(existing.getInfectionResult()) || directXray) ? 1 : 0);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setSuccessCount(dataList.size());

        // 仅对新插入且感染筛查阳性、或已含胸片+首次诊断的记录自动创建潜伏感染记录。
        // 注意：diagnosisResult 不在此处预填，需操作员在"待诊断"页面点击"诊断"后由
        // referral 流程写入；否则会被潜伏列表的 diagnosisResult 过滤器排除，
        // 导致导入的确诊/疑似记录在"待诊断"中不可见。
        // 无论导入数据是否已含胸片与诊断，trackingStatus 始终初始化为 0（待追踪）。
        // 操作员需手动完成追踪流程后，才可进行诊断。
        // 若导入数据已含 diagnosisFirst（保存在筛查表），待诊断页确认后才会写入 latent 并分流。
        List<LatentInfection> latentList = toInsert.stream()
                .filter(d -> d.getIsLatent() == 1)
                .map(d -> LatentInfection.builder()
                            .screeningId(d.getId())
                            .populationType("school")
                            .name(d.getName())
                            .idNumber(d.getIdNumber())
                            .gender(d.getGender())
                            .age(d.getAge())
                            .phone(d.getPhone())
                            .infectionResult(d.getInfectionResult())
                            .trackingStatus(0)
                            .notInPlaceCount(0)
                            .archived(0)
                            .hasChestXray(d.getHasChestXray())
                            .chestXrayDate(d.getChestXrayDate())
                            .chestXrayResult(d.getChestXrayResult())
                            .departmentId(d.getDepartmentId())
                            .build())
                .toList();
        // 更新的记录中，若 isLatent 变为1且尚无潜伏感染记录，则补创建
        List<LatentInfection> latentFromUpdated = toUpdate.stream()
                .filter(d -> d.getIsLatent() == 1)
                .filter(d -> !latentInfectionService.lambdaQuery()
                        .eq(LatentInfection::getScreeningId, d.getId())
                        .eq(LatentInfection::getPopulationType, "school")
                        .exists())
                .map(d -> LatentInfection.builder()
                            .screeningId(d.getId())
                            .populationType("school")
                            .name(d.getName())
                            .idNumber(d.getIdNumber())
                            .gender(d.getGender())
                            .age(d.getAge())
                            .phone(d.getPhone())
                            .infectionResult(d.getInfectionResult())
                            .trackingStatus(0)
                            .notInPlaceCount(0)
                            .archived(0)
                            .hasChestXray(d.getHasChestXray())
                            .chestXrayDate(d.getChestXrayDate())
                            .chestXrayResult(d.getChestXrayResult())
                            .departmentId(d.getDepartmentId())
                            .build())
                .toList();
        List<LatentInfection> allLatent = new ArrayList<>(latentList);
        allLatent.addAll(latentFromUpdated);
        if (!allLatent.isEmpty()) {
            latentInfectionService.saveBatch(allLatent, 500);
            log.info("自动创建学校人群潜伏感染记录 {} 条", allLatent.size());
        }
        syncLatentFromScreening(toUpdate, "school");

        return result;
    }

    /**
     * 增量导入时，将胸片与首次诊断同步到已存在的潜伏感染记录，
     * 避免筛查表已更新但待诊断列表仍为空的情况。
     */
    private void syncLatentFromScreening(List<ScreeningSchool> records, String populationType) {
        for (ScreeningSchool d : records) {
            if (d.getIsLatent() != 1 || d.getId() == null) continue;
            LatentInfection latent = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getScreeningId, d.getId())
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getArchived, 0)
                    .isNull(LatentInfection::getReferralResult)
                    .last("LIMIT 1")
                    .one();
            if (latent == null) continue;

            var update = latentInfectionService.lambdaUpdate()
                    .eq(LatentInfection::getId, latent.getId());
            boolean changed = false;
            if (StrUtil.isNotBlank(d.getHasChestXray())) {
                update.set(LatentInfection::getHasChestXray, d.getHasChestXray());
                changed = true;
            }
            if (d.getChestXrayDate() != null) {
                update.set(LatentInfection::getChestXrayDate, d.getChestXrayDate());
                changed = true;
            }
            if (StrUtil.isNotBlank(d.getChestXrayResult())) {
                update.set(LatentInfection::getChestXrayResult, d.getChestXrayResult());
                changed = true;
            }
            if (changed) {
                update.update();
            }
        }
    }

    /** 年度筛选：优先取感染筛查日期年份，无感染筛查日期时取胸片检查日期年份 */
    private static final String SCREEN_YEAR_SQL_EXPR =
            "((screen_date IS NOT NULL AND YEAR(screen_date) = {0})"
                    + " OR (screen_date IS NULL AND chest_xray_date IS NOT NULL AND YEAR(chest_xray_date) = {0}))";

    @Override
    public IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                             String schoolName, String district, Integer isLatent, String diagnosisFirst,
                                             String phone, String year, String entryUnit) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningSchool::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningSchool::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(schoolName), ScreeningSchool::getSchoolName, schoolName)
                .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), ScreeningSchool::getPhone, phone)
                .eq(isLatent != null, ScreeningSchool::getIsLatent, isLatent)
                .eq(StrUtil.isNotBlank(diagnosisFirst), ScreeningSchool::getDiagnosisFirst, diagnosisFirst);
        applyScreenYearFilter(wrapper, year);
        applyEntryUnitFilter(wrapper, entryUnit);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        wrapper.orderByDesc(ScreeningSchool::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /** 年度筛选：优先感染筛查日期，无则取胸片检查日期 */
    private void applyScreenYearFilter(LambdaQueryWrapper<ScreeningSchool> wrapper, String year) {
        if (StrUtil.isBlank(year)) {
            return;
        }
        try {
            wrapper.apply(SCREEN_YEAR_SQL_EXPR, Integer.parseInt(year.trim()));
        } catch (NumberFormatException ignored) {
            wrapper.eq(ScreeningSchool::getId, -1L);
        }
    }

    /** 录入单位：按部门名称模糊匹配 department_id */
    private void applyEntryUnitFilter(LambdaQueryWrapper<ScreeningSchool> wrapper, String entryUnit) {
        if (StrUtil.isBlank(entryUnit)) {
            return;
        }
        List<Long> deptIds = departmentService.resolveIdsByNameLike(entryUnit);
        if (deptIds.isEmpty()) {
            wrapper.eq(ScreeningSchool::getId, -1L);
        } else {
            wrapper.in(ScreeningSchool::getDepartmentId, deptIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningSchool data) {
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        boolean directXray = hasDirectXrayAndDiagnosis(data);
        data.setIsLatent((isPositive(data.getInfectionResult()) || directXray) ? 1 : 0);
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        save(data);

        if (data.getIsLatent() == 1) {
            // diagnosisResult 不在此预填，由"待诊断"页面诊断后由 referral 流程写入。
            // trackingStatus 始终初始化为 0，操作员需手动完成追踪后才可进行诊断。
            LatentInfection latent = LatentInfection.builder()
                    .screeningId(data.getId())
                    .populationType("school")
                    .name(data.getName())
                    .idNumber(data.getIdNumber())
                    .gender(data.getGender())
                    .age(data.getAge())
                    .phone(data.getPhone())
                    .infectionResult(data.getInfectionResult())
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .hasChestXray(data.getHasChestXray())
                    .chestXrayDate(data.getChestXrayDate())
                    .chestXrayResult(data.getChestXrayResult())
                    .departmentId(data.getDepartmentId())
                    .build();
            latentInfectionService.save(latent);
        }
    }

    private boolean isPositive(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) return false;
        return POSITIVE_KEYWORDS.stream().anyMatch(infectionResult::contains);
    }

    /**
     * 判断是否包含可直接同步的胸片检查与诊断数据。
     * 感染筛查阴性 + 胸片正常时直接结束流程，不进入待诊断。
     */
    private boolean hasDirectXrayAndDiagnosis(ScreeningSchool data) {
        if (!isPositive(data.getInfectionResult()) && "正常".equals(data.getChestXrayResult())) {
            return false;
        }
        return "是".equals(data.getHasChestXray()) && StrUtil.isNotBlank(data.getDiagnosisFirst());
    }

    /** 18位身份证格式 + 校验位验证 */
    private boolean isValidIdCard(String id) {
        // 仅校验格式（18位 + 字符规则）。Excel 以数值型存储身份证号时会丢失浮点精度，导致校验码错误，故不做校验码验证。
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    /** 11位手机号验证 */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /** 证件类型为居民身份证（或未填）时按身份证规则校验 */
    private boolean isIdCardType(String idType) {
        return StrUtil.isBlank(idType) || "居民身份证".equals(idType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromQuestionnaire(ScreeningSchool data) {
        if (isIdCardType(data.getIdType()) && StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        boolean directXray = hasDirectXrayAndDiagnosis(data);
        data.setIsLatent((isPositive(data.getInfectionResult()) || directXray) ? 1 : 0);
        data.setDepartmentId(null);
        save(data);

        if (data.getIsLatent() == 1) {
            LatentInfection latent = LatentInfection.builder()
                    .screeningId(data.getId())
                    .populationType("school")
                    .name(data.getName())
                    .idNumber(data.getIdNumber())
                    .gender(data.getGender())
                    .age(data.getAge())
                    .phone(data.getPhone())
                    .infectionResult(data.getInfectionResult())
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .hasChestXray(data.getHasChestXray())
                    .chestXrayDate(data.getChestXrayDate())
                    .chestXrayResult(data.getChestXrayResult())
                    .departmentId(null)
                    .build();
            latentInfectionService.save(latent);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningSchool data) {
        if (getById(data.getId()) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 根据感染筛查结果重新计算潜伏判定
        data.setIsLatent(isPositive(data.getInfectionResult()) ? 1 : 0);
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
        if (getById(id) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 查找关联的潜伏感染记录
        List<LatentInfection> latentList = latentInfectionService.lambdaQuery()
                .eq(LatentInfection::getScreeningId, id)
                .eq(LatentInfection::getPopulationType, "school")
                .list();
        for (LatentInfection latent : latentList) {
            deleteCascadeFromLatent(latent.getId());
        }
        // 删除筛查记录本体
        removeById(id);
        log.info("级联删除学校人群筛查记录 id={}", id);
    }

    /**
     * 从潜伏感染记录开始向下级联删除所有关联数据
     */
    private void deleteCascadeFromLatent(Long latentId) {
        // 删除关联患者及患者下游数据
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, latentId)
                .list();
        for (Patient patient : patientList) {
            Long pid = patient.getId();
            firstVisitService.lambdaUpdate().eq(cn.luyou.model.FirstVisit::getPatientId, pid).remove();
            followUpVisitService.lambdaUpdate().eq(cn.luyou.model.FollowUpVisit::getPatientId, pid).remove();
            medicationManagementService.lambdaUpdate().eq(cn.luyou.model.MedicationManagement::getPatientId, pid).remove();
            medicationPickupService.lambdaUpdate().eq(cn.luyou.model.MedicationPickup::getPatientId, pid).remove();
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            deleteNoticeAndMessages(pid, "patient");
            deleteReferralsAndMessages(pid);
            patientService.removeById(pid);
        }
        // 删除督导表、潜伏随访、按期检查
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        // 删除潜伏通知单及关联消息
        deleteNoticeAndMessages(latentId, "latent");
        // 删除潜伏分级诊疗记录及关联消息
        deleteReferralsAndMessages(latentId);
        // 删除潜伏感染记录本体
        latentInfectionService.removeById(latentId);
    }

    /** 删除指定业务ID的通知单及关联系统消息 */
    private void deleteNoticeAndMessages(Long bizId, String noticeType) {
        List<Long> noticeIds = noticeService.lambdaQuery()
                .eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .list().stream().map(Notice::getId).toList();
        if (!noticeIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, noticeIds).remove();
        }
        noticeService.lambdaUpdate()
                .eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .remove();
    }

    /** 删除指定业务ID的分级诊疗记录及关联系统消息 */
    private void deleteReferralsAndMessages(Long bizId) {
        List<Long> referralIds = referralService.lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .list().stream().map(Referral::getId).toList();
        if (!referralIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, referralIds).remove();
            referralService.lambdaUpdate().eq(Referral::getBizId, bizId).remove();
        }
    }
}
