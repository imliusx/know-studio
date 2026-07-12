package know.studio.arag.retrieval.domain;

import know.studio.arag.knowledge.api.KnowledgeBaseInfo;

import java.util.List;

public interface KnowledgeBaseScopeSelector {

    KnowledgeBaseScopeDecision select(String question, List<KnowledgeBaseInfo> knowledgeBases);
}
