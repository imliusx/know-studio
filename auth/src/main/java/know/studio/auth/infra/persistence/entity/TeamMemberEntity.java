package know.studio.auth.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("team_members")
public class TeamMemberEntity {

    @TableId
    private Long id;
    private Long teamId;
    private Long userId;
    private String teamRole;
}
