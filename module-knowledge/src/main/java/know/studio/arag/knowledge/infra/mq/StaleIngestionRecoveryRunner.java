package know.studio.arag.knowledge.infra.mq;

import know.studio.arag.knowledge.domain.DocumentRecord;
import know.studio.arag.knowledge.domain.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleIngestionRecoveryRunner {

    private static final int RECOVERY_BATCH_SIZE = 100;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(15);

    private final KnowledgeRepository repository;
    private final IngestionMessagePublisher publisher;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recover();
    }

    @Scheduled(fixedDelayString = "${arag.ingestion.recovery.scan-interval:1m}")
    public void recoverScheduled() {
        recover();
    }

    void recover() {
        List<DocumentRecord> recovered = repository.recoverStaleProcessing(
                Instant.now().minus(PROCESSING_TIMEOUT),
                RECOVERY_BATCH_SIZE
        );
        for (DocumentRecord document : recovered) {
            try {
                publisher.publish(document.knowledgeBaseId(), document.id());
            } catch (RuntimeException exception) {
                repository.deferRecoveredDocument(
                        document.knowledgeBaseId(),
                        document.id(),
                        "Recovery republish failed: " + exception.getMessage()
                );
                log.error(
                        "Failed to republish recovered ingestion knowledgeBaseId={} documentId={}",
                        document.knowledgeBaseId(),
                        document.id(),
                        exception
                );
            }
        }
        if (!recovered.isEmpty()) {
            log.warn("Recovered stale ingestion tasks count={}", recovered.size());
        }
    }
}
