package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.SysMessageReminderConfigMapper;
import cn.luyou.model.SysMessageReminderConfig;
import cn.luyou.service.SysMessageReminderConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMessageReminderConfigServiceImpl implements SysMessageReminderConfigService {

    private final SysMessageReminderConfigMapper configMapper;

    @Override
    public List<SysMessageReminderConfig> listAll() {
        return configMapper.selectList(new LambdaQueryWrapper<SysMessageReminderConfig>()
                .orderByAsc(SysMessageReminderConfig::getSort)
                .orderByAsc(SysMessageReminderConfig::getId));
    }

    @Override
    public boolean isEnabled(String code) {
        if (StrUtil.isBlank(code)) {
            return true;
        }
        SysMessageReminderConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysMessageReminderConfig>()
                .eq(SysMessageReminderConfig::getCode, code)
                .last("LIMIT 1"));
        if (config == null) {
            return true;
        }
        return !Integer.valueOf(0).equals(config.getEnabled());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(String code, boolean enabled) {
        if (StrUtil.isBlank(code)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "提醒编码不能为空");
        }
        SysMessageReminderConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SysMessageReminderConfig>()
                .eq(SysMessageReminderConfig::getCode, code)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "提醒配置不存在：" + code);
        }
        configMapper.update(null, new LambdaUpdateWrapper<SysMessageReminderConfig>()
                .eq(SysMessageReminderConfig::getCode, code)
                .set(SysMessageReminderConfig::getEnabled, enabled ? 1 : 0));
    }
}
