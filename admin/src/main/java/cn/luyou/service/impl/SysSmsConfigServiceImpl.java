package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.SysSmsConfigMapper;
import cn.luyou.model.SysSmsConfig;
import cn.luyou.model.vo.SmsConfigVO;
import cn.luyou.service.SysSmsConfigService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SysSmsConfigServiceImpl extends ServiceImpl<SysSmsConfigMapper, SysSmsConfig>
        implements SysSmsConfigService {

    private static final String MASK = "********";

    @Override
    public void assertSuperAdmin() {
        Integer role = BaseContext.getCurrentRole();
        if (role == null || role != 1) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "仅超级管理员可管理短信配置");
        }
    }

    @Override
    public SmsConfigVO getConfig() {
        SysSmsConfig cfg = loadOrCreate();
        boolean keyConfigured = StrUtil.isNotBlank(cfg.getSecretKey());
        return SmsConfigVO.builder()
                .id(cfg.getId())
                .enabled(Integer.valueOf(1).equals(cfg.getEnabled()))
                .secretId(cfg.getSecretId())
                .secretKeyMasked(keyConfigured ? MASK : "")
                .secretKeyConfigured(keyConfigured)
                .sdkAppId(cfg.getSdkAppId())
                .signName(cfg.getSignName())
                .templateId(cfg.getTemplateId())
                .region(StrUtil.blankToDefault(cfg.getRegion(), "ap-guangzhou"))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Map<String, Object> body) {
        SysSmsConfig cfg = loadOrCreate();
        if (body.containsKey("enabled")) {
            Object v = body.get("enabled");
            boolean on = v instanceof Boolean ? (Boolean) v : "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
            cfg.setEnabled(on ? 1 : 0);
        }
        if (body.containsKey("secretId")) {
            cfg.setSecretId(trimToNull(body.get("secretId")));
        }
        if (body.containsKey("secretKey")) {
            String key = trimToNull(body.get("secretKey"));
            // 空 / 脱敏占位 → 保留原密钥
            if (key != null && !MASK.equals(key) && !key.contains("*")) {
                cfg.setSecretKey(key);
            }
        }
        if (body.containsKey("sdkAppId")) {
            cfg.setSdkAppId(trimToNull(body.get("sdkAppId")));
        }
        if (body.containsKey("signName")) {
            cfg.setSignName(trimToNull(body.get("signName")));
        }
        if (body.containsKey("templateId")) {
            cfg.setTemplateId(trimToNull(body.get("templateId")));
        }
        if (body.containsKey("region")) {
            String region = trimToNull(body.get("region"));
            cfg.setRegion(region != null ? region : "ap-guangzhou");
        }
        updateById(cfg);
    }

    /** 供短信发送读取完整配置（含密钥） */
    @Override
    public SysSmsConfig getRawConfig() {
        return loadOrCreate();
    }

    private SysSmsConfig loadOrCreate() {
        SysSmsConfig cfg = getOne(new LambdaQueryWrapper<SysSmsConfig>().last("LIMIT 1"), false);
        if (cfg != null) {
            return cfg;
        }
        cfg = SysSmsConfig.builder()
                .enabled(0)
                .region("ap-guangzhou")
                .build();
        save(cfg);
        return cfg;
    }

    private static String trimToNull(Object val) {
        if (val == null) {
            return null;
        }
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }
}
