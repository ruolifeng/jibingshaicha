package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.EpidemicImportMapper;
import cn.luyou.model.EpidemicImport;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.EpidemicImportService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpidemicImportServiceImpl extends ServiceImpl<EpidemicImportMapper, EpidemicImport>
        implements EpidemicImportService {

    private final DepartmentService departmentService;
    private final PatientService patientService;
    private final LatentInfectionService latentInfectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        String batchNo = IdUtil.fastSimpleUUID();
        List<Map<Integer, String>> allRows = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("大疫情表解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel读取失败");
        }

        if (allRows.size() < 2) {
            return Map.of("count", 0, "batchNo", batchNo);
        }

        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }

        int count = 0;
        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());
        for (Map<Integer, String> row : dataRows) {
            String name = getFieldByHeader(row, headerIndex, "患者姓名", "姓名");
            String idNumber = getFieldByHeader(row, headerIndex, "有效证件号", "证件号", "身份证号", "身份证");
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                continue;
            }

            String gender = getFieldByHeader(row, headerIndex, "性别");
            String birthDateText = getFieldByHeader(row, headerIndex, "出生日期");
            String ageText = getFieldByHeader(row, headerIndex, "年龄");
            String phone = getFieldByHeader(row, headerIndex, "联系电话", "电话");
            String currentAddress = getFieldByHeader(row, headerIndex, "现详细住址", "现住地址区现住详细", "现住址");
            String caseCategory = getFieldByHeader(row, headerIndex, "病例分类");
            String diseaseName = getFieldByHeader(row, headerIndex, "疾病名称");
            String reportUnit = getFieldByHeader(row, headerIndex, "报告单位");
            LocalDate birthDate = parseDate(birthDateText);

            // 去重策略：证件号优先；证件号为空时按 姓名+出生日期+联系电话 兜底
            boolean exists = lambdaQuery()
                    .and(w -> {
                        if (StrUtil.isNotBlank(idNumber)) {
                            w.eq(EpidemicImport::getIdNumber, idNumber);
                        } else {
                            w.eq(EpidemicImport::getName, name)
                                    .eq(birthDate != null, EpidemicImport::getBirthDate, birthDate)
                                    .eq(StrUtil.isNotBlank(phone), EpidemicImport::getPhone, phone);
                        }
                    })
                    .exists();
            if (exists) {
                continue;
            }

            EpidemicImport entity = EpidemicImport.builder()
                    .name(name)
                    .idNumber(idNumber)
                    .gender(gender)
                    .birthDate(birthDate)
                    .age(parseInt(ageText))
                    .phone(phone)
                    .currentAddress(currentAddress)
                    .caseCategory(caseCategory)
                    .diseaseName(diseaseName)
                    .reportUnit(reportUnit)
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .uploadBatch(batchNo)
                    .departmentId(BaseContext.getCurrentDepartmentId())
                    .creatorId(BaseContext.getCurrentId())
                    .build();
            save(entity);
            count++;
        }

        return Map.of("count", count, "batchNo", batchNo);
    }

    @Override
    public IPage<EpidemicImport> queryPage(
            int page,
            int size,
            String name,
            String idNumber,
            Integer trackingStatus,
            Integer archived
    ) {
        LambdaQueryWrapper<EpidemicImport> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), EpidemicImport::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), EpidemicImport::getIdNumber, idNumber)
                .eq(trackingStatus != null, EpidemicImport::getTrackingStatus, trackingStatus)
                .eq(archived != null, EpidemicImport::getArchived, archived)
                .orderByDesc(EpidemicImport::getCreateTime);

        if (!BaseContext.isSuperAdmin()) {
            List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
            wrapper.in(EpidemicImport::getDepartmentId, deptIds);
        }

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark) {
        EpidemicImport entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (Integer.valueOf(1).equals(entity.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "记录已归档，不能再追踪");
        }

        switch (status) {
            case 1 -> entity.setTrackingStatus(1);
            case 2 -> {
                int count = (entity.getNotInPlaceCount() == null ? 0 : entity.getNotInPlaceCount()) + 1;
                entity.setNotInPlaceCount(count);
                if (count >= 3) {
                    entity.setTrackingStatus(4);
                    entity.setTrackingRemark(remark);
                    entity.setArchived(1);
                } else {
                    entity.setTrackingStatus(2);
                }
            }
            case 3 -> {
                entity.setTrackingStatus(3);
                entity.setTrackingRemark(remark);
                entity.setArchived(1);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的追踪状态");
        }

        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXray(Long id, Map<String, Object> data) {
        EpidemicImport entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入胸片结果");
        }
        if (Integer.valueOf(1).equals(entity.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "记录已归档，不能再录入胸片");
        }

        String hasXray = data.getOrDefault("hasChestXray", "").toString();
        String xrayResult = data.getOrDefault("chestXrayResult", "").toString();
        LocalDate xrayDate = null;
        Object xrayDateObj = data.get("chestXrayDate");
        if (xrayDateObj != null && StrUtil.isNotBlank(xrayDateObj.toString())) {
            xrayDate = parseDate(xrayDateObj.toString());
        }

        entity.setHasChestXray(hasXray);
        entity.setChestXrayDate(xrayDate);
        entity.setChestXrayResult(xrayResult);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnosis(Long id, String diagnosisResult) {
        EpidemicImport entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入诊断结果");
        }
        if (Integer.valueOf(1).equals(entity.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "记录已归档，不能再录入诊断");
        }
        if (StrUtil.isBlank(diagnosisResult)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }

        entity.setDiagnosisResult(diagnosisResult);
        entity.setDiagnosisTime(LocalDateTime.now());

        switch (diagnosisResult) {
            case "排除", "其他" -> entity.setArchived(1);
            case "疑似肺结核" -> {
                // 保留在待诊断列表，不自动分流
            }
            case "潜伏感染者" -> {
                Long latentId = createLatentFromEpidemic(entity);
                entity.setTargetLatentId(latentId);
                entity.setArchived(1);
            }
            case "确诊患者" -> {
                Long patientId = createPatientFromEpidemic(entity);
                entity.setTargetPatientId(patientId);
                entity.setArchived(1);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的诊断结果");
        }

        updateById(entity);
    }

    private Long createPatientFromEpidemic(EpidemicImport entity) {
        Patient existed = patientService.lambdaQuery()
                .eq(StrUtil.isNotBlank(entity.getIdNumber()), Patient::getIdNumber, entity.getIdNumber())
                .eq(Patient::getPopulationType, "epidemic")
                .eq(Patient::getName, entity.getName())
                .last("LIMIT 1")
                .one();
        if (existed != null) {
            return existed.getId();
        }

        Patient patient = Patient.builder()
                .populationType("epidemic")
                .source("epidemic")
                .name(entity.getName())
                .gender(entity.getGender())
                .birthDate(entity.getBirthDate())
                .age(entity.getAge())
                .idNumber(entity.getIdNumber())
                .phone(entity.getPhone())
                .currentAddress(entity.getCurrentAddress())
                .diagnosisResult("确诊患者")
                .archived(0)
                .departmentId(entity.getDepartmentId())
                .build();
        patientService.save(patient);
        return patient.getId();
    }

    private Long createLatentFromEpidemic(EpidemicImport entity) {
        LatentInfection existed = latentInfectionService.lambdaQuery()
                .eq(StrUtil.isNotBlank(entity.getIdNumber()), LatentInfection::getIdNumber, entity.getIdNumber())
                .eq(LatentInfection::getPopulationType, "epidemic")
                .eq(LatentInfection::getName, entity.getName())
                .eq(LatentInfection::getArchived, 0)
                .last("LIMIT 1")
                .one();
        if (existed != null) {
            return existed.getId();
        }

        LatentInfection latent = LatentInfection.builder()
                .populationType("epidemic")
                .name(entity.getName())
                .idNumber(entity.getIdNumber())
                .gender(entity.getGender())
                .age(entity.getAge())
                .phone(entity.getPhone())
                .trackingStatus(1)
                .notInPlaceCount(entity.getNotInPlaceCount() == null ? 0 : entity.getNotInPlaceCount())
                .trackingRemark(entity.getTrackingRemark())
                .hasChestXray(entity.getHasChestXray())
                .chestXrayDate(entity.getChestXrayDate())
                .chestXrayResult(entity.getChestXrayResult())
                .diagnosisFirst("潜伏感染者")
                .diagnosisResult("潜伏感染者")
                .referralResult("latent")
                .archived(0)
                .departmentId(entity.getDepartmentId())
                .build();
        latentInfectionService.save(latent);
        return latent.getId();
    }

    private String getFieldByHeader(Map<Integer, String> row, Map<String, Integer> headerIndex, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Integer idx = headerIndex.get(fieldName);
            if (idx != null) {
                String val = row.get(idx);
                if (StrUtil.isNotBlank(val)) {
                    return val.trim();
                }
            }
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                if (entry.getKey().contains(fieldName)) {
                    String val = row.get(entry.getValue());
                    if (StrUtil.isNotBlank(val)) {
                        return val.trim();
                    }
                }
            }
        }
        return null;
    }

    private Integer parseInt(String text) {
        if (StrUtil.isBlank(text)) return null;
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {
        if (StrUtil.isBlank(text)) return null;
        String val = text.trim();

        // 兼容 Excel 序列日期（如 45678 / 45678.0）
        if (val.matches("^\\d+(\\.\\d+)?$")) {
            try {
                double serial = Double.parseDouble(val);
                if (serial > 59) {
                    // Excel 1900 日期系统：序列号 1 对应 1899-12-31，Java 按 1899-12-30 计算可兼容闰年缺陷
                    return LocalDate.of(1899, 12, 30).plusDays((long) Math.floor(serial));
                }
            } catch (Exception ignored) {
            }
        }

        try {
            return LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e1) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }
}

