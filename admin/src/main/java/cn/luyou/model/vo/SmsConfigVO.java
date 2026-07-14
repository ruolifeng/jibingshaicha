package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 短信配置回显（SecretKey 脱敏） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsConfigVO {
    private Long id;
    /** 是否开启 */
    private Boolean enabled;
    private String secretId;
    /** 脱敏后的 SecretKey；未配置时为空 */
    private String secretKeyMasked;
    /** 是否已保存过 SecretKey */
    private Boolean secretKeyConfigured;
    private String sdkAppId;
    private String signName;
    private String templateId;
    private String region;
}
