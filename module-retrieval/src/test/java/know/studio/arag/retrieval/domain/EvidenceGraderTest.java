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
        assertThat(grader.grade(List.of())).isEqualTo(EvidenceLevel.NONE);
    }

    @Test
    void gradesStrongRerankScoreAsSufficient() {
        assertThat(grader.grade(List.of(candidate(0.82, Set.of(RetrievalSource.VECTOR)))))
                .isEqualTo(EvidenceLevel.SUFFICIENT);
    }

    @Test
    void usesCrossChannelAgreementWhenRerankIsUnavailable() {
        FusedCandidate hybrid = candidate(
                null,
                Set.of(RetrievalSource.VECTOR, RetrievalSource.KEYWORD)
        );

        assertThat(grader.grade(List.of(hybrid)))
                .isEqualTo(EvidenceLevel.PARTIAL);
    }

    private static FusedCandidate candidate(Double rerankScore, Set<RetrievalSource> sources) {
        return new FusedCandidate(
                11L,
                1L,
                2L,
                0,
                "guide.md",
                "content",
                0.03,
                rerankScore,
                sources,
                1
        );
    }
}
