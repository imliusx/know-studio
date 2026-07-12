package know.studio.arag.knowledge.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;
import know.studio.arag.knowledge.domain.KnowledgeBase;
import know.studio.arag.knowledge.domain.KnowledgeBaseRepository;
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
