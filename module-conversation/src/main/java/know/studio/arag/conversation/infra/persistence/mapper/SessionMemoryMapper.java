package know.studio.arag.conversation.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.conversation.infra.persistence.entity.SessionMemoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SessionMemoryMapper extends BaseMapper<SessionMemoryEntity> {

    SessionMemoryEntity selectOwned(
            @Param("workspaceId") long workspaceId,
            @Param("userId") long userId,
            @Param("sessionId") long sessionId
    );

    int upsert(SessionMemoryEntity entity);
}
