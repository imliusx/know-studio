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
@TableName("session_memory")
public class SessionMemoryEntity {

    @TableId
    private Long id;
    private Long sessionId;
    private String compactSummary;
    private String sessionSummary;
    private Long summarizedThroughMessageId;
    private Instant updatedAt;
}
