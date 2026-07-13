package know.studio.auth.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("users")
public class UserEntity {

    @TableId
    private Long id;
    private String email;
    private String displayName;
    private String passwordHash;
    private String systemRole;
    private String status;
}
