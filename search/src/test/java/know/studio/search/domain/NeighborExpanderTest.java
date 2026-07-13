package know.studio.search.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NeighborExpanderTest {

    private final NeighborExpander expander = new NeighborExpander();

    @Test
    void addsAdjacentChunksFromSameDocumentWithDecayedScore() {
        FusedCandidate seed = new FusedCandidate(
                11L,
                10L,
                100L,
                5,
                "guide.md",
                "seed",
                0.03,
                null,
                Set.of(RetrievalSource.VECTOR, RetrievalSource.KEYWORD),
                1
        );
        NeighborChunk previous = new NeighborChunk(11L, 9L, 100L, 4, "guide.md", "previous");
        NeighborChunk otherDocument = new NeighborChunk(11L, 20L, 200L, 4, "other.md", "other");

        List<FusedCandidate> result = expander.expand(
                List.of(seed),
                List.of(previous, otherDocument),
                10
        );

        assertThat(result).extracting(FusedCandidate::chunkId).containsExactly(10L, 9L);
        assertThat(result.get(1).rrfScore()).isEqualTo(0.03 * 0.85);
        assertThat(result.get(1).sources()).isEqualTo(seed.sources());
    }
}
