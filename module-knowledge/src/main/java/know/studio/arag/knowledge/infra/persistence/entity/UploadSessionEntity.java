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
@TableName("upload_sessions")
public class UploadSessionEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String contentHash;
    private Integer totalChunks;
    private String status;
    private Long createdBy;
    private Long documentId;
    private Instant expiresAt;
}
