package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.utils.BaseContext;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 密接人群筛查 Service（新模板73列，基于 finalScreeningResult 分类）
 *
 * 分类规则（AE列 = final_screening_result）：
 *  - 活动性肺结核  → ccStatus=1，直接创建患者管理记录
 *  - 潜伏感染者    → ccStatus=2，进入密接潜伏感染专属流程
 *  - 未做          → ccStatus=4，进入6/12/24月随访监测
 *  - 未发现异常    → ccStatus=6，进入3月复查流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningCloseContactServiceImpl extends ServiceImpl<ScreeningCloseContactMapper, ScreeningCloseContact>
        implements ScreeningCloseContactService {

    private final PatientService patientService;
    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final EpidemicReportService epidemicReportService;

    /** 活动性肺结核的最终筛查结果标识（模板中的文字） */
    private static final String RESULT_ACTIVE_TB = "活动性肺结核";
    /** 潜伏感染者 */
    private static final String RESULT_LATENT = "潜伏感染者";
    /** 未做 */
    private static final String RESULT_NOT_DONE = "未做";
    /** 未发现异常 */
    private static final String RESULT_NORMAL = "未发现异常";

    // ==================== 上传与导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        List<ScreeningCloseContact> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        AtomicInteger rowNum = new AtomicInteger(3); // 数据从第3行（跳过2行表头+示例）

        try {
            EasyExcel.read(file.getInputStream(), ScreeningCloseContact.class, new ReadListener<ScreeningCloseContact>() {
                @Override
                public void invoke(ScreeningCloseContact data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "接触者身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "接触者手机号格式不正确");
                    }
                    // 从登记日期提取年份
                    if (data.getRegistrationDate() != null) {
                        data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
                    }
                    data.setUploadBatch(batchId);
                    data.setDepartmentId(BaseContext.getCurrentDepartmentId());
                    dataList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("密接人群筛查数据解析完成，共 {} 条", dataList.size());
                }
            }).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        // 增量导入：按接触者身份证号匹配已有记录（同一人多次导入时合并随访数据）
        List<ScreeningCloseContact> toInsert = new ArrayList<>();
        List<ScreeningCloseContact> toUpdate = new ArrayList<>();

        for (ScreeningCloseContact d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                determineStatus(d);
                toInsert.add(d);
                continue;
            }
            ScreeningCloseContact existing = lambdaQuery()
                    .eq(ScreeningCloseContact::getIdNumber, d.getIdNumber())
                    .last("LIMIT 1")
                    .one();
            if (existing != null) {
                mergeFollowupData(existing, d);
                determineStatus(existing);
                toUpdate.add(existing);
            } else {
                determineStatus(d);
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        // 对新录入且为活动性肺结核的，直接创建患者记录
        List<ScreeningCloseContact> newRecords = new ArrayList<>(toInsert);
        List<ScreeningCloseContact> updatedActiveRecords = toUpdate.stream()
                .filter(d -> RESULT_ACTIVE_TB.equals(d.getFinalScreeningResult()))
                .toList();
        newRecords.addAll(updatedActiveRecords);

        List<Patient> patientList = new ArrayList<>();
        for (ScreeningCloseContact d : newRecords) {
            if (!RESULT_ACTIVE_TB.equals(d.getFinalScreeningResult())) continue;
            boolean alreadyExists = patientService.lambdaQuery()
                    .eq(Patient::getScreeningId, d.getId())
                    .eq(Patient::getPopulationType, "closeContact")
                    .exists();
            if (alreadyExists) continue;
            patientList.add(buildPatient(d));
        }
        if (!patientList.isEmpty()) {
            patientService.saveBatch(patientList, 500);
            log.info("密接人群 - 活动性肺结核自动创建患者记录 {} 条", patientList.size());
        }

        result.setSuccessCount(dataList.size());
        return result;
    }

    /**
     * 基于随访数据合并（同一接触者多次导入时，补全后续月份随访字段）
     */
    private void mergeFollowupData(ScreeningCloseContact existing, ScreeningCloseContact incoming) {
        // 原患者信息可能更新
        if (StrUtil.isNotBlank(incoming.getSourcePatientName())) existing.setSourcePatientName(incoming.getSourcePatientName());

        // 基本信息始终以最新为准
        if (StrUtil.isNotBlank(incoming.getName())) existing.setName(incoming.getName());
        if (StrUtil.isNotBlank(incoming.getPhone())) existing.setPhone(incoming.getPhone());
        if (StrUtil.isNotBlank(incoming.getCurrentAddress())) existing.setCurrentAddress(incoming.getCurrentAddress());

        // 初次筛查（若已有则保留，以防覆盖旧数据）
        if (StrUtil.isNotBlank(incoming.getFinalScreeningResult())) {
            existing.setFinalScreeningResult(incoming.getFinalScreeningResult());
            existing.setInfectionCheckResult(incoming.getInfectionCheckResult());
            existing.setImagingResult(incoming.getImagingResult());
            existing.setSputumCheckResult(incoming.getSputumCheckResult());
        }

        // 预防治疗情况
        if (StrUtil.isNotBlank(incoming.getHasPreventiveTreatment())) existing.setHasPreventiveTreatment(incoming.getHasPreventiveTreatment());
        if (StrUtil.isNotBlank(incoming.getPreventivePlan())) existing.setPreventivePlan(incoming.getPreventivePlan());
        if (StrUtil.isNotBlank(incoming.getTreatmentCompleted())) existing.setTreatmentCompleted(incoming.getTreatmentCompleted());
        if (StrUtil.isNotBlank(incoming.getIncompleteReason())) existing.setIncompleteReason(incoming.getIncompleteReason());

        // 6月随访
        if (StrUtil.isNotBlank(incoming.getFollowup6Result())) {
            existing.setFollowup6DueDate(incoming.getFollowup6DueDate());
            existing.setFollowup6ScreenDate(incoming.getFollowup6ScreenDate());
            existing.setFollowup6Symptom1(incoming.getFollowup6Symptom1());
            existing.setFollowup6ImagingResult(incoming.getFollowup6ImagingResult());
            existing.setFollowup6SputumResult(incoming.getFollowup6SputumResult());
            existing.setFollowup6Result(incoming.getFollowup6Result());
        }
        // 12月随访
        if (StrUtil.isNotBlank(incoming.getFollowup12Result())) {
            existing.setFollowup12DueDate(incoming.getFollowup12DueDate());
            existing.setFollowup12ScreenDate(incoming.getFollowup12ScreenDate());
            existing.setFollowup12Symptom1(incoming.getFollowup12Symptom1());
            existing.setFollowup12ImagingResult(incoming.getFollowup12ImagingResult());
            existing.setFollowup12SputumResult(incoming.getFollowup12SputumResult());
            existing.setFollowup12Result(incoming.getFollowup12Result());
        }
        // 24月随访
        if (StrUtil.isNotBlank(incoming.getFollowup24Result())) {
            existing.setFollowup24DueDate(incoming.getFollowup24DueDate());
            existing.setFollowup24ScreenDate(incoming.getFollowup24ScreenDate());
            existing.setFollowup24Symptom1(incoming.getFollowup24Symptom1());
            existing.setFollowup24ImagingResult(incoming.getFollowup24ImagingResult());
            existing.setFollowup24SputumResult(incoming.getFollowup24SputumResult());
            existing.setFollowup24Result(incoming.getFollowup24Result());
        }

        if (StrUtil.isNotBlank(incoming.getRemark())) existing.setRemark(incoming.getRemark());
    }

    /**
     * 根据 finalScreeningResult 设置系统流程状态 ccStatus
     * 注意：若当前 ccStatus 已经处于 "进行中/已归档" 等高级状态，则不降级（仅对初始状态赋值）
     */
    private void determineStatus(ScreeningCloseContact data) {
        if (data.getCcStatus() != null && data.getCcStatus() > 0) return; // 已有业务状态不覆盖
        String result = data.getFinalScreeningResult();
        if (RESULT_ACTIVE_TB.equals(result)) {
            data.setCcStatus(1); // 活动性肺结核
        } else if (RESULT_LATENT.equals(result)) {
            data.setCcStatus(2); // 潜伏感染者-管理中
        } else if (RESULT_NOT_DONE.equals(result)) {
            data.setCcStatus(4); // 随访监测中
        } else if (RESULT_NORMAL.equals(result)) {
            data.setCcStatus(6); // 未发现异常-待3月复查
        } else {
            data.setCcStatus(0); // 待处理
        }
    }

    private Patient buildPatient(ScreeningCloseContact d) {
        return Patient.builder()
                .screeningId(d.getId())
                .populationType("closeContact")
                .name(d.getName())
                .gender(d.getGender())
                .age(d.getAge())
                .idNumber(d.getIdNumber())
                .phone(d.getPhone())
                .householdAddress(d.getHouseholdAddress())
                .currentAddress(d.getCurrentAddress())
                .diagnosisResult("活动性肺结核")
                .source("confirmed")
                .archived(0)
                .departmentId(d.getDepartmentId())
                .build();
    }

    // ==================== 查询 ====================

    @Override
    public IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                                   String district, Integer ccStatus, String finalScreeningResult) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningCloseContact::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningCloseContact::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), ScreeningCloseContact::getDistrict, district)
                .eq(ccStatus != null, ScreeningCloseContact::getCcStatus, ccStatus)
                .eq(StrUtil.isNotBlank(finalScreeningResult), ScreeningCloseContact::getFinalScreeningResult, finalScreeningResult)
                .orderByDesc(ScreeningCloseContact::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            wrapper.eq(ScreeningCloseContact::getDepartmentId, BaseContext.getCurrentDepartmentId());
        }
        return page(new Page<>(page, size), wrapper);
    }

    // ==================== 单条增删改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningCloseContact data) {
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        determineStatus(data);
        data.setDepartmentId(BaseContext.getCurrentDepartmentId());
        save(data);
        if (RESULT_ACTIVE_TB.equals(data.getFinalScreeningResult())) {
            patientService.save(buildPatient(data));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningCloseContact data) {
        if (getById(data.getId()) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
        ScreeningCloseContact existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 级联删除患者管理数据
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getScreeningId, id)
                .eq(Patient::getPopulationType, "closeContact")
                .list();
        for (Patient patient : patientList) {
            Long pid = patient.getId();
            firstVisitService.lambdaUpdate().eq(cn.luyou.model.FirstVisit::getPatientId, pid).remove();
            followUpVisitService.lambdaUpdate().eq(cn.luyou.model.FollowUpVisit::getPatientId, pid).remove();
            medicationManagementService.lambdaUpdate().eq(cn.luyou.model.MedicationManagement::getPatientId, pid).remove();
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            noticeService.lambdaUpdate()
                    .eq(Notice::getBizId, pid).eq(Notice::getNoticeType, "patient").remove();
            patientService.removeById(pid);
        }
        // 删除潜伏感染者的督导表等
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, id).remove();
        removeById(id);
        log.info("级联删除密接人群筛查记录 id={}", id);
    }

    // ==================== 密接专属业务操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setExpectedTreatmentEndDate(Long id, LocalDate expectedDate) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        record.setExpectedTreatmentEndDate(expectedDate);
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTreatmentDone(Long id, boolean done) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        if (done) {
            // 完成：归档
            record.setCcStatus(3);
            record.setTreatmentCompleted("是");
        } else {
            // 未完成：进入随访监测
            record.setCcStatus(4);
            record.setTreatmentCompleted("否");
        }
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitThreeMonthCheck(Long id, LocalDate checkDate, String checkResult, String finalResult) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        record.setThreeMonthCheckDate(checkDate);
        record.setThreeMonthCheckResult(checkResult);
        record.setThreeMonthFinalResult(finalResult);
        if ("阴性".equals(finalResult)) {
            record.setCcStatus(7); // 3月复查阴性，结束
        } else if ("阳性".equals(finalResult)) {
            // 转入潜伏感染者流程
            record.setFinalScreeningResult(RESULT_LATENT);
            record.setCcStatus(2);
        }
        updateById(record);
    }

    @Override
    public Map<String, Long> countByFinalResult() {
        List<ScreeningCloseContact> all = lambdaQuery()
                .select(ScreeningCloseContact::getFinalScreeningResult)
                .isNotNull(ScreeningCloseContact::getFinalScreeningResult)
                .list();
        return all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getFinalScreeningResult() == null ? "未分类" : s.getFinalScreeningResult(),
                        Collectors.counting()
                ));
    }

    // ==================== 工具方法 ====================

    private boolean isValidIdCard(String id) {
        // 仅校验格式（18位 + 字符规则）。Excel 以数值型存储身份证号时会丢失浮点精度，导致校验码错误，故不做校验码验证。
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
