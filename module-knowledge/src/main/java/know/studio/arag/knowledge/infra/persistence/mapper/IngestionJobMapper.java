package know.studio.arag.knowledge.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.knowledge.infra.persistence.entity.IngestionJobEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestionJobMapper extends BaseMapper<IngestionJobEntity> {
}
