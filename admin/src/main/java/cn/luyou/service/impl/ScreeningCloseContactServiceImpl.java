package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.SupervisionFormService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 密接人群筛查 Service（V4 三轮判定逻辑）
 *
 * 三轮判定规则：
 *  1. 首次感染结果阳性 → is_latent=1, active_round=1
 *  2. 首次阴性，半年后阳性 → is_latent=1, active_round=2
 *  3. 首次、半年后均阴，一年后阳性 → is_latent=1, active_round=3
 *  4. 三轮均阴性 → is_latent=0（筛查记录保留供统计，不创建潜伏感染记录）
 *  5. 部分轮为空（数据尚未录全） → is_latent=0（暂存，等待后续录入）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningCloseContactServiceImpl extends ServiceImpl<ScreeningCloseContactMapper, ScreeningCloseContact>
        implements ScreeningCloseContactService {

    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final EpidemicReportService epidemicReportService;

    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "PPD+", "PPD++", "PPD+++", "EC阳性", "IGRA阳性"
    );

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        List<ScreeningCloseContact> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        AtomicInteger rowNum = new AtomicInteger(3); // 数据从第3行开始

        try {
            EasyExcel.read(file.getInputStream(), ScreeningCloseContact.class, new ReadListener<ScreeningCloseContact>() {
                @Override
                public void invoke(ScreeningCloseContact data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "手机号格式不正确");
                    }
                    data.setUploadBatch(batchId);
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

        // 增量导入：按证件号匹配已有记录，合并后续轮次数据
        List<ScreeningCloseContact> toInsert = new ArrayList<>();
        List<ScreeningCloseContact> toUpdate = new ArrayList<>();
        for (ScreeningCloseContact d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                determineLatecy(d);
                toInsert.add(d);
                continue;
            }
            ScreeningCloseContact existing = lambdaQuery()
                    .eq(ScreeningCloseContact::getIdNumber, d.getIdNumber())
                    .last("LIMIT 1")
                    .one();
            if (existing != null) {
                mergeRoundData(existing, d);
                determineLatecy(existing);
                toUpdate.add(existing);
            } else {
                determineLatecy(d);
                toInsert.add(d);
            }
        }
        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        List<ScreeningCloseContact> allProcessed = new ArrayList<>(toInsert);
        allProcessed.addAll(toUpdate);

        // 仅对新判定为阳性且尚无潜伏感染记录的创建记录
        List<LatentInfection> latentList = new ArrayList<>();
        for (ScreeningCloseContact d : allProcessed) {
            if (d.getIsLatent() != 1) continue;
            boolean alreadyExists = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getScreeningId, d.getId())
                    .eq(LatentInfection::getPopulationType, "closeContact")
                    .exists();
            if (alreadyExists) continue;
            String infectionResult = switch (d.getActiveRound()) {
                case 1 -> d.getFirstInfectionResult();
                case 2 -> d.getHalfYearInfectionResult();
                case 3 -> d.getOneYearInfectionResult();
                default -> "";
            };
            latentList.add(LatentInfection.builder()
                    .screeningId(d.getId())
                    .populationType("closeContact")
                    .name(d.getName())
                    .idNumber(d.getIdNumber())
                    .gender(d.getGender())
                    .age(d.getAge())
                    .phone(d.getPhone())
                    .infectionResult(infectionResult)
                    .activeRound(d.getActiveRound())
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .build());
        }
        if (!latentList.isEmpty()) {
            latentInfectionService.saveBatch(latentList, 500);
            log.info("自动创建密接人群潜伏感染记录 {} 条", latentList.size());
        }

        result.setSuccessCount(dataList.size());
        return result;
    }

    /**
     * 将新导入数据中非空的轮次数据合并到已有记录中（增量更新）。
     */
    private void mergeRoundData(ScreeningCloseContact existing, ScreeningCloseContact incoming) {
        // 基本信息始终更新
        if (StrUtil.isNotBlank(incoming.getName())) existing.setName(incoming.getName());
        if (StrUtil.isNotBlank(incoming.getPhone())) existing.setPhone(incoming.getPhone());
        if (StrUtil.isNotBlank(incoming.getCurrentAddress())) existing.setCurrentAddress(incoming.getCurrentAddress());

        // 首次轮数据
        if (StrUtil.isNotBlank(incoming.getFirstInfectionResult())) {
            existing.setFirstScreenDate(incoming.getFirstScreenDate());
            existing.setFirstSymptomResult(incoming.getFirstSymptomResult());
            existing.setFirstInfectionMethod(incoming.getFirstInfectionMethod());
            existing.setFirstScreenResult(incoming.getFirstScreenResult());
            existing.setFirstInfectionResult(incoming.getFirstInfectionResult());
            existing.setFirstHasChestXray(incoming.getFirstHasChestXray());
            existing.setFirstChestXrayDate(incoming.getFirstChestXrayDate());
            existing.setFirstChestXrayResult(incoming.getFirstChestXrayResult());
            existing.setFirstDiagnosis(incoming.getFirstDiagnosis());
        }
        // 半年后轮数据
        if (StrUtil.isNotBlank(incoming.getHalfYearInfectionResult())) {
            existing.setHalfYearScreenDate(incoming.getHalfYearScreenDate());
            existing.setHalfYearSymptomResult(incoming.getHalfYearSymptomResult());
            existing.setHalfYearInfectionMethod(incoming.getHalfYearInfectionMethod());
            existing.setHalfYearScreenResult(incoming.getHalfYearScreenResult());
            existing.setHalfYearInfectionResult(incoming.getHalfYearInfectionResult());
            existing.setHalfYearHasChestXray(incoming.getHalfYearHasChestXray());
            existing.setHalfYearChestXrayDate(incoming.getHalfYearChestXrayDate());
            existing.setHalfYearChestXrayResult(incoming.getHalfYearChestXrayResult());
            existing.setHalfYearDiagnosis(incoming.getHalfYearDiagnosis());
        }
        // 一年后轮数据
        if (StrUtil.isNotBlank(incoming.getOneYearInfectionResult())) {
            existing.setOneYearScreenDate(incoming.getOneYearScreenDate());
            existing.setOneYearSymptomResult(incoming.getOneYearSymptomResult());
            existing.setOneYearInfectionMethod(incoming.getOneYearInfectionMethod());
            existing.setOneYearScreenResult(incoming.getOneYearScreenResult());
            existing.setOneYearInfectionResult(incoming.getOneYearInfectionResult());
            existing.setOneYearHasChestXray(incoming.getOneYearHasChestXray());
            existing.setOneYearChestXrayDate(incoming.getOneYearChestXrayDate());
            existing.setOneYearChestXrayResult(incoming.getOneYearChestXrayResult());
            existing.setOneYearDiagnosis(incoming.getOneYearDiagnosis());
        }
        // 预防治疗管理情况
        if (StrUtil.isNotBlank(incoming.getHasPreventiveTreatment())) existing.setHasPreventiveTreatment(incoming.getHasPreventiveTreatment());
        if (StrUtil.isNotBlank(incoming.getPreventivePlan())) existing.setPreventivePlan(incoming.getPreventivePlan());
        if (incoming.getPreventiveStartDate() != null) existing.setPreventiveStartDate(incoming.getPreventiveStartDate());
        if (incoming.getPreventiveEndDate() != null) existing.setPreventiveEndDate(incoming.getPreventiveEndDate());
        if (StrUtil.isNotBlank(incoming.getPreventiveResult())) existing.setPreventiveResult(incoming.getPreventiveResult());
        if (StrUtil.isNotBlank(incoming.getPreventiveManager())) existing.setPreventiveManager(incoming.getPreventiveManager());
        // 惠民方式 + 备注
        if (StrUtil.isNotBlank(incoming.getBenefitMethod())) existing.setBenefitMethod(incoming.getBenefitMethod());
        if (StrUtil.isNotBlank(incoming.getRemark())) existing.setRemark(incoming.getRemark());
    }

    @Override
    public IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                                    String district, Integer isLatent) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningCloseContact::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningCloseContact::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), ScreeningCloseContact::getDistrict, district)
                .eq(isLatent != null, ScreeningCloseContact::getIsLatent, isLatent)
                .orderByDesc(ScreeningCloseContact::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningCloseContact data) {
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        determineLatecy(data);
        save(data);

        if (data.getIsLatent() == 1) {
            String infectionResult = switch (data.getActiveRound()) {
                case 1 -> data.getFirstInfectionResult();
                case 2 -> data.getHalfYearInfectionResult();
                case 3 -> data.getOneYearInfectionResult();
                default -> "";
            };
            LatentInfection latent = LatentInfection.builder()
                    .screeningId(data.getId())
                    .populationType("closeContact")
                    .name(data.getName())
                    .idNumber(data.getIdNumber())
                    .gender(data.getGender())
                    .age(data.getAge())
                    .phone(data.getPhone())
                    .infectionResult(infectionResult)
                    .activeRound(data.getActiveRound())
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .build();
            latentInfectionService.save(latent);
        }
    }

    /**
     * 三轮顺序判定逻辑（严格按轮次顺序：首次 → 半年后 → 一年后）
     * - 首次阳性 → round 1
     * - 首次阴、半年后阳性 → round 2
     * - 首次阴、半年后阴、一年后阳性 → round 3
     * - 三轮均阴 → 归档（isLatent=0）
     * - 未填满 → 暂存待后续
     */
    private void determineLatecy(ScreeningCloseContact data) {
        boolean firstFilled = StrUtil.isNotBlank(data.getFirstInfectionResult());
        boolean halfFilled  = StrUtil.isNotBlank(data.getHalfYearInfectionResult());
        boolean oneFilled   = StrUtil.isNotBlank(data.getOneYearInfectionResult());

        // 首次必须先填
        if (!firstFilled) {
            data.setIsLatent(0);
            return;
        }
        // 首次阳性 → round 1
        if (isPositive(data.getFirstInfectionResult())) {
            data.setIsLatent(1);
            data.setActiveRound(1);
            return;
        }
        // 首次阴性，半年后未填 → 暂存
        if (!halfFilled) {
            data.setIsLatent(0);
            return;
        }
        // 首次阴、半年后阳性 → round 2
        if (isPositive(data.getHalfYearInfectionResult())) {
            data.setIsLatent(1);
            data.setActiveRound(2);
            return;
        }
        // 首次阴、半年后阴、一年后未填 → 暂存
        if (!oneFilled) {
            data.setIsLatent(0);
            return;
        }
        // 首次阴、半年后阴、一年后阳性 → round 3
        if (isPositive(data.getOneYearInfectionResult())) {
            data.setIsLatent(1);
            data.setActiveRound(3);
            return;
        }
        // 三轮均阴性 → 归档
        data.setIsLatent(0);
    }

    private boolean isPositive(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) return false;
        return POSITIVE_KEYWORDS.stream().anyMatch(infectionResult::contains);
    }

    private boolean isValidIdCard(String id) {
        if (id == null || id.length() != 18) return false;
        if (!id.matches("\\d{17}[\\dXx]")) return false;
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String[] checkCodes = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += Character.getNumericValue(id.charAt(i)) * weights[i];
        return checkCodes[sum % 11].equalsIgnoreCase(String.valueOf(id.charAt(17)));
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningCloseContact data) {
        if (getById(data.getId()) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 保留系统计算字段（isLatent/activeRound），重新执行三轮判定
        determineLatecy(data);
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
        if (getById(id) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        List<LatentInfection> latentList = latentInfectionService.lambdaQuery()
                .eq(LatentInfection::getScreeningId, id)
                .eq(LatentInfection::getPopulationType, "closeContact")
                .list();
        for (LatentInfection latent : latentList) {
            deleteCascadeFromLatent(latent.getId());
        }
        removeById(id);
        log.info("级联删除密接人群筛查记录 id={}", id);
    }

    private void deleteCascadeFromLatent(Long latentId) {
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, latentId)
                .list();
        for (Patient patient : patientList) {
            Long pid = patient.getId();
            firstVisitService.lambdaUpdate().eq(cn.luyou.model.FirstVisit::getPatientId, pid).remove();
            followUpVisitService.lambdaUpdate().eq(cn.luyou.model.FollowUpVisit::getPatientId, pid).remove();
            medicationManagementService.lambdaUpdate().eq(cn.luyou.model.MedicationManagement::getPatientId, pid).remove();
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            noticeService.lambdaUpdate()
                    .eq(Notice::getBizId, pid)
                    .eq(Notice::getNoticeType, "patient")
                    .remove();
            patientService.removeById(pid);
        }
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        noticeService.lambdaUpdate()
                .eq(Notice::getBizId, latentId)
                .eq(Notice::getNoticeType, "latent")
                .remove();
        latentInfectionService.removeById(latentId);
    }
}
