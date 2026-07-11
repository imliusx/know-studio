package know.studio.arag.retrieval.api;

import java.util.List;

public record EvidenceBundle(List<Evidence> evidence, EvidenceLevel level, String guidance) {

    public EvidenceBundle {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
