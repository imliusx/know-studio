package know.studio.arag.knowledge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("ingestion_jobs")
public class IngestionJobEntity {

    @TableId
    private Long id;
    private Long workspaceId;
    private Long documentId;
    private String jobType;
    private String status;
    private String error;
}
