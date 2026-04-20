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

    @Override
    public IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                             String name, String idNumber, Integer trackingStatus, Integer archived) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), LatentInfection::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                .eq(archived != null, LatentInfection::getArchived, archived)
                .orderByDesc(LatentInfection::getCreateTime);
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
                latent.setBirthDate(c.getBirthDate());
                latent.setEthnicity(c.getEthnicity());
                latent.setCurrentAddress(c.getCurrentAddress());
                latent.setHouseholdAddress(c.getHouseholdAddress());
                latent.setCrowdCategory("密接");
                Integer round = latent.getActiveRound() == null ? 1 : latent.getActiveRound();
                switch (round) {
                    case 1 -> {
                        latent.setScreenDate(c.getFirstScreenDate());
                        latent.setScreenMethod(c.getFirstInfectionMethod());
                        latent.setScreenResult(c.getFirstScreenResult());
                    }
                    case 2 -> {
                        latent.setScreenDate(c.getHalfYearScreenDate());
                        latent.setScreenMethod(c.getHalfYearInfectionMethod());
                        latent.setScreenResult(c.getHalfYearScreenResult());
                    }
                    case 3 -> {
                        latent.setScreenDate(c.getOneYearScreenDate());
                        latent.setScreenMethod(c.getOneYearInfectionMethod());
                        latent.setScreenResult(c.getOneYearScreenResult());
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

        entity.setHasChestXray(hasXray);
        entity.setChestXrayDate(xrayDate);
        entity.setChestXrayResult(xrayResult);
        entity.setDiagnosisFirst(diagnosisFirst);
        entity.setDiagnosisResult(diagnosisFirst);
        updateById(entity);

        // V4 sheet2：胸片与诊断同步回写到筛查表
        writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult, diagnosisFirst);
    }

    /**
     * 将胸片与诊断数据回写到对应的筛查管理表。
     */
    private void writeBackXrayToScreening(LatentInfection entity, String hasXray,
                                          LocalDate xrayDate, String xrayResult, String diagnosis) {
        Long sid = entity.getScreeningId();
        if (sid == null) return;
        String type = entity.getPopulationType();
        if (StrUtil.isBlank(type)) return;

        switch (type) {
            case "school" -> {
                ScreeningSchool s = screeningSchoolMapper.selectById(sid);
                if (s != null) {
                    if (StrUtil.isNotBlank(hasXray)) s.setHasChestXray(hasXray);
                    if (xrayDate != null) s.setChestXrayDate(xrayDate);
                    if (StrUtil.isNotBlank(xrayResult)) s.setChestXrayResult(xrayResult);
                    if (StrUtil.isNotBlank(diagnosis)) s.setDiagnosisFirst(diagnosis);
                    screeningSchoolMapper.updateById(s);
                }
            }
            case "keyPopulation" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(sid);
                if (k != null) {
                    if (StrUtil.isNotBlank(hasXray)) k.setHasChestXray(hasXray);
                    if (xrayDate != null) k.setChestXrayDate(xrayDate);
                    if (StrUtil.isNotBlank(xrayResult)) k.setChestXrayResult(xrayResult);
                    if (StrUtil.isNotBlank(diagnosis)) k.setDiagnosisFirst(diagnosis);
                    screeningKeyPopulationMapper.updateById(k);
                }
            }
            case "closeContact" -> {
                ScreeningCloseContact c = screeningCloseContactMapper.selectById(sid);
                if (c != null) {
                    Integer round = entity.getActiveRound();
                    if (round != null) {
                        switch (round) {
                            case 1 -> {
                                if (StrUtil.isNotBlank(hasXray)) c.setFirstHasChestXray(hasXray);
                                if (xrayDate != null) c.setFirstChestXrayDate(xrayDate);
                                if (StrUtil.isNotBlank(xrayResult)) c.setFirstChestXrayResult(xrayResult);
                                if (StrUtil.isNotBlank(diagnosis)) c.setFirstDiagnosis(diagnosis);
                            }
                            case 2 -> {
                                if (StrUtil.isNotBlank(hasXray)) c.setHalfYearHasChestXray(hasXray);
                                if (xrayDate != null) c.setHalfYearChestXrayDate(xrayDate);
                                if (StrUtil.isNotBlank(xrayResult)) c.setHalfYearChestXrayResult(xrayResult);
                                if (StrUtil.isNotBlank(diagnosis)) c.setHalfYearDiagnosis(diagnosis);
                            }
                            case 3 -> {
                                if (StrUtil.isNotBlank(hasXray)) c.setOneYearHasChestXray(hasXray);
                                if (xrayDate != null) c.setOneYearChestXrayDate(xrayDate);
                                if (StrUtil.isNotBlank(xrayResult)) c.setOneYearChestXrayResult(xrayResult);
                                if (StrUtil.isNotBlank(diagnosis)) c.setOneYearDiagnosis(diagnosis);
                            }
                        }
                    }
                    screeningCloseContactMapper.updateById(c);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importXrayBatch(MultipartFile file, String populationType) {
        int headerRows = switch (populationType) {
            case "keyPopulation" -> 5;
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
            String idNumber = getStrCell(row, 9);
            if (StrUtil.isBlank(idNumber)) continue;

            LatentInfection entity = lambdaQuery()
                    .eq(LatentInfection::getIdNumber, idNumber)
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getArchived, 0)
                    .last("LIMIT 1")
                    .one();
            if (entity == null || !Integer.valueOf(1).equals(entity.getTrackingStatus())) continue;
            if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) continue;

            // 根据人群类型确定 Excel 中胸片/诊断字段的列索引
            int hasXrayIdx, xrayDateIdx, xrayResultIdx, diagnosisIdx;
            switch (populationType) {
                case "school" -> {
                    // 学校人群：Z(25)-AC(28)
                    hasXrayIdx = 25; xrayDateIdx = 26; xrayResultIdx = 27; diagnosisIdx = 28;
                }
                case "keyPopulation" -> {
                    // 重点人群：AK(36)-AO(39)
                    hasXrayIdx = 36; xrayDateIdx = 37; xrayResultIdx = 38; diagnosisIdx = 39;
                }
                case "closeContact" -> {
                    // 密接人群：按阳性轮次读取对应列组
                    Integer round = entity.getActiveRound();
                    if (round == null) round = 1;
                    switch (round) {
                        case 1 -> { hasXrayIdx = 24; xrayDateIdx = 25; xrayResultIdx = 26; diagnosisIdx = 27; }
                        case 2 -> { hasXrayIdx = 33; xrayDateIdx = 34; xrayResultIdx = 35; diagnosisIdx = 36; }
                        case 3 -> { hasXrayIdx = 42; xrayDateIdx = 43; xrayResultIdx = 44; diagnosisIdx = 45; }
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
            if (StrUtil.isNotBlank(hasXray)) entity.setHasChestXray(hasXray);
            if (xrayDate != null) entity.setChestXrayDate(xrayDate);
            if (StrUtil.isNotBlank(xrayResult)) entity.setChestXrayResult(xrayResult);
            entity.setDiagnosisFirst(diagnosisFirst);
            entity.setDiagnosisResult(diagnosisFirst);
            updateById(entity);
            writeBackXrayToScreening(entity, hasXray,
                    xrayDate, xrayResult, diagnosisFirst);
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
                        .archived(0);
                // 从筛查表补全患者档案字段
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
                        pb.birthDate(c.getBirthDate()).idType(c.getIdType())
                          .ethnicity(c.getEthnicity())
                          .householdAddress(c.getHouseholdAddress())
                          .currentAddress(c.getCurrentAddress());
                    }
                }
                patientService.save(pb.build());
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "latent" -> entity.setDiagnosisResult("潜伏感染者");
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的转诊结果");
        }

        updateById(entity);
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
