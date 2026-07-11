package know.studio.arag.knowledge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("upload_chunks")
public class UploadChunkEntity {

    @TableId
    private Long id;
    private Long uploadSessionId;
    private Integer chunkIndex;
    private String objectKey;
    private Long chunkSize;
    private String chunkHash;
}
