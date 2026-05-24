package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.utils.BaseContext;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.ReferralTrackingMapper;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.model.ReferralTracking;
import cn.luyou.model.User;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralTrackingService;
import cn.luyou.service.SysMessageService;
import cn.luyou.service.UserService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralTrackingServiceImpl extends ServiceImpl<ReferralTrackingMapper, ReferralTracking>
        implements ReferralTrackingService {

    private final UserService userService;
    private final PatientService patientService;
    private final LatentInfectionMapper latentInfectionMapper;
    private final SysMessageService sysMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReferralTracking create(Map<String, Object> params) {
        String bizMode = getStr(params, "bizMode");
        if (StrUtil.isBlank(bizMode)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "bizMode 不能为空（recommend/track）");
        }
        Long currentUserId = BaseContext.getCurrentId();
        User currentUser = userService.getById(currentUserId);

        ReferralTracking record = ReferralTracking.builder()
                .bizMode(bizMode)
                .sourceType("manual")
                .name(getStr(params, "name"))
                .gender(getStr(params, "gender"))
                .birthDate(parseDate(params.get("birthDate")))
                .age(getInt(params, "age"))
                .idType(getStr(params, "idType"))
                .idNumber(getStr(params, "idNumber"))
                .ethnicity(getStr(params, "ethnicity"))
                .phone(getStr(params, "phone"))
                .householdAddress(getStr(params, "householdAddress"))
                .currentAddress(getStr(params, "currentAddress"))
                .crowdCategory(getStr(params, "crowdCategory"))
                .trackingStatus(0)
                .notInPlaceCount(0)
                .archived(0)
                .creatorId(currentUserId)
                .departmentId(currentUser != null ? currentUser.getDepartmentId() : null)
                .build();

        if ("recommend".equals(bizMode)) {
            validateRecommendRequired(params);
            Long receiverUserId = getLong(params, "receiverUserId");
            User receiver = userService.getById(receiverUserId);
            if (receiver == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人不存在");
            }
            record.setReceiverUserId(receiverUserId);
            record.setReceiverDeptId(receiver.getDepartmentId());
            record.setRecommendStatus(0);
            record.setRecommendReason(getStr(params, "recommendReason"));
        } else if ("track".equals(bizMode)) {
            validateTrackRequired(params);
            record.setTrackReason(getStr(params, "trackReason"));
        }

        save(record);
        return record;
    }

    @Override
    public IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                              String name, String idNumber,
                                              Integer trackingStatus, Integer archived,
                                              String phone, String township,
                                              String dateFrom, String dateTo, String sourceType) {
        LambdaQueryWrapper<ReferralTracking> wrapper = buildQueryWrapper(
                bizMode, name, idNumber, trackingStatus, archived, phone, township, dateFrom, dateTo, sourceType);

        IPage<ReferralTracking> pageResult = page(new Page<>(page, size), wrapper);

        pageResult.getRecords().forEach(r -> {
            if (r.getReceiverUserId() != null) {
                User receiver = userService.getById(r.getReceiverUserId());
                if (receiver != null) {
                    r.setReceiverUserName(receiver.getRealName());
                }
            }
        });

        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importEpidemic(MultipartFile file) {
        String batchNo = IdUtil.fastSimpleUUID();
        List<Map<Integer, String>> allRows = readExcelRows(file);

        if (allRows.size() < 2) {
            return Map.of("count", 0, "batchNo", batchNo);
        }

        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = buildHeaderIndex(headerRow);
        int count = 0;

        for (Map<Integer, String> row : allRows.subList(1, allRows.size())) {
            String cardId = getFieldByHeader(row, headerIndex, "卡片ID");
            String name = getFieldByHeader(row, headerIndex, "患者姓名", "姓名");
            String idNumber = getFieldByHeader(row, headerIndex, "有效证件号", "证件号", "身份证号", "身份证");
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber) && StrUtil.isBlank(cardId)) {
                continue;
            }

            if (existsEpidemicRecord(cardId, idNumber, name,
                    getFieldByHeader(row, headerIndex, "出生日期"),
                    getFieldByHeader(row, headerIndex, "联系电话", "电话"))) {
                continue;
            }

            String currentAddress = getFieldByHeader(row, headerIndex,
                    "现住详细地址", "现详细住址", "现住地址区现住详细", "现住址", "现住地址");
            String township = getFieldByHeader(row, headerIndex, "乡镇", "病人属于");
            if (StrUtil.isBlank(township)) {
                township = extractTownship(currentAddress);
            }

            ReferralTracking entity = ReferralTracking.builder()
                    .bizMode("track")
                    .sourceType("epidemic")
                    .cardId(cardId)
                    .name(name)
                    .parentName(getFieldByHeader(row, headerIndex, "患儿家长姓名"))
                    .idNumber(idNumber)
                    .gender(getFieldByHeader(row, headerIndex, "性别"))
                    .birthDate(parseDate(getFieldByHeader(row, headerIndex, "出生日期")))
                    .age(parseInt(getFieldByHeader(row, headerIndex, "年龄")))
                    .workplace(getFieldByHeader(row, headerIndex, "患者工作单位"))
                    .phone(getFieldByHeader(row, headerIndex, "联系电话", "电话"))
                    .currentAddress(currentAddress)
                    .township(township)
                    .crowdCategory(getFieldByHeader(row, headerIndex, "人群分类"))
                    .caseCategory(getFieldByHeader(row, headerIndex, "病例分类"))
                    .diseaseName(getFieldByHeader(row, headerIndex, "疾病名称"))
                    .reportUnit(getFieldByHeader(row, headerIndex, "报告单位"))
                    .reportCardTime(parseDateTime(getFieldByHeader(row, headerIndex, "报告卡录入时间")))
                    .epidemicRemark(getFieldByHeader(row, headerIndex, "备注"))
                    .trackReason("大疫情导入")
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

        log.info("大疫情导入追踪记录完成，count={}, batchNo={}", count, batchNo);
        return Map.of("count", count, "batchNo", batchNo);
    }

    @Override
    public void exportTrack(HttpServletResponse response, String bizMode,
                            String name, String idNumber, String phone, String township,
                            String dateFrom, String dateTo, String sourceType) {
        LambdaQueryWrapper<ReferralTracking> wrapper = buildQueryWrapper(
                bizMode, name, idNumber, null, null, phone, township, dateFrom, dateTo, sourceType);
        List<ReferralTracking> records = list(wrapper);

        List<List<String>> head = Arrays.asList(
                List.of("数据来源"), List.of("卡片ID"), List.of("患者姓名"), List.of("患儿家长姓名"),
                List.of("有效证件号"), List.of("性别"), List.of("出生日期"), List.of("年龄"),
                List.of("患者工作单位"), List.of("联系电话"), List.of("乡镇"), List.of("现住详细地址"),
                List.of("人群分类"), List.of("病例分类"), List.of("疾病名称"), List.of("报告单位"),
                List.of("报告卡录入时间"), List.of("备注"), List.of("追踪状态"), List.of("诊断结果"),
                List.of("创建时间"), List.of("到位时间")
        );

        List<List<Object>> rows = new ArrayList<>();
        for (ReferralTracking r : records) {
            rows.add(Arrays.asList(
                    "epidemic".equals(r.getSourceType()) ? "大疫情导入" : "手动录入",
                    r.getCardId(), r.getName(), r.getParentName(), r.getIdNumber(), r.getGender(),
                    r.getBirthDate() != null ? r.getBirthDate().toString() : "",
                    r.getAge(), r.getWorkplace(), r.getPhone(), r.getTownship(), r.getCurrentAddress(),
                    r.getCrowdCategory(), r.getCaseCategory(), r.getDiseaseName(), r.getReportUnit(),
                    r.getReportCardTime() != null ? r.getReportCardTime().toString() : "",
                    r.getEpidemicRemark(), trackingStatusLabel(r.getTrackingStatus()),
                    r.getDiagnosisResult(),
                    r.getCreateTime() != null ? r.getCreateTime().toString() : "",
                    r.getArrivalTime() != null ? r.getArrivalTime().toString() : ""
            ));
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("追踪记录导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream()).head(head).sheet("追踪记录").doWrite(rows);
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "导出失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (getStr(params, "name") != null) record.setName(getStr(params, "name"));
        if (getStr(params, "gender") != null) record.setGender(getStr(params, "gender"));
        if (params.get("birthDate") != null && StrUtil.isNotBlank(params.get("birthDate").toString())) {
            record.setBirthDate(parseDate(params.get("birthDate")));
        }
        if (getInt(params, "age") != null) record.setAge(getInt(params, "age"));
        if (getStr(params, "idType") != null) record.setIdType(getStr(params, "idType"));
        if (getStr(params, "idNumber") != null) record.setIdNumber(getStr(params, "idNumber"));
        if (getStr(params, "ethnicity") != null) record.setEthnicity(getStr(params, "ethnicity"));
        if (getStr(params, "phone") != null) record.setPhone(getStr(params, "phone"));
        if (getStr(params, "householdAddress") != null) record.setHouseholdAddress(getStr(params, "householdAddress"));
        if (getStr(params, "currentAddress") != null) record.setCurrentAddress(getStr(params, "currentAddress"));
        if (getStr(params, "crowdCategory") != null) record.setCrowdCategory(getStr(params, "crowdCategory"));
        if (getStr(params, "trackReason") != null) record.setTrackReason(getStr(params, "trackReason"));
        if (getStr(params, "cardId") != null) record.setCardId(getStr(params, "cardId"));
        if (getStr(params, "parentName") != null) record.setParentName(getStr(params, "parentName"));
        if (getStr(params, "workplace") != null) record.setWorkplace(getStr(params, "workplace"));
        if (getStr(params, "township") != null) record.setTownship(getStr(params, "township"));
        if (getStr(params, "caseCategory") != null) record.setCaseCategory(getStr(params, "caseCategory"));
        if (getStr(params, "diseaseName") != null) record.setDiseaseName(getStr(params, "diseaseName"));
        if (getStr(params, "reportUnit") != null) record.setReportUnit(getStr(params, "reportUnit"));
        if (params.get("reportCardTime") != null && StrUtil.isNotBlank(params.get("reportCardTime").toString())) {
            record.setReportCardTime(parseDateTime(params.get("reportCardTime").toString()));
        }
        if (getStr(params, "epidemicRemark") != null) record.setEpidemicRemark(getStr(params, "epidemicRemark"));
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendRecommend(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        if (!"recommend".equals(record.getBizMode())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介模式可发送推介通知");
        }
        if (record.getRecommendStatus() != null && record.getRecommendStatus() > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介通知已发送，不可重复发送");
        }
        if (record.getReceiverUserId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "未指定接收人");
        }

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 1)
                .set(ReferralTracking::getRecommendSentTime, LocalDateTime.now())
                .update();

        // 向接收人发送系统消息
        String title = "新推介通知单待接收";
        String reasonPart = StrUtil.isNotBlank(record.getRecommendReason())
                ? "，推介原因：" + record.getRecommendReason() : "";
        String content = String.format("收到「%s」的推介通知单（人群分类：%s%s），请尽快确认接收。",
                StrUtil.blankToDefault(record.getName(), "（未知姓名）"),
                StrUtil.blankToDefault(record.getCrowdCategory(), "-"),
                reasonPart);
        sysMessageService.sendMessage(record.getReceiverUserId(), title, content, "referral", id);
        log.info("推介通知单已发送，recordId={}, receiverUserId={}", id, record.getReceiverUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecommend(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        if (!"recommend".equals(record.getBizMode())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介模式可确认推介");
        }
        if (!Integer.valueOf(1).equals(record.getRecommendStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前状态无法确认（须为已发送状态）");
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 2)
                .set(ReferralTracking::getRecommendConfirmTime, LocalDateTime.now())
                .update();

        // 通知推介发起人
        if (record.getCreatorId() != null) {
            String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
            sysMessageService.sendMessage(record.getCreatorId(), "推介通知单已确认接收",
                    String.format("「%s」的推介通知单已被接收方确认，可进入追踪环节。", name),
                    "referral", id);
        }
        log.info("推介通知单已确认接收，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRecommend(Long id, String reason) {
        ReferralTracking record = getAndCheckExist(id);
        if (!"recommend".equals(record.getBizMode())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介模式可拒绝推介");
        }
        if (!Integer.valueOf(1).equals(record.getRecommendStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前状态无法拒绝（须为已发送状态）");
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 3)
                .set(ReferralTracking::getRejectedReason, reason)
                .set(ReferralTracking::getRecommendConfirmTime, LocalDateTime.now())
                .set(ReferralTracking::getArchived, 1)
                .update();

        // 通知推介发起人
        if (record.getCreatorId() != null) {
            String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
            sysMessageService.sendMessage(record.getCreatorId(), "推介通知单已被拒绝",
                    String.format("「%s」的推介通知单被接收方拒绝，原因：%s",
                            name, StrUtil.blankToDefault(reason, "（未填写）")),
                    "referral", id);
        }
        log.info("推介通知单已被拒绝，recordId={}, reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark) {
        ReferralTracking record = getAndCheckExist(id);
        if ("recommend".equals(record.getBizMode())
                && !Integer.valueOf(2).equals(record.getRecommendStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介通知单尚未被接收方确认，暂不可追踪");
        }

        // 推介模式：仅接收方可进行追踪
        if ("recommend".equals(record.getBizMode())) {
            checkRecommendReceiver(record);
        }

        // 已归档/已完成流程则不允许再操作
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法继续追踪");
        }

        // 仅待追踪或未到位状态可继续追踪
        Integer trackingStatus = record.getTrackingStatus();
        if (trackingStatus != null && trackingStatus != 0 && trackingStatus != 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前追踪状态不允许继续操作");
        }

        if (status == null || status < 1 || status > 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "追踪状态值无效（1到位 2未到位 3其他）");
        }

        // 未到位必须填写原因
        if (status == 2 && StrUtil.isBlank(remark)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "未到位时必须填写原因");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> history = parseTrackingHistory(record.getTrackingHistoryJson());

        switch (status) {
            case 1 -> {
                // 到位：记录到位时间，不需要原因
                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 1);
                entry.put("trackTime", now.toString());
                history.add(entry);

                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 1)
                        .set(ReferralTracking::getArrivalTime, now)
                        .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                        .update();
                log.info("推介追踪到位，recordId={}", id);
            }
            case 2 -> {
                // 未到位：累计次数，记录追踪时间和原因，第3次强制结束
                int newCount = (record.getNotInPlaceCount() == null ? 0 : record.getNotInPlaceCount()) + 1;

                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 2);
                entry.put("trackTime", now.toString());
                entry.put("reason", remark);
                history.add(entry);

                if (newCount >= 3) {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 4)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(ReferralTracking::getTrackingRemark, remark)
                            .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                            .set(ReferralTracking::getArchived, 1)
                            .update();
                    log.info("推介追踪3次未到位强制结束，recordId={}", id);
                } else {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 2)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(ReferralTracking::getTrackingRemark, remark)
                            .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                            .update();
                }
            }
            case 3 -> {
                // 其他：归档
                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 3);
                entry.put("trackTime", now.toString());
                if (StrUtil.isNotBlank(remark)) {
                    entry.put("reason", remark);
                }
                history.add(entry);

                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 3)
                        .set(StrUtil.isNotBlank(remark), ReferralTracking::getTrackingRemark, remark)
                        .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪选择其他，已归档，recordId={}", id);
            }
        }
    }

    /** 解析追踪历史 JSON */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTrackingHistory(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return (List<Map<String, Object>>) (List<?>) JSONUtil.toList(json, Map.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScreening(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (!Integer.valueOf(1).equals(record.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后才可录入筛查信息");
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(getStr(params, "hasInfectionScreen") != null,
                        ReferralTracking::getHasInfectionScreen, getStr(params, "hasInfectionScreen"))
                .set(params.get("screenDate") != null,
                        ReferralTracking::getScreenDate, parseDate(params.get("screenDate")))
                .set(getStr(params, "screenMethod") != null,
                        ReferralTracking::getScreenMethod, getStr(params, "screenMethod"))
                .set(getStr(params, "screenResult") != null,
                        ReferralTracking::getScreenResult, getStr(params, "screenResult"))
                .set(getStr(params, "infectionResult") != null,
                        ReferralTracking::getInfectionResult, getStr(params, "infectionResult"))
                .set(getStr(params, "hasChestXray") != null,
                        ReferralTracking::getHasChestXray, getStr(params, "hasChestXray"))
                .set(params.get("chestXrayDate") != null,
                        ReferralTracking::getChestXrayDate, parseDate(params.get("chestXrayDate")))
                .set(getStr(params, "chestXrayResult") != null,
                        ReferralTracking::getChestXrayResult, getStr(params, "chestXrayResult"))
                .set(getStr(params, "symptomsJson") != null,
                        ReferralTracking::getSymptomsJson, getStr(params, "symptomsJson"))
                .update();

        log.info("推介追踪筛查信息已保存，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnosis(Long id, String diagnosisResult) {
        if (StrUtil.isBlank(diagnosisResult)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }
        ReferralTracking record = getAndCheckExist(id);
        if (!Integer.valueOf(1).equals(record.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后才可录入诊断结果");
        }
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法修改诊断结果");
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getDiagnosisResult, diagnosisResult)
                .set(ReferralTracking::getDiagnosisTime, LocalDateTime.now())
                .update();

        ReferralTracking updated = getById(id);

        switch (diagnosisResult) {
            case "排除", "其他" -> {
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪诊断归档（{}），recordId={}", diagnosisResult, id);
            }
            case "确诊患者" -> {
                Long patientId = createPatientFromTracking(updated);
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTargetPatientId, patientId)
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪确诊，已创建患者记录 patientId={}，recordId={}", patientId, id);
            }
            case "潜伏感染者" -> {
                Long latentId = createLatentFromTracking(updated);
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTargetLatentId, latentId)
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪潜伏感染者，已创建潜伏记录 latentId={}，recordId={}", latentId, id);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "无效的诊断结果，有效值：排除/确诊患者/潜伏感染者/其他");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        removeById(record.getId());
        log.info("推介追踪记录已删除，recordId={}", id);
    }

    // ===== 私有工具方法 =====

    private ReferralTracking getAndCheckExist(Long id) {
        ReferralTracking record = getById(id);
        if (record == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介追踪记录不存在");
        }
        return record;
    }

    /** 推介模式：校验当前用户是否为接收人 */
    private void checkRecommendReceiver(ReferralTracking record) {
        if (!"recommend".equals(record.getBizMode())) {
            return;
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (record.getReceiverUserId() == null || !record.getReceiverUserId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介接收人可进行此操作");
        }
    }

    /** 从推介追踪记录创建患者档案（populationType='referral'） */
    private Long createPatientFromTracking(ReferralTracking r) {
        // 幂等：若已创建则返回现有患者ID
        if (r.getTargetPatientId() != null) return r.getTargetPatientId();

        Patient patient = Patient.builder()
                .screeningId(null)
                .latentInfectionId(null)
                .populationType("referral")
                .name(r.getName())
                .gender(r.getGender())
                .birthDate(r.getBirthDate())
                .age(r.getAge())
                .idType(r.getIdType())
                .idNumber(r.getIdNumber())
                .ethnicity(r.getEthnicity())
                .phone(r.getPhone())
                .householdAddress(r.getHouseholdAddress())
                .currentAddress(r.getCurrentAddress())
                .diagnosisResult("确诊患者")
                .source("referral")
                .archived(0)
                .departmentId(r.getDepartmentId())
                .build();

        patientService.save(patient);
        return patient.getId();
    }

    /** 从推介追踪记录创建潜伏感染记录（populationType='referral'） */
    private Long createLatentFromTracking(ReferralTracking r) {
        if (r.getTargetLatentId() != null) return r.getTargetLatentId();

        LatentInfection latent = LatentInfection.builder()
                .screeningId(null)
                .populationType("referral")
                .name(r.getName())
                .idNumber(r.getIdNumber())
                .gender(r.getGender())
                .age(r.getAge())
                .phone(r.getPhone())
                .infectionResult(r.getInfectionResult())
                .trackingStatus(1)
                .notInPlaceCount(0)
                .hasChestXray(r.getHasChestXray())
                .chestXrayDate(r.getChestXrayDate())
                .chestXrayResult(r.getChestXrayResult())
                .diagnosisFirst("潜伏感染者")
                .diagnosisResult("潜伏感染者")
                .referralResult("latent")
                .treatmentPhase(0)
                .archived(0)
                .departmentId(r.getDepartmentId())
                .build();

        latentInfectionMapper.insert(latent);
        return latent.getId();
    }

    /** 追踪模式必填项校验 */
    private void validateTrackRequired(Map<String, Object> params) {
        if (StrUtil.isBlank(getStr(params, "idNumber"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写证件号");
        }
        if (StrUtil.isBlank(getStr(params, "phone"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写联系电话");
        }
        if (StrUtil.isBlank(getStr(params, "currentAddress"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写现住址");
        }
        if (StrUtil.isBlank(getStr(params, "crowdCategory"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择人群分类");
        }
        if (StrUtil.isBlank(getStr(params, "trackReason"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写追踪原因");
        }
    }

    /** 推介模式必填项校验 */
    private void validateRecommendRequired(Map<String, Object> params) {
        if (StrUtil.isBlank(getStr(params, "idNumber"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写证件号");
        }
        if (StrUtil.isBlank(getStr(params, "phone"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写联系电话");
        }
        if (StrUtil.isBlank(getStr(params, "currentAddress"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写现住址");
        }
        if (StrUtil.isBlank(getStr(params, "crowdCategory"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择人群分类");
        }
        if (StrUtil.isBlank(getStr(params, "recommendReason"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写推介原因");
        }
        if (getLong(params, "receiverUserId") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择推介接收人");
        }
    }

    private String getStr(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val == null ? null : val.toString();
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        return Integer.valueOf(val.toString());
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        return Long.valueOf(val.toString());
    }

    private LocalDate parseDate(Object val) {
        if (val == null) return null;
        String text = val.toString().trim();
        if (StrUtil.isBlank(text)) return null;

        if (text.matches("^\\d+(\\.\\d+)?$")) {
            try {
                double serial = Double.parseDouble(text);
                if (serial > 59) {
                    return LocalDate.of(1899, 12, 30).plusDays((long) Math.floor(serial));
                }
            } catch (Exception ignored) {
            }
        }

        try {
            return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e1) {
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (StrUtil.isBlank(text)) return null;
        String val = text.trim();
        if (val.matches("^\\d+(\\.\\d+)?$")) {
            LocalDate date = parseDate(val);
            return date != null ? date.atStartOfDay() : null;
        }
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd"}) {
            try {
                if (pattern.contains("HH")) {
                    return LocalDateTime.parse(val, DateTimeFormatter.ofPattern(pattern));
                }
                LocalDate date = LocalDate.parse(val, DateTimeFormatter.ofPattern(pattern));
                return date.atStartOfDay();
            } catch (Exception ignored) {
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

    private List<Map<Integer, String>> readExcelRows(MultipartFile file) {
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
        return allRows;
    }

    private Map<String, Integer> buildHeaderIndex(Map<Integer, String> headerRow) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        return headerIndex;
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
                // 短字段名仅精确匹配，避免「姓名」误匹配「患儿家长姓名」
                if (fieldName.length() <= 2) {
                    if (entry.getKey().equals(fieldName)) {
                        String val = row.get(entry.getValue());
                        if (StrUtil.isNotBlank(val)) {
                            return val.trim();
                        }
                    }
                } else if (entry.getKey().contains(fieldName)) {
                    String val = row.get(entry.getValue());
                    if (StrUtil.isNotBlank(val)) {
                        return val.trim();
                    }
                }
            }
        }
        return null;
    }

    private boolean existsEpidemicRecord(String cardId, String idNumber, String name, String birthDateText, String phone) {
        if (StrUtil.isNotBlank(cardId)) {
            return lambdaQuery()
                    .eq(ReferralTracking::getBizMode, "track")
                    .eq(ReferralTracking::getSourceType, "epidemic")
                    .eq(ReferralTracking::getCardId, cardId)
                    .exists();
        }
        LocalDate birthDate = parseDate(birthDateText);
        return lambdaQuery()
                .eq(ReferralTracking::getBizMode, "track")
                .eq(ReferralTracking::getSourceType, "epidemic")
                .and(w -> {
                    if (StrUtil.isNotBlank(idNumber)) {
                        w.eq(ReferralTracking::getIdNumber, idNumber);
                    } else {
                        w.eq(ReferralTracking::getName, name)
                                .eq(birthDate != null, ReferralTracking::getBirthDate, birthDate)
                                .eq(StrUtil.isNotBlank(phone), ReferralTracking::getPhone, phone);
                    }
                })
                .exists();
    }

    private String extractTownship(String address) {
        if (StrUtil.isBlank(address)) return null;
        int idx = address.indexOf("乡");
        if (idx > 0) {
            int start = Math.max(address.lastIndexOf("县", idx), address.lastIndexOf("区", idx));
            start = Math.max(start, address.lastIndexOf("市", idx));
            return address.substring(start + 1, idx + 1);
        }
        idx = address.indexOf("镇");
        if (idx > 0) {
            int start = Math.max(address.lastIndexOf("县", idx), address.lastIndexOf("区", idx));
            start = Math.max(start, address.lastIndexOf("市", idx));
            return address.substring(start + 1, idx + 1);
        }
        return null;
    }

    private LambdaQueryWrapper<ReferralTracking> buildQueryWrapper(
            String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
            String phone, String township, String dateFrom, String dateTo, String sourceType) {
        LocalDateTime from = parseDateTime(dateFrom);
        LocalDateTime to = parseDateTime(dateTo);
        if (to != null && to.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            to = to.plusDays(1).minusSeconds(1);
        }

        return new LambdaQueryWrapper<ReferralTracking>()
                .eq(StrUtil.isNotBlank(bizMode), ReferralTracking::getBizMode, bizMode)
                .like(StrUtil.isNotBlank(name), ReferralTracking::getName, name)
                .like(StrUtil.isNotBlank(idNumber), ReferralTracking::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), ReferralTracking::getPhone, phone)
                .like(StrUtil.isNotBlank(township), ReferralTracking::getTownship, township)
                .eq(trackingStatus != null, ReferralTracking::getTrackingStatus, trackingStatus)
                .eq(archived != null, ReferralTracking::getArchived, archived)
                .eq(StrUtil.isNotBlank(sourceType), ReferralTracking::getSourceType, sourceType)
                .ge(from != null, ReferralTracking::getCreateTime, from)
                .le(to != null, ReferralTracking::getCreateTime, to)
                .orderByDesc(ReferralTracking::getCreateTime);
    }

    private String trackingStatusLabel(Integer status) {
        if (status == null) return "待追踪";
        return switch (status) {
            case 1 -> "到位";
            case 2 -> "未到位";
            case 3 -> "其他";
            case 4 -> "强制结束";
            default -> "待追踪";
        };
    }
}
