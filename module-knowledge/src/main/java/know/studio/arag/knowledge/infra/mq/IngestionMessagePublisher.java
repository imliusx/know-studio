package know.studio.arag.knowledge.infra.mq;

import know.studio.arag.knowledge.domain.DocumentUploadCompletedEvent;
import know.studio.arag.platform.core.mq.MqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionMessagePublisher {

    private final MqPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploadCompleted(DocumentUploadCompletedEvent event) {
        publish(event.knowledgeBaseId(), event.documentId());
    }

    public void publish(long knowledgeBaseId, long documentId) {
        String messageId = publisher.sendConfirmed(
                IngestionMqTopology.EXCHANGE,
                IngestionMqTopology.ROUTING_KEY,
                new IngestionMessage(knowledgeBaseId, documentId)
        );
        log.info(
                "Published ingestion task knowledgeBaseId={} documentId={} messageId={}",
                knowledgeBaseId,
                documentId,
                messageId
        );
    }
}
