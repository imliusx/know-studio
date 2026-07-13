package know.studio.auth.rest;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import know.studio.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class IdentityWebConfiguration implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    try {
                        StpUtil.checkLogin();
                    } catch (NotLoginException exception) {
                        throw new UnauthorizedException();
                    }
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register")
                .order(0);
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")
                .order(10);
    }
}
