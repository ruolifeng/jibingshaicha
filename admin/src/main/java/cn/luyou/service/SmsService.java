package cn.luyou.service;

/**
 * 系统消息联动短信发送
 */
public interface SmsService {

    /**
     * 系统消息创建后异步发送短信（失败只记日志，不抛业务异常）
     */
    void sendForMessageAsync(Long receiverId, String title, String content, String type);

    /**
     * 随访/督导到期等场景：按用户联系电话直接发送短信（不依赖站内消息）。
     */
    void sendReminderSmsAsync(Long receiverId, String content);

    /**
     * 同步发送测试短信（供超管配置页验证）
     *
     * @return 成功说明或抛出业务异常
     */
    String sendTestSms(String phone, String message);
}
