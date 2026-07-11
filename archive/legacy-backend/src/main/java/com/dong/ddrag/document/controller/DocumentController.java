package com.dong.ddrag.document.controller;

import com.dong.ddrag.common.api.ApiResponse;
import com.dong.ddrag.document.model.dto.DocumentQuery;
import com.dong.ddrag.document.model.dto.UploadDocumentRequest;
import com.dong.ddrag.document.model.dto.UploadInitRequest;
import com.dong.ddrag.document.model.dto.UploadChunkRequest;
import com.dong.ddrag.document.model.vo.DocumentListItemVO;
import com.dong.ddrag.document.model.vo.DocumentPreviewVO;
import com.dong.ddrag.document.model.vo.UploadInitResponse;
import com.dong.ddrag.document.model.vo.UploadStatusResponse;
import com.dong.ddrag.document.service.DocumentService;
import com.dong.ddrag.document.service.DocumentUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文档接口入口（对应走读指南「链路 A」的起点）。提供两种上传方式：
 * <ul>
 *   <li><b>分片上传</b>：init → chunks(可断点续传/并行) → complete，支持大文件、秒传、续传
 *       <pre> POST /upload/init → POST /upload/chunks → POST /upload/{id}/complete </pre></li>
 *   <li><b>整文件直传</b>：POST /upload（小文件用，一次性 multipart 上传）</li>
 * </ul>
 * 上传完成后状态置 PROCESSING，ETL(解析→切片→向量化)由异步事件触发，见 DocumentService。
 * 另含文档列表/删除/重试入库/预览等管理接口。
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentUploadService documentUploadService;

    public DocumentController(DocumentService documentService, DocumentUploadService documentUploadService) {
        this.documentService = documentService;
        this.documentUploadService = documentUploadService;
    }

    /** 分片上传①：初始化。带 fileHash 做"秒传"判断，已存在 READY 文档则直接复用；另有续传会话则返回已传分片。 */
    @PostMapping(path = "/upload/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UploadInitResponse> initUpload(
            @RequestBody UploadInitRequest uploadRequest,
            HttpServletRequest request
    ) {
        return ApiResponse.success(documentUploadService.initUpload(request, uploadRequest));
    }

    /** 分片上传②：上传单个分片。每片独立写 MinIO + upsert 记录，支持乱序/重传(幂等)。 */
    @PostMapping(path = "/upload/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadStatusResponse> uploadChunk(
            @ModelAttribute UploadChunkRequest uploadRequest,
            HttpServletRequest request
    ) {
        documentUploadService.uploadChunk(request, uploadRequest);
        return ApiResponse.success(documentUploadService.getUploadStatus(request, uploadRequest.uploadId()));
    }

    /** 分片上传查询：查已传分片清单（断点续传时前端据此跳过已传片）。 */
    @GetMapping("/upload/{uploadId}")
    public ApiResponse<UploadStatusResponse> getUploadStatus(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(documentUploadService.getUploadStatus(request, uploadId));
    }

    /** 分片上传③：完成。校验分片齐全 → MinIO 合并成完整文件 → 落库 → 触发异步 ETL。 */
    @PostMapping("/upload/{uploadId}/complete")
    public ApiResponse<Long> completeUpload(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(documentUploadService.completeUpload(request, uploadId));
    }

    /**
     * 整文件直传（小文件用）。一次性 multipart 上传，内部复用同一套落库 + 异步 ETL 触发。
     * 与分片上传三步式区别：无需 init/chunks/complete 协议，适合 < 10MB 的小文件。
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> uploadDocument(
            @ModelAttribute UploadDocumentRequest uploadRequest,
            HttpServletRequest request
    ) {
        return ApiResponse.success(documentService.uploadDocument(request, uploadRequest));
    }

    @GetMapping
    public List<DocumentListItemVO> listDocuments(
            @ModelAttribute DocumentQuery query,
            HttpServletRequest request
    ) {
        return documentService.listDocuments(request, query);
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long documentId,
            @RequestParam Long groupId,
            HttpServletRequest request
    ) {
        documentService.softDeleteDocument(request, groupId, documentId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{documentId}/retry-ingestion")
    public ApiResponse<Void> retryDocumentIngestion(
            @PathVariable Long documentId,
            @RequestParam Long groupId,
            HttpServletRequest request
    ) {
        documentService.retryFailedDocumentIngestion(request, groupId, documentId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{documentId}/preview")
    public DocumentPreviewVO previewDocument(
            @PathVariable Long documentId,
            @RequestParam Long groupId,
            HttpServletRequest request
    ) {
        return documentService.previewDocument(request, groupId, documentId);
    }
}
