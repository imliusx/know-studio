package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.KnowledgeBasePermission;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KnowledgeBaseRepository {

    void insert(KnowledgeBase knowledgeBase);

    void insertTeamGrant(long grantId, long knowledgeBaseId, long teamId, KnowledgeBasePermission permission);

    Optional<KnowledgeBase> findById(long knowledgeBaseId);

    List<KnowledgeBase> findActiveByIds(Collection<Long> knowledgeBaseIds);

    List<KnowledgeBase> findAllActive();

    List<KnowledgeBase> findCompanyVisible();

    List<KnowledgeBase> findCreatedBy(long userId);

    Map<Long, KnowledgeBasePermission> findPermissions(Collection<Long> teamIds);

    Optional<KnowledgeBasePermission> findPermission(long knowledgeBaseId, Collection<Long> teamIds);
}
