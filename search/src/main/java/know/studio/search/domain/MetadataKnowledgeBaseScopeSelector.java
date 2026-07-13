package know.studio.search.domain;

import know.studio.knowledge.api.KnowledgeBaseInfo;
import know.studio.common.trace.RagTraceNode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MetadataKnowledgeBaseScopeSelector implements KnowledgeBaseScopeSelector {

    private static final double MIN_COVERAGE = 0.30;
    private static final double MAX_SELECTION_GAP = 0.10;

    @Override
    @RagTraceNode("retrieval.knowledge-base-route")
    public KnowledgeBaseScopeDecision select(
            String question,
            List<KnowledgeBaseInfo> knowledgeBases
    ) {
        if (knowledgeBases.size() <= 1) {
            return new KnowledgeBaseScopeDecision(
                    knowledgeBases.stream()
                            .map(KnowledgeBaseInfo::knowledgeBaseId)
                            .collect(java.util.stream.Collectors.toSet()),
                    1.0
            );
        }
        Set<String> terms = EvidenceGrader.queryTerms(question);
        if (terms.isEmpty()) {
            return KnowledgeBaseScopeDecision.uncertain();
        }
        List<ScoredKnowledgeBase> scored = knowledgeBases.stream()
                .map(knowledgeBase -> new ScoredKnowledgeBase(
                        knowledgeBase.knowledgeBaseId(),
                        coverage(knowledgeBase, terms)
                ))
                .sorted(Comparator.comparingDouble(ScoredKnowledgeBase::coverage).reversed())
                .toList();
        double bestCoverage = scored.getFirst().coverage();
        if (bestCoverage < MIN_COVERAGE) {
            return KnowledgeBaseScopeDecision.uncertain();
        }
        Set<Long> selectedIds = scored.stream()
                .filter(candidate -> candidate.coverage() >= MIN_COVERAGE)
                .filter(candidate -> bestCoverage - candidate.coverage() <= MAX_SELECTION_GAP)
                .map(ScoredKnowledgeBase::knowledgeBaseId)
                .collect(java.util.stream.Collectors.toSet());
        double confidence = Math.min(0.99, 0.70 + bestCoverage * 0.29);
        return new KnowledgeBaseScopeDecision(selectedIds, confidence);
    }

    private static double coverage(KnowledgeBaseInfo knowledgeBase, Set<String> terms) {
        String metadata = (knowledgeBase.name() + " "
                + (knowledgeBase.description() == null ? "" : knowledgeBase.description()))
                .toLowerCase(Locale.ROOT);
        long matched = terms.stream().filter(metadata::contains).count();
        return (double) matched / terms.size();
    }

    private record ScoredKnowledgeBase(long knowledgeBaseId, double coverage) {
    }
}
