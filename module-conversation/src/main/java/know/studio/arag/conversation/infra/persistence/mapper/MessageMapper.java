package know.studio.arag.conversation.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.conversation.infra.persistence.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    int insertJson(MessageEntity entity);

    List<MessageEntity> selectRecentOwned(
            @Param("workspaceId") long workspaceId,
            @Param("userId") long userId,
            @Param("sessionId") long sessionId,
            @Param("limit") int limit
    );

    List<MessageEntity> selectAllOwned(
            @Param("workspaceId") long workspaceId,
            @Param("userId") long userId,
            @Param("sessionId") long sessionId,
            @Param("afterMessageId") long afterMessageId
    );

    int countOwned(
            @Param("workspaceId") long workspaceId,
            @Param("userId") long userId,
            @Param("sessionId") long sessionId
    );

    long sumTokensOwned(
            @Param("workspaceId") long workspaceId,
            @Param("userId") long userId,
            @Param("sessionId") long sessionId
    );
}
