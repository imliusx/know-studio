package know.studio.arag.knowledge.api;

import java.util.List;
import java.util.Set;

public interface KnowledgeAccessApi {

    List<KnowledgeBaseInfo> listReadable();

    Set<Long> readableKnowledgeBaseIds();

    KnowledgeBasePermission requireReadable(long knowledgeBaseId);

    KnowledgeBasePermission requireManageable(long knowledgeBaseId);
}
