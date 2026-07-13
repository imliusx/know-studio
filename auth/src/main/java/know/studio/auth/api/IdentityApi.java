package know.studio.auth.api;

public interface IdentityApi {

    CurrentIdentity currentUser();

    default CurrentIdentity requireSystemAdmin() {
        CurrentIdentity current = currentUser();
        if (current.systemRole() != SystemRole.ADMIN) {
            throw new know.studio.common.exception.ForbiddenException("需要系统管理员权限");
        }
        return current;
    }

    default TeamRole requireTeamRole(long teamId, TeamRole requiredRole) {
        throw new know.studio.common.exception.ForbiddenException("无权访问该团队");
    }

    default java.util.Map<Long, TeamRole> currentUserTeamRoles() {
        return java.util.Map.of();
    }

    default java.util.Set<Long> currentUserTeamIds() {
        return currentUserTeamRoles().keySet();
    }
}
