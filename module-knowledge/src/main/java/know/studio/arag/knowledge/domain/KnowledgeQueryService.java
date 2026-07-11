package know.studio.arag.knowledge.domain;

import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.knowledge.api.DocumentView;
import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.knowledge.api.KnowledgeApi;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService implements KnowledgeApi {

    private final KnowledgeRepository repository;
    private final IdentityApi identityApi;
    private final DocumentIndexPort indexPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DocumentView getDocument(long workspaceId, long documentId) {
        identityApi.requireWorkspaceReadable(workspaceId);
        DocumentRecord document = repository.findDocument(workspaceId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        return toView(document);
    }

    @Override
    public List<DocumentView> listDocuments(long workspaceId, DocumentStatus status, String fileName) {
        identityApi.requireWorkspaceReadable(workspaceId);
        return repository.findDocuments(workspaceId, status, normalize(fileName)).stream()
                .map(KnowledgeQueryService::toView)
                .toList();
    }

    @Override
    @Transactional
    public void deleteDocument(long workspaceId, long documentId) {
        identityApi.requireRole(workspaceId, WorkspaceRole.ADMIN);
        repository.findDocument(workspaceId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!repository.markDocumentDeleted(workspaceId, documentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "文档当前状态不可删除");
        }
        repository.markDocumentChunksDeleted(workspaceId, documentId);
        indexPort.delete(workspaceId, documentId);
    }

    @Override
    @Transactional
    public void retryIngestion(long workspaceId, long documentId) {
        identityApi.requireRole(workspaceId, WorkspaceRole.ADMIN);
        repository.findDocument(workspaceId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!repository.resetFailedDocument(workspaceId, documentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅失败文档可重试入库");
        }
        eventPublisher.publishEvent(new DocumentUploadCompletedEvent(workspaceId, documentId));
    }

    private static DocumentView toView(DocumentRecord document) {
        return new DocumentView(
                document.id(),
                document.workspaceId(),
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
