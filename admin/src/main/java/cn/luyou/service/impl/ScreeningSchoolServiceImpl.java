package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningSchoolService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningSchoolServiceImpl extends ServiceImpl<ScreeningSchoolMapper, ScreeningSchool>
        implements ScreeningSchoolService {

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

        saveBatch(dataList, 500);
        result.setSuccessCount(dataList.size());

        // 感染筛查结果阳性者 或 包含胸片诊断数据者，自动创建潜伏感染记录
        List<LatentInfection> latentList = dataList.stream()
                .filter(d -> d.getIsLatent() == 1)
                .map(d -> {
                    boolean directXray = hasDirectXrayAndDiagnosis(d);
                    return LatentInfection.builder()
                            .screeningId(d.getId())
                            .populationType("school")
                            .name(d.getName())
                            .idNumber(d.getIdNumber())
                            .gender(d.getGender())
                            .age(d.getAge())
                            .phone(d.getPhone())
                            .infectionResult(d.getInfectionResult())
                            .trackingStatus(directXray ? 1 : 0)
                            .notInPlaceCount(0)
                            .archived(0)
                            .hasChestXray(d.getHasChestXray())
                            .chestXrayDate(d.getChestXrayDate())
                            .chestXrayResult(d.getChestXrayResult())
                            .diagnosisFirst(d.getDiagnosisFirst())
                            .diagnosisResult(d.getDiagnosisFirst())
                            .build();
                })
                .toList();
        if (!latentList.isEmpty()) {
            latentInfectionService.saveBatch(latentList, 500);
            log.info("自动创建学校人群潜伏感染记录 {} 条", latentList.size());
        }

        return result;
    }

    @Override
    public IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                             String schoolName, String district, Integer isLatent) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningSchool::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningSchool::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(schoolName), ScreeningSchool::getSchoolName, schoolName)
                .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district)
                .eq(isLatent != null, ScreeningSchool::getIsLatent, isLatent)
                .orderByDesc(ScreeningSchool::getCreateTime);
        return page(new Page<>(page, size), wrapper);
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
                    .trackingStatus(directXray ? 1 : 0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .hasChestXray(data.getHasChestXray())
                    .chestXrayDate(data.getChestXrayDate())
                    .chestXrayResult(data.getChestXrayResult())
                    .diagnosisFirst(data.getDiagnosisFirst())
                    .diagnosisResult(data.getDiagnosisFirst())
                    .build();
            latentInfectionService.save(latent);
        }
    }

    private boolean isPositive(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) return false;
        return POSITIVE_KEYWORDS.stream().anyMatch(infectionResult::contains);
    }

    /** 判断是否包含可直接同步的胸片检查与诊断数据 */
    private boolean hasDirectXrayAndDiagnosis(ScreeningSchool data) {
        return "是".equals(data.getHasChestXray()) && StrUtil.isNotBlank(data.getDiagnosisFirst());
    }

    /** 18位身份证格式 + 校验位验证 */
    private boolean isValidIdCard(String id) {
        if (id == null || id.length() != 18) return false;
        if (!id.matches("\\d{17}[\\dXx]")) return false;
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String[] checkCodes = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += Character.getNumericValue(id.charAt(i)) * weights[i];
        return checkCodes[sum % 11].equalsIgnoreCase(String.valueOf(id.charAt(17)));
    }

    /** 11位手机号验证 */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
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
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            noticeService.lambdaUpdate()
                    .eq(Notice::getBizId, pid)
                    .eq(Notice::getNoticeType, "patient")
                    .remove();
            patientService.removeById(pid);
        }
        // 删除督导表、潜伏随访、按期检查
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        // 删除潜伏通知单
        noticeService.lambdaUpdate()
                .eq(Notice::getBizId, latentId)
                .eq(Notice::getNoticeType, "latent")
                .remove();
        // 删除潜伏感染记录本体
        latentInfectionService.removeById(latentId);
    }
}
