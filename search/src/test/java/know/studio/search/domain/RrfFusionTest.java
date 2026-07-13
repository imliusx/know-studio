package know.studio.search.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RrfFusionTest {

    private final RrfFusion fusion = new RrfFusion();

    @Test
    void promotesCandidateReturnedByBothChannels() {
        SearchCandidate vectorOnly = candidate(1L, 0.95, RetrievalSource.VECTOR);
        SearchCandidate hybridVector = candidate(2L, 0.80, RetrievalSource.VECTOR);
        SearchCandidate hybridKeyword = candidate(2L, 12.0, RetrievalSource.KEYWORD);
        SearchCandidate keywordOnly = candidate(3L, 11.0, RetrievalSource.KEYWORD);

        List<FusedCandidate> result = fusion.fuse(
                List.of(
                        List.of(vectorOnly, hybridVector),
                        List.of(hybridKeyword, keywordOnly)
                ),
                10
        );

        assertThat(result).extracting(FusedCandidate::chunkId).containsExactly(2L, 1L, 3L);
        assertThat(result.getFirst().sources())
                .containsExactlyInAnyOrder(RetrievalSource.VECTOR, RetrievalSource.KEYWORD);
    }

    @Test
    void appliesStableChunkIdTieBreaker() {
        List<FusedCandidate> result = fusion.fuse(
                List.of(List.of(
                        candidate(2L, 1.0, RetrievalSource.VECTOR),
                        candidate(3L, 0.9, RetrievalSource.VECTOR)
                ), List.of(
                        candidate(1L, 1.0, RetrievalSource.KEYWORD),
                        candidate(3L, 0.9, RetrievalSource.KEYWORD)
                )),
                2
        );

        assertThat(result).extracting(FusedCandidate::chunkId).containsExactly(3L, 1L);
    }

    private static SearchCandidate candidate(long chunkId, double score, RetrievalSource source) {
        return new SearchCandidate(
                11L,
                chunkId,
                100L,
                Math.toIntExact(chunkId),
                "guide.md",
                "chunk-" + chunkId,
                score,
                source
        );
    }
}
