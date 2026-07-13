package know.studio.search.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateClustererTest {

    private final CandidateClusterer clusterer = new CandidateClusterer();

    @Test
    void mergesContiguousChunksAndKeepsHighestScoreAnchor() {
        FusedCandidate first = candidate(10L, 4, "first", 0.02);
        FusedCandidate anchor = candidate(11L, 5, "anchor", 0.03);
        FusedCandidate separate = candidate(13L, 7, "separate", 0.01);

        List<FusedCandidate> result = clusterer.cluster(List.of(first, anchor, separate), 10);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().chunkId()).isEqualTo(11L);
        assertThat(result.getFirst().text()).isEqualTo("first\n\nanchor");
        assertThat(result.get(1).chunkId()).isEqualTo(13L);
    }

    @Test
    void doesNotChainAnEntireDocumentIntoOneEvidenceItem() {
        List<FusedCandidate> candidates = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> candidate(100L + index, index, "chunk-" + index, index == 4 ? 0.20 : 0.01))
                .toList();

        List<FusedCandidate> result = clusterer.cluster(candidates, 10);

        assertThat(result).hasSizeGreaterThan(1);
        assertThat(result.getFirst().chunkId()).isEqualTo(104L);
        assertThat(result.getFirst().text()).isEqualTo("chunk-3\n\nchunk-4\n\nchunk-5");
        assertThat(result).allSatisfy(cluster ->
                assertThat(cluster.text().split("\\n\\n")).hasSizeLessThanOrEqualTo(3));
    }

    private static FusedCandidate candidate(long chunkId, int index, String text, double score) {
        return new FusedCandidate(
                11L,
                chunkId,
                100L,
                index,
                "guide.md",
                text,
                score,
                null,
                Set.of(RetrievalSource.VECTOR),
                1
        );
    }
}
