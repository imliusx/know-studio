package know.studio.arag.identity.rest;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import know.studio.arag.platform.core.context.UserContext;
import know.studio.arag.platform.core.trace.TraceContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = TraceContext.startIfAbsent();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        UserContext.set(new UserContext.Principal(userId, traceId));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        UserContext.clear();
        TraceContext.clear();
    }
}
