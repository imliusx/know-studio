package know.studio.auth.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("teams")
public class TeamEntity {

    @TableId
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Long createdBy;
    private String status;
}
