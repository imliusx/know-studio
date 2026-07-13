package know.studio.knowledge.domain;

import know.studio.knowledge.api.KnowledgeBasePermission;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KnowledgeBaseRepository {

    void insert(KnowledgeBase knowledgeBase);

    default boolean update(KnowledgeBase knowledgeBase) {
        return false;
    }

    default boolean deactivate(long knowledgeBaseId) {
        return false;
    }

    void insertTeamGrant(long grantId, long knowledgeBaseId, long teamId, KnowledgeBasePermission permission);

    default void saveTeamGrant(
            long grantId,
            long knowledgeBaseId,
            long teamId,
            KnowledgeBasePermission permission
    ) {
        insertTeamGrant(grantId, knowledgeBaseId, teamId, permission);
    }

    default boolean deleteTeamGrant(long knowledgeBaseId, long teamId) {
        return false;
    }

    default Map<Long, KnowledgeBasePermission> findTeamGrants(long knowledgeBaseId) {
        return Map.of();
    }

    Optional<KnowledgeBase> findById(long knowledgeBaseId);

    List<KnowledgeBase> findActiveByIds(Collection<Long> knowledgeBaseIds);

    List<KnowledgeBase> findAllActive();

    List<KnowledgeBase> findCompanyVisible();

    List<KnowledgeBase> findCreatedBy(long userId);

    Map<Long, KnowledgeBasePermission> findPermissions(Collection<Long> teamIds);

    Optional<KnowledgeBasePermission> findPermission(long knowledgeBaseId, Collection<Long> teamIds);
}
