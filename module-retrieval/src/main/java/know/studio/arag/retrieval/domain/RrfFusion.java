package know.studio.arag.retrieval.domain;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RrfFusion {

    private static final int RRF_CONSTANT = 60;

    public List<FusedCandidate> fuse(List<List<SearchCandidate>> rankings, int limit) {
        Map<Long, Accumulator> accumulators = new LinkedHashMap<>();
        for (List<SearchCandidate> ranking : rankings) {
            for (int index = 0; index < ranking.size(); index++) {
                SearchCandidate candidate = ranking.get(index);
                Accumulator accumulator = accumulators.computeIfAbsent(
                        candidate.chunkId(),
                        ignored -> new Accumulator(candidate)
                );
                accumulator.score += 1.0 / (RRF_CONSTANT + index + 1);
                accumulator.sources.add(candidate.source());
            }
        }
        return accumulators.values().stream()
                .map(Accumulator::toCandidate)
                .sorted(Comparator.comparingDouble(FusedCandidate::rrfScore).reversed()
                        .thenComparingLong(FusedCandidate::chunkId))
                .limit(limit)
                .toList();
    }

    private static final class Accumulator {

        private final SearchCandidate candidate;
        private final EnumSet<RetrievalSource> sources = EnumSet.noneOf(RetrievalSource.class);
        private double score;

        private Accumulator(SearchCandidate candidate) {
            this.candidate = candidate;
        }

        private FusedCandidate toCandidate() {
            return new FusedCandidate(
                    candidate.knowledgeBaseId(),
                    candidate.chunkId(),
                    candidate.documentId(),
                    candidate.chunkIndex(),
                    candidate.fileName(),
                    candidate.text(),
                    score,
                    null,
                    sources,
                    1
            );
        }
    }
}
