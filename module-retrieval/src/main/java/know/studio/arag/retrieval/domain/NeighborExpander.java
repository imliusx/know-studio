package know.studio.arag.retrieval.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NeighborExpander {

    public List<FusedCandidate> expand(
            List<FusedCandidate> seeds,
            List<NeighborChunk> neighbors,
            int limit
    ) {
        Map<Long, FusedCandidate> expanded = new LinkedHashMap<>();
        seeds.forEach(seed -> expanded.put(seed.chunkId(), seed));
        for (NeighborChunk neighbor : neighbors) {
            if (expanded.containsKey(neighbor.chunkId())) {
                continue;
            }
            FusedCandidate nearest = nearestSeed(neighbor, seeds);
            if (nearest == null) {
                continue;
            }
            int distance = Math.abs(neighbor.chunkIndex() - nearest.chunkIndex());
            double score = nearest.rrfScore() * Math.pow(0.85, distance);
            expanded.put(neighbor.chunkId(), new FusedCandidate(
                    neighbor.knowledgeBaseId(),
                    neighbor.chunkId(),
                    neighbor.documentId(),
                    neighbor.chunkIndex(),
                    neighbor.fileName(),
                    neighbor.text(),
                    score,
                    null,
                    nearest.sources(),
                    1
            ));
        }
        return expanded.values().stream()
                .sorted(Comparator.comparingDouble(FusedCandidate::rrfScore).reversed()
                        .thenComparingLong(FusedCandidate::documentId)
                        .thenComparingInt(FusedCandidate::chunkIndex))
                .limit(limit)
                .toList();
    }

    private static FusedCandidate nearestSeed(NeighborChunk neighbor, List<FusedCandidate> seeds) {
        List<FusedCandidate> sameDocument = new ArrayList<>();
        for (FusedCandidate seed : seeds) {
            if (seed.knowledgeBaseId() == neighbor.knowledgeBaseId()
                    && seed.documentId() == neighbor.documentId()) {
                sameDocument.add(seed);
            }
        }
        return sameDocument.stream()
                .min(Comparator.comparingInt(seed -> Math.abs(seed.chunkIndex() - neighbor.chunkIndex())))
                .orElse(null);
    }
}
