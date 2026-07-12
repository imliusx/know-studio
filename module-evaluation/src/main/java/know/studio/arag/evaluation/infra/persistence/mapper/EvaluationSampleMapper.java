package know.studio.arag.evaluation.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import know.studio.arag.evaluation.infra.persistence.entity.EvaluationSampleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EvaluationSampleMapper extends BaseMapper<EvaluationSampleEntity> {

    int insertJson(EvaluationSampleEntity entity);

    List<EvaluationSampleEntity> selectOwned(
            @Param("knowledgeBaseId") long knowledgeBaseId,
            @Param("datasetId") long datasetId
    );
}
