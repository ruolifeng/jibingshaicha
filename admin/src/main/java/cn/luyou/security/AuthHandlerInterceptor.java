package cn.luyou.security;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

/**
 * JWT 认证拦截器
 *
 * @author ruolifeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandlerInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    /**
     * 登录接口无需 Token（与 {@link cn.luyou.config.AuthWebMvcConfigurer} 排除规则一致）。
     * <p>
     * 配置了 {@code server.servlet.context-path=/api/v1} 时，此处匹配到的路径为应用内路径 {@code /user/login}。
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = urlPathHelper.getPathWithinApplication(request);
        return "/user/login".equals(path);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isLoginRequest(request)) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED);
        }
        // Bearer 前缀处理
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.verifyToken(token)) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        BaseContext.setCurrentId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        BaseContext.remove();
    }
}
