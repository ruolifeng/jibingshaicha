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
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.QueryDateRangeUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatentInfectionServiceImpl extends ServiceImpl<LatentInfectionMapper, LatentInfection>
        implements LatentInfectionService {

    private final DepartmentService departmentService;
    private final PatientService patientService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;
    private final NoticeMapper noticeMapper;
    private final SupervisionFormMapper supervisionFormMapper;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
    private final SupervisionFormService supervisionFormService;
    private final NoticeService noticeService;
    private final ReferralService referralService;
    private final SysMessageService sysMessageService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final EpidemicReportService epidemicReportService;

    private static final Set<String> MANUAL_POPULATION_TYPES = Set.of(
            "school", "keyPopulation", "regular", "epidemic", "referral"
    );

    /** 导入时含首次诊断且需自动结案归档的诊断（不进入患者管理） */
    private static final List<String> DIAGNOSIS_AUTO_CLOSE = List.of("确诊患者");

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
                                             String referralResult, String diagnosisFirst,
                                             String phone, String dateFrom, String dateTo) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(populationType)) {
            // 指定来源时精确匹配（密接模块传 closeContact，其他模块传具体值）
            wrapper.eq(LatentInfection::getPopulationType, populationType);
        } else {
            // 聚合查询时自动排除密接（密接潜伏感染在密接人群管理菜单中单独管理）
            wrapper.ne(LatentInfection::getPopulationType, "closeContact");
        }
        wrapper.like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                .eq(archived != null, LatentInfection::getArchived, archived)
                .eq(StrUtil.isNotBlank(diagnosisFirst), LatentInfection::getDiagnosisFirst, diagnosisFirst)
                // 潜伏感染列表始终排除确诊患者/疑似肺结核（已结案归档，不进入患者管理）。
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

        wrapper.ge(createFrom != null, LatentInfection::getCreateTime, createFrom)
                .le(createTo != null, LatentInfection::getCreateTime, createTo)
                .orderByDesc(LatentInfection::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            Integer currentRole = BaseContext.getCurrentRole();
            if (currentRole != null && currentRole == 6) {
                // 五级管理员：只能看到发给自己的通知单所关联的潜伏感染记录
                Long userId = BaseContext.getCurrentId();
                wrapper.inSql(LatentInfection::getId,
                        "SELECT biz_id FROM notice WHERE receiver_org_id = " + userId
                                + " AND notice_type = 'latent' AND deleted = 0");
            } else {
                List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
                wrapper.in(LatentInfection::getDepartmentId, deptIds);
            }
        }
        IPage<LatentInfection> result = page(new Page<>(page, size), wrapper);

        // 补充通知单发送状态：用于前端控制“发送通知单”禁用和督导表启用
        List<LatentInfection> records = result.getRecords();
        if (records == null || records.isEmpty()) return result;

        records.forEach(this::fillNoticeAutoFields);
        fillNoticeAndSupervisionStatus(records, populationType);

        return result;
    }

    /** 补充通知单发送状态与督导表状态（列表/详情共用） */
    private void fillNoticeAndSupervisionStatus(List<LatentInfection> records, String populationType) {
        if (records == null || records.isEmpty()) return;

        List<Long> latentIds = records.stream()
                .map(LatentInfection::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (latentIds.isEmpty()) {
            records.forEach(r -> {
                r.setNoticeSent(false);
                r.setNoticeStatus(null);
                r.setNoticeId(null);
                r.setSupervisionCompleted(false);
                r.setSupervisionStatus(0);
            });
            return;
        }

        Map<Long, cn.luyou.model.Notice> noticeMap = noticeMapper.selectList(
                new LambdaQueryWrapper<cn.luyou.model.Notice>()
                        .in(cn.luyou.model.Notice::getBizId, latentIds)
                        .eq(cn.luyou.model.Notice::getNoticeType, "latent")
                        .eq(StrUtil.isNotBlank(populationType), cn.luyou.model.Notice::getPopulationType, populationType)
                        .orderByDesc(cn.luyou.model.Notice::getId)
        ).stream().collect(java.util.stream.Collectors.toMap(
                cn.luyou.model.Notice::getBizId,
                n -> n,
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));

        records.forEach(r -> {
            cn.luyou.model.Notice notice = noticeMap.get(r.getId());
            if (notice != null) {
                r.setNoticeStatus(notice.getStatus());
                r.setNoticeId(notice.getId());
                r.setNoticeSent(notice.getStatus() != null && notice.getStatus() >= 1);
            } else {
                r.setNoticeStatus(null);
                r.setNoticeId(null);
                r.setNoticeSent(false);
            }
        });

        Map<Long, Integer> supervisionStatusMap = supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>()
                        .in(SupervisionForm::getLatentInfectionId, latentIds)
                        .orderByDesc(SupervisionForm::getCreateTime)
        ).stream().collect(java.util.stream.Collectors.toMap(
                SupervisionForm::getLatentInfectionId,
                SupervisionForm::getStatus,
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));
        records.forEach(r -> {
            Integer status = supervisionStatusMap.get(r.getId());
            r.setSupervisionStatus(status != null ? status : 0);
            r.setSupervisionCompleted(Integer.valueOf(2).equals(status));
        });
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
        // 兼容入口：批量导入与旧前端继续走此方法（一次性同时传胸片+诊断）。
        // 内部拆分为两步以复用 V13 的拆分逻辑。
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
        // 先写胸片（不校验"已录入"，沿用旧行为：覆盖式写入）
        doSaveXray(entity, data, /* skipExistsCheck */ true);
        // 重新加载后写诊断并触发转诊
        LatentInfection refreshed = getById(id);
        doSaveDiagnosis(refreshed, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXrayOnly(Long id, Map<String, Object> data) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入胸片结果");
        }
        if (StrUtil.isNotBlank(entity.getChestXrayResult())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片结果已录入，不可重复操作");
        }
        doSaveXray(entity, data, /* skipExistsCheck */ false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnosisOnly(Long id, Map<String, Object> data) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入诊断结果");
        }
        if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果已录入，不可重复操作");
        }
        doSaveDiagnosis(entity, data);
    }

    /** 内部：写胸片字段 + 回写筛查表。skipExistsCheck=true 时不做"已录入"校验（兼容老接口）。 */
    private void doSaveXray(LatentInfection entity, Map<String, Object> data, boolean skipExistsCheck) {
        String hasXray = data.getOrDefault("hasChestXray", "").toString();
        String xrayResult = data.getOrDefault("chestXrayResult", "").toString();
        LocalDate xrayDate = null;
        Object xrayDateObj = data.get("chestXrayDate");
        if (xrayDateObj != null && StrUtil.isNotBlank(xrayDateObj.toString())) {
            xrayDate = LocalDate.parse(xrayDateObj.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, entity.getId())
                .set(LatentInfection::getHasChestXray, hasXray)
                .set(LatentInfection::getChestXrayDate, xrayDate)
                .set(LatentInfection::getChestXrayResult, xrayResult)
                .update();
        // 同步回写到筛查表（不传 diagnosisFirst）
        writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult, null);
    }

    /** 内部：写诊断字段 + 回写筛查表 + 触发转诊映射 */
    private void doSaveDiagnosis(LatentInfection entity, Map<String, Object> data) {
        String diagnosisFirst = data.getOrDefault("diagnosisFirst", "").toString();
        if (StrUtil.isBlank(diagnosisFirst)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, entity.getId())
                .set(LatentInfection::getDiagnosisFirst, diagnosisFirst)
                .update();
        // 同步回写筛查表诊断字段（不动胸片）
        writeBackXrayToScreening(entity, null, null, null, diagnosisFirst);

        // 根据首次诊断自动驱动转诊（与 referral() 语义一致）
        String referralCode = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
        if (referralCode != null) {
            LatentInfection refreshed = getById(entity.getId());
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
            case "keyPopulation", "regular" -> {
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
            case "keyPopulation", "regular" -> 4;
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
                case "keyPopulation", "regular" -> {
                    // 重点/常规筛查：hasChestXray(37) chestXrayDate(38) chestXrayResult(39) diagnosisFirst(40)
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
            String hasXray = getStrCell(row, hasXrayIdx);
            LocalDate xrayDate = parseDateCell(row.get(xrayDateIdx));
            String xrayResult = getStrCell(row, xrayResultIdx);

            // 至少包含胸片结果或确认诊断才更新（支持仅导入胸片结果）
            if (StrUtil.isBlank(xrayResult) && StrUtil.isBlank(diagnosisFirst)) continue;

            // 写入胸片字段；确认诊断可选，有值时一并写入并驱动转诊
            var updateWrapper = lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(StrUtil.isNotBlank(hasXray), LatentInfection::getHasChestXray, hasXray)
                    .set(xrayDate != null, LatentInfection::getChestXrayDate, xrayDate)
                    .set(StrUtil.isNotBlank(xrayResult), LatentInfection::getChestXrayResult, xrayResult);
            if (StrUtil.isNotBlank(diagnosisFirst)) {
                updateWrapper.set(LatentInfection::getDiagnosisFirst, diagnosisFirst);
            }
            updateWrapper.update();

            writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult,
                    StrUtil.isNotBlank(diagnosisFirst) ? diagnosisFirst : null);

            // 与单条录入保持一致：有确认诊断时自动驱动转诊
            if (StrUtil.isNotBlank(diagnosisFirst)) {
                String referralCode = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
                if (referralCode != null) {
                    LatentInfection refreshed = getById(entity.getId());
                    applyReferralOutcome(refreshed, referralCode, null);
                }
            }
            updated++;
        }
        log.info("批量导入胸片结果，populationType={}，成功更新 {} 条", populationType, updated);
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
     * 执行转诊后的状态变更：写入 referralResult/diagnosisResult，并按结果归档或进入潜伏感染管理。
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
            case "confirmed" -> {
                // 筛查确诊患者仅结案归档，不进入患者管理（患者管理数据仅来自专病信息表导入）
                entity.setDiagnosisResult("确诊患者");
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "suspected" -> {
                entity.setDiagnosisResult("疑似肺结核");
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
            if (!DIAGNOSIS_AUTO_CLOSE.contains(diagnosisFirst)) continue;

            String referralResult = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
            lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(LatentInfection::getReferralResult, referralResult)
                    .set(LatentInfection::getDiagnosisResult, diagnosisFirst)
                    .set(LatentInfection::getArchived, 1)
                    .set(LatentInfection::getArchivedTime, LocalDateTime.now())
                    .update();

            log.info("导入时自动结案 latentId={} diagnosisFirst={} referralResult={}", entity.getId(), diagnosisFirst, referralResult);
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

    @Override
    public LatentInfection getDetail(Long id) {
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        fillNoticeAutoFields(latent);
        fillNoticeAndSupervisionStatus(List.of(latent), latent.getPopulationType());
        return latent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicInfo(Long id, Map<String, Object> body) {
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        if (body.get("name") != null) latent.setName(body.get("name").toString());
        if (body.get("gender") != null) latent.setGender(body.get("gender").toString());
        if (body.get("age") != null) {
            Object ageVal = body.get("age");
            latent.setAge(ageVal == null || "".equals(ageVal.toString()) ? null : Integer.valueOf(ageVal.toString()));
        }
        if (body.get("idNumber") != null) latent.setIdNumber(body.get("idNumber").toString());
        if (body.get("phone") != null) latent.setPhone(body.get("phone").toString());
        if (body.get("infectionResult") != null) latent.setInfectionResult(body.get("infectionResult").toString());
        if (body.get("diagnosisFirst") != null) latent.setDiagnosisFirst(body.get("diagnosisFirst").toString());
        if (body.get("hasChestXray") != null) latent.setHasChestXray(body.get("hasChestXray").toString());
        if (body.get("chestXrayDate") != null) {
            latent.setChestXrayDate(parseDateCell(body.get("chestXrayDate")));
        }
        if (body.get("chestXrayResult") != null) latent.setChestXrayResult(body.get("chestXrayResult").toString());
        if (body.get("trackingRemark") != null) latent.setTrackingRemark(body.get("trackingRemark").toString());
        updateById(latent);
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

        LatentInfection latent = LatentInfection.builder()
                .populationType(populationType)
                .name(name)
                .idNumber(idNumber)
                .phone(phone)
                .gender(body.getOrDefault("gender", "").toString())
                .age(parseIntegerField(body.get("age")))
                .infectionResult(body.getOrDefault("infectionResult", "").toString())
                .diagnosisFirst(body.getOrDefault("diagnosisFirst", "").toString())
                .hasChestXray(body.getOrDefault("hasChestXray", "").toString())
                .chestXrayDate(parseDateCell(body.get("chestXrayDate")))
                .chestXrayResult(body.getOrDefault("chestXrayResult", "").toString())
                .trackingRemark(body.getOrDefault("trackingRemark", "").toString())
                .trackingStatus(0)
                .notInPlaceCount(0)
                .archived(0)
                .departmentId(BaseContext.getCurrentDepartmentId())
                .build();
        save(latent);
        log.info("手动新增潜伏感染记录 id={}, populationType={}", latent.getId(), populationType);
        return latent.getId();
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
                    log.info("潜伏感染者批量导入解析完成，共 {} 行（含表头）", allRows.size());
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
                    result.addError(rowNum, name, "数据来源无效（请填写：学生筛查/重点人群/疫情筛查/大疫情/推介）");
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
                        .eq(LatentInfection::getIdNumber, idNumber)
                        .eq(LatentInfection::getPopulationType, populationType)
                        .eq(LatentInfection::getArchived, 0)
                        .exists()) {
                    result.addError(rowNum, name, "该证件号在此数据来源下已存在");
                    continue;
                }

                LatentInfection latent = LatentInfection.builder()
                        .populationType(populationType)
                        .name(name)
                        .idNumber(idNumber)
                        .phone(phone)
                        .gender(getImportField(row, headerIndex, "性别"))
                        .age(parseIntegerField(getImportField(row, headerIndex, "年龄")))
                        .infectionResult(getImportField(row, headerIndex, "感染筛查结果"))
                        .diagnosisFirst(getImportField(row, headerIndex, "首次诊断"))
                        .hasChestXray(getImportField(row, headerIndex, "是否胸片检查"))
                        .chestXrayDate(parseDateCell(getImportField(row, headerIndex, "胸片检查日期")))
                        .chestXrayResult(getImportField(row, headerIndex, "胸片检查结果"))
                        .trackingRemark(getImportField(row, headerIndex, "追踪备注"))
                        .trackingStatus(0)
                        .notInPlaceCount(0)
                        .archived(0)
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .build();
                save(latent);
                importedKeys.add(dedupeKey);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.addError(rowNum, getImportField(row, headerIndex, "姓名"), "数据解析失败：" + e.getMessage());
            }
        }

        if (result.getSuccessCount() == 0 && result.getErrors().isEmpty()) {
            result.addError(0, "", "未找到有效数据行，请确认已填写姓名和证件号");
        }

        log.info("潜伏感染者批量导入完成，成功 {} 条，错误 {} 条", result.getSuccessCount(), result.getErrors().size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCascade(Long id) {
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        if ("closeContact".equals(latent.getPopulationType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "密接来源记录请在密接人群管理模块操作");
        }
        doDeleteCascade(id);
        log.info("级联删除潜伏感染记录 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCascade(List<Long> ids) {
        for (Long id : ids) {
            deleteCascade(id);
        }
    }

    private void doDeleteCascade(Long latentId) {
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, latentId)
                .list();
        for (Patient patient : patientList) {
            Long pid = patient.getId();
            firstVisitService.lambdaUpdate().eq(cn.luyou.model.FirstVisit::getPatientId, pid).remove();
            followUpVisitService.lambdaUpdate().eq(cn.luyou.model.FollowUpVisit::getPatientId, pid).remove();
            medicationManagementService.lambdaUpdate().eq(cn.luyou.model.MedicationManagement::getPatientId, pid).remove();
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            deleteNoticeAndMessages(pid, "patient");
            deleteReferralsAndMessages(pid);
            patientService.removeById(pid);
        }
        supervisionFormService.lambdaUpdate()
                .eq(SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        deleteNoticeAndMessages(latentId, "latent");
        deleteReferralsAndMessages(latentId);
        removeById(latentId);
    }

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

    private void deleteReferralsAndMessages(Long bizId) {
        List<Long> referralIds = referralService.lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .list().stream().map(Referral::getId).toList();
        if (!referralIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, referralIds).remove();
            referralService.lambdaUpdate().eq(Referral::getBizId, bizId).remove();
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

    /** 兼容 Excel 数值单元格（科学计数法、末尾 .0） */
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
            default -> "";
        };
    }
}
