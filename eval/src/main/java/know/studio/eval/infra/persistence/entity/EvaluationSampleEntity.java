package know.studio.eval.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("eval_samples")
public class EvaluationSampleEntity {

    @TableId
    private Long id;
    private Long knowledgeBaseId;
    private Long datasetId;
    private String question;
    private String relevantChunkIds;
    private String expectedAnswer;
    private Boolean expectRefusal;
    private Instant createdAt;
}
