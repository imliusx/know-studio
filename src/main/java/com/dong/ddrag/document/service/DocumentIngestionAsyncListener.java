package com.dong.ddrag.document.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档入库异步监听器（对应走读指南「链路 A」的 ETL 触发点）。
 *
 * <p>⭐ 两个注解组合是关键设计：
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)}：只在"文档元数据事务提交成功后"才触发，
 *       避免事务回滚了却照样跑 ETL，也保证 ETL 能读到已落库的 document 记录。</li>
 *   <li>{@code @Async}：在独立线程池执行，上传接口不阻塞——大文件解析/向量化很慢，绝不能卡住 HTTP 响应。</li>
 * </ul>
 * 这是"上传与 ETL 解耦"的真正落点：DocumentService 只发事件，这里异步消费跑 ETL。
 */
@Component
public class DocumentIngestionAsyncListener {

    private final DocumentIngestionAsyncService documentIngestionAsyncService;

    public DocumentIngestionAsyncListener(DocumentIngestionAsyncService documentIngestionAsyncService) {
        this.documentIngestionAsyncService = documentIngestionAsyncService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DocumentIngestionRequestedEvent event) {
        documentIngestionAsyncService.ingestDocument(event.documentId(), event.groupId());
    }
}
