package know.studio.arag.knowledge.rest;

import jakarta.validation.Valid;
import know.studio.arag.knowledge.api.DocumentView;
import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.knowledge.domain.DocumentUploadService;
import know.studio.arag.knowledge.domain.KnowledgeQueryService;
import know.studio.arag.knowledge.domain.UploadInitResult;
import know.studio.arag.knowledge.domain.UploadProgress;
import know.studio.arag.platform.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}")
@RequiredArgsConstructor
public class DocumentUploadController {

    public static final String CHUNK_HASH_HEADER = "X-Chunk-SHA256";

    private final DocumentUploadService uploadService;
    private final KnowledgeQueryService queryService;

    @PostMapping("/documents/uploads")
    public ApiResponse<UploadInitResult> initiate(
            @PathVariable long knowledgeBaseId,
            @Valid @RequestBody InitiateUploadRequest request
    ) {
        return ApiResponse.ok(uploadService.initiate(
                knowledgeBaseId,
                request.fileName(),
                request.contentType(),
                request.fileSize(),
                request.contentHash(),
                request.totalChunks()
        ));
    }

    @PutMapping(
            value = "/documents/uploads/{sessionId}/chunks/{chunkIndex}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UploadProgress> uploadChunk(
            @PathVariable long knowledgeBaseId,
            @PathVariable long sessionId,
            @PathVariable int chunkIndex,
            @RequestHeader(CHUNK_HASH_HEADER) String chunkHash,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(uploadService.uploadChunk(
                knowledgeBaseId,
                sessionId,
                chunkIndex,
                file.getSize(),
                file.getContentType(),
                chunkHash,
                file::getInputStream
        ));
    }

    @GetMapping("/documents/uploads/{sessionId}")
    public ApiResponse<UploadProgress> progress(
            @PathVariable long knowledgeBaseId,
            @PathVariable long sessionId
    ) {
        return ApiResponse.ok(uploadService.progress(knowledgeBaseId, sessionId));
    }

    @PostMapping("/documents/uploads/{sessionId}/complete")
    public ApiResponse<Map<String, Long>> complete(
            @PathVariable long knowledgeBaseId,
            @PathVariable long sessionId
    ) {
        return ApiResponse.ok(Map.of("documentId", uploadService.complete(knowledgeBaseId, sessionId)));
    }

    @GetMapping("/documents/{documentId}")
    public ApiResponse<DocumentView> getDocument(
            @PathVariable long knowledgeBaseId,
            @PathVariable long documentId
    ) {
        return ApiResponse.ok(queryService.getDocument(knowledgeBaseId, documentId));
    }

    @GetMapping("/documents")
    public ApiResponse<List<DocumentView>> listDocuments(
            @PathVariable long knowledgeBaseId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String fileName
    ) {
        return ApiResponse.ok(queryService.listDocuments(knowledgeBaseId, status, fileName));
    }

    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable long knowledgeBaseId,
            @PathVariable long documentId
    ) {
        queryService.deleteDocument(knowledgeBaseId, documentId);
        return ApiResponse.ok();
    }

    @PostMapping("/documents/{documentId}/retry-ingestion")
    public ApiResponse<Void> retryIngestion(
            @PathVariable long knowledgeBaseId,
            @PathVariable long documentId
    ) {
        queryService.retryIngestion(knowledgeBaseId, documentId);
        return ApiResponse.ok();
    }
}
