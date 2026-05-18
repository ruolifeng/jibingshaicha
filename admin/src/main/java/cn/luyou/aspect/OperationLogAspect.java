package cn.luyou.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.annotation.OperationLog;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.luyou.service.OperationLogService;
import cn.luyou.utils.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面（V13）
 *
 * <p>切入所有标注 {@link OperationLog} 的方法，在方法执行前后捕获请求信息、当前登录用户、
 * 异常错误并持久化到 {@code operation_log} 表（异步保存）。
 *
 * <p>注意：登录接口因调用时 BaseContext 尚未注入，单独由 {@code UserController#login} 显式记录。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final UserMapper userMapper;

    /** 敏感字段（不写入请求参数日志） */
    private static final String[] SENSITIVE_KEYS = {"password", "pwd", "newPassword", "oldPassword"};

    @Around("@annotation(cn.luyou.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        cn.luyou.model.OperationLog logEntity = new cn.luyou.model.OperationLog();
        Throwable thrown = null;
        Object result;
        try {
            result = pjp.proceed();
            logEntity.setResultStatus(1);
            return result;
        } catch (Throwable e) {
            thrown = e;
            logEntity.setResultStatus(0);
            logEntity.setErrorMessage(truncate(e.getMessage(), 2048));
            throw e;
        } finally {
            try {
                long cost = System.currentTimeMillis() - start;
                fillFromAnnotation(pjp, logEntity);
                fillFromRequest(logEntity);
                fillFromContext(logEntity);
                if (logEntity.getResultStatus() == null) {
                    logEntity.setResultStatus(thrown == null ? 1 : 0);
                }
                logEntity.setCostMs(cost);
                if (Boolean.TRUE.equals(shouldSaveParams(pjp))) {
                    logEntity.setRequestParams(serializeParams(pjp));
                }
                operationLogService.saveAsync(logEntity);
            } catch (Exception ex) {
                log.warn("record operation log failed: {}", ex.getMessage());
            }
        }
    }

    private void fillFromAnnotation(ProceedingJoinPoint pjp, cn.luyou.model.OperationLog logEntity) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        OperationLog ann = method.getAnnotation(OperationLog.class);
        if (ann == null) return;
        logEntity.setOpType(ann.type());
        logEntity.setOpModule(StrUtil.blankToDefault(ann.module(), null));
        logEntity.setOpAction(StrUtil.blankToDefault(ann.action(), null));
    }

    private Boolean shouldSaveParams(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        OperationLog ann = method.getAnnotation(OperationLog.class);
        return ann == null || ann.saveParams();
    }

    private void fillFromRequest(cn.luyou.model.OperationLog logEntity) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest req = attrs.getRequest();
        logEntity.setRequestMethod(req.getMethod());
        logEntity.setRequestUrl(truncate(req.getRequestURI(), 256));
        logEntity.setIp(getClientIp(req));
        logEntity.setUserAgent(truncate(req.getHeader("User-Agent"), 256));
    }

    private void fillFromContext(cn.luyou.model.OperationLog logEntity) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) return;
        logEntity.setUserId(userId);
        logEntity.setRole(BaseContext.getCurrentRole());
        logEntity.setDepartmentId(BaseContext.getCurrentDepartmentId());
        try {
            User user = userMapper.selectById(userId);
            if (user != null) {
                logEntity.setUserName(user.getUsername());
                logEntity.setRealName(user.getRealName());
            }
        } catch (Exception ignore) { /* 不阻塞主流程 */ }
    }

    /** 把请求参数序列化为 JSON，过滤敏感字段、过滤无法序列化的对象 */
    private String serializeParams(ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String[] names = sig.getParameterNames();
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object v = args[i];
                if (v == null) continue;
                if (v instanceof MultipartFile mf) {
                    map.put(names != null && i < names.length ? names[i] : ("arg" + i),
                            "<file:" + mf.getOriginalFilename() + ">");
                    continue;
                }
                if (v instanceof jakarta.servlet.http.HttpServletRequest
                        || v instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                String key = names != null && i < names.length ? names[i] : ("arg" + i);
                if (isSensitiveKey(key)) {
                    map.put(key, "***");
                    continue;
                }
                map.put(key, v);
            }
            String json = JSONUtil.toJsonStr(map);
            return truncate(json, 8192);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        for (String s : SENSITIVE_KEYS) {
            if (s.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return truncate(ip, 64);
    }

    /** 截取字符串至指定长度（避免依赖 hutool 具体版本的 maxLength 方法） */
    private static String truncate(String src, int maxLen) {
        if (src == null) return null;
        return src.length() <= maxLen ? src : src.substring(0, maxLen);
    }
}
