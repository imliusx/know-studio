package know.studio.arag.knowledge.infra.mq;

import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.knowledge.domain.DocumentRecord;
import know.studio.arag.knowledge.domain.KnowledgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleIngestionRecoveryRunnerTest {

    @Mock
    private KnowledgeRepository repository;
    @Mock
    private IngestionMessagePublisher publisher;

    @Test
    void republishesEveryRecoveredDocumentEvenWhenOnePublishFails() {
        DocumentRecord first = document(11L, 101L);
        DocumentRecord second = document(22L, 202L);
        when(repository.recoverStaleProcessing(any(Instant.class), eq(100)))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(11L, 101L);
        StaleIngestionRecoveryRunner runner = new StaleIngestionRecoveryRunner(repository, publisher);

        runner.recover();

        verify(publisher).publish(11L, 101L);
        verify(publisher).publish(22L, 202L);
        verify(repository).deferRecoveredDocument(
                11L,
                101L,
                "Recovery republish failed: broker unavailable"
        );
    }

    private static DocumentRecord document(long knowledgeBaseId, long documentId) {
        return new DocumentRecord(
                documentId,
                knowledgeBaseId,
                "guide.md",
                "documents/" + documentId,
                "text/markdown",
                100L,
                "hash-" + documentId,
                DocumentStatus.FAILED,
                null,
                "Recovered stale ingestion task",
                0,
                33L
        );
    }
}
