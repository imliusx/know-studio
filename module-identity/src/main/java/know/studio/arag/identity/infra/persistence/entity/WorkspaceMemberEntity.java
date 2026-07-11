package know.studio.arag.identity.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("workspace_members")
public class WorkspaceMemberEntity {

    @TableId
    private Long id;
    private Long workspaceId;
    private Long userId;
    private String workspaceRole;
}
