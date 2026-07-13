package know.studio.knowledge.domain;

import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.auth.api.SystemRole;
import know.studio.auth.api.TeamRole;
import know.studio.knowledge.api.KnowledgeAccessApi;
import know.studio.knowledge.api.KnowledgeBaseInfo;
import know.studio.knowledge.api.KnowledgeBasePermission;
import know.studio.knowledge.api.KnowledgeBaseTeamGrantInfo;
import know.studio.knowledge.api.KnowledgeBaseVisibility;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import know.studio.common.exception.ForbiddenException;
import know.studio.common.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService implements KnowledgeAccessApi {

    private final KnowledgeBaseRepository repository;
    private final IdentityApi identityApi;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public long create(
            String name,
            String description,
            KnowledgeBaseVisibility visibility,
            Long ownerTeamId
    ) {
        CurrentIdentity current = identityApi.currentUser();
        if (ownerTeamId != null) {
            identityApi.requireTeamRole(ownerTeamId, TeamRole.TEAM_ADMIN);
        } else if (current.systemRole() != SystemRole.ADMIN) {
            throw new ForbiddenException("只有系统管理员可以创建无归属团队的知识库");
        }
        if (visibility == KnowledgeBaseVisibility.TEAM && ownerTeamId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队知识库必须指定归属团队");
        }

        long knowledgeBaseId = idGenerator.nextId();
        repository.insert(new KnowledgeBase(
                knowledgeBaseId,
                name.trim(),
                description == null ? null : description.trim(),
                visibility,
                ownerTeamId,
                current.userId(),
                "ACTIVE"
        ));
        if (ownerTeamId != null) {
            repository.insertTeamGrant(
                    idGenerator.nextId(),
                    knowledgeBaseId,
                    ownerTeamId,
                    KnowledgeBasePermission.MANAGE
            );
        }
        return knowledgeBaseId;
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseInfo get(long knowledgeBaseId) {
        KnowledgeBasePermission permission = requireReadable(knowledgeBaseId);
        KnowledgeBase knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        return toInfo(knowledgeBase, permission);
    }

    @Transactional
    public void update(
            long knowledgeBaseId,
            String name,
            String description,
            KnowledgeBaseVisibility visibility,
            Long ownerTeamId
    ) {
        requireManageable(knowledgeBaseId);
        KnowledgeBase current = requireKnowledgeBase(knowledgeBaseId);
        validateOwnership(visibility, ownerTeamId);
        if (ownerTeamId != null && !ownerTeamId.equals(current.ownerTeamId())) {
            identityApi.requireTeamRole(ownerTeamId, TeamRole.TEAM_ADMIN);
        }
        KnowledgeBase updated = new KnowledgeBase(
                knowledgeBaseId,
                name.trim(),
                normalize(description),
                visibility,
                ownerTeamId,
                current.createdBy(),
                current.status()
        );
        if (!repository.update(updated)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
        if (ownerTeamId != null) {
            repository.saveTeamGrant(
                    idGenerator.nextId(),
                    knowledgeBaseId,
                    ownerTeamId,
                    KnowledgeBasePermission.MANAGE
            );
        }
    }

    @Transactional
    public void delete(long knowledgeBaseId) {
        requireManageable(knowledgeBaseId);
        if (!repository.deactivate(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在");
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseTeamGrantInfo> listTeamGrants(long knowledgeBaseId) {
        requireManageable(knowledgeBaseId);
        return repository.findTeamGrants(knowledgeBaseId).entrySet().stream()
                .map(entry -> new KnowledgeBaseTeamGrantInfo(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public void saveTeamGrant(long knowledgeBaseId, long teamId, KnowledgeBasePermission permission) {
        requireManageable(knowledgeBaseId);
        KnowledgeBase knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        if (knowledgeBase.ownerTeamId() != null
                && knowledgeBase.ownerTeamId() == teamId
                && permission != KnowledgeBasePermission.MANAGE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "归属团队必须保留 MANAGE 权限");
        }
        repository.saveTeamGrant(idGenerator.nextId(), knowledgeBaseId, teamId, permission);
    }

    @Transactional
    public void deleteTeamGrant(long knowledgeBaseId, long teamId) {
        requireManageable(knowledgeBaseId);
        KnowledgeBase knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        if (knowledgeBase.ownerTeamId() != null && knowledgeBase.ownerTeamId() == teamId) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除归属团队授权");
        }
        if (!repository.deleteTeamGrant(knowledgeBaseId, teamId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库团队授权不存在");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<KnowledgeBaseInfo> listReadable() {
        CurrentIdentity current = identityApi.currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return repository.findAllActive().stream()
                    .map(kb -> toInfo(kb, KnowledgeBasePermission.MANAGE))
                    .toList();
        }

        Map<Long, KnowledgeBasePermission> permissions = effectivePermissions(current);
        return repository.findActiveByIds(permissions.keySet()).stream()
                .map(kb -> toInfo(kb, permissions.get(kb.id())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> readableKnowledgeBaseIds() {
        CurrentIdentity current = identityApi.currentUser();
        if (current.systemRole() == SystemRole.ADMIN) {
            return repository.findAllActive().stream()
                    .map(KnowledgeBase::id)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return Set.copyOf(effectivePermissions(current).keySet());
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBasePermission requireReadable(long knowledgeBaseId) {
        return requirePermission(knowledgeBaseId, KnowledgeBasePermission.READ);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBasePermission requireManageable(long knowledgeBaseId) {
        return requirePermission(knowledgeBaseId, KnowledgeBasePermission.MANAGE);
    }

    private KnowledgeBasePermission requirePermission(
            long knowledgeBaseId,
            KnowledgeBasePermission required
    ) {
        CurrentIdentity current = identityApi.currentUser();
        KnowledgeBase knowledgeBase = repository.findById(knowledgeBaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (current.systemRole() == SystemRole.ADMIN || knowledgeBase.createdBy() == current.userId()) {
            return KnowledgeBasePermission.MANAGE;
        }
        if (required == KnowledgeBasePermission.READ
                && knowledgeBase.visibility() == KnowledgeBaseVisibility.COMPANY) {
            return KnowledgeBasePermission.READ;
        }
        KnowledgeBasePermission actual = teamPermission(knowledgeBaseId);
        if (!actual.allows(required)) {
            throw new ForbiddenException("知识库权限不足");
        }
        return actual;
    }

    private KnowledgeBase requireKnowledgeBase(long knowledgeBaseId) {
        return repository.findById(knowledgeBaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
    }

    private static void validateOwnership(KnowledgeBaseVisibility visibility, Long ownerTeamId) {
        if (visibility == KnowledgeBaseVisibility.TEAM && ownerTeamId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队知识库必须指定归属团队");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<Long, KnowledgeBasePermission> effectivePermissions(CurrentIdentity current) {
        Map<Long, TeamRole> teamRoles = identityApi.currentUserTeamRoles();
        Map<Long, KnowledgeBasePermission> permissions = new HashMap<>();
        repository.findPermissions(teamRoles.keySet()).keySet().forEach(
                knowledgeBaseId -> permissions.put(knowledgeBaseId, KnowledgeBasePermission.READ)
        );
        repository.findPermissions(teamAdminIds(teamRoles)).forEach(
                (knowledgeBaseId, permission) -> permissions.merge(
                        knowledgeBaseId,
                        permission,
                        KnowledgeBaseService::strongerPermission
                )
        );
        repository.findCompanyVisible().forEach(kb -> permissions.putIfAbsent(
                kb.id(), KnowledgeBasePermission.READ
        ));
        repository.findCreatedBy(current.userId()).forEach(kb -> permissions.put(
                kb.id(), KnowledgeBasePermission.MANAGE
        ));
        return permissions;
    }

    private KnowledgeBasePermission teamPermission(long knowledgeBaseId) {
        Map<Long, TeamRole> teamRoles = identityApi.currentUserTeamRoles();
        KnowledgeBasePermission readable = repository.findPermission(knowledgeBaseId, teamRoles.keySet())
                .map(permission -> KnowledgeBasePermission.READ)
                .orElseThrow(() -> new ForbiddenException("无权访问该知识库"));
        return repository.findPermission(knowledgeBaseId, teamAdminIds(teamRoles))
                .orElse(readable);
    }

    private static Set<Long> teamAdminIds(Map<Long, TeamRole> teamRoles) {
        return teamRoles.entrySet().stream()
                .filter(entry -> entry.getValue() == TeamRole.TEAM_ADMIN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static KnowledgeBasePermission strongerPermission(
            KnowledgeBasePermission first,
            KnowledgeBasePermission second
    ) {
        return second.allows(first) ? second : first;
    }

    private static KnowledgeBaseInfo toInfo(
            KnowledgeBase knowledgeBase,
            KnowledgeBasePermission permission
    ) {
        return new KnowledgeBaseInfo(
                knowledgeBase.id(),
                knowledgeBase.name(),
                knowledgeBase.description(),
                knowledgeBase.visibility(),
                knowledgeBase.ownerTeamId(),
                permission
        );
    }
}
