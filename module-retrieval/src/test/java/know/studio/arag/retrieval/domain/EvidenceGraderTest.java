package know.studio.arag.retrieval.domain;

import know.studio.arag.retrieval.api.EvidenceLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceGraderTest {

    private final EvidenceGrader grader = new EvidenceGrader();

    @Test
    void rejectsEmptyEvidence() {
        assertThat(grader.grade("question", List.of())).isEqualTo(EvidenceLevel.NONE);
    }

    @Test
    void gradesStrongRerankScoreAsSufficient() {
        assertThat(grader.grade(
                "unrelated question",
                List.of(candidate("content", 0.82, Set.of(RetrievalSource.VECTOR)))
        ))
                .isEqualTo(EvidenceLevel.SUFFICIENT);
    }

    @Test
    void usesCrossChannelAgreementWhenRerankIsUnavailable() {
        FusedCandidate hybrid = candidate(
                "RAG combines retrieval and generation",
                null,
                Set.of(RetrievalSource.VECTOR, RetrievalSource.KEYWORD)
        );

        assertThat(grader.grade("What is RAG retrieval?", List.of(hybrid)))
                .isEqualTo(EvidenceLevel.PARTIAL);
    }

    @Test
    void rejectsCrossChannelHitsWithoutQuestionTermCoverage() {
        FusedCandidate unrelated = candidate(
                "Java classes should use UpperCamelCase naming",
                null,
                Set.of(RetrievalSource.VECTOR, RetrievalSource.KEYWORD)
        );

        assertThat(grader.grade(
                "客户拜访的交通、餐饮和住宿费用怎么报销",
                List.of(unrelated)
        )).isEqualTo(EvidenceLevel.NONE);
    }

    @Test
    void measuresChineseAndAsciiQuestionTermCoverage() {
        FusedCandidate relevant = candidate(
                "Java 类名使用 UpperCamelCase 风格",
                null,
                Set.of(RetrievalSource.VECTOR, RetrievalSource.KEYWORD)
        );

        assertThat(EvidenceGrader.lexicalCoverage("Java 类名如何命名", List.of(relevant)))
                .isGreaterThanOrEqualTo(0.34);
    }

    private static FusedCandidate candidate(
            String text,
            Double rerankScore,
            Set<RetrievalSource> sources
    ) {
        return new FusedCandidate(
                11L,
                1L,
                2L,
                0,
                "guide.md",
                text,
                0.03,
                rerankScore,
                sources,
                1
        );
    }
}
