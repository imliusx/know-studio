package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.WorkspaceInfo;
import know.studio.arag.identity.api.WorkspaceMemberInfo;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.identity.api.TeamInfo;
import know.studio.arag.identity.api.TeamMemberInfo;
import know.studio.arag.identity.api.TeamRole;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import know.studio.arag.platform.core.exception.ForbiddenException;
import know.studio.arag.platform.core.exception.UnauthorizedException;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentityService implements IdentityApi {

    private final IdentityRepository repository;
    private final LoginSession loginSession;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public AuthSession register(String email, String displayName, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (repository.findUserByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已注册");
        }

        UserAccount user = new UserAccount(
                idGenerator.nextId(),
                normalizedEmail,
                displayName.trim(),
                passwordEncoder.encode(password),
                SystemRole.USER,
                UserStatus.ACTIVE
        );
        try {
            repository.insertUser(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已注册");
        }
        loginSession.login(user.id());
        return authSession(user);
    }

    @Transactional
    public AuthSession login(String email, String password) {
        UserAccount user = repository.findUserByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UnauthorizedException("邮箱或密码错误"));
        if (user.status() != UserStatus.ACTIVE || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new UnauthorizedException("邮箱或密码错误");
        }
        loginSession.login(user.id());
        repository.updateLastLogin(user.id());
        return authSession(user);
    }

    public void logout() {
        if (loginSession.isLoggedIn()) {
            loginSession.logout();
        }
    }

    @Transactional
    public long createWorkspace(String name, String description) {
        CurrentIdentity current = currentUser();
        long workspaceId = idGenerator.nextId();
        repository.insertWorkspace(new Workspace(
                workspaceId,
                name.trim(),
                description == null ? null : description.trim(),
                current.userId(),
                WorkspaceStatus.ACTIVE
        ));
        repository.insertMember(idGenerator.nextId(), workspaceId, current.userId(), WorkspaceRole.OWNER);
        return workspaceId;
    }

    @Transactional
    public void addMember(long workspaceId, String email, WorkspaceRole role) {
        requireRole(workspaceId, WorkspaceRole.ADMIN);
        if (role == WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能通过成员接口转移所有者");
        }
        UserAccount user = repository.findUserByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        try {
            repository.insertMember(idGenerator.nextId(), workspaceId, user.id(), role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已在工作空间中");
        }
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInfo> listWorkspaces() {
        CurrentIdentity current = currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return repository.findActiveWorkspaces().stream()
                    .map(workspace -> toInfo(workspace, WorkspaceRole.OWNER))
                    .toList();
        }
        return repository.findWorkspaceAccesses(current.userId()).stream()
                .map(access -> toInfo(access.workspace(), access.role()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberInfo> listMembers(long workspaceId) {
        requireRole(workspaceId, WorkspaceRole.ADMIN);
        return repository.findWorkspaceMembers(workspaceId).stream()
                .map(member -> new WorkspaceMemberInfo(
                        member.user().id(),
                        member.user().email(),
                        member.user().displayName(),
                        member.role()
                ))
                .toList();
    }

    @Transactional
    public long createTeam(String name, String description, Long parentId) {
        CurrentIdentity current = requireSystemAdmin();
        long teamId = idGenerator.nextId();
        repository.insertTeam(new Team(
                teamId,
                name.trim(),
                description == null ? null : description.trim(),
                parentId,
                current.userId(),
                WorkspaceStatus.ACTIVE
        ));
        repository.insertTeamMember(idGenerator.nextId(), teamId, current.userId(), TeamRole.TEAM_ADMIN);
        return teamId;
    }

    @Transactional
    public void addTeamMember(long teamId, String email, TeamRole role) {
        requireTeamRole(teamId, TeamRole.TEAM_ADMIN);
        UserAccount user = repository.findUserByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        try {
            repository.insertTeamMember(idGenerator.nextId(), teamId, user.id(), role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已在团队中");
        }
    }

    @Transactional(readOnly = true)
    public List<TeamInfo> listTeams() {
        CurrentIdentity current = currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return repository.findActiveTeams().stream()
                    .map(team -> toInfo(team, TeamRole.TEAM_ADMIN))
                    .toList();
        }
        return repository.findTeamAccesses(current.userId()).stream()
                .map(access -> toInfo(access.team(), access.role()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberInfo> listTeamMembers(long teamId) {
        requireTeamRole(teamId, TeamRole.TEAM_ADMIN);
        return repository.findTeamMembers(teamId).stream()
                .map(member -> new TeamMemberInfo(
                        member.user().id(),
                        member.user().email(),
                        member.user().displayName(),
                        member.role()
                ))
                .toList();
    }

    @Override
    public CurrentIdentity currentUser() {
        if (!loginSession.isLoggedIn()) {
            throw new UnauthorizedException();
        }
        UserAccount user = repository.findUserById(loginSession.currentUserId())
                .orElseThrow(UnauthorizedException::new);
        if (user.status() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("账户已停用");
        }
        return toCurrentIdentity(user);
    }

    @Override
    public WorkspaceRole requireWorkspaceReadable(long workspaceId) {
        return requireRole(workspaceId, WorkspaceRole.MEMBER);
    }

    @Override
    public WorkspaceRole requireRole(long workspaceId, WorkspaceRole requiredRole) {
        CurrentIdentity current = currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return WorkspaceRole.OWNER;
        }
        WorkspaceRole actualRole = repository.findWorkspaceRole(workspaceId, current.userId())
                .orElseThrow(() -> new ForbiddenException("无权访问该工作空间"));
        if (!actualRole.allows(requiredRole)) {
            throw new ForbiddenException("工作空间角色权限不足");
        }
        return actualRole;
    }

    @Override
    public CurrentIdentity requireSystemAdmin() {
        CurrentIdentity current = currentUser();
        if (current.systemRole() != SystemRole.ADMIN) {
            throw new ForbiddenException("需要系统管理员权限");
        }
        return current;
    }

    @Override
    public TeamRole requireTeamRole(long teamId, TeamRole requiredRole) {
        CurrentIdentity current = currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return TeamRole.TEAM_ADMIN;
        }
        TeamRole actualRole = repository.findTeamRole(teamId, current.userId())
                .orElseThrow(() -> new ForbiddenException("无权访问该团队"));
        if (!actualRole.allows(requiredRole)) {
            throw new ForbiddenException("团队角色权限不足");
        }
        return actualRole;
    }

    @Override
    public Set<Long> currentUserTeamIds() {
        CurrentIdentity current = currentUser();
        return repository.findTeamAccesses(current.userId()).stream()
                .map(access -> access.team().id())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private AuthSession authSession(UserAccount user) {
        return new AuthSession(toCurrentIdentity(user), loginSession.tokenName(), loginSession.tokenValue());
    }

    private static CurrentIdentity toCurrentIdentity(UserAccount user) {
        return new CurrentIdentity(user.id(), user.email(), user.displayName(), user.systemRole());
    }

    private static WorkspaceInfo toInfo(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceInfo(
                workspace.id(),
                workspace.name(),
                workspace.description(),
                workspace.ownerId(),
                role
        );
    }

    private static TeamInfo toInfo(Team team, TeamRole role) {
        return new TeamInfo(team.id(), team.name(), team.description(), role);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
