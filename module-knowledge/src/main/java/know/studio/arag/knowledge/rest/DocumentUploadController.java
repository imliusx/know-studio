package know.studio.arag.knowledge.rest;

import jakarta.validation.Valid;
import know.studio.arag.knowledge.api.DocumentView;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class DocumentUploadController {

    public static final String CHUNK_HASH_HEADER = "X-Chunk-SHA256";

    private final DocumentUploadService uploadService;
    private final KnowledgeQueryService queryService;

    @PostMapping("/documents/uploads")
    public ApiResponse<UploadInitResult> initiate(
            @PathVariable long workspaceId,
            @Valid @RequestBody InitiateUploadRequest request
    ) {
        return ApiResponse.ok(uploadService.initiate(
                workspaceId,
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
            @PathVariable long workspaceId,
            @PathVariable long sessionId,
            @PathVariable int chunkIndex,
            @RequestHeader(CHUNK_HASH_HEADER) String chunkHash,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(uploadService.uploadChunk(
                workspaceId,
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
            @PathVariable long workspaceId,
            @PathVariable long sessionId
    ) {
        return ApiResponse.ok(uploadService.progress(workspaceId, sessionId));
    }

    @PostMapping("/documents/uploads/{sessionId}/complete")
    public ApiResponse<Map<String, Long>> complete(
            @PathVariable long workspaceId,
            @PathVariable long sessionId
    ) {
        return ApiResponse.ok(Map.of("documentId", uploadService.complete(workspaceId, sessionId)));
    }

    @GetMapping("/documents/{documentId}")
    public ApiResponse<DocumentView> getDocument(
            @PathVariable long workspaceId,
            @PathVariable long documentId
    ) {
        return ApiResponse.ok(queryService.getDocument(workspaceId, documentId));
    }
}
