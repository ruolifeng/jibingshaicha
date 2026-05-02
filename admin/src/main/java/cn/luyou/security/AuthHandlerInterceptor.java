package cn.luyou.security;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
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
 * JWT 认证拦截器，鉴权通过后将 userId、role、departmentId 写入 BaseContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandlerInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

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
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.verifyToken(token)) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED);
        }
        BaseContext.setCurrentId(userId);
        BaseContext.setCurrentRole(user.getRole());
        BaseContext.setCurrentDepartmentId(user.getDepartmentId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.remove();
    }
}
