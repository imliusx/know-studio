package know.studio.arag.retrieval.infra.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VectorSearchRow {

    private Long knowledgeBaseId;
    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private String fileName;
    private String chunkText;
    private Double score;
}
