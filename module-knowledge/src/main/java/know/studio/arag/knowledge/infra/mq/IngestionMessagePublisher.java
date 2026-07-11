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
        publish(event.workspaceId(), event.documentId());
    }

    public void publish(long workspaceId, long documentId) {
        String messageId = publisher.sendConfirmed(
                IngestionMqTopology.EXCHANGE,
                IngestionMqTopology.ROUTING_KEY,
                new IngestionMessage(workspaceId, documentId)
        );
        log.info(
                "Published ingestion task workspaceId={} documentId={} messageId={}",
                workspaceId,
                documentId,
                messageId
        );
    }
}
