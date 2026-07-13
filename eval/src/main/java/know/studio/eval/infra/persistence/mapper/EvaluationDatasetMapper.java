package know.studio.eval.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.eval.infra.persistence.entity.EvaluationDatasetEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EvaluationDatasetMapper extends BaseMapper<EvaluationDatasetEntity> {
}
