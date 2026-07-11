package know.studio.arag.identity.rest;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import know.studio.arag.platform.core.context.UserContext;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import know.studio.arag.platform.core.trace.TraceContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String WORKSPACE_HEADER = "X-Workspace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = TraceContext.startIfAbsent();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        Long workspaceId = parseWorkspaceId(request.getHeader(WORKSPACE_HEADER));
        UserContext.set(new UserContext.Principal(userId, workspaceId, traceId));
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

    private static Long parseWorkspaceId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, WORKSPACE_HEADER + " 必须是整数");
        }
    }
}
