package know.studio.knowledge.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.knowledge.infra.persistence.entity.UploadChunkEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadChunkMapper extends BaseMapper<UploadChunkEntity> {
}
