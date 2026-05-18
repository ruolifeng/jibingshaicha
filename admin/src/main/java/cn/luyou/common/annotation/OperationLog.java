package cn.luyou.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（V13）
 *
 * <p>在 Controller 方法上添加此注解后，AOP 会自动记录操作日志到 {@code operation_log} 表。
 *
 * <pre>
 * 示例：
 *
 *   {@literal @}OperationLog(type = "delete", module = "patient", action = "删除患者")
 *   public ResultResponse&lt;Void&gt; deletePatient(@PathVariable Long id) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OperationLog {

    /**
     * 操作类型（与 init.sql / 文档约束一致）
     * <ul>
     *   <li>login   登录</li>
     *   <li>import  导入</li>
     *   <li>delete  删除</li>
     *   <li>update  修改</li>
     *   <li>export  导出</li>
     *   <li>create  新增（扩展位）</li>
     *   <li>logout  登出（扩展位）</li>
     * </ul>
     */
    String type();

    /** 业务模块，例如：screening / latent / patient / referral / system */
    String module() default "";

    /** 中文动作描述，例如："删除患者" */
    String action() default "";

    /** 是否记录请求参数（默认记录） */
    boolean saveParams() default true;
}
