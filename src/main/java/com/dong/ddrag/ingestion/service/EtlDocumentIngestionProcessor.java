package com.dong.ddrag.ingestion.service;

import com.dong.ddrag.common.exception.BusinessException;
import com.dong.ddrag.document.mapper.DocumentMapper;
import com.dong.ddrag.document.model.entity.DocumentEntity;
import com.dong.ddrag.ingestion.chunk.ChunkService;
import com.dong.ddrag.ingestion.model.entity.DocumentChunkEntity;
import com.dong.ddrag.ingestion.parser.factory.DocumentParserFactory;
import com.dong.ddrag.ingestion.reader.StoredObjectDocumentReader;
import com.dong.ddrag.ingestion.transformer.StructureAwareChunkTransformer;
import com.dong.ddrag.ingestion.transformer.TextCleanupTransformer;
import com.dong.ddrag.ingestion.vector.VectorIngestionService;
import com.dong.ddrag.storage.service.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档入库的"总指挥"——把一份上传的文档从原始文件加工成"可被 RAG 检索"的状态。
 *
 * <p>对应走读指南「链路 A：文档入库」。{@code process()} 方法按固定顺序执行完整 ETL 管道：
 * <pre>
 *   读取原文(MinIO) → 解析 → 文本清洗 → 切片 → chunk 落库(PostgreSQL)
 *      → 向量写入(pgvector) → [关键词索引由 ingestion 链路另写 ES]
 * </pre>
 * 每一步的产物都会进入不同存储，共同构成"可检索"的基础：
 * <ul>
 *   <li>原文 → MinIO（对象存储）</li>
 *   <li>切片正文 → document_chunks 表（PostgreSQL）</li>
 *   <li>切片向量 → pgvector（语义检索用）</li>
 *   <li>切片关键词索引 → Elasticsearch（关键词检索用）</li>
 * </ul>
 */
public class EtlDocumentIngestionProcessor implements DocumentIngestionProcessor {

    private static final Logger log = LoggerFactory.getLogger(EtlDocumentIngestionProcessor.class);
    private static final String DOCUMENT_NOT_FOUND_MESSAGE = "待入库文档不存在";
    private static final int PREVIEW_MAX_LENGTH = 200;

    private final DocumentMapper documentMapper;
    private final ObjectStorageService storageService;
    private final DocumentParserFactory parserFactory;
    private final TextCleanupTransformer textCleanupTransformer;
    private final StructureAwareChunkTransformer chunkTransformer;
    private final ChunkService chunkService;
    private final VectorIngestionService vectorService;

    public EtlDocumentIngestionProcessor(
            DocumentMapper documentMapper,
            ObjectStorageService storageService,
            DocumentParserFactory parserFactory,
            TextCleanupTransformer textCleanupTransformer,
            StructureAwareChunkTransformer chunkTransformer,
            ChunkService chunkService,
            VectorIngestionService vectorService
    ) {
        this.documentMapper = documentMapper;
        this.storageService = storageService;
        this.parserFactory = parserFactory;
        this.textCleanupTransformer = textCleanupTransformer;
        this.chunkTransformer = chunkTransformer;
        this.chunkService = chunkService;
        this.vectorService = vectorService;
    }

    @Override
    public void process(Long documentId, Long groupId) {
        log.info("开始执行文档ETL: documentId={}, groupId={}", documentId, groupId);

        // ① 读取文档记录（含 MinIO 里的对象路径）。groupId 在这里也参与查询，
        //    既是定位文档，也是一道权限校验：非本组文档直接抛"不存在"。
        DocumentEntity documentEntity = findDocument(documentId, groupId);

        // ② 从 MinIO 把原始文件读出来，按文件类型(pdf/docx/…)解析成纯文本 Document 列表。
        StoredObjectDocumentReader reader =
                new StoredObjectDocumentReader(storageService, parserFactory, documentEntity);
        List<Document> rawDocuments = reader.get();
        log.info("文档读取完成: documentId={}, groupId={}, rawDocuments={}",
                documentId, groupId, rawDocuments.size());

        // ③ 文本清洗：去乱码、规范化空白与不可见字符，提升后续切片与检索质量。
        List<Document> cleanedDocuments = textCleanupTransformer.apply(rawDocuments);
        log.info("文本清洗完成: documentId={}, groupId={}, cleanedDocuments={}",
                documentId, groupId, cleanedDocuments.size());

        // ④ 把清洗后的前若干字存为"预览文本"，供文档列表/预览接口快速展示，无需再读全文。
        persistPreviewText(documentId, groupId, cleanedDocuments);

        // ⑤ 切片(Chunking)：把长文本切成约 500 token/片、相邻片有 80 token 重叠的小片段。
        //    StructureAwareChunkTransformer 会尽量沿段落/标题边界切，避免把一句话拦腰切断。
        List<Document> chunkDocuments = chunkTransformer.apply(cleanedDocuments);
        log.info("文档切片完成: documentId={}, groupId={}, chunkDocuments={}",
                documentId, groupId, chunkDocuments.size());

        // ⑥ 切片落库：写入 document_chunks 表，并拿到带主键的实体（后续向量化要用这些 id）。
        List<DocumentChunkEntity> chunks =
                chunkService.saveChunkDocuments(documentId, groupId, chunkDocuments);
        log.info("切片落库完成: documentId={}, groupId={}, persistedChunks={}",
                documentId, groupId, chunks.size());

        // ⑦ 向量写入：对每个 chunk 调 Ollama 计算 embedding，分批写入 pgvector，
        //    这是"语义检索(按意思找)"能够成立的基础。关键词索引(ES)由 ingestion 链路另一处完成。
        vectorService.ingestChunks(chunks);
        log.info("向量写入完成: documentId={}, groupId={}, vectorChunks={}",
                documentId, groupId, chunks.size());
    }

    private DocumentEntity findDocument(Long documentId, Long groupId) {
        DocumentEntity documentEntity = documentMapper.selectByIdAndGroupId(documentId, groupId);
        if (documentEntity == null) {
            throw new BusinessException(DOCUMENT_NOT_FOUND_MESSAGE);
        }
        return documentEntity;
    }

    private void persistPreviewText(Long documentId, Long groupId, List<Document> cleanedDocuments) {
        String previewText = cleanedDocuments.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .map(String::trim)
                .map(this::truncatePreviewText)
                .orElse(null);
        int updated = documentMapper.updatePreviewText(documentId, groupId, previewText);
        if (updated == 0) {
            throw new BusinessException("文档预览写入失败");
        }
    }

    private String truncatePreviewText(String previewText) {
        if (previewText.length() <= PREVIEW_MAX_LENGTH) {
            return previewText;
        }
        return previewText.substring(0, PREVIEW_MAX_LENGTH);
    }
}
