package know.studio.knowledge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("document_chunks")
public class DocumentChunkEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private Integer charStart;
    private Integer charEnd;
    private String sectionPath;
    private String status;
    private Boolean deleted;
}
