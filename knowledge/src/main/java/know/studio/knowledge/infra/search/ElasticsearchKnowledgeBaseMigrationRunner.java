package know.studio.knowledge.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchKnowledgeBaseMigrationRunner implements ApplicationRunner {

    private static final String INDEX_NAME = "arag_document_chunks";

    private final ElasticsearchClient client;

    @Override
    public void run(ApplicationArguments args) {
        try {
            UpdateByQueryResponse response = client.updateByQuery(request -> request
                    .index(INDEX_NAME)
                    .conflicts(Conflicts.Proceed)
                    .refresh(true)
                    .query(query -> query.bool(bool -> bool
                            .must(clause -> clause.exists(exists -> exists.field("workspaceId")))
                            .mustNot(clause -> clause.exists(exists -> exists.field("knowledgeBaseId")))))
                    .script(script -> script
                            .lang("painless")
                            .source("ctx._source.knowledgeBaseId = ctx._source.workspaceId"))
            );
            if (response.updated() != null && response.updated() > 0) {
                log.info("Migrated legacy Elasticsearch chunks to knowledgeBaseId count={}", response.updated());
            }
        } catch (IOException | RuntimeException exception) {
            log.warn("Deferred Elasticsearch knowledgeBaseId migration index={}", INDEX_NAME, exception);
        }
    }
}
