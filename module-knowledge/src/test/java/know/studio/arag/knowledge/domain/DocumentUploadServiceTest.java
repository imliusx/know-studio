package know.studio.arag.knowledge.domain;

import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.knowledge.api.KnowledgeAccessApi;
import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import know.studio.arag.platform.core.exception.ForbiddenException;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    private static final long KNOWLEDGE_BASE_ID = 11L;
    private static final long USER_ID = 22L;
    private static final long SESSION_ID = 33L;
    private static final String CONTENT_HASH = hash("complete document");

    @Mock
    private KnowledgeRepository repository;
    @Mock
    private ObjectStoragePort storage;
    @Mock
    private IdentityApi identityApi;
    @Mock
    private KnowledgeAccessApi knowledgeAccessApi;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(
                repository,
                storage,
                identityApi,
                knowledgeAccessApi,
                idGenerator,
                eventPublisher,
                new UploadPolicy(Duration.ofHours(24))
        );
    }

    @Test
    void returnsReadyDocumentForInstantUpload() {
        stubCurrentUser();
        DocumentRecord ready = document(101L, DocumentStatus.READY);
        when(repository.findReadyDocumentByHash(KNOWLEDGE_BASE_ID, CONTENT_HASH)).thenReturn(Optional.of(ready));

        UploadInitResult result = service.initiate(
                KNOWLEDGE_BASE_ID, "guide.pdf", "application/pdf", 17L, CONTENT_HASH, 1
        );

        assertThat(result.instantUpload()).isTrue();
        assertThat(result.documentId()).isEqualTo(101L);
        assertThat(result.uploadSessionId()).isNull();
        verify(repository, never()).insertUploadSession(any());
    }

    @Test
    void resumesActiveSessionAndReturnsUploadedChunks() {
        stubCurrentUser();
        UploadSession session = session(UploadStatus.UPLOADING, null, 3, 17L, CONTENT_HASH);
        when(repository.findReadyDocumentByHash(KNOWLEDGE_BASE_ID, CONTENT_HASH)).thenReturn(Optional.empty());
        when(repository.findActiveUploadSessionByHash(KNOWLEDGE_BASE_ID, CONTENT_HASH))
                .thenReturn(Optional.of(session));
        when(repository.findUploadChunks(SESSION_ID)).thenReturn(List.of(
                chunk(0, 5L, hash("part-0")),
                chunk(2, 7L, hash("part-2"))
        ));

        UploadInitResult result = service.initiate(
                KNOWLEDGE_BASE_ID, "guide.pdf", "application/pdf", 17L, CONTENT_HASH, 3
        );

        assertThat(result.instantUpload()).isFalse();
        assertThat(result.uploadSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.uploadedChunks()).containsExactly(0, 2);
        verify(repository, never()).insertUploadSession(any());
    }

    @Test
    void acceptsIdempotentDuplicateChunkWithoutWritingStorageAgain() {
        String chunkHash = hash("chunk");
        UploadSession session = session(UploadStatus.UPLOADING, null, 1, 5L, CONTENT_HASH);
        when(repository.findUploadSession(KNOWLEDGE_BASE_ID, SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.findUploadChunks(SESSION_ID)).thenReturn(List.of(chunk(0, 5L, chunkHash)));

        UploadProgress progress = service.uploadChunk(
                KNOWLEDGE_BASE_ID,
                SESSION_ID,
                0,
                5L,
                "text/plain",
                chunkHash,
                () -> new ByteArrayInputStream("chunk".getBytes(StandardCharsets.UTF_8))
        );

        assertThat(progress.uploadedChunks()).containsExactly(0);
        verifyNoInteractions(storage);
        verify(repository, never()).insertUploadChunk(any());
    }

    @Test
    void rejectsConflictingDuplicateChunk() {
        UploadSession session = session(UploadStatus.UPLOADING, null, 1, 5L, CONTENT_HASH);
        when(repository.findUploadSession(KNOWLEDGE_BASE_ID, SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.findUploadChunks(SESSION_ID)).thenReturn(List.of(chunk(0, 5L, hash("other"))));

        assertThatThrownBy(() -> service.uploadChunk(
                KNOWLEDGE_BASE_ID,
                SESSION_ID,
                0,
                5L,
                "text/plain",
                hash("chunk"),
                () -> new ByteArrayInputStream("chunk".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);

        verifyNoInteractions(storage);
    }

    @Test
    void rejectsIncompleteUploadBeforeComposingObject() {
        UploadSession session = session(UploadStatus.UPLOADING, null, 2, 10L, CONTENT_HASH);
        when(repository.findUploadSession(KNOWLEDGE_BASE_ID, SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.findUploadChunks(SESSION_ID)).thenReturn(List.of(chunk(0, 5L, hash("part"))));

        assertThatThrownBy(() -> service.complete(KNOWLEDGE_BASE_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);

        verifyNoInteractions(storage);
        verify(repository, never()).insertDocument(any());
    }

    @Test
    void deletesComposedObjectWhenFullFileHashDoesNotMatch() {
        byte[] actualContent = "wrong content".getBytes(StandardCharsets.UTF_8);
        UploadSession session = session(UploadStatus.UPLOADING, null, 1, actualContent.length, CONTENT_HASH);
        UploadChunk chunk = chunk(0, actualContent.length, hash("wrong content"));
        when(repository.findUploadSession(KNOWLEDGE_BASE_ID, SESSION_ID)).thenReturn(Optional.of(session));
        when(repository.findUploadChunks(SESSION_ID)).thenReturn(List.of(chunk));
        when(idGenerator.nextId()).thenReturn(501L);
        when(storage.open("documents/11/501/guide.pdf")).thenReturn(new ByteArrayInputStream(actualContent));

        assertThatThrownBy(() -> service.complete(KNOWLEDGE_BASE_ID, SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SHA-256");

        verify(storage).delete(List.of("documents/11/501/guide.pdf"));
        verify(repository, never()).insertDocument(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deniesUnauthorizedKnowledgeBaseAccessBeforeReadingUploadState() {
        doThrow(new ForbiddenException()).when(knowledgeAccessApi).requireManageable(KNOWLEDGE_BASE_ID);

        assertThatThrownBy(() -> service.progress(KNOWLEDGE_BASE_ID, SESSION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).findUploadSession(anyLong(), anyLong());
    }

    private static UploadSession session(
            UploadStatus status,
            Long documentId,
            int totalChunks,
            long fileSize,
            String contentHash
    ) {
        return new UploadSession(
                SESSION_ID,
                KNOWLEDGE_BASE_ID,
                "guide.pdf",
                "application/pdf",
                fileSize,
                contentHash,
                totalChunks,
                status,
                USER_ID,
                documentId,
                Instant.now().plusSeconds(3_600)
        );
    }

    private static UploadChunk chunk(int index, long size, String hash) {
        return new UploadChunk(100L + index, SESSION_ID, index, "chunks/" + index, size, hash);
    }

    private static DocumentRecord document(long id, DocumentStatus status) {
        return new DocumentRecord(
                id,
                KNOWLEDGE_BASE_ID,
                "guide.pdf",
                "documents/guide.pdf",
                "application/pdf",
                17L,
                CONTENT_HASH,
                status,
                null,
                null,
                0,
                USER_ID
        );
    }

    private static String hash(String value) {
        return Hashing.sha256(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
    }

    private void stubCurrentUser() {
        when(identityApi.currentUser()).thenReturn(
                new CurrentIdentity(USER_ID, "user@example.com", "User", SystemRole.USER)
        );
    }
}
