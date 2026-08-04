package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.EpidemicImportMapper;
import cn.luyou.model.EpidemicImport;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.service.EpidemicImportService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.ImportIdentitySupport;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpidemicImportServiceImpl extends ServiceImpl<EpidemicImportMapper, EpidemicImport>
        implements EpidemicImportService {

    private final PatientService patientService;
    private final LatentInfectionService latentInfectionService;
    private final ScreeningScopeHelper screeningScopeHelper;

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
            String idNumber = ImportIdentitySupport.normalizeIdNumber(
                    getFieldByHeader(row, headerIndex, "有效证件号", "证件号", "身份证号", "身份证"));
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                continue;
            }

            String gender = getFieldByHeader(row, headerIndex, "性别");
            String birthDateText = getFieldByHeader(row, headerIndex, "出生日期");
            String ageText = getFieldByHeader(row, headerIndex, "年龄");
            String phone = getFieldByHeader(row, headerIndex, "联系电话", "电话");
            String currentAddress = getFieldByHeader(row, headerIndex,
                    "现住详细地址", "现详细住址", "现住地址区现住详细", "现住址", "现住地址");
            String caseCategory = getFieldByHeader(row, headerIndex, "病例分类");
            String diseaseName = getFieldByHeader(row, headerIndex, "疾病名称");
            String reportUnit = getFieldByHeader(row, headerIndex, "报告单位");
            LocalDate birthDate = parseDate(birthDateText);

            // 去重策略：真实证件号优先；证件号为空/「无」时按 姓名+出生日期+联系电话 兜底（仅在当前用户辖区内去重）
            LambdaQueryWrapper<EpidemicImport> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.and(w -> {
                if (StrUtil.isNotBlank(idNumber)) {
                    w.eq(EpidemicImport::getIdNumber, idNumber);
                } else {
                    w.eq(EpidemicImport::getName, name)
                            .eq(birthDate != null, EpidemicImport::getBirthDate, birthDate)
                            .eq(StrUtil.isNotBlank(phone), EpidemicImport::getPhone, phone);
                }
            });
            screeningScopeHelper.applyImportDedupScope(
                    dupWrapper, EpidemicImport::getDepartmentId, null, EpidemicImport::getCreatorId);
            boolean exists = count(dupWrapper) > 0;
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
                    .departmentId(screeningScopeHelper.resolveUploadDepartmentId())
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
            Integer archived,
            String diagnosisResult
    ) {
        LambdaQueryWrapper<EpidemicImport> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), EpidemicImport::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), EpidemicImport::getIdNumber, idNumber)
                .eq(trackingStatus != null, EpidemicImport::getTrackingStatus, trackingStatus)
                .eq(archived != null, EpidemicImport::getArchived, archived)
                .eq(StrUtil.isNotBlank(diagnosisResult), EpidemicImport::getDiagnosisResult, diagnosisResult)
                .orderByDesc(EpidemicImport::getCreateTime);

        screeningScopeHelper.applySimpleDepartmentScope(wrapper, EpidemicImport::getDepartmentId);

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
            case "疑似结核", "疑似肺结核" -> {
                // 保留在待诊断列表，不自动分流
            }
            case "潜伏感染者" -> {
                Long latentId = createLatentFromEpidemic(entity);
                entity.setTargetLatentId(latentId);
                entity.setArchived(1);
            }
            case "确诊患者" -> {
                // 大疫情筛查确诊患者仅结案，不进入患者管理（患者管理数据仅来自专病信息表导入）
                entity.setArchived(1);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的诊断结果");
        }

        updateById(entity);
    }

    private Long createPatientFromEpidemic(EpidemicImport entity) {
        String idNumber = ImportIdentitySupport.normalizeIdNumber(entity.getIdNumber());
        entity.setIdNumber(idNumber);

        Patient existed = null;
        if (StrUtil.isNotBlank(idNumber)) {
            existed = patientService.lambdaQuery()
                    .eq(Patient::getIdNumber, idNumber)
                    .eq(Patient::getPopulationType, "epidemic")
                    .eq(entity.getDepartmentId() != null, Patient::getDepartmentId, entity.getDepartmentId())
                    .last("LIMIT 1")
                    .one();
        } else if (StrUtil.isNotBlank(entity.getName())
                && (entity.getBirthDate() != null || StrUtil.isNotBlank(entity.getPhone()))) {
            // 无真实证件号：勿仅按姓名匹配，避免同名误复用
            existed = patientService.lambdaQuery()
                    .eq(Patient::getPopulationType, "epidemic")
                    .eq(Patient::getName, entity.getName())
                    .eq(entity.getBirthDate() != null, Patient::getBirthDate, entity.getBirthDate())
                    .eq(StrUtil.isNotBlank(entity.getPhone()), Patient::getPhone, entity.getPhone())
                    .eq(entity.getDepartmentId() != null, Patient::getDepartmentId, entity.getDepartmentId())
                    .last("LIMIT 1")
                    .one();
        }
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
                .idNumber(idNumber)
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
        String idNumber = ImportIdentitySupport.normalizeIdNumber(entity.getIdNumber());
        entity.setIdNumber(idNumber);

        LatentInfection existed = null;
        if (StrUtil.isNotBlank(idNumber)) {
            existed = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getIdNumber, idNumber)
                    .eq(LatentInfection::getPopulationType, "epidemic")
                    .eq(LatentInfection::getArchived, 0)
                    .eq(entity.getDepartmentId() != null, LatentInfection::getDepartmentId, entity.getDepartmentId())
                    .last("LIMIT 1")
                    .one();
        } else if (StrUtil.isNotBlank(entity.getName()) && StrUtil.isNotBlank(entity.getPhone())) {
            // 无真实证件号：姓名+电话+部门匹配，禁止仅按姓名命中同名记录
            existed = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getPopulationType, "epidemic")
                    .eq(LatentInfection::getName, entity.getName())
                    .eq(LatentInfection::getPhone, entity.getPhone())
                    .eq(LatentInfection::getArchived, 0)
                    .eq(entity.getDepartmentId() != null, LatentInfection::getDepartmentId, entity.getDepartmentId())
                    .last("LIMIT 1")
                    .one();
        }
        if (existed != null) {
            return existed.getId();
        }

        LatentInfection latent = LatentInfection.builder()
                .populationType("epidemic")
                .name(entity.getName())
                .idNumber(idNumber)
                .gender(entity.getGender())
                .age(entity.getAge())
                .phone(entity.getPhone())
                .currentAddress(entity.getCurrentAddress())
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
                .creatorId(BaseContext.getCurrentId())
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
        return FlexibleDateParseUtil.parse(text);
    }
}

