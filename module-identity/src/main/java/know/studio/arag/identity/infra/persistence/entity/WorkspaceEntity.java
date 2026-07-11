package know.studio.arag.identity.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("workspaces")
public class WorkspaceEntity {

    @TableId
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String status;
}
