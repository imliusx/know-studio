package know.studio.arag.retrieval.domain;

import know.studio.arag.knowledge.api.KnowledgeBaseInfo;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;
import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataKnowledgeBaseScopeSelectorTest {

    private final MetadataKnowledgeBaseScopeSelector selector =
            new MetadataKnowledgeBaseScopeSelector();

    @Test
    void selectsKnowledgeBaseWithMatchingMetadata() {
        KnowledgeBaseScopeDecision result = selector.select(
                "Java 类名如何命名？",
                List.of(
                        knowledgeBase(11L, "技术文档库", "Java 开发规范和工程实践"),
                        knowledgeBase(12L, "人事制度库", "考勤、休假与员工福利制度")
                )
        );

        assertThat(result.knowledgeBaseIds()).containsExactly(11L);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.75);
    }

    @Test
    void returnsUncertainDecisionWhenMetadataDoesNotMatch() {
        KnowledgeBaseScopeDecision result = selector.select(
                "客户拜访产生的费用怎么报销",
                List.of(
                        knowledgeBase(11L, "技术文档库", "Java 开发规范和工程实践"),
                        knowledgeBase(12L, "人事制度库", "考勤、休假与员工福利制度")
                )
        );

        assertThat(result).isEqualTo(KnowledgeBaseScopeDecision.uncertain());
    }

    private static KnowledgeBaseInfo knowledgeBase(
            long knowledgeBaseId,
            String name,
            String description
    ) {
        return new KnowledgeBaseInfo(
                knowledgeBaseId,
                name,
                description,
                KnowledgeBaseVisibility.PRIVATE,
                null,
                KnowledgeBasePermission.MANAGE
        );
    }
}
