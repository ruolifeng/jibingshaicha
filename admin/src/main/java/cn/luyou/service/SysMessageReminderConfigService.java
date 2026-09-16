package cn.luyou.service;

import cn.luyou.model.SysMessageReminderConfig;

import java.util.List;

public interface SysMessageReminderConfigService {

    /** 全部提醒配置（按 sort） */
    List<SysMessageReminderConfig> listAll();

    /** 是否开启；配置不存在时默认开启，避免漏提醒 */
    boolean isEnabled(String code);

    /** 更新开关 */
    void updateEnabled(String code, boolean enabled);
}
