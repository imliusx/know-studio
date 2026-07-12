package know.studio.arag.knowledge.infra.search;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "arag_document_chunks", createIndex = true)
@Getter
@Setter
@NoArgsConstructor
public class ChunkSearchDocument {

    @Id
    private Long id;
    @Field(type = FieldType.Long)
    private Long knowledgeBaseId;
    @Field(type = FieldType.Long)
    private Long documentId;
    @Field(type = FieldType.Integer)
    private Integer chunkIndex;
    @Field(type = FieldType.Keyword)
    private String fileName;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String chunkText;
    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Boolean)
    private Boolean deleted;
}
