package know.studio.arag.knowledge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("documents")
public class DocumentEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private String contentHash;
    private String status;
    private String previewText;
    private String failureReason;
    private Integer chunkCount;
    private Long createdBy;
    private Instant updatedAt;
}
