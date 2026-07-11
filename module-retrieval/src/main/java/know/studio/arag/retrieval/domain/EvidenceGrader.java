package know.studio.arag.retrieval.domain;

import know.studio.arag.retrieval.api.EvidenceLevel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvidenceGrader {

    public EvidenceLevel grade(List<FusedCandidate> candidates) {
        if (candidates.isEmpty()) {
            return EvidenceLevel.NONE;
        }
        FusedCandidate top = candidates.getFirst();
        if (top.rerankScore() != null) {
            if (top.rerankScore() >= 0.72) {
                return EvidenceLevel.SUFFICIENT;
            }
            if (top.rerankScore() >= 0.45) {
                return EvidenceLevel.PARTIAL;
            }
            if (top.rerankScore() >= 0.20) {
                return EvidenceLevel.WEAK;
            }
            return EvidenceLevel.NONE;
        }
        long hybridHits = candidates.stream()
                .filter(candidate -> candidate.sources().size() > 1)
                .count();
        if (hybridHits >= 2 || hybridHits == 1 && candidates.size() >= 3) {
            return EvidenceLevel.SUFFICIENT;
        }
        if (hybridHits == 1) {
            return EvidenceLevel.PARTIAL;
        }
        int supportCount = candidates.stream().mapToInt(FusedCandidate::supportCount).sum();
        return supportCount >= 2 ? EvidenceLevel.WEAK : EvidenceLevel.NONE;
    }
}
