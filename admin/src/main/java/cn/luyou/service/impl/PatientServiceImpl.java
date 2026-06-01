package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.constant.PatientImportHeaders;
import cn.luyou.constant.PatientManualImportHeaders;
import cn.luyou.model.EpidemicReport;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.ImportResult;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.MedicationPickup;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.mapper.FirstVisitMapper;
import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.mapper.MedicationPickupMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.PatientService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.QueryDateRangeUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient>
        implements PatientService {

    private static final Set<String> MANUAL_POPULATION_TYPES = Set.of(
            "school", "keyPopulation", "regular", "epidemic", "referral", "closeContact", "specialDisease"
    );

    /** 专病表多列命中时，按业务优先级取一项作为通知单人群分类 */
    private static final List<String> CROWD_CATEGORY_PRIORITY = List.of(
            "密接", "学生", "教职工", "老年人", "糖尿病", "双感", "既往结核", "非重点人群"
    );

    /** 手动新增/导入：前端 camelCase 字段 → epidemicData 中文键 */
    private static final List<String[]> MANUAL_EPIDEMIC_MAPPINGS = List.of(
            new String[]{"crowdCategory", "人群分类"},
            new String[]{"currentManagementUnit", "现管单位"},
            new String[]{"registrationNo", "登记号"},
            new String[]{"contactName", "联系人姓名"},
            new String[]{"contactRelation", "联系人监护人与本人关系"},
            new String[]{"contactGuardianPhone", "联系人监护人电话号码"},
            new String[]{"comorbidity", "合并症"},
            new String[]{"treatmentClass", "治疗分类"},
            new String[]{"medicationManagementUnit", "服药管理单位"},
            new String[]{"patientRemark", "备注"},
            new String[]{"firstTreatmentPlan", "首次治疗方案"},
            new String[]{"drugSensitivityR", "药敏结果：利福平（R）"},
            new String[]{"drugSensitivityH", "药敏结果：异烟肼（H）"}
    );

    private final DataScopeHelper dataScopeHelper;
    private final EpidemicReportService epidemicReportService;
    private final ObjectMapper objectMapper;
    private final NoticeMapper noticeMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final FollowUpVisitMapper followUpVisitMapper;
    private final MedicationManagementMapper medicationManagementMapper;
    private final MedicationPickupMapper medicationPickupMapper;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;

    @Override
    public IPage<Patient> queryPage(int page, int size, String populationType,
                                     String name, String idNumber, String phone, String currentAddress,
                                     String diagnosisResult, Integer archived, String dateFrom, String dateTo) {
        LambdaQueryWrapper<Patient> wrapper = buildPatientQueryWrapper(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult, archived, dateFrom, dateTo, null, null);
        wrapper.orderByDesc(Patient::getCreateTime);
        IPage<Patient> result = page(new Page<>(page, size), wrapper);
        fillNoticeStatus(result.getRecords(), populationType);
        fillFirstVisitStatus(result.getRecords());
        fillMedicationPickupSummary(result.getRecords());
        fillScreeningXrayData(result.getRecords(), populationType);
        fillEpidemicExtraFields(result.getRecords());
        return result;
    }

    @Override
    public List<Patient> listForExport(String populationType, String name, String idNumber,
                                        String phone, String currentAddress, String diagnosisResult,
                                        Integer archived, String dateFrom, String dateTo,
                                        String startTime, String endTime) {
        LambdaQueryWrapper<Patient> wrapper = buildPatientQueryWrapper(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                archived, dateFrom, dateTo, startTime, endTime);
        if (Integer.valueOf(1).equals(archived)) {
            wrapper.orderByAsc(Patient::getPopulationType).orderByDesc(Patient::getArchivedTime);
        } else {
            wrapper.orderByAsc(Patient::getPopulationType).orderByDesc(Patient::getCreateTime);
        }
        List<Patient> patients = list(wrapper);
        fillEpidemicExtraFields(patients);
        return patients;
    }

    private LambdaQueryWrapper<Patient> buildPatientQueryWrapper(String populationType, String name,
                                                                  String idNumber, String phone,
                                                                  String currentAddress, String diagnosisResult,
                                                                  Integer archived,
                                                                  String dateFrom, String dateTo,
                                                                  String startTime, String endTime) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), Patient::getPhone, phone)
                .like(StrUtil.isNotBlank(currentAddress), Patient::getCurrentAddress, currentAddress)
                .eq(StrUtil.isNotBlank(diagnosisResult), Patient::getDiagnosisResult, diagnosisResult)
                .eq(archived != null, Patient::getArchived, archived);
        if (Integer.valueOf(1).equals(archived)
                && (StrUtil.isNotBlank(startTime) || StrUtil.isNotBlank(endTime))) {
            wrapper.ge(StrUtil.isNotBlank(startTime), Patient::getArchivedTime, startTime)
                    .le(StrUtil.isNotBlank(endTime), Patient::getArchivedTime, endTime + " 23:59:59");
        } else {
            wrapper.ge(createFrom != null, Patient::getCreateTime, createFrom)
                    .le(createTo != null, Patient::getCreateTime, createTo);
        }
        applyPatientScopeFilter(wrapper);
        return wrapper;
    }

    /** 与列表查询保持一致的数据权限过滤 */
    private void applyPatientScopeFilter(LambdaQueryWrapper<Patient> wrapper) {
        dataScopeHelper.applyPatientScope(wrapper);
    }

    /** 五级用户已完成首次随访的可编辑天数 */
    private static final int FIRST_VISIT_EDIT_DAYS_LEVEL5 = 10;

    /** 批量查询首次随访状态并填充到每条记录 */
    private void fillFirstVisitStatus(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<FirstVisit> fw = new LambdaQueryWrapper<>();
        fw.in(FirstVisit::getPatientId, patientIds)
                .select(FirstVisit::getPatientId, FirstVisit::getStatus, FirstVisit::getCreateTime);
        Map<Long, FirstVisit> visitMap = firstVisitMapper.selectList(fw).stream()
                .collect(Collectors.toMap(FirstVisit::getPatientId, v -> v, (a, b) -> a));
        Integer currentRole = BaseContext.getCurrentRole();
        patients.forEach(p -> {
            FirstVisit visit = visitMap.get(p.getId());
            if (visit == null) {
                p.setHasFirstVisit(false);
                p.setFirstVisitStatus(null);
                p.setFirstVisitEditable(true);
                return;
            }
            boolean completed = Integer.valueOf(1).equals(visit.getStatus());
            p.setHasFirstVisit(completed);
            p.setFirstVisitStatus(visit.getStatus());
            p.setFirstVisitEditable(isFirstVisitEditable(currentRole, visit));
        });
    }

    /** 批量查询领药记录摘要并填充到每条记录 */
    private void fillMedicationPickupSummary(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<MedicationPickup> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MedicationPickup::getPatientId, patientIds)
                .orderByAsc(MedicationPickup::getCreateTime);
        List<MedicationPickup> pickups = medicationPickupMapper.selectList(wrapper);
        Map<Long, List<MedicationPickup>> grouped = pickups.stream()
                .collect(Collectors.groupingBy(MedicationPickup::getPatientId));
        patients.forEach(p -> {
            List<MedicationPickup> list = grouped.get(p.getId());
            if (list == null || list.isEmpty()) {
                p.setMedicationPickupCount(0);
                p.setMedicationPickTime(null);
                p.setMedicationChemotherapy(null);
                p.setMedicationDrugForm(null);
                return;
            }
            p.setMedicationPickupCount(list.size());
            MedicationPickup latest = list.get(list.size() - 1);
            p.setMedicationPickTime(latest.getPickupTime() != null ? latest.getPickupTime().toString() : null);
            p.setMedicationChemotherapy(formatDrugNames(latest.getDrugs()));
            if (latest.getQuantity() != null && StrUtil.isNotBlank(latest.getQuantityUnit())) {
                p.setMedicationDrugForm(latest.getQuantity().stripTrailingZeros().toPlainString()
                        + latest.getQuantityUnit());
            } else {
                p.setMedicationDrugForm(null);
            }
        });
    }

    private String formatDrugNames(String drugsJson) {
        if (StrUtil.isBlank(drugsJson)) return null;
        try {
            JSONArray array = JSONUtil.parseArray(drugsJson);
            return array.stream()
                    .map(item -> {
                        if (item instanceof JSONObject obj) {
                            return obj.getStr("name");
                        }
                        return null;
                    })
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining("、"));
        } catch (Exception e) {
            return null;
        }
    }

    /** 五级用户：已完成首次随访创建后 10 天内可改；管理员（非五级）随时可改 */
    private boolean isFirstVisitEditable(Integer role, FirstVisit visit) {
        if (visit == null || !Integer.valueOf(1).equals(visit.getStatus())) {
            return true;
        }
        if (role == null || role != 6) {
            return true;
        }
        if (visit.getCreateTime() == null) {
            return true;
        }
        return !visit.getCreateTime().plusDays(FIRST_VISIT_EDIT_DAYS_LEVEL5).isBefore(LocalDateTime.now());
    }

    /** 从筛查表关联填充胸片检查日期和结果（仅转诊确诊患者有 screeningId） */
    private void fillScreeningXrayData(List<Patient> patients, String populationType) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> screeningIds = patients.stream()
                .filter(p -> p.getScreeningId() != null)
                .map(Patient::getScreeningId)
                .distinct()
                .collect(Collectors.toList());
        if (screeningIds.isEmpty()) return;

        if ("school".equals(populationType)) {
            List<ScreeningSchool> schools = screeningSchoolMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningSchool> schoolMap = schools.stream()
                    .collect(Collectors.toMap(ScreeningSchool::getId, s -> s));
            patients.forEach(p -> {
                ScreeningSchool s = schoolMap.get(p.getScreeningId());
                if (s != null) {
                    p.setChestXrayDate(s.getChestXrayDate());
                    p.setChestXrayResult(s.getChestXrayResult());
                    p.setScreenDate(s.getScreenDate());
                    p.setScreenMethod(s.getScreenMethod());
                    p.setInfectionResult(s.getInfectionResult());
                }
            });
        } else if ("keyPopulation".equals(populationType) || "regular".equals(populationType)) {
            // regular 筛查数据也存储在 screening_key_population 表中（通过 source_type 区分）
            List<ScreeningKeyPopulation> keyPops = screeningKeyPopulationMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningKeyPopulation> keyPopMap = keyPops.stream()
                    .collect(Collectors.toMap(ScreeningKeyPopulation::getId, k -> k));
            patients.forEach(p -> {
                ScreeningKeyPopulation k = keyPopMap.get(p.getScreeningId());
                if (k != null) {
                    p.setChestXrayDate(k.getChestXrayDate());
                    p.setChestXrayResult(k.getChestXrayResult());
                    p.setScreenDate(k.getScreenDate());
                    p.setScreenMethod(k.getScreenMethod());
                    p.setInfectionResult(k.getInfectionResult());
                }
            });
        } else if ("closeContact".equals(populationType)) {
            List<ScreeningCloseContact> closeContacts = screeningCloseContactMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningCloseContact> closeContactMap = closeContacts.stream()
                    .collect(Collectors.toMap(ScreeningCloseContact::getId, c -> c));
            patients.forEach(p -> {
                ScreeningCloseContact c = closeContactMap.get(p.getScreeningId());
                if (c != null) {
                    // 密接人群胸片字段为 imagingDate/imagingResult，统一映射到 Patient 的 chestXrayDate/chestXrayResult
                    p.setChestXrayDate(c.getImagingDate());
                    p.setChestXrayResult(c.getImagingResult());
                }
            });
        }
    }

    /** 批量查询患者通知单状态并填充到每条记录 */
    private void fillNoticeStatus(List<Patient> patients, String populationType) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<Notice> nw = new LambdaQueryWrapper<>();
        nw.in(Notice::getBizId, patientIds)
          .eq(Notice::getNoticeType, "patient")
          .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType);
        List<Notice> notices = noticeMapper.selectList(nw);
        // 每个 bizId 保留最新一条（取 id 最大的，因为插入顺序即为时间顺序）
        Map<Long, Notice> noticeMap = notices.stream()
                .collect(Collectors.toMap(
                        Notice::getBizId,
                        n -> n,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));
        patients.forEach(p -> {
            Notice n = noticeMap.get(p.getId());
            if (n != null) {
                p.setNoticeStatus(n.getStatus());
                p.setNoticeId(n.getId());
                p.setNoticeSentTime(n.getSentTime());
                p.setNoticeConfirmedTime(n.getConfirmedTime());
                p.setNoticeMedicationUnit(n.getMedicationManagementUnit());
                p.setNoticeRemark(n.getRemark());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importEpidemic(MultipartFile file, String populationType) {
        String batchId = IdUtil.fastSimpleUUID();
        // headRowNumber(0) 使首行（表头）也作为数据行读入，以便构建列索引映射
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
            log.warn("大疫情表无数据行，跳过导入");
            return 0;
        }

        // 解析第一行表头，构建 字段名 -> 列索引 映射
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        log.info("大疫情表表头解析：{}", headerIndex.keySet());

        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());

        int matchCount = 0;
        for (Map<Integer, String> row : dataRows) {
            String nameVal = getFieldByHeader(row, headerIndex, "姓名");
            String idNumberVal = getFieldByHeader(row, headerIndex, "证件号", "身份证号", "身份证");

            // 跳过姓名和证件号均为空的空行
            if (StrUtil.isBlank(nameVal) && StrUtil.isBlank(idNumberVal)) {
                continue;
            }

            Map<String, String> namedRow = buildNamedRowFields(row, headerIndex);
            String rawJson;
            try {
                rawJson = objectMapper.writeValueAsString(namedRow);
            } catch (Exception e) {
                rawJson = namedRow.toString();
            }

            // 优先按证件号精确匹配，再按姓名模糊匹配
            Patient matched = null;
            if (StrUtil.isNotBlank(idNumberVal)) {
                matched = lambdaQuery()
                        .eq(Patient::getPopulationType, populationType)
                        .eq(Patient::getIdNumber, idNumberVal)
                        .last("LIMIT 1")
                        .one();
            }
            if (matched == null && StrUtil.isNotBlank(nameVal)) {
                matched = lambdaQuery()
                        .eq(Patient::getPopulationType, populationType)
                        .like(Patient::getName, nameVal)
                        .last("LIMIT 1")
                        .one();
            }

            EpidemicReport report = EpidemicReport.builder()
                    .populationType(populationType)
                    .rawData(rawJson)
                    .uploadBatch(batchId)
                    .build();

            if (matched != null) {
                matched.setEpidemicData(rawJson);
                updateById(matched);
                report.setPatientId(matched.getId());
                report.setMatched(1);
                matchCount++;
            } else {
                Patient newPatient = Patient.builder()
                        .populationType(populationType)
                        .name(nameVal)
                        .idNumber(idNumberVal)
                        .source("epidemic")
                        .archived(0)
                        .epidemicData(rawJson)
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .build();
                save(newPatient);
                report.setPatientId(newPatient.getId());
                report.setMatched(0);
            }

            epidemicReportService.save(report);
        }

        log.info("大疫情导入完成：共 {} 条数据，匹配 {} 条", dataRows.size(), matchCount);
        return dataRows.size();
    }

    @Override
    public void archivePatient(Long id) {
        archivePatient(id, null);
    }

    @Override
    public void archivePatient(Long id, String archiveRemark) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        patient.setArchived(1);
        patient.setArchivedTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(archiveRemark)) {
            patient.setArchiveRemark(archiveRemark);
        }
        updateById(patient);
    }

    @Override
    public void restoreTransferredPatient(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            return;
        }
        if (!Integer.valueOf(1).equals(patient.getArchived())) {
            return;
        }
        if (!PatientService.ARCHIVE_REMARK_TRANSFERRED_OUT.equals(patient.getArchiveRemark())) {
            return;
        }
        patient.setArchived(0);
        patient.setArchivedTime(null);
        patient.setArchiveRemark(null);
        updateById(patient);
    }

    @Override
    public void unarchivePatientFromStopTreatment(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        if (!Integer.valueOf(1).equals(patient.getArchived())) {
            return;
        }
        if (!PatientService.isStopTreatmentArchiveRemark(patient.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅停止治疗归档的患者可解锁");
        }
        patient.setArchived(0);
        patient.setArchivedTime(null);
        patient.setArchiveRemark(null);
        updateById(patient);
    }

    @Override
    public IPage<Patient> queryHistoryPage(int page, int size, String populationType,
                                            String name, String idNumber, String phone,
                                            String diagnosisResult, String startTime, String endTime) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .eq(Patient::getArchived, 1)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), Patient::getPhone, phone)
                .eq(StrUtil.isNotBlank(diagnosisResult), Patient::getDiagnosisResult, diagnosisResult)
                .ge(StrUtil.isNotBlank(startTime), Patient::getArchivedTime, startTime)
                .le(StrUtil.isNotBlank(endTime), Patient::getArchivedTime, endTime + " 23:59:59")
                .orderByDesc(Patient::getArchivedTime);
        dataScopeHelper.applyPatientScope(wrapper);
        IPage<Patient> result = page(new Page<>(page, size), wrapper);
        fillNoticeStatus(result.getRecords(), populationType);
        fillFirstVisitStatus(result.getRecords());
        fillMedicationPickupSummary(result.getRecords());
        fillEpidemicExtraFields(result.getRecords());
        return result;
    }

    /** 从 epidemicData JSON 解析导入扩展字段（人群分类、现管单位、治疗分类及完整导入字段） */
    private void fillEpidemicExtraFields(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        for (Patient patient : patients) {
            Map<String, String> fields = parseImportFields(patient);
            patient.setImportFields(fields);
            patient.setRegistrationNo(fields.getOrDefault("登记号", ""));
            patient.setTreatmentClass(fields.getOrDefault("治疗分类", ""));
            if ("specialDisease".equals(patient.getPopulationType()) && hasKeyPopulationColumns(fields)) {
                patient.setCrowdCategory(resolveCrowdCategoryFromImportFields(fields));
            } else {
                String crowdCategory = fields.get("人群分类");
                if (StrUtil.isNotBlank(crowdCategory)) {
                    patient.setCrowdCategory(crowdCategory);
                }
            }
            String currentUnit = fields.getOrDefault("现管单位", fields.get("现管理单位"));
            if (StrUtil.isNotBlank(currentUnit)) {
                patient.setCurrentManagementUnit(currentUnit);
            }
        }
    }

    private boolean hasKeyPopulationColumns(Map<String, String> fields) {
        return fields.keySet().stream().anyMatch(k -> k.startsWith("重点人群"));
    }

    /** 从已解析的导入字段 Map 推导专病网人群分类 */
    private String resolveCrowdCategoryFromImportFields(Map<String, String> fields) {
        List<String> matched = new ArrayList<>();
        if ("是".equals(fields.get("重点人群-A.密切接触者"))) matched.add("密接");
        if ("是".equals(fields.get("重点人群-E.学校托幼机构人员"))) matched.add("学生");
        if ("是".equals(fields.get("重点人群-D.医务人员"))) matched.add("教职工");
        if ("是".equals(fields.get("重点人群-J.养老院居住者"))
                || "是".equals(fields.get("重点人群-K.福利院居住者"))) {
            matched.add("老年人");
        }
        if ("是".equals(fields.get("重点人群-C.糖尿病患者"))) matched.add("糖尿病");
        if ("是".equals(fields.get("重点人群-B.HIV/AIDS患者"))) matched.add("双感");

        String keyPopulation = fields.get("重点人群");
        if (StrUtil.isNotBlank(keyPopulation) && !"否".equals(keyPopulation.trim())) {
            String mapped = mapKeyPopulationLabel(keyPopulation);
            if (mapped != null && !matched.contains(mapped)) {
                matched.add(mapped);
            }
        }

        if (matched.isEmpty()) {
            String stored = fields.get("人群分类");
            if (StrUtil.isNotBlank(stored) && CROWD_CATEGORY_PRIORITY.contains(stored)) {
                return stored;
            }
            return StrUtil.isNotBlank(stored) ? stored : "非重点人群";
        }
        for (String category : CROWD_CATEGORY_PRIORITY) {
            if (matched.contains(category)) {
                return category;
            }
        }
        return matched.get(0);
    }

    /** 解析 epidemicData：支持表头键名 JSON 及 legacy 列索引 JSON */
    private Map<String, String> parseImportFields(Patient patient) {
        if (StrUtil.isBlank(patient.getEpidemicData())) {
            return Collections.emptyMap();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(patient.getEpidemicData(), Map.class);
            if (raw.isEmpty()) {
                return Collections.emptyMap();
            }
            boolean indexKeys = raw.keySet().stream().allMatch(k -> k.matches("\\d+"));
            Map<String, String> fields = new LinkedHashMap<>();
            if (indexKeys) {
                List<String> headers = "specialDisease".equals(patient.getPopulationType())
                        ? PatientImportHeaders.SPECIAL_DISEASE
                        : PatientImportHeaders.EPIDEMIC_REPORT;
                raw.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(
                                Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey())))
                        .forEach(entry -> {
                            int idx = Integer.parseInt(entry.getKey());
                            if (idx >= 0 && idx < headers.size() && entry.getValue() != null
                                    && StrUtil.isNotBlank(entry.getValue().toString())) {
                                fields.put(headers.get(idx), entry.getValue().toString().trim());
                            }
                        });
            } else {
                raw.forEach((key, value) -> {
                    if (value != null && StrUtil.isNotBlank(value.toString())) {
                        fields.put(key, value.toString().trim());
                    }
                });
            }
            return fields;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    /** 将 Excel 行按表头映射为 字段名 -> 值 */
    private Map<String, String> buildNamedRowFields(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            String val = row.get(entry.getValue());
            if (StrUtil.isNotBlank(val)) {
                fields.put(entry.getKey(), val.trim());
            }
        }
        return fields;
    }

    /**
     * 专病表人群分类：T 列「人群分类」为职业，实际分类来自 U 列「重点人群」及 V-AE 各子列。
     */
    private String resolveSpecialDiseaseCrowdCategory(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        List<String> matched = new ArrayList<>();
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-A.密切接触者"))) {
            matched.add("密接");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-E.学校托幼机构人员"))) {
            matched.add("学生");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-D.医务人员"))) {
            matched.add("教职工");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-J.养老院居住者"))
                || "是".equals(getFieldByHeader(row, headerIndex, "重点人群-K.福利院居住者"))) {
            matched.add("老年人");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-C.糖尿病患者"))) {
            matched.add("糖尿病");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-B.HIV/AIDS患者"))) {
            matched.add("双感");
        }

        String keyPopulation = getFieldByHeader(row, headerIndex, "重点人群");
        if (StrUtil.isNotBlank(keyPopulation) && !"否".equals(keyPopulation.trim())) {
            String mapped = mapKeyPopulationLabel(keyPopulation);
            if (mapped != null && !matched.contains(mapped)) {
                matched.add(mapped);
            }
        }

        if (matched.isEmpty()) {
            return "非重点人群";
        }
        for (String category : CROWD_CATEGORY_PRIORITY) {
            if (matched.contains(category)) {
                return category;
            }
        }
        return matched.get(0);
    }

    private String mapKeyPopulationLabel(String raw) {
        if (StrUtil.isBlank(raw)) return null;
        String val = raw.trim();
        if ("否".equals(val)) return null;
        if (val.contains("密切") || val.contains("密接")) return "密接";
        if (val.contains("学校") || val.contains("托幼")) return "学生";
        if (val.contains("医务人员") || val.contains("教职工")) return "教职工";
        if (val.contains("养老") || val.contains("福利院")) return "老年人";
        if (val.contains("糖尿病")) return "糖尿病";
        if (val.contains("HIV") || val.contains("AIDS") || val.contains("双感")) return "双感";
        if (val.contains("既往") && val.contains("结核")) return "既往结核";
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePatient(Long id) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        // 级联软删：首次随访
        firstVisitMapper.delete(new LambdaQueryWrapper<FirstVisit>()
                .eq(FirstVisit::getPatientId, id));
        // 级联软删：后续随访
        followUpVisitMapper.delete(new LambdaQueryWrapper<FollowUpVisit>()
                .eq(FollowUpVisit::getPatientId, id));
        // 级联软删：服药管理
        medicationManagementMapper.delete(new LambdaQueryWrapper<MedicationManagement>()
                .eq(MedicationManagement::getPatientId, id));
        // 级联软删：领药记录
        medicationPickupMapper.delete(new LambdaQueryWrapper<MedicationPickup>()
                .eq(MedicationPickup::getPatientId, id));
        // 级联软删：通知单
        noticeMapper.delete(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getBizId, id)
                .eq(Notice::getNoticeType, "patient"));
        // 删除患者本体（MyBatis-Plus 逻辑删除：设 deleted=1）
        removeById(id);
        log.info("删除患者 id={} 及其级联数据", id);
    }

    /**
     * 根据表头名称从数据行中提取字段值（仅精确匹配）。
     */
    private String getFieldByHeaderExact(Map<Integer, String> row, Map<String, Integer> headerIndex,
                                         String... fieldNames) {
        for (String fieldName : fieldNames) {
            Integer idx = headerIndex.get(fieldName);
            if (idx == null) continue;
            String val = row.get(idx);
            if (StrUtil.isNotBlank(val)) {
                return val.trim();
            }
        }
        return null;
    }

    /**
     * 专病网现住址：去掉省、市级前缀，仅保留区县及后续详细地址。
     * 例：四川省自贡市富顺县代寺镇… → 富顺县代寺镇…
     */
    private String normalizeSpecialDiseaseCurrentAddress(String address) {
        if (StrUtil.isBlank(address)) return address;
        String normalized = address.trim();
        normalized = normalized.replaceFirst("^[^省\\s]+省", "");
        normalized = normalized.replaceFirst("^[^市\\s]+市", "");
        return normalized.trim();
    }

    /**
     * 根据表头名称从数据行中提取字段值。
     * 支持多个候选字段名，先精确匹配，再按"表头包含关键字"模糊匹配。
     */
    private String getFieldByHeader(Map<Integer, String> row, Map<String, Integer> headerIndex,
                                    String... fieldNames) {
        for (String fieldName : fieldNames) {
            // 精确匹配
            Integer idx = headerIndex.get(fieldName);
            if (idx != null) {
                String val = row.get(idx);
                if (StrUtil.isNotBlank(val)) {
                    return val.trim();
                }
            }
            // 模糊匹配（表头中包含目标字段名）
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importSpecialDisease(MultipartFile file) {
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new com.alibaba.excel.read.listener.ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, com.alibaba.excel.context.AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }
                @Override
                public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                    log.info("专病表解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (java.io.IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "专病表Excel读取失败");
        }

        if (allRows.size() < 2) {
            log.warn("专病表无数据行，跳过导入");
            return 0;
        }

        // 解析表头
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        log.info("专病表表头解析：{}", headerIndex.keySet());

        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());
        int count = 0;
        for (Map<Integer, String> row : dataRows) {
            String name = getFieldByHeader(row, headerIndex, "患者姓名", "姓名");
            String idNumber = getFieldByHeader(row, headerIndex, "身份证号", "有效证件号", "证件号");
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) continue;

            String gender = getFieldByHeader(row, headerIndex, "性别");
            String birthDateStr = getFieldByHeader(row, headerIndex, "出生日期");
            String ageStr = getFieldByHeader(row, headerIndex, "年龄");
            String phone = getFieldByHeader(row, headerIndex, "患者联系电话", "联系电话", "电话");
            // 专病表地址须精确匹配「详细」列，避免误取「现地址类型」「户籍地址类别」
            String currentAddress = normalizeSpecialDiseaseCurrentAddress(
                    getFieldByHeaderExact(row, headerIndex, "现地址详细", "现详细住址"));
            String householdAddress = getFieldByHeaderExact(row, headerIndex, "户籍地址详细");
            String diagnosisResult = getFieldByHeader(row, headerIndex, "诊断结果");
            String crowdCategory = resolveSpecialDiseaseCrowdCategory(row, headerIndex);
            String currentUnit = getFieldByHeader(row, headerIndex, "现管理单位", "现管单位", "首管理单位");

            // 将全部专病网导入字段存入 epidemicData JSON
            Map<String, String> extraFields = buildNamedRowFields(row, headerIndex);
            if (StrUtil.isNotBlank(crowdCategory)) extraFields.put("人群分类", crowdCategory);
            if (StrUtil.isNotBlank(currentUnit)) {
                extraFields.put("现管单位", currentUnit);
                extraFields.put("现管理单位", currentUnit);
            }
            String extraJson = null;
            try {
                extraJson = objectMapper.writeValueAsString(extraFields);
            } catch (Exception ignored) {}

            java.time.LocalDate birthDate = null;
            if (StrUtil.isNotBlank(birthDateStr)) {
                try { birthDate = java.time.LocalDate.parse(birthDateStr); } catch (Exception ignored) {
                    try { birthDate = java.time.LocalDate.parse(birthDateStr,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")); } catch (Exception ignored2) {}
                }
            }
            Integer age = null;
            if (StrUtil.isNotBlank(ageStr)) {
                try { age = Integer.parseInt(ageStr.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            Patient patient = Patient.builder()
                    .populationType("specialDisease")
                    .source("specialDisease")
                    .name(name)
                    .idType("居民身份证")
                    .idNumber(idNumber)
                    .gender(gender)
                    .birthDate(birthDate)
                    .age(age)
                    .phone(phone)
                    .currentAddress(currentAddress)
                    .householdAddress(householdAddress)
                    .diagnosisResult(diagnosisResult)
                    .epidemicData(extraJson)
                    .archived(0)
                    .departmentId(BaseContext.getCurrentDepartmentId())
                    .creatorId(BaseContext.getCurrentId())
                    .build();

            save(patient);
            count++;
        }

        log.info("专病表导入完成：成功创建 {} 条患者记录", count);
        return count;
    }

    @Override
    public Patient getDetail(Long id) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者记录不存在");
        }
        fillNoticeStatus(List.of(patient), patient.getPopulationType());
        fillFirstVisitStatus(List.of(patient));
        fillMedicationPickupSummary(List.of(patient));
        fillScreeningXrayData(List.of(patient), patient.getPopulationType());
        fillEpidemicExtraFields(List.of(patient));
        return patient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicInfo(Long id, Map<String, Object> body) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者记录不存在");
        }
        if (body.get("populationType") != null) {
            String populationType = body.get("populationType").toString().trim();
            if (StrUtil.isNotBlank(populationType)) {
                if (!MANUAL_POPULATION_TYPES.contains(populationType)) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的数据来源");
                }
                patient.setPopulationType(populationType);
            }
        }
        if (body.get("name") != null) patient.setName(body.get("name").toString());
        if (body.get("gender") != null) patient.setGender(body.get("gender").toString());
        if (body.containsKey("birthDate")) {
            String bd = body.get("birthDate") == null ? "" : body.get("birthDate").toString();
            patient.setBirthDate(StrUtil.isNotBlank(bd) ? LocalDate.parse(bd) : null);
        }
        if (body.containsKey("age")) {
            Object ageVal = body.get("age");
            patient.setAge(ageVal == null || "".equals(String.valueOf(ageVal)) ? null : Integer.valueOf(ageVal.toString()));
        }
        if (body.get("idType") != null) patient.setIdType(body.get("idType").toString());
        if (body.get("idNumber") != null) {
            String idNumber = body.get("idNumber").toString().trim();
            if (StrUtil.isNotBlank(idNumber) && !isValidIdCard(idNumber)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
            }
            patient.setIdNumber(idNumber);
        }
        if (body.get("ethnicity") != null) patient.setEthnicity(body.get("ethnicity").toString());
        if (body.get("phone") != null) {
            String phone = body.get("phone").toString().trim();
            if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
            }
            patient.setPhone(phone);
        }
        if (body.get("householdAddress") != null) patient.setHouseholdAddress(body.get("householdAddress").toString());
        if (body.get("currentAddress") != null) patient.setCurrentAddress(body.get("currentAddress").toString());
        if (body.get("diagnosisResult") != null) patient.setDiagnosisResult(body.get("diagnosisResult").toString());
        mergeEpidemicExtraFields(patient, body);
        updateById(patient);
        updateLinkedScreening(patient, body);
    }

    /** 合并手动录入扩展字段到 epidemicData JSON */
    private void mergeEpidemicExtraFields(Patient patient, Map<String, Object> body) {
        boolean hasManualField = MANUAL_EPIDEMIC_MAPPINGS.stream()
                .anyMatch(pair -> body.containsKey(pair[0]));
        if (!hasManualField) {
            return;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(patient.getEpidemicData())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> existing = objectMapper.readValue(patient.getEpidemicData(), Map.class);
                extra.putAll(existing);
            } catch (Exception ignored) {
                // 原 JSON 无效时重建
            }
        }
        for (String[] pair : MANUAL_EPIDEMIC_MAPPINGS) {
            String bodyKey = pair[0];
            String jsonKey = pair[1];
            if (!body.containsKey(bodyKey)) {
                continue;
            }
            String value = body.get(bodyKey) == null ? "" : body.get(bodyKey).toString().trim();
            if (StrUtil.isNotBlank(value)) {
                extra.put(jsonKey, value);
            } else {
                extra.remove(jsonKey);
            }
        }
        try {
            patient.setEpidemicData(extra.isEmpty() ? null : objectMapper.writeValueAsString(extra));
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "扩展字段保存失败");
        }
    }

    /** 同步更新关联筛查记录中的筛查/胸片字段 */
    private void updateLinkedScreening(Patient patient, Map<String, Object> body) {
        if (patient.getScreeningId() == null) return;
        Long screeningId = patient.getScreeningId();
        String populationType = patient.getPopulationType();
        LocalDate screenDate = body.containsKey("screenDate") ? parseLocalDateField(body.get("screenDate")) : null;
        String screenMethod = body.containsKey("screenMethod") ? stringField(body.get("screenMethod")) : null;
        String infectionResult = body.containsKey("infectionResult") ? stringField(body.get("infectionResult")) : null;
        LocalDate chestXrayDate = body.containsKey("chestXrayDate") ? parseLocalDateField(body.get("chestXrayDate")) : null;
        String chestXrayResult = body.containsKey("chestXrayResult") ? stringField(body.get("chestXrayResult")) : null;

        if ("school".equals(populationType)) {
            ScreeningSchool screening = screeningSchoolMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("screenDate")) screening.setScreenDate(screenDate);
            if (body.containsKey("screenMethod")) screening.setScreenMethod(screenMethod);
            if (body.containsKey("infectionResult")) screening.setInfectionResult(infectionResult);
            if (body.containsKey("chestXrayDate")) screening.setChestXrayDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setChestXrayResult(chestXrayResult);
            screeningSchoolMapper.updateById(screening);
        } else if ("keyPopulation".equals(populationType) || "regular".equals(populationType)) {
            ScreeningKeyPopulation screening = screeningKeyPopulationMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("screenDate")) screening.setScreenDate(screenDate);
            if (body.containsKey("screenMethod")) screening.setScreenMethod(screenMethod);
            if (body.containsKey("infectionResult")) screening.setInfectionResult(infectionResult);
            if (body.containsKey("chestXrayDate")) screening.setChestXrayDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setChestXrayResult(chestXrayResult);
            screeningKeyPopulationMapper.updateById(screening);
        } else if ("closeContact".equals(populationType)) {
            ScreeningCloseContact screening = screeningCloseContactMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("chestXrayDate")) screening.setImagingDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setImagingResult(chestXrayResult);
            screeningCloseContactMapper.updateById(screening);
        }
    }

    private LocalDate parseLocalDateField(Object val) {
        if (val == null || StrUtil.isBlank(val.toString())) return null;
        String str = val.toString().trim();
        try {
            return LocalDate.parse(str);
        } catch (Exception e1) {
            try {
                return LocalDate.parse(str, java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(str, java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private String stringField(Object val) {
        return val == null ? null : val.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(Map<String, Object> body) {
        String name = body.getOrDefault("name", "").toString().trim();
        String idNumber = body.getOrDefault("idNumber", "").toString().trim();
        String populationType = body.getOrDefault("populationType", "").toString().trim();
        String phone = body.getOrDefault("phone", "").toString().trim();

        if (StrUtil.isBlank(name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "姓名不能为空");
        }
        if (StrUtil.isBlank(idNumber)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "证件号不能为空");
        }
        if (!isValidIdCard(idNumber)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }
        if (!MANUAL_POPULATION_TYPES.contains(populationType)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的数据来源");
        }

        Patient patient = Patient.builder()
                .populationType(populationType)
                .name(name)
                .idNumber(idNumber)
                .phone(phone)
                .gender(body.getOrDefault("gender", "").toString())
                .age(parseIntegerField(body.get("age")))
                .idType(body.getOrDefault("idType", "居民身份证").toString())
                .ethnicity(body.getOrDefault("ethnicity", "").toString())
                .householdAddress(body.getOrDefault("householdAddress", "").toString())
                .currentAddress(body.getOrDefault("currentAddress", "").toString())
                .diagnosisResult(body.getOrDefault("diagnosisResult", "").toString())
                .source("manual")
                .archived(0)
                .departmentId(BaseContext.getCurrentDepartmentId())
                .creatorId(BaseContext.getCurrentId())
                .build();

        String birthDate = body.getOrDefault("birthDate", "").toString().trim();
        if (StrUtil.isNotBlank(birthDate)) {
            patient.setBirthDate(LocalDate.parse(birthDate));
        }

        save(patient);
        mergeEpidemicExtraFields(patient, body);
        if (StrUtil.isNotBlank(patient.getEpidemicData())) {
            updateById(patient);
        }
        log.info("手动新增在管患者 id={}, populationType={}", patient.getId(), populationType);
        return patient.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importManualBatch(MultipartFile file) {
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("在管患者批量导入解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败：" + e.getMessage());
        }

        if (allRows.size() < 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }

        if (!headerIndex.containsKey("姓名") || !headerIndex.containsKey("证件号") || !headerIndex.containsKey("数据来源")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "模板表头不正确，请下载最新模板后重试");
        }

        ImportResult result = new ImportResult();
        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());
        Set<String> importedKeys = new HashSet<>();
        for (int i = 0; i < dataRows.size(); i++) {
            Map<Integer, String> row = dataRows.get(i);
            int rowNum = i + 2;
            try {
                String name = getImportField(row, headerIndex, "姓名");
                String idNumber = normalizeExcelCellText(getImportField(row, headerIndex, "证件号"));
                if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                    continue;
                }

                String populationTypeRaw = getImportField(row, headerIndex, "数据来源");
                String populationType = resolvePopulationType(populationTypeRaw);
                String phone = normalizeExcelCellText(getImportField(row, headerIndex, "联系电话"));

                boolean hasError = false;
                if (StrUtil.isBlank(name)) {
                    result.addError(rowNum, idNumber, "姓名不能为空");
                    hasError = true;
                }
                if (StrUtil.isBlank(idNumber)) {
                    result.addError(rowNum, name, "证件号不能为空");
                    hasError = true;
                } else if (!isValidIdCard(idNumber)) {
                    result.addError(rowNum, name, "身份证号格式不正确");
                    hasError = true;
                }
                if (StrUtil.isBlank(populationType)) {
                    result.addError(rowNum, name, "数据来源无效");
                    hasError = true;
                }
                if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
                    result.addError(rowNum, name, "手机号格式不正确");
                    hasError = true;
                }
                if (hasError) {
                    continue;
                }

                String dedupeKey = populationType + ":" + idNumber;
                if (importedKeys.contains(dedupeKey)) {
                    result.addError(rowNum, name, "该证件号在本文件中重复");
                    continue;
                }
                if (lambdaQuery()
                        .eq(Patient::getIdNumber, idNumber)
                        .eq(Patient::getPopulationType, populationType)
                        .eq(Patient::getArchived, 0)
                        .exists()) {
                    result.addError(rowNum, name, "该证件号在此数据来源下已存在");
                    continue;
                }

                Map<String, String> epidemicFields = buildEpidemicFieldsFromImportRow(row, headerIndex);
                String epidemicJson = epidemicFields.isEmpty()
                        ? null
                        : objectMapper.writeValueAsString(epidemicFields);

                Patient patient = Patient.builder()
                        .populationType(populationType)
                        .name(name)
                        .idNumber(idNumber)
                        .phone(phone)
                        .gender(getImportField(row, headerIndex, "性别"))
                        .age(parseIntegerField(getImportField(row, headerIndex, "年龄")))
                        .idType(StrUtil.blankToDefault(getImportField(row, headerIndex, "证件类型"), "居民身份证"))
                        .ethnicity(getImportField(row, headerIndex, "民族"))
                        .householdAddress(getImportField(row, headerIndex, "户籍地址"))
                        .currentAddress(getImportField(row, headerIndex, "现住址"))
                        .diagnosisResult(getImportField(row, headerIndex, "诊断结果"))
                        .epidemicData(epidemicJson)
                        .source("manual")
                        .archived(0)
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .creatorId(BaseContext.getCurrentId())
                        .build();

                String birthDate = getImportField(row, headerIndex, "出生日期");
                if (StrUtil.isNotBlank(birthDate)) {
                    LocalDate parsedBirthDate = parseLocalDateField(birthDate);
                    if (parsedBirthDate == null) {
                        result.addError(rowNum, name, "出生日期格式不正确");
                        continue;
                    }
                    patient.setBirthDate(parsedBirthDate);
                }

                save(patient);
                importedKeys.add(dedupeKey);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.addError(rowNum, getImportField(row, headerIndex, "姓名"), "数据解析失败：" + e.getMessage());
            }
        }

        if (result.getSuccessCount() == 0 && result.getErrors().isEmpty()) {
            result.addError(0, "", "未找到有效数据行，请确认已填写姓名和证件号");
        }

        log.info("在管患者批量导入完成，成功 {} 条，错误 {} 条", result.getSuccessCount(), result.getErrors().size());
        return result;
    }

    private Map<String, String> buildEpidemicFieldsFromImportRow(
            Map<Integer, String> row, Map<String, Integer> headerIndex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String header : PatientManualImportHeaders.FIELDS) {
            if ("数据来源".equals(header) || "姓名".equals(header) || "性别".equals(header)
                    || "出生日期".equals(header) || "年龄".equals(header) || "证件类型".equals(header)
                    || "证件号".equals(header) || "民族".equals(header) || "联系电话".equals(header)
                    || "户籍地址".equals(header) || "现住址".equals(header) || "诊断结果".equals(header)) {
                continue;
            }
            String value = getImportField(row, headerIndex, header);
            if ("联系人监护人电话号码".equals(header)) {
                value = normalizeExcelCellText(value);
            }
            if (StrUtil.isNotBlank(value)) {
                fields.put(header, value);
            }
        }
        return fields;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePatients(List<Long> ids) {
        for (Long id : ids) {
            deletePatient(id);
        }
    }

    private Integer parseIntegerField(Object val) {
        if (val == null || StrUtil.isBlank(val.toString())) return null;
        try {
            String digits = val.toString().trim().replaceAll("[^0-9]", "");
            if (StrUtil.isBlank(digits)) return null;
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isValidIdCard(String id) {
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private String getImportField(Map<Integer, String> row, Map<String, Integer> headerIndex, String... headers) {
        for (String header : headers) {
            Integer idx = headerIndex.get(header);
            if (idx == null) continue;
            String val = row.get(idx);
            if (StrUtil.isNotBlank(val)) return val.trim();
        }
        return "";
    }

    private String normalizeExcelCellText(String val) {
        if (StrUtil.isBlank(val)) return "";
        String text = val.trim();
        if (text.matches(".*[eE].*") || text.matches("\\d+\\.0+")) {
            try {
                return new java.math.BigDecimal(text).toPlainString();
            } catch (Exception ignored) {
                return text;
            }
        }
        return text;
    }

    private String resolvePopulationType(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String v = raw.trim();
        if (MANUAL_POPULATION_TYPES.contains(v)) return v;
        return switch (v) {
            case "学生筛查" -> "school";
            case "重点人群" -> "keyPopulation";
            case "疫情筛查", "常规筛查" -> "regular";
            case "大疫情" -> "epidemic";
            case "推介" -> "referral";
            case "密接" -> "closeContact";
            case "专病网" -> "specialDisease";
            default -> "";
        };
    }
}
