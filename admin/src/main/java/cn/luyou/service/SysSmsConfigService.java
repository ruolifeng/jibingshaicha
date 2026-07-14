package cn.luyou.service;

import cn.luyou.model.SysSmsConfig;
import cn.luyou.model.vo.SmsConfigVO;

import java.util.Map;

public interface SysSmsConfigService {

    /** 获取当前配置（SecretKey 脱敏） */
    SmsConfigVO getConfig();

    /** 保存配置；secretKey 为空表示沿用旧值 */
    void saveConfig(Map<String, Object> body);

    /** 读取完整配置（含密钥，仅服务端内部使用） */
    SysSmsConfig getRawConfig();

    /** 仅超级管理员可操作 */
    void assertSuperAdmin();
}
