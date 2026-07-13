package know.studio.knowledge.infra.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("knowledge_base_team_grants")
public class KnowledgeBaseTeamGrantEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private Long teamId;
    private String permission;
}
