package know.studio.knowledge.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import know.studio.knowledge.api.KnowledgeBasePermission;
import know.studio.knowledge.api.KnowledgeBaseVisibility;
import know.studio.knowledge.domain.KnowledgeBase;
import know.studio.knowledge.domain.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MybatisKnowledgeBaseRepository implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseTeamGrantMapper grantMapper;

    @Override
    public void insert(KnowledgeBase knowledgeBase) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(knowledgeBase.id());
        entity.setName(knowledgeBase.name());
        entity.setDescription(knowledgeBase.description());
        entity.setVisibility(knowledgeBase.visibility().name());
        entity.setOwnerTeamId(knowledgeBase.ownerTeamId());
        entity.setCreatedBy(knowledgeBase.createdBy());
        entity.setStatus(knowledgeBase.status());
        knowledgeBaseMapper.insert(entity);
    }

    @Override
    public boolean update(KnowledgeBase knowledgeBase) {
        return knowledgeBaseMapper.update(Wrappers.<KnowledgeBaseEntity>lambdaUpdate()
                .eq(KnowledgeBaseEntity::getId, knowledgeBase.id())
                .eq(KnowledgeBaseEntity::getStatus, "ACTIVE")
                .set(KnowledgeBaseEntity::getName, knowledgeBase.name())
                .set(KnowledgeBaseEntity::getDescription, knowledgeBase.description())
                .set(KnowledgeBaseEntity::getVisibility, knowledgeBase.visibility().name())
                .set(KnowledgeBaseEntity::getOwnerTeamId, knowledgeBase.ownerTeamId())) == 1;
    }

    @Override
    public boolean deactivate(long knowledgeBaseId) {
        return knowledgeBaseMapper.update(Wrappers.<KnowledgeBaseEntity>lambdaUpdate()
                .eq(KnowledgeBaseEntity::getId, knowledgeBaseId)
                .eq(KnowledgeBaseEntity::getStatus, "ACTIVE")
                .set(KnowledgeBaseEntity::getStatus, "ARCHIVED")) == 1;
    }

    @Override
    public void insertTeamGrant(
            long grantId,
            long knowledgeBaseId,
            long teamId,
            KnowledgeBasePermission permission
    ) {
        KnowledgeBaseTeamGrantEntity entity = new KnowledgeBaseTeamGrantEntity();
        entity.setId(grantId);
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setTeamId(teamId);
        entity.setPermission(permission.name());
        grantMapper.insert(entity);
    }

    @Override
    public void saveTeamGrant(
            long grantId,
            long knowledgeBaseId,
            long teamId,
            KnowledgeBasePermission permission
    ) {
        KnowledgeBaseTeamGrantEntity existing = grantMapper.selectOne(
                Wrappers.<KnowledgeBaseTeamGrantEntity>lambdaQuery()
                        .eq(KnowledgeBaseTeamGrantEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeBaseTeamGrantEntity::getTeamId, teamId)
        );
        if (existing == null) {
            insertTeamGrant(grantId, knowledgeBaseId, teamId, permission);
            return;
        }
        grantMapper.update(Wrappers.<KnowledgeBaseTeamGrantEntity>lambdaUpdate()
                .eq(KnowledgeBaseTeamGrantEntity::getId, existing.getId())
                .set(KnowledgeBaseTeamGrantEntity::getPermission, permission.name()));
    }

    @Override
    public boolean deleteTeamGrant(long knowledgeBaseId, long teamId) {
        return grantMapper.delete(Wrappers.<KnowledgeBaseTeamGrantEntity>lambdaQuery()
                .eq(KnowledgeBaseTeamGrantEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeBaseTeamGrantEntity::getTeamId, teamId)) == 1;
    }

    @Override
    public Map<Long, KnowledgeBasePermission> findTeamGrants(long knowledgeBaseId) {
        return grantMapper.selectList(Wrappers.<KnowledgeBaseTeamGrantEntity>lambdaQuery()
                        .eq(KnowledgeBaseTeamGrantEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(KnowledgeBaseTeamGrantEntity::getTeamId))
                .stream()
                .collect(Collectors.toMap(
                        KnowledgeBaseTeamGrantEntity::getTeamId,
                        entity -> KnowledgeBasePermission.valueOf(entity.getPermission())
                ));
    }

    @Override
    public Optional<KnowledgeBase> findById(long knowledgeBaseId) {
        return Optional.ofNullable(knowledgeBaseMapper.selectOne(
                Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .eq(KnowledgeBaseEntity::getId, knowledgeBaseId)
                        .eq(KnowledgeBaseEntity::getStatus, "ACTIVE")
        )).map(MybatisKnowledgeBaseRepository::toDomain);
    }

    @Override
    public List<KnowledgeBase> findActiveByIds(Collection<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds.isEmpty()) {
            return List.of();
        }
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .in(KnowledgeBaseEntity::getId, knowledgeBaseIds)
                        .eq(KnowledgeBaseEntity::getStatus, "ACTIVE")
                        .orderByAsc(KnowledgeBaseEntity::getName))
                .stream().map(MybatisKnowledgeBaseRepository::toDomain).toList();
    }

    @Override
    public List<KnowledgeBase> findAllActive() {
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .eq(KnowledgeBaseEntity::getStatus, "ACTIVE")
                        .orderByAsc(KnowledgeBaseEntity::getName))
                .stream().map(MybatisKnowledgeBaseRepository::toDomain).toList();
    }

    @Override
    public List<KnowledgeBase> findCompanyVisible() {
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .eq(KnowledgeBaseEntity::getVisibility, KnowledgeBaseVisibility.COMPANY.name())
                        .eq(KnowledgeBaseEntity::getStatus, "ACTIVE"))
                .stream().map(MybatisKnowledgeBaseRepository::toDomain).toList();
    }

    @Override
    public List<KnowledgeBase> findCreatedBy(long userId) {
        return knowledgeBaseMapper.selectList(Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .eq(KnowledgeBaseEntity::getCreatedBy, userId)
                        .eq(KnowledgeBaseEntity::getStatus, "ACTIVE"))
                .stream().map(MybatisKnowledgeBaseRepository::toDomain).toList();
    }

    @Override
    public Map<Long, KnowledgeBasePermission> findPermissions(Collection<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return grantMapper.selectList(Wrappers.<KnowledgeBaseTeamGrantEntity>lambdaQuery()
                        .in(KnowledgeBaseTeamGrantEntity::getTeamId, teamIds))
                .stream()
                .collect(Collectors.toMap(
                        KnowledgeBaseTeamGrantEntity::getKnowledgeBaseId,
                        entity -> KnowledgeBasePermission.valueOf(entity.getPermission()),
                        MybatisKnowledgeBaseRepository::stronger
                ));
    }

    @Override
    public Optional<KnowledgeBasePermission> findPermission(
            long knowledgeBaseId,
            Collection<Long> teamIds
    ) {
        return Optional.ofNullable(findPermissions(teamIds).get(knowledgeBaseId));
    }

    private static KnowledgeBasePermission stronger(
            KnowledgeBasePermission left,
            KnowledgeBasePermission right
    ) {
        return left.allows(right) ? left : right;
    }

    private static KnowledgeBase toDomain(KnowledgeBaseEntity entity) {
        return new KnowledgeBase(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                KnowledgeBaseVisibility.valueOf(entity.getVisibility()),
                entity.getOwnerTeamId(),
                entity.getCreatedBy(),
                entity.getStatus()
        );
    }
}
