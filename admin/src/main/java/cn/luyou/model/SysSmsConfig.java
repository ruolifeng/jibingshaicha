package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统短信配置（单行，腾讯云）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sms_config")
public class SysSmsConfig extends BaseEntity {

    /** 是否开启：0否 1是 */
    private Integer enabled;

    private String secretId;

    private String secretKey;

    /** 短信应用 SdkAppId */
    private String sdkAppId;

    /** 短信签名 */
    private String signName;

    /** 模板 ID（单变量） */
    private String templateId;

    /** 地域，默认 ap-guangzhou */
    private String region;
}
