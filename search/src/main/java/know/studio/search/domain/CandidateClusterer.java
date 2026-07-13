package know.studio.search.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CandidateClusterer {

    private static final int NEIGHBOR_RADIUS = 1;

    public List<FusedCandidate> cluster(List<FusedCandidate> candidates, int limit) {
        Map<DocumentKey, List<FusedCandidate>> byDocument = candidates.stream()
                .collect(Collectors.groupingBy(candidate -> new DocumentKey(
                        candidate.knowledgeBaseId(),
                        candidate.documentId()
                )));
        List<FusedCandidate> clusters = new ArrayList<>();
        for (List<FusedCandidate> documentCandidates : byDocument.values()) {
            List<FusedCandidate> anchors = documentCandidates.stream()
                    .sorted(Comparator.comparingDouble(FusedCandidate::finalScore).reversed()
                            .thenComparingInt(FusedCandidate::chunkIndex))
                    .toList();
            Set<Long> assignedChunkIds = new HashSet<>();
            for (FusedCandidate anchor : anchors) {
                if (!assignedChunkIds.add(anchor.chunkId())) {
                    continue;
                }
                List<FusedCandidate> cluster = documentCandidates.stream()
                        .filter(candidate -> !assignedChunkIds.contains(candidate.chunkId()))
                        .filter(candidate -> Math.abs(candidate.chunkIndex() - anchor.chunkIndex()) <= NEIGHBOR_RADIUS)
                        .sorted(Comparator.comparingInt(FusedCandidate::chunkIndex))
                        .toList();
                assignedChunkIds.addAll(cluster.stream().map(FusedCandidate::chunkId).toList());
                List<FusedCandidate> focused = new ArrayList<>(cluster.size() + 1);
                focused.add(anchor);
                focused.addAll(cluster);
                clusters.add(toCluster(focused));
            }
        }
        return clusters.stream()
                .sorted(Comparator.comparingDouble(FusedCandidate::finalScore).reversed()
                        .thenComparingLong(FusedCandidate::documentId)
                        .thenComparingInt(FusedCandidate::chunkIndex))
                .limit(limit)
                .toList();
    }

    private static FusedCandidate toCluster(List<FusedCandidate> candidates) {
        FusedCandidate anchor = candidates.stream()
                .max(Comparator.comparingDouble(FusedCandidate::finalScore))
                .orElseThrow();
        EnumSet<RetrievalSource> sources = EnumSet.noneOf(RetrievalSource.class);
        candidates.forEach(candidate -> sources.addAll(candidate.sources()));
        String text = candidates.stream()
                .sorted(Comparator.comparingInt(FusedCandidate::chunkIndex))
                .map(FusedCandidate::text)
                .distinct()
                .collect(Collectors.joining("\n\n"));
        return new FusedCandidate(
                anchor.knowledgeBaseId(),
                anchor.chunkId(),
                anchor.documentId(),
                anchor.chunkIndex(),
                anchor.fileName(),
                text,
                anchor.rrfScore(),
                anchor.rerankScore(),
                sources,
                candidates.stream().mapToInt(FusedCandidate::supportCount).sum()
        );
    }

    private record DocumentKey(long knowledgeBaseId, long documentId) {
    }
}
