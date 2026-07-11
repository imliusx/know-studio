package know.studio.arag.knowledge.domain;

import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.knowledge.api.DocumentView;
import know.studio.arag.knowledge.api.KnowledgeApi;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService implements KnowledgeApi {

    private final KnowledgeRepository repository;
    private final IdentityApi identityApi;

    @Override
    public DocumentView getDocument(long workspaceId, long documentId) {
        identityApi.requireWorkspaceReadable(workspaceId);
        DocumentRecord document = repository.findDocument(workspaceId, documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        return new DocumentView(
                document.id(),
                document.workspaceId(),
                document.fileName(),
                document.contentType(),
                document.fileSize(),
                document.contentHash(),
                document.status(),
                document.chunkCount(),
                document.failureReason()
        );
    }
}
