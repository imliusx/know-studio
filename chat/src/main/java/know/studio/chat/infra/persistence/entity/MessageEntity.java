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
@TableName("messages")
public class MessageEntity {

    @TableId
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Integer tokens;
    private String metadata;
    private Instant createdAt;
}
