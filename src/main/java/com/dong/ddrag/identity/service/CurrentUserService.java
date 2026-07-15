package com.dong.ddrag.identity.service;

import cn.dev33.satoken.stp.StpUtil;
import com.dong.ddrag.common.enums.SystemRole;
import com.dong.ddrag.common.enums.UserStatus;
import com.dong.ddrag.common.exception.BusinessException;
import com.dong.ddrag.common.exception.ForbiddenException;
import com.dong.ddrag.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrentUserService {

    private final JdbcTemplate jdbcTemplate;

    public CurrentUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CurrentUser getRequiredCurrentUser(HttpServletRequest request) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new UnauthorizedException("当前请求未登录");
        }
        return loadUserById(Long.valueOf(loginId.toString()));
    }

    public CurrentUser requireSystemAdmin(HttpServletRequest request) {
        CurrentUser currentUser = getRequiredCurrentUser(request);
        if (currentUser.systemRole() != SystemRole.ADMIN) {
            throw new ForbiddenException("当前用户不是系统管理员");
        }
        return currentUser;
    }

    public CurrentUser requireBusinessUser(HttpServletRequest request) {
        CurrentUser currentUser = getRequiredCurrentUser(request);
        if (currentUser.systemRole() == SystemRole.ADMIN) {
            throw new ForbiddenException("系统管理员不能访问普通业务区");
        }
        return currentUser;
    }

    private CurrentUser loadUserById(Long userId) {
        List<CurrentUser> users = jdbcTemplate.query(
                """
                select id, user_code, display_name, system_role, status, must_change_password
                from users
                where id = ?
                """,
                (resultSet, rowNum) -> mapCurrentUser(resultSet.getLong("id"),
                        resultSet.getString("user_code"),
                        resultSet.getString("display_name"),
                        resultSet.getString("system_role"),
                        resultSet.getString("status"),
                        resultSet.getBoolean("must_change_password")),
                userId
        );
        if (users.isEmpty()) {
            throw new BusinessException("当前用户不存在");
        }
        return users.getFirst();
    }

    private CurrentUser mapCurrentUser(
            Long userId,
            String userCode,
            String displayName,
            String systemRole,
            String status,
            boolean mustChangePassword
    ) {
        if (UserStatus.DISABLED.name().equals(status)) {
            throw new BusinessException("账号已被禁用");
        }
        return new CurrentUser(
                userId,
                userCode,
                displayName,
                SystemRole.valueOf(systemRole),
                mustChangePassword
        );
    }

    public record CurrentUser(
            Long userId,
            String userCode,
            String displayName,
            SystemRole systemRole,
            boolean mustChangePassword
    ) {
        public CurrentUser(Long userId, String userCode, String displayName) {
            this(userId, userCode, displayName, SystemRole.USER, false);
        }
    }
}
