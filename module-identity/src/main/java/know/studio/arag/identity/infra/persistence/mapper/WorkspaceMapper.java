package know.studio.arag.identity.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.identity.infra.persistence.entity.WorkspaceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspaceEntity> {
}
