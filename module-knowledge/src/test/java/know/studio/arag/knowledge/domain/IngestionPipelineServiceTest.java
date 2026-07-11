package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.platform.ai.embedding.EmbeddingClient;
import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.routing.CircuitBreakerConfig;
import know.studio.arag.platform.ai.routing.ProviderCircuitBreakerRegistry;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionPipelineServiceTest {

    private static final long WORKSPACE_ID = 11L;
    private static final long DOCUMENT_ID = 22L;

    @Mock
    private KnowledgeRepository repository;
    @Mock
    private DocumentParserPort parser;
    @Mock
    private DocumentIndexPort indexPort;

    @Test
    void processesDocumentAndMarksItReady() {
        DocumentRecord document = document();
        when(repository.claimDocumentForProcessing(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(true);
        when(repository.findDocument(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(parser.parse(document)).thenReturn(new ParsedDocument("# Intro\n\nUseful content.", "text/markdown"));
        IngestionPipelineService service = service(texts -> vectors(texts.size()));

        service.process(WORKSPACE_ID, DOCUMENT_ID);

        ArgumentCaptor<List<DocumentChunk>> chunks = ArgumentCaptor.captor();
        verify(indexPort).replace(
                org.mockito.ArgumentMatchers.eq(WORKSPACE_ID),
                org.mockito.ArgumentMatchers.eq(document),
                chunks.capture(),
                org.mockito.ArgumentMatchers.anyList()
        );
        assertThat(chunks.getValue()).isNotEmpty();
        verify(repository).markDocumentReady(
                org.mockito.ArgumentMatchers.eq(WORKSPACE_ID),
                org.mockito.ArgumentMatchers.eq(DOCUMENT_ID),
                anyString(),
                org.mockito.ArgumentMatchers.eq(chunks.getValue().size())
        );
        verify(repository).markIngestionCompleted(DOCUMENT_ID);
        verify(repository, never()).markDocumentFailed(anyLong(), anyLong(), anyString());
    }

    @Test
    void skipsDocumentThatWasAlreadyClaimed() {
        when(repository.claimDocumentForProcessing(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(false);

        service(texts -> vectors(texts.size())).process(WORKSPACE_ID, DOCUMENT_ID);

        verify(repository, never()).findDocument(anyLong(), anyLong());
        verify(parser, never()).parse(org.mockito.ArgumentMatchers.any());
        verifyNoTerminalStateChanges();
    }

    @Test
    void marksDocumentFailedWhenParsingFails() {
        DocumentRecord document = document();
        when(repository.claimDocumentForProcessing(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(true);
        when(repository.findDocument(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(parser.parse(document)).thenThrow(new IllegalStateException("parse failed"));

        assertThatThrownBy(() -> service(texts -> vectors(texts.size())).process(WORKSPACE_ID, DOCUMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("parse failed");

        verify(repository).markDocumentFailed(WORKSPACE_ID, DOCUMENT_ID, "parse failed");
        verify(repository).markIngestionFailed(DOCUMENT_ID, "parse failed");
        verify(indexPort, never()).replace(
                anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void rejectsEmbeddingCountMismatch() {
        DocumentRecord document = document();
        when(repository.claimDocumentForProcessing(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(true);
        when(repository.findDocument(WORKSPACE_ID, DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(parser.parse(document)).thenReturn(new ParsedDocument("Useful content.", "text/plain"));

        assertThatThrownBy(() -> service(ignored -> List.of()).process(WORKSPACE_ID, DOCUMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Embedding count");

        verify(repository).markDocumentFailed(
                WORKSPACE_ID,
                DOCUMENT_ID,
                "Embedding count does not match chunk count"
        );
        verify(repository).markIngestionFailed(DOCUMENT_ID, "Embedding count does not match chunk count");
    }

    private IngestionPipelineService service(Function<List<String>, List<float[]>> embedder) {
        AiProvider provider = new AiProvider() {
            @Override
            public String id() {
                return "test-embedding";
            }

            @Override
            public int priority() {
                return 1;
            }

            @Override
            public Set<AiCapability> capabilities() {
                return Set.of(AiCapability.EMBEDDING);
            }

            @Override
            public List<float[]> embed(List<String> texts) {
                return embedder.apply(texts);
            }
        };
        EmbeddingClient embeddingClient = new EmbeddingClient(
                List.of(provider),
                new ProviderCircuitBreakerRegistry(new CircuitBreakerConfig(3, Duration.ofSeconds(30)))
        );
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);
        return new IngestionPipelineService(
                repository,
                parser,
                new StructuredTextChunker(idGenerator),
                embeddingClient,
                indexPort,
                idGenerator
        );
    }

    private void verifyNoTerminalStateChanges() {
        verify(repository, never()).markDocumentReady(anyLong(), anyLong(), anyString(), anyInt());
        verify(repository, never()).markDocumentFailed(anyLong(), anyLong(), anyString());
        verify(repository, never()).markIngestionCompleted(anyLong());
        verify(repository, never()).markIngestionFailed(anyLong(), anyString());
    }

    private static List<float[]> vectors(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(ignored -> new float[1_024])
                .toList();
    }

    private static DocumentRecord document() {
        return new DocumentRecord(
                DOCUMENT_ID,
                WORKSPACE_ID,
                "guide.md",
                "documents/guide.md",
                "text/markdown",
                100L,
                "hash",
                DocumentStatus.PENDING,
                null,
                null,
                0,
                33L
        );
    }
}
