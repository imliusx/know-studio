package know.studio.arag.knowledge.domain;

import know.studio.arag.platform.ai.embedding.EmbeddingClient;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionPipelineService implements IngestionTaskHandler {

    private final KnowledgeRepository repository;
    private final DocumentParserPort parser;
    private final StructuredTextChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final DocumentIndexPort indexPort;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public void process(long workspaceId, long documentId) {
        repository.ensureIngestionJob(idGenerator.nextId(), workspaceId, documentId);
        if (!repository.claimDocumentForProcessing(workspaceId, documentId)) {
            log.info("Skipped already claimed ingestion workspaceId={} documentId={}", workspaceId, documentId);
            return;
        }
        try {
            DocumentRecord document = repository.findDocument(workspaceId, documentId)
                    .orElseThrow(() -> new IllegalStateException("Document disappeared after ingestion claim"));
            ParsedDocument parsed = node(documentId, "PARSE", () -> parser.parse(document));
            String cleaned = node(documentId, "CLEAN", () -> StructuredTextChunker.clean(parsed.text()));
            List<DocumentChunk> chunks = node(
                    documentId,
                    "CHUNK",
                    () -> chunker.chunk(workspaceId, documentId, cleaned)
            );
            if (chunks.isEmpty()) {
                throw new IllegalStateException("Document contains no indexable text");
            }
            List<float[]> embeddings = node(
                    documentId,
                    "EMBED",
                    () -> embeddingClient.embed(chunks.stream().map(DocumentChunk::text).toList())
            );
            if (embeddings.size() != chunks.size()) {
                throw new IllegalStateException("Embedding count does not match chunk count");
            }
            node(documentId, "INDEX", () -> {
                indexPort.replace(workspaceId, document, chunks, embeddings);
                return null;
            });
            repository.markDocumentReady(workspaceId, documentId, preview(cleaned), chunks.size());
            repository.markIngestionCompleted(documentId);
        } catch (RuntimeException exception) {
            repository.markDocumentFailed(workspaceId, documentId, exception.getMessage());
            repository.markIngestionFailed(documentId, exception.getMessage());
            throw exception;
        }
    }

    private <T> T node(long documentId, String name, NodeAction<T> action) {
        long started = System.nanoTime();
        try {
            T result = action.execute();
            repository.appendNodeLog(documentId, name, "SUCCESS", elapsedMillis(started), "");
            return result;
        } catch (RuntimeException exception) {
            repository.appendNodeLog(
                    documentId,
                    name,
                    "FAILED",
                    elapsedMillis(started),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private static String preview(String text) {
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    @FunctionalInterface
    private interface NodeAction<T> {

        T execute();
    }
}
