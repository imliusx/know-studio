package know.studio.knowledge.domain;

import know.studio.knowledge.api.KnowledgeAccessApi;
import know.studio.knowledge.api.KnowledgeBasePermission;
import know.studio.knowledge.api.DocumentView;
import know.studio.knowledge.api.DocumentStatus;
import know.studio.knowledge.api.KnowledgeApi;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService implements KnowledgeApi {

    private final KnowledgeRepository repository;
    private final KnowledgeAccessApi knowledgeAccessApi;
    private final DocumentIndexPort indexPort;
    private final ObjectStoragePort objectStoragePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DocumentView getDocument(long knowledgeBaseId, long documentId) {
        KnowledgeBasePermission permission = knowledgeAccessApi.requireReadable(knowledgeBaseId);
        DocumentRecord document = requireVisibleDocument(knowledgeBaseId, documentId, permission);
        return toView(document);
    }

    @Override
    public List<DocumentView> listDocuments(long knowledgeBaseId, DocumentStatus status, String fileName) {
        KnowledgeBasePermission permission = knowledgeAccessApi.requireReadable(knowledgeBaseId);
        DocumentStatus effectiveStatus = permission == KnowledgeBasePermission.READ
                ? DocumentStatus.READY
                : status;
        return repository.findDocuments(knowledgeBaseId, effectiveStatus, normalize(fileName)).stream()
                .map(KnowledgeQueryService::toView)
                .toList();
    }

    public DocumentContent openDocumentContent(long knowledgeBaseId, long documentId) {
        KnowledgeBasePermission permission = knowledgeAccessApi.requireReadable(knowledgeBaseId);
        DocumentRecord document = requireVisibleDocument(knowledgeBaseId, documentId, permission);
        return new DocumentContent(
                document.fileName(),
                document.contentType(),
                document.fileSize(),
                objectStoragePort.open(document.objectKey())
        );
    }

    private DocumentRecord requireVisibleDocument(
            long knowledgeBaseId,
            long documentId,
            KnowledgeBasePermission permission
    ) {
        DocumentRecord document = repository.findDocument(knowledgeBaseId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (document.status() == DocumentStatus.DELETED
                || (permission == KnowledgeBasePermission.READ && document.status() != DocumentStatus.READY)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        return document;
    }

    @Override
    @Transactional
    public void deleteDocument(long knowledgeBaseId, long documentId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        repository.findDocument(knowledgeBaseId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!repository.markDocumentDeleted(knowledgeBaseId, documentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "文档当前状态不可删除");
        }
        repository.markDocumentChunksDeleted(knowledgeBaseId, documentId);
        indexPort.delete(knowledgeBaseId, documentId);
    }

    @Override
    @Transactional
    public void retryIngestion(long knowledgeBaseId, long documentId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        repository.findDocument(knowledgeBaseId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!repository.resetFailedDocument(knowledgeBaseId, documentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅失败文档可重试入库");
        }
        eventPublisher.publishEvent(new DocumentUploadCompletedEvent(knowledgeBaseId, documentId));
    }

    private static DocumentView toView(DocumentRecord document) {
        return new DocumentView(
                document.id(),
                document.knowledgeBaseId(),
                document.fileName(),
                document.contentType(),
                document.fileSize(),
                document.contentHash(),
                document.status(),
                document.chunkCount(),
                document.failureReason(),
                document.previewText()
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
