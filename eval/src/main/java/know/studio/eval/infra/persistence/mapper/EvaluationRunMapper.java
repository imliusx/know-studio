package know.studio.eval.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.eval.infra.persistence.entity.EvaluationRunEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EvaluationRunMapper extends BaseMapper<EvaluationRunEntity> {

    int insertJson(EvaluationRunEntity entity);
}
