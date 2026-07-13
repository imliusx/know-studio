package know.studio.knowledge.infra.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("knowledge_bases")
public class KnowledgeBaseEntity {

    @TableId
    private Long id;
    private String name;
    private String description;
    private String visibility;
    private Long ownerTeamId;
    private Long createdBy;
    private String status;
}
