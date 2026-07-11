package know.studio.arag.knowledge.domain;

import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.knowledge.api.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeQueryServiceTest {

    @Test
    void deletesDocumentIndexesAfterAdminAuthorization() {
        Fixture fixture = new Fixture(DocumentStatus.READY);
        when(fixture.repository.markDocumentDeleted(10L, 20L)).thenReturn(true);

        fixture.service.deleteDocument(10L, 20L);

        verify(fixture.identityApi).requireRole(10L, WorkspaceRole.ADMIN);
        verify(fixture.repository).markDocumentChunksDeleted(10L, 20L);
        verify(fixture.indexPort).delete(10L, 20L);
    }

    @Test
    void retriesFailedDocumentThroughAfterCommitEvent() {
        Fixture fixture = new Fixture(DocumentStatus.FAILED);
        when(fixture.repository.resetFailedDocument(10L, 20L)).thenReturn(true);

        fixture.service.retryIngestion(10L, 20L);

        verify(fixture.identityApi).requireRole(10L, WorkspaceRole.ADMIN);
        verify(fixture.eventPublisher).publishEvent(new DocumentUploadCompletedEvent(10L, 20L));
    }

    private static final class Fixture {

        private final KnowledgeRepository repository = mock(KnowledgeRepository.class);
        private final IdentityApi identityApi = mock(IdentityApi.class);
        private final DocumentIndexPort indexPort = mock(DocumentIndexPort.class);
        private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        private final KnowledgeQueryService service = new KnowledgeQueryService(
                repository,
                identityApi,
                indexPort,
                eventPublisher
        );

        private Fixture(DocumentStatus status) {
            when(repository.findDocument(10L, 20L)).thenReturn(Optional.of(new DocumentRecord(
                    20L,
                    10L,
                    "document.md",
                    "documents/20",
                    "text/markdown",
                    100L,
                    "hash",
                    status,
                    null,
                    status == DocumentStatus.FAILED ? "failed" : null,
                    1,
                    30L
            )));
        }
    }
}
