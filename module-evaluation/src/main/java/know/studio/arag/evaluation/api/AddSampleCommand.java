package know.studio.arag.evaluation.api;

import java.util.List;

public record AddSampleCommand(
        long knowledgeBaseId,
        long datasetId,
        String question,
        List<Long> relevantChunkIds,
        String expectedAnswer
) {

    public AddSampleCommand {
        relevantChunkIds = relevantChunkIds == null ? List.of() : List.copyOf(relevantChunkIds);
    }
}
