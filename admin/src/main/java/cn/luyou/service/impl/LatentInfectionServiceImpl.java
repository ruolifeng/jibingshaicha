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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatentInfectionServiceImpl extends ServiceImpl<LatentInfectionMapper, LatentInfection>
        implements LatentInfectionService {

    private final PatientService patientService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;

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
        return page(new Page<>(page, size), wrapper);
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
                    s.setHasChestXray(hasXray);
                    s.setChestXrayDate(xrayDate);
                    s.setChestXrayResult(xrayResult);
                    s.setDiagnosisFirst(diagnosis);
                    screeningSchoolMapper.updateById(s);
                }
            }
            case "keyPopulation" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(sid);
                if (k != null) {
                    k.setHasChestXray(hasXray);
                    k.setChestXrayDate(xrayDate);
                    k.setChestXrayResult(xrayResult);
                    k.setDiagnosisFirst(diagnosis);
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
                                c.setFirstHasChestXray(hasXray);
                                c.setFirstChestXrayDate(xrayDate);
                                c.setFirstChestXrayResult(xrayResult);
                                c.setFirstDiagnosis(diagnosis);
                            }
                            case 2 -> {
                                c.setHalfYearHasChestXray(hasXray);
                                c.setHalfYearChestXrayDate(xrayDate);
                                c.setHalfYearChestXrayResult(xrayResult);
                                c.setHalfYearDiagnosis(diagnosis);
                            }
                            case 3 -> {
                                c.setOneYearHasChestXray(hasXray);
                                c.setOneYearChestXrayDate(xrayDate);
                                c.setOneYearChestXrayResult(xrayResult);
                                c.setOneYearDiagnosis(diagnosis);
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

            entity.setHasChestXray(getStrCell(row, hasXrayIdx));
            LocalDate xrayDate = parseDateCell(row.get(xrayDateIdx));
            entity.setChestXrayDate(xrayDate);
            String xrayResult = getStrCell(row, xrayResultIdx);
            entity.setChestXrayResult(xrayResult);
            entity.setDiagnosisFirst(diagnosisFirst);
            entity.setDiagnosisResult(diagnosisFirst);
            updateById(entity);
            writeBackXrayToScreening(entity, entity.getHasChestXray(),
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
