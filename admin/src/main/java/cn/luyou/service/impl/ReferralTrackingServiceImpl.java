package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                                              Integer trackingStatus, Integer archived) {
        LambdaQueryWrapper<ReferralTracking> wrapper = new LambdaQueryWrapper<ReferralTracking>()
                .eq(StrUtil.isNotBlank(bizMode), ReferralTracking::getBizMode, bizMode)
                .like(StrUtil.isNotBlank(name), ReferralTracking::getName, name)
                .like(StrUtil.isNotBlank(idNumber), ReferralTracking::getIdNumber, idNumber)
                .eq(trackingStatus != null, ReferralTracking::getTrackingStatus, trackingStatus)
                .eq(archived != null, ReferralTracking::getArchived, archived)
                .orderByDesc(ReferralTracking::getCreateTime);

        IPage<ReferralTracking> pageResult = page(new Page<>(page, size), wrapper);

        // 填充接收人姓名
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
    public void update(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (getStr(params, "name") != null) record.setName(getStr(params, "name"));
        if (getStr(params, "gender") != null) record.setGender(getStr(params, "gender"));
        if (params.get("birthDate") != null) record.setBirthDate(parseDate(params.get("birthDate")));
        if (getInt(params, "age") != null) record.setAge(getInt(params, "age"));
        if (getStr(params, "idType") != null) record.setIdType(getStr(params, "idType"));
        if (getStr(params, "idNumber") != null) record.setIdNumber(getStr(params, "idNumber"));
        if (getStr(params, "ethnicity") != null) record.setEthnicity(getStr(params, "ethnicity"));
        if (getStr(params, "phone") != null) record.setPhone(getStr(params, "phone"));
        if (getStr(params, "householdAddress") != null) record.setHouseholdAddress(getStr(params, "householdAddress"));
        if (getStr(params, "currentAddress") != null) record.setCurrentAddress(getStr(params, "currentAddress"));
        if (getStr(params, "crowdCategory") != null) record.setCrowdCategory(getStr(params, "crowdCategory"));
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

        // 推介模式须已被接收才能追踪
        if ("recommend".equals(record.getBizMode())
                && !Integer.valueOf(2).equals(record.getRecommendStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介通知单尚未被接收方确认，暂不可追踪");
        }

        // 已归档/已完成流程则不允许再操作
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法继续追踪");
        }

        if (status == null || status < 1 || status > 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "追踪状态值无效（1到位 2未到位 3其他）");
        }

        switch (status) {
            case 1 -> {
                // 到位
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 1)
                        .set(StrUtil.isNotBlank(remark), ReferralTracking::getTrackingRemark, remark)
                        .update();
                log.info("推介追踪到位，recordId={}", id);
            }
            case 2 -> {
                // 未到位：累计次数，第3次强制结束
                int newCount = (record.getNotInPlaceCount() == null ? 0 : record.getNotInPlaceCount()) + 1;
                if (newCount >= 3) {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 4)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(StrUtil.isNotBlank(remark), ReferralTracking::getTrackingRemark, remark)
                            .set(ReferralTracking::getArchived, 1)
                            .update();
                    log.info("推介追踪3次未到位强制结束，recordId={}", id);
                } else {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 2)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(StrUtil.isNotBlank(remark), ReferralTracking::getTrackingRemark, remark)
                            .update();
                }
            }
            case 3 -> {
                // 其他：归档
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 3)
                        .set(StrUtil.isNotBlank(remark), ReferralTracking::getTrackingRemark, remark)
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪选择其他，已归档，recordId={}", id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScreening(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (!Integer.valueOf(1).equals(record.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后才可录入筛查信息");
        }

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
        try {
            return LocalDate.parse(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
