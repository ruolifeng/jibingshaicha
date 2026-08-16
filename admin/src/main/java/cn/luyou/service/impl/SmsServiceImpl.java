package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.SysSmsConfig;
import cn.luyou.model.User;
import cn.luyou.service.SmsService;
import cn.luyou.service.SysSmsConfigService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信发送（系统消息联动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private static final int MAX_TEMPLATE_PARAM_LEN = 40;
    private static final String PHONE_PATTERN = "^1\\d{10}$";
    /** 腾讯云模板变量禁用/易导致发送失败的字符 */
    private static final String FORBIDDEN_CHARS = "[【】\\{\\}￥★※✓☞&^]";

    private final SysSmsConfigService smsConfigService;
    private final UserMapper userMapper;

    @Override
    @Async
    public void sendForMessageAsync(Long receiverId, String title, String content, String type) {
        try {
            if (receiverId == null) {
                return;
            }
            SysSmsConfig cfg = smsConfigService.getRawConfig();
            if (!isConfigReady(cfg)) {
                return;
            }
            User user = userMapper.selectById(receiverId);
            if (user == null) {
                log.warn("短信跳过：接收用户不存在 receiverId={}", receiverId);
                return;
            }
            String phone = StrUtil.trim(user.getPhone());
            if (!isValidMobile(phone)) {
                log.info("短信跳过：用户无有效联系电话 userId={} username={}", user.getId(), user.getUsername());
                return;
            }
            String summary = buildSummary(title, content);
            doSend(cfg, phone, summary);
            log.info("短信已发送 userId={} phone={} type={}", receiverId, maskPhone(phone), type);
        } catch (Exception e) {
            log.error("短信发送失败 receiverId={} type={}: {}", receiverId, type, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendReminderSmsAsync(Long receiverId, String content) {
        sendForMessageAsync(receiverId, StrUtil.blankToDefault(content, "随访督导提醒"), content, "visit_supervision_due");
    }

    @Override
    public String sendTestSms(String phone, String message) {
        SysSmsConfig cfg = smsConfigService.getRawConfig();
        if (!Integer.valueOf(1).equals(cfg.getEnabled())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先开启短信发送开关");
        }
        if (!isConfigReadyIgnoreEnabled(cfg)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先完整填写腾讯云短信配置");
        }
        String mobile = StrUtil.trim(phone);
        if (!isValidMobile(mobile)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写正确的手机号");
        }
        String summary = sanitizeTemplateParam(StrUtil.blankToDefault(StrUtil.trim(message), "短信配置测试"));
        if (StrUtil.isBlank(summary)) {
            summary = "短信配置测试";
        }
        try {
            doSend(cfg, mobile, truncate(summary, MAX_TEMPLATE_PARAM_LEN));
            return "测试短信已发送至 " + maskPhone(mobile);
        } catch (TencentCloudSDKException e) {
            log.error("测试短信失败: {}", e.getMessage(), e);
            throw new ServiceException(StatusEnum.SERVICE_ERROR, "腾讯云发送失败：" + e.getMessage());
        }
    }

    private void doSend(SysSmsConfig cfg, String phone, String templateParam) throws TencentCloudSDKException {
        String region = StrUtil.blankToDefault(cfg.getRegion(), "ap-guangzhou");
        Credential cred = new Credential(cfg.getSecretId(), cfg.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        SmsClient client = new SmsClient(cred, region, clientProfile);

        SendSmsRequest req = new SendSmsRequest();
        req.setSmsSdkAppId(cfg.getSdkAppId());
        req.setSignName(cfg.getSignName());
        req.setTemplateId(cfg.getTemplateId());
        req.setPhoneNumberSet(new String[]{"+86" + phone});
        req.setTemplateParamSet(new String[]{templateParam});

        SendSmsResponse resp = client.SendSms(req);
        SendStatus[] statuses = resp.getSendStatusSet();
        if (statuses == null || statuses.length == 0) {
            throw new TencentCloudSDKException("腾讯云无返回发送状态");
        }
        SendStatus status = statuses[0];
        if (status.getCode() == null || !"Ok".equalsIgnoreCase(status.getCode())) {
            throw new TencentCloudSDKException(
                    StrUtil.blankToDefault(status.getMessage(), status.getCode()));
        }
    }

    private boolean isConfigReady(SysSmsConfig cfg) {
        return cfg != null
                && Integer.valueOf(1).equals(cfg.getEnabled())
                && isConfigReadyIgnoreEnabled(cfg);
    }

    private boolean isConfigReadyIgnoreEnabled(SysSmsConfig cfg) {
        return cfg != null
                && StrUtil.isNotBlank(cfg.getSecretId())
                && StrUtil.isNotBlank(cfg.getSecretKey())
                && StrUtil.isNotBlank(cfg.getSdkAppId())
                && StrUtil.isNotBlank(cfg.getSignName())
                && StrUtil.isNotBlank(cfg.getTemplateId());
    }

    private static boolean isValidMobile(String phone) {
        return StrUtil.isNotBlank(phone) && phone.matches(PHONE_PATTERN);
    }

    private static String buildSummary(String title, String content) {
        // 优先用标题：变量越短越稳妥，且避免消息正文中的特殊符号导致腾讯云拒发
        String t = sanitizeTemplateParam(StrUtil.nullToEmpty(title));
        if (StrUtil.isNotBlank(t)) {
            return truncate(t, MAX_TEMPLATE_PARAM_LEN);
        }
        String c = sanitizeTemplateParam(StrUtil.nullToEmpty(content));
        if (StrUtil.isBlank(c)) {
            c = "您有新的系统消息请登录平台查看";
        }
        return truncate(c, MAX_TEMPLATE_PARAM_LEN);
    }

    private static String sanitizeTemplateParam(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replaceAll("\\s+", "")
                .replaceAll(FORBIDDEN_CHARS, "")
                .replaceAll("[\\r\\n\\t]", "");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
