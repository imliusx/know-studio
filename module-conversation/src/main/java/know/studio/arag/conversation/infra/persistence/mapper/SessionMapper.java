package know.studio.arag.conversation.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.conversation.infra.persistence.entity.SessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
}
