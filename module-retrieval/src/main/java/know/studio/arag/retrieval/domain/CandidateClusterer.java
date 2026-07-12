package know.studio.arag.retrieval.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CandidateClusterer {

    public List<FusedCandidate> cluster(List<FusedCandidate> candidates, int limit) {
        Map<DocumentKey, List<FusedCandidate>> byDocument = candidates.stream()
                .collect(Collectors.groupingBy(candidate -> new DocumentKey(
                        candidate.knowledgeBaseId(),
                        candidate.documentId()
                )));
        List<FusedCandidate> clusters = new ArrayList<>();
        for (List<FusedCandidate> documentCandidates : byDocument.values()) {
            List<FusedCandidate> sorted = documentCandidates.stream()
                    .sorted(Comparator.comparingInt(FusedCandidate::chunkIndex))
                    .toList();
            List<FusedCandidate> current = new ArrayList<>();
            for (FusedCandidate candidate : sorted) {
                if (!current.isEmpty()
                        && candidate.chunkIndex() > current.getLast().chunkIndex() + 1) {
                    clusters.add(toCluster(current));
                    current.clear();
                }
                current.add(candidate);
            }
            if (!current.isEmpty()) {
                clusters.add(toCluster(current));
            }
        }
        return clusters.stream()
                .sorted(Comparator.comparingDouble(FusedCandidate::rrfScore).reversed()
                        .thenComparingLong(FusedCandidate::documentId)
                        .thenComparingInt(FusedCandidate::chunkIndex))
                .limit(limit)
                .toList();
    }

    private static FusedCandidate toCluster(List<FusedCandidate> candidates) {
        FusedCandidate anchor = candidates.stream()
                .max(Comparator.comparingDouble(FusedCandidate::rrfScore))
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
                null,
                sources,
                candidates.stream().mapToInt(FusedCandidate::supportCount).sum()
        );
    }

    private record DocumentKey(long knowledgeBaseId, long documentId) {
    }
}
