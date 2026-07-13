package know.studio.eval.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("eval_runs")
public class EvaluationRunEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private Long datasetId;
    private Long userId;
    private String config;
    private BigDecimal recallAtK;
    private Integer sampleCount;
    private Long avgLatencyMs;
    private String extra;
    private Instant createdAt;
}
