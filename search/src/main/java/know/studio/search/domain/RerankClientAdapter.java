package know.studio.search.domain;

import know.studio.ai.provider.RerankDocument;
import know.studio.ai.provider.RerankResult;
import know.studio.ai.rerank.RerankClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RerankClientAdapter implements RerankPort {

    private final RerankClient client;

    @Override
    public List<FusedCandidate> rerank(String query, List<FusedCandidate> candidates) {
        List<RerankResult> results = client.rerank(
                query,
                candidates.stream()
                        .map(candidate -> new RerankDocument(Long.toString(candidate.chunkId()), candidate.text()))
                        .toList()
        );
        Map<Long, FusedCandidate> byId = candidates.stream()
                .collect(Collectors.toMap(FusedCandidate::chunkId, Function.identity()));
        return results.stream()
                .map(result -> {
                    FusedCandidate candidate = byId.get(Long.parseLong(result.documentId()));
                    return candidate == null ? null : candidate.withRerankScore(result.score());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(FusedCandidate::finalScore).reversed())
                .toList();
    }
}
