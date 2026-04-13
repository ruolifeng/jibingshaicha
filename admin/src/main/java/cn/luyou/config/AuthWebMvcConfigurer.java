package cn.luyou.config;

import cn.luyou.security.AuthHandlerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 全局配置：注册鉴权拦截器
 *
 * @author ruolifeng
 */
@Configuration
@RequiredArgsConstructor
public class AuthWebMvcConfigurer implements WebMvcConfigurer {

    private final AuthHandlerInterceptor authHandlerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authHandlerInterceptor)
                .addPathPatterns("/**")
                // 放行登录接口和 Swagger/Knife4j 文档路径
                .excludePathPatterns(
                        "/user/login",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
    }
}
