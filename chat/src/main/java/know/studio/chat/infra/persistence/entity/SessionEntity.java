package know.studio.chat.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("sessions")
public class SessionEntity {

    @TableId
    private Long id;
    private Long userId;
    private String title;
    private Boolean toolMode;
    private Boolean deepThinking;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
