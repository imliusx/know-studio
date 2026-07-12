package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.knowledge.api.KnowledgeAccessApi;
import know.studio.arag.knowledge.api.KnowledgeBasePermission;
import know.studio.arag.platform.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeQueryServiceTest {

    @Test
    void deletesDocumentIndexesAfterAdminAuthorization() {
        Fixture fixture = new Fixture(DocumentStatus.READY);
        when(fixture.repository.markDocumentDeleted(10L, 20L)).thenReturn(true);

        fixture.service.deleteDocument(10L, 20L);

        verify(fixture.knowledgeAccessApi).requireManageable(10L);
        verify(fixture.repository).markDocumentChunksDeleted(10L, 20L);
        verify(fixture.indexPort).delete(10L, 20L);
    }

    @Test
    void retriesFailedDocumentThroughAfterCommitEvent() {
        Fixture fixture = new Fixture(DocumentStatus.FAILED);
        when(fixture.repository.resetFailedDocument(10L, 20L)).thenReturn(true);

        fixture.service.retryIngestion(10L, 20L);

        verify(fixture.knowledgeAccessApi).requireManageable(10L);
        verify(fixture.eventPublisher).publishEvent(new DocumentUploadCompletedEvent(10L, 20L));
    }

    @Test
    void opensDocumentContentOnlyAfterReadAuthorization() throws Exception {
        Fixture fixture = new Fixture(DocumentStatus.READY);
        when(fixture.knowledgeAccessApi.requireReadable(10L)).thenReturn(KnowledgeBasePermission.READ);
        when(fixture.objectStoragePort.open("documents/20"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        DocumentContent content = fixture.service.openDocumentContent(10L, 20L);

        verify(fixture.knowledgeAccessApi).requireReadable(10L);
        verify(fixture.repository).findDocument(10L, 20L);
        verify(fixture.objectStoragePort).open("documents/20");
        assertThat(content.fileName()).isEqualTo("document.md");
        assertThat(content.inputStream().readAllBytes()).isEqualTo("content".getBytes());
    }

    @Test
    void readPermissionCannotOpenNonReadyDocument() {
        Fixture fixture = new Fixture(DocumentStatus.FAILED);
        when(fixture.knowledgeAccessApi.requireReadable(10L)).thenReturn(KnowledgeBasePermission.READ);

        assertThatThrownBy(() -> fixture.service.openDocumentContent(10L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文档不存在");
        verify(fixture.objectStoragePort, org.mockito.Mockito.never()).open("documents/20");
    }

    private static final class Fixture {

        private final KnowledgeRepository repository = mock(KnowledgeRepository.class);
        private final KnowledgeAccessApi knowledgeAccessApi = mock(KnowledgeAccessApi.class);
        private final DocumentIndexPort indexPort = mock(DocumentIndexPort.class);
        private final ObjectStoragePort objectStoragePort = mock(ObjectStoragePort.class);
        private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        private final KnowledgeQueryService service = new KnowledgeQueryService(
                repository,
                knowledgeAccessApi,
                indexPort,
                objectStoragePort,
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
