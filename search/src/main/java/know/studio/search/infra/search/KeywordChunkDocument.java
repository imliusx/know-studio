package know.studio.search.infra.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeywordChunkDocument {

    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String fileName;
    private String chunkText;
    private String status;
    private Boolean deleted;
}
