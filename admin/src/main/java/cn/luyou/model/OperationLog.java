package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 操作日志实体（V13）
 *
 * <p>记录系统关键操作行为：登录 / 导入 / 删除 / 修改 / 导出（共 5 类，按用户要求）。
 * 同时保留扩展位 create / logout 以备后续启用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String userName;

    /** 操作人真实姓名 */
    private String realName;

    /** 所属部门ID */
    private Long departmentId;

    /** 角色：1-6 */
    private Integer role;

    /** 操作类型：login / import / delete / update / export（+ create / logout 扩展位） */
    private String opType;

    /** 业务模块：screening / latent / patient / referral / system / ... */
    private String opModule;

    /** 动作描述（中文，自由文本） */
    private String opAction;

    /** 关联业务ID */
    private Long bizId;

    /** 关联业务类型 */
    private String bizType;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数（JSON 字符串，敏感字段已脱敏） */
    private String requestParams;

    /** 客户端IP */
    private String ip;

    /** 客户端 UA */
    private String userAgent;

    /** 1成功 0失败 */
    private Integer resultStatus;

    /** 失败错误信息 */
    private String errorMessage;

    /** 耗时（毫秒） */
    private Long costMs;
}
