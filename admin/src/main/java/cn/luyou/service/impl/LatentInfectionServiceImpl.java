package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatentInfectionServiceImpl extends ServiceImpl<LatentInfectionMapper, LatentInfection>
        implements LatentInfectionService {

    private final PatientService patientService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;
    private final NoticeMapper noticeMapper;

    private static final List<String> DIAGNOSIS_TO_PATIENT = Arrays.asList("疑似肺结核", "确诊患者");

    /**
     * 首次诊断结果（diagnosisFirst）→ 转诊编码（referralResult）映射。
     * 录入胸片诊断或批量导入胸片诊断后，根据该映射自动驱动转诊流程，
     * 与"诊断"按钮 referral() 方法的语义保持一致。
     */
    private static final Map<String, String> DIAGNOSIS_TO_REFERRAL;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("排除", "excluded");
        m.put("其他", "other");
        m.put("确诊患者", "confirmed");
        m.put("疑似肺结核", "suspected");
        m.put("潜伏感染者", "latent");
        DIAGNOSIS_TO_REFERRAL = java.util.Collections.unmodifiableMap(m);
    }

    @Override
    public IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                             String name, String idNumber, Integer trackingStatus, Integer archived,
                                             String referralResult) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), LatentInfection::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                .eq(archived != null, LatentInfection::getArchived, archived)
                // 潜伏感染列表始终排除确诊患者/疑似肺结核，这些数据属于患者管理模块。
                // 注意：SQL 中 NULL NOT IN (...) 结果为 NULL（即被过滤掉），
                // 必须显式放行 diagnosisResult 为 NULL 的记录（导入后未录入诊断的待诊断数据）。
                .and(w -> w.isNull(LatentInfection::getDiagnosisResult)
                        .or()
                        .notIn(LatentInfection::getDiagnosisResult, Arrays.asList("确诊患者", "疑似肺结核")));

        // referralResult 过滤：pending = 查尚未转诊的记录；具体值 = 精确匹配
        if ("pending".equals(referralResult)) {
            wrapper.isNull(LatentInfection::getReferralResult);
        } else if (StrUtil.isNotBlank(referralResult)) {
            wrapper.eq(LatentInfection::getReferralResult, referralResult);
        }

        wrapper.orderByDesc(LatentInfection::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            wrapper.eq(LatentInfection::getDepartmentId, BaseContext.getCurrentDepartmentId());
        }
        IPage<LatentInfection> result = page(new Page<>(page, size), wrapper);

        // 补充通知单发送状态：用于前端控制“发送通知单”禁用和督导表启用
        List<LatentInfection> records = result.getRecords();
        if (records == null || records.isEmpty()) return result;

        List<Long> latentIds = records.stream()
                .map(LatentInfection::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        records.forEach(this::fillNoticeAutoFields);
        if (latentIds.isEmpty()) {
            records.forEach(r -> r.setNoticeSent(false));
            return result;
        }

        Set<Long> sentBizIds = new HashSet<>(noticeMapper.selectList(
                new LambdaQueryWrapper<cn.luyou.model.Notice>()
                        .in(cn.luyou.model.Notice::getBizId, latentIds)
                        .eq(cn.luyou.model.Notice::getNoticeType, "latent")
                        .eq(cn.luyou.model.Notice::getPopulationType, populationType)
                        .select(cn.luyou.model.Notice::getBizId)
        ).stream().map(cn.luyou.model.Notice::getBizId).toList());

        records.forEach(r -> r.setNoticeSent(sentBizIds.contains(r.getId())));
        return result;
    }

    private void fillNoticeAutoFields(LatentInfection latent) {
        if (latent == null || latent.getScreeningId() == null || StrUtil.isBlank(latent.getPopulationType())) return;
        switch (latent.getPopulationType()) {
            case "school" -> {
                ScreeningSchool s = screeningSchoolMapper.selectById(latent.getScreeningId());
                if (s == null) return;
                latent.setBirthDate(s.getBirthDate());
                latent.setEthnicity(s.getEthnicity());
                latent.setCurrentAddress(s.getCurrentAddress());
                latent.setHouseholdAddress(s.getHouseholdAddress());
                latent.setScreenDate(s.getScreenDate());
                latent.setScreenMethod(s.getScreenMethod());
                latent.setScreenResult(s.getScreenResult());
                // 学校人群通知单人群分类默认“学生”
                latent.setCrowdCategory("学生");
            }
            case "keyPopulation" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(latent.getScreeningId());
                if (k == null) return;
                latent.setBirthDate(k.getBirthDate());
                latent.setEthnicity(k.getEthnicity());
                latent.setCurrentAddress(k.getCurrentAddress());
                latent.setHouseholdAddress(k.getHouseholdAddress());
                latent.setScreenDate(k.getScreenDate());
                latent.setScreenMethod(k.getScreenMethod());
                latent.setScreenResult(k.getScreenResult());
                latent.setCrowdCategory(resolveKeyPopulationCrowdCategory(k));
            }
            case "closeContact" -> {
                ScreeningCloseContact c = screeningCloseContactMapper.selectById(latent.getScreeningId());
                if (c == null) return;
                // ScreeningCloseContact 无独立 birthDate 字段，通知单出生日期留空由前端手填
                latent.setEthnicity(c.getEthnicity());
                latent.setCurrentAddress(c.getCurrentAddress());
                latent.setHouseholdAddress(c.getHouseholdAddress());
                latent.setCrowdCategory("密接");
                Integer round = latent.getActiveRound() == null ? 1 : latent.getActiveRound();
                switch (round) {
                    case 1 -> {
                        // 首次筛查：index 18=firstScreenDate, 22=infectionCheckMethod, 23=infectionCheckResult
                        latent.setScreenDate(c.getFirstScreenDate());
                        latent.setScreenMethod(c.getInfectionCheckMethod());
                        latent.setScreenResult(c.getInfectionCheckResult());
                    }
                    case 2 -> {
                        // 6月随访：index 40=followup6ScreenDate, 44=followup6ImagingMethod, 49=followup6Result
                        latent.setScreenDate(c.getFollowup6ScreenDate());
                        latent.setScreenMethod(c.getFollowup6ImagingMethod());
                        latent.setScreenResult(c.getFollowup6Result());
                    }
                    case 3 -> {
                        // 12月随访：index 51=followup12ScreenDate, 55=followup12ImagingMethod, 60=followup12Result
                        latent.setScreenDate(c.getFollowup12ScreenDate());
                        latent.setScreenMethod(c.getFollowup12ImagingMethod());
                        latent.setScreenResult(c.getFollowup12Result());
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    private String resolveKeyPopulationCrowdCategory(ScreeningKeyPopulation k) {
        if ("是".equals(k.getCrowdCategoryClose())) return "密接";
        if ("是".equals(k.getCrowdCategoryStudent())) return "学生";
        if ("是".equals(k.getCrowdCategoryTeacher())) return "教职工";
        if ("是".equals(k.getCrowdCategoryElder())) return "老年人";
        if ("是".equals(k.getCrowdCategoryDiabetes())) return "糖尿病";
        if ("是".equals(k.getCrowdCategoryDual())) return "双感";
        if ("是".equals(k.getCrowdCategoryTbHist())) return "既往结核";
        if ("是".equals(k.getCrowdCategoryNormal())) return "非重点人群";
        return "";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }

        switch (status) {
            case 1 -> entity.setTrackingStatus(1); // 到位
            case 2 -> {
                // 未到位
                int count = entity.getNotInPlaceCount() + 1;
                entity.setNotInPlaceCount(count);
                if (count >= 3) {
                    entity.setTrackingStatus(4); // 强制结束
                    entity.setTrackingRemark(remark);
                    entity.setArchived(1);
                    entity.setArchivedTime(LocalDateTime.now());
                } else {
                    entity.setTrackingStatus(2);
                }
            }
            case 3 -> {
                // 其他
                entity.setTrackingStatus(3);
                entity.setTrackingRemark(remark);
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的追踪状态");
        }

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXrayAndDiagnosis(Long id, Map<String, Object> data) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入胸片与诊断结果");
        }
        if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片与诊断已录入，不可重复操作");
        }

        String diagnosisFirst = data.getOrDefault("diagnosisFirst", "").toString();
        if (StrUtil.isBlank(diagnosisFirst)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }

        String hasXray = data.getOrDefault("hasChestXray", "").toString();
        String xrayResult = data.getOrDefault("chestXrayResult", "").toString();
        LocalDate xrayDate = null;
        Object xrayDateObj = data.get("chestXrayDate");
        if (xrayDateObj != null && StrUtil.isNotBlank(xrayDateObj.toString())) {
            xrayDate = LocalDate.parse(xrayDateObj.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        // 写入胸片字段与首次诊断（diagnosisResult 由后续联动转诊决定，不在此处直接写入，
        // 避免数据被过早过滤而从待诊断/患者管理列表中"消失"）
        lambdaUpdate()
                .eq(LatentInfection::getId, entity.getId())
                .set(LatentInfection::getHasChestXray, hasXray)
                .set(LatentInfection::getChestXrayDate, xrayDate)
                .set(LatentInfection::getChestXrayResult, xrayResult)
                .set(LatentInfection::getDiagnosisFirst, diagnosisFirst)
                .update();

        // V4 sheet2：胸片与诊断同步回写到筛查表
        writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult, diagnosisFirst);

        // 根据首次诊断自动驱动转诊：
        //  - 排除/其他       → 归档
        //  - 确诊患者/疑似肺结核 → 创建患者档案 + 归档（数据进入患者管理）
        //  - 潜伏感染者      → 进入潜伏感染管理（不归档）
        // 与 referral() 的语义一致，避免再让用户多点一次"诊断"按钮。
        String referralCode = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
        if (referralCode != null) {
            // 重新加载，确保后续 updateById 写回的字段最新
            LatentInfection refreshed = getById(id);
            applyReferralOutcome(refreshed, referralCode, null);
        }
    }

    /**
     * 将胸片与诊断数据回写到对应的筛查管理表。
     * 使用 LambdaUpdateWrapper 精确更新目标字段，避免 updateById 回写其他无关字段。
     */
    private void writeBackXrayToScreening(LatentInfection entity, String hasXray,
                                          LocalDate xrayDate, String xrayResult, String diagnosis) {
        Long sid = entity.getScreeningId();
        if (sid == null) return;
        String type = entity.getPopulationType();
        if (StrUtil.isBlank(type)) return;

        switch (type) {
            case "school" -> {
                var update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ScreeningSchool>()
                        .eq(ScreeningSchool::getId, sid);
                if (StrUtil.isNotBlank(hasXray)) update.set(ScreeningSchool::getHasChestXray, hasXray);
                if (xrayDate != null) update.set(ScreeningSchool::getChestXrayDate, xrayDate);
                if (StrUtil.isNotBlank(xrayResult)) update.set(ScreeningSchool::getChestXrayResult, xrayResult);
                if (StrUtil.isNotBlank(diagnosis)) update.set(ScreeningSchool::getDiagnosisFirst, diagnosis);
                screeningSchoolMapper.update(null, update);
            }
            case "keyPopulation" -> {
                var update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ScreeningKeyPopulation>()
                        .eq(ScreeningKeyPopulation::getId, sid);
                if (StrUtil.isNotBlank(hasXray)) update.set(ScreeningKeyPopulation::getHasChestXray, hasXray);
                if (xrayDate != null) update.set(ScreeningKeyPopulation::getChestXrayDate, xrayDate);
                if (StrUtil.isNotBlank(xrayResult)) update.set(ScreeningKeyPopulation::getChestXrayResult, xrayResult);
                if (StrUtil.isNotBlank(diagnosis)) update.set(ScreeningKeyPopulation::getDiagnosisFirst, diagnosis);
                screeningKeyPopulationMapper.update(null, update);
            }
            case "closeContact" -> {
                // 密接人群的胸片回写到 latent_infection 表已完成，无需再回写到筛查表
                // （密接筛查表结构与胸片字段不对应，由各轮次随访字段承载）
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importXrayBatch(MultipartFile file, String populationType) {
        // 与各人群主导入的 headRowNumber 保持一致
        int headerRows = switch (populationType) {
            case "keyPopulation" -> 4;
            default -> 2;
        };

        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream())
                    .headRowNumber(headerRows)
                    .sheet()
                    .doReadSync()
                    .forEach(row -> rows.add((Map<String, Object>) row));
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败：" + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        int updated = 0;
        for (Map<String, Object> row : rows) {
            // 密接人群证件号在列12，其余人群在列9
            int idNumberIdx = "closeContact".equals(populationType) ? 12 : 9;
            String idNumber = getStrCell(row, idNumberIdx);
            if (StrUtil.isBlank(idNumber)) continue;

            LatentInfection entity = lambdaQuery()
                    .eq(LatentInfection::getIdNumber, idNumber)
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getArchived, 0)
                    .last("LIMIT 1")
                    .one();
            if (entity == null || !Integer.valueOf(1).equals(entity.getTrackingStatus())) continue;
            if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) continue;

            // 根据人群类型确定 Excel 中胸片/诊断字段的列索引（与模型 @ExcelProperty(index=N) 对齐）
            int hasXrayIdx, xrayDateIdx, xrayResultIdx, diagnosisIdx;
            switch (populationType) {
                case "school" -> {
                    // 学校人群：hasChestXray(25) chestXrayDate(26) chestXrayResult(27) diagnosisFirst(28)
                    hasXrayIdx = 25; xrayDateIdx = 26; xrayResultIdx = 27; diagnosisIdx = 28;
                }
                case "keyPopulation" -> {
                    // 重点人群：hasChestXray(37) chestXrayDate(38) chestXrayResult(39) diagnosisFirst(40)
                    hasXrayIdx = 37; xrayDateIdx = 38; xrayResultIdx = 39; diagnosisIdx = 40;
                }
                case "closeContact" -> {
                    // 密接人群：按阳性轮次读取对应列组
                    Integer round = entity.getActiveRound();
                    if (round == null) round = 1;
                    switch (round) {
                        // 首次筛查：imagingDate(24) imagingMethod(25) imagingResult(26) finalScreeningResult(30)
                        case 1 -> { hasXrayIdx = 24; xrayDateIdx = 25; xrayResultIdx = 26; diagnosisIdx = 30; }
                        // 6月随访：followup6ImagingDate(43) followup6ImagingMethod(44) followup6ImagingResult(45) followup6Result(49)
                        case 2 -> { hasXrayIdx = 43; xrayDateIdx = 44; xrayResultIdx = 45; diagnosisIdx = 49; }
                        // 12月随访：followup12ImagingDate(54) followup12ImagingMethod(55) followup12ImagingResult(56) followup12Result(60)
                        case 3 -> { hasXrayIdx = 54; xrayDateIdx = 55; xrayResultIdx = 56; diagnosisIdx = 60; }
                        default -> { continue; }
                    }
                }
                default -> { continue; }
            }

            String diagnosisFirst = getStrCell(row, diagnosisIdx);
            if (StrUtil.isBlank(diagnosisFirst)) continue;

            String hasXray = getStrCell(row, hasXrayIdx);
            LocalDate xrayDate = parseDateCell(row.get(xrayDateIdx));
            String xrayResult = getStrCell(row, xrayResultIdx);

            // 仅写入胸片字段与首次诊断；diagnosisResult 由后续联动转诊设置
            lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(StrUtil.isNotBlank(hasXray), LatentInfection::getHasChestXray, hasXray)
                    .set(xrayDate != null, LatentInfection::getChestXrayDate, xrayDate)
                    .set(StrUtil.isNotBlank(xrayResult), LatentInfection::getChestXrayResult, xrayResult)
                    .set(LatentInfection::getDiagnosisFirst, diagnosisFirst)
                    .update();

            writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult, diagnosisFirst);

            // 与单条录入保持一致：根据 diagnosisFirst 自动驱动转诊
            String referralCode = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
            if (referralCode != null) {
                LatentInfection refreshed = getById(entity.getId());
                applyReferralOutcome(refreshed, referralCode, null);
            }
            updated++;
        }
        log.info("批量导入胸片诊断，populationType={}，成功更新 {} 条", populationType, updated);
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void referral(Long id, String result, String remark) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先完成追踪到位操作后再进行转诊");
        }
        if (StrUtil.isBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先录入胸片检查与诊断结果后再进行转诊");
        }
        if (StrUtil.isNotBlank(entity.getReferralResult())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已完成转诊，不可重复操作");
        }

        // 胸片诊断为确诊患者/疑似肺结核时，不允许转诊为潜伏感染者
        if ("latent".equals(result) &&
                ("确诊患者".equals(entity.getDiagnosisFirst()) || "疑似肺结核".equals(entity.getDiagnosisFirst()))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片诊断结果为「" + entity.getDiagnosisFirst() + "」，不可转诊为潜伏感染者，请选择正确的转诊结果");
        }

        applyReferralOutcome(entity, result, remark);
    }

    /**
     * 执行转诊后的状态变更：写入 referralResult/diagnosisResult，
     * 必要时创建患者档案并将潜伏感染记录归档。
     * 注意：方法只负责状态写入，不做参数合法性校验，调用方须自行校验。
     */
    private void applyReferralOutcome(LatentInfection entity, String result, String remark) {
        entity.setReferralResult(result);
        entity.setReferralRemark(remark);

        switch (result) {
            case "excluded" -> {
                entity.setDiagnosisResult("排除");
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "other" -> {
                entity.setDiagnosisResult("其他");
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "confirmed", "suspected" -> {
                entity.setDiagnosisResult("confirmed".equals(result) ? "确诊患者" : "疑似肺结核");
                createPatientFromLatent(entity);
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "latent" -> entity.setDiagnosisResult("潜伏感染者");
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的转诊结果");
        }

        updateById(entity);
    }

    /**
     * 根据潜伏感染记录创建对应的患者档案，并从筛查表补全人口学字段。
     * 幂等：若该 latentInfectionId 已存在患者记录则直接跳过。
     */
    private void createPatientFromLatent(LatentInfection entity) {
        boolean alreadyExists = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, entity.getId())
                .exists();
        if (alreadyExists) return;

        Patient.PatientBuilder pb = Patient.builder()
                .screeningId(entity.getScreeningId())
                .latentInfectionId(entity.getId())
                .populationType(entity.getPopulationType())
                .name(entity.getName())
                .gender(entity.getGender())
                .age(entity.getAge())
                .idNumber(entity.getIdNumber())
                .phone(entity.getPhone())
                .diagnosisResult(entity.getDiagnosisResult())
                .source("confirmed")
                .archived(0)
                .departmentId(entity.getDepartmentId());

        String popType = entity.getPopulationType();
        if ("school".equals(popType) && entity.getScreeningId() != null) {
            ScreeningSchool s = screeningSchoolMapper.selectById(entity.getScreeningId());
            if (s != null) {
                pb.birthDate(s.getBirthDate()).idType(s.getIdType())
                  .ethnicity(s.getEthnicity())
                  .householdAddress(s.getHouseholdAddress())
                  .currentAddress(s.getCurrentAddress());
            }
        } else if ("keyPopulation".equals(popType) && entity.getScreeningId() != null) {
            ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(entity.getScreeningId());
            if (k != null) {
                pb.birthDate(k.getBirthDate()).idType(k.getIdType())
                  .ethnicity(k.getEthnicity())
                  .householdAddress(k.getHouseholdAddress())
                  .currentAddress(k.getCurrentAddress());
            }
        } else if ("closeContact".equals(popType) && entity.getScreeningId() != null) {
            ScreeningCloseContact c = screeningCloseContactMapper.selectById(entity.getScreeningId());
            if (c != null) {
                // ScreeningCloseContact 无 birthDate/idType 字段，患者档案中留空
                pb.ethnicity(c.getEthnicity())
                  .householdAddress(c.getHouseholdAddress())
                  .currentAddress(c.getCurrentAddress());
            }
        }

        patientService.save(pb.build());
    }

    @Override
    public void setMedicationStatus(Long id, Integer medicationStatus) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (entity.getTreatmentPhase() == null || entity.getTreatmentPhase() != 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前记录不在预防治疗阶段");
        }
        entity.setMedicationStatus(medicationStatus);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeCase(Long id) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        entity.setTreatmentPhase(2);
        entity.setArchived(1);
        entity.setArchivedTime(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReferralForDirectDiagnosis(List<LatentInfection> latents) {
        for (LatentInfection entity : latents) {
            String diagnosisFirst = entity.getDiagnosisFirst();
            if (!DIAGNOSIS_TO_PATIENT.contains(diagnosisFirst)) continue;

            // 设置诊断结果用于患者档案；若已存在对应患者记录则跳过创建
            entity.setDiagnosisResult(diagnosisFirst);
            createPatientFromLatent(entity);

            // 将潜伏感染记录标记为已转诊归档
            String referralResult = "确诊患者".equals(diagnosisFirst) ? "confirmed" : "suspected";
            lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(LatentInfection::getReferralResult, referralResult)
                    .set(LatentInfection::getDiagnosisResult, diagnosisFirst)
                    .set(LatentInfection::getArchived, 1)
                    .set(LatentInfection::getArchivedTime, LocalDateTime.now())
                    .update();

            log.info("导入时自动转诊 latentId={} diagnosisFirst={} referralResult={}", entity.getId(), diagnosisFirst, referralResult);
        }
    }

    private String getStrCell(Map<String, Object> row, int index) {
        Object val = row.get(index);
        return val == null ? "" : val.toString().trim();
    }

    /**
     * 兼容 Excel 日期单元格的多种返回类型（Date、LocalDateTime、字符串等）
     */
    private LocalDate parseDateCell(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate();
        if (val instanceof java.util.Date d) return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        String str = val.toString().trim();
        if (StrUtil.isBlank(str)) return null;
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e1) {
            try {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                } catch (Exception e3) {
                    log.warn("无法解析日期: {}", str);
                    return null;
                }
            }
        }
    }
}
