package know.studio.chat.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.chat.infra.persistence.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    int insertJson(MessageEntity entity);

    List<MessageEntity> selectRecentOwned(
            @Param("userId") long userId,
            @Param("sessionId") long sessionId,
            @Param("limit") int limit
    );

    List<MessageEntity> selectAllOwned(
            @Param("userId") long userId,
            @Param("sessionId") long sessionId,
            @Param("afterMessageId") long afterMessageId
    );

    int countOwned(
            @Param("userId") long userId,
            @Param("sessionId") long sessionId
    );

    long sumTokensOwned(
            @Param("userId") long userId,
            @Param("sessionId") long sessionId
    );
}
