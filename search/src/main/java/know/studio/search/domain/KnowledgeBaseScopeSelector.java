package know.studio.search.domain;

import know.studio.knowledge.api.KnowledgeBaseInfo;

import java.util.List;

public interface KnowledgeBaseScopeSelector {

    KnowledgeBaseScopeDecision select(String question, List<KnowledgeBaseInfo> knowledgeBases);
}
