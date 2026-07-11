package know.studio.arag.knowledge.infra.mq;

import know.studio.arag.platform.core.mq.MqPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionRetryDispatcherTest {

    private static final IngestionMessage PAYLOAD = new IngestionMessage(11L, 22L);

    @Mock
    private MqPublisher publisher;

    private IngestionRetryDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        IngestionMqProperties properties = new IngestionMqProperties();
        properties.setRetryDelays(List.of(Duration.ofSeconds(5), Duration.ofSeconds(30)));
        dispatcher = new IngestionRetryDispatcher(publisher, properties);
    }

    @Test
    void dispatchesToNextRetryQueue() {
        dispatcher.dispatch(PAYLOAD, 0, "message-1");

        verify(publisher).sendConfirmed(
                IngestionMqTopology.RETRY_EXCHANGE,
                IngestionMqTopology.retryRoutingKey(1),
                PAYLOAD,
                Map.of(IngestionMqTopology.RETRY_COUNT_HEADER, 1),
                "message-1"
        );
    }

    @Test
    void dispatchesToDeadLetterQueueAfterRetriesAreExhausted() {
        dispatcher.dispatch(PAYLOAD, 2, "message-1");

        verify(publisher).sendConfirmed(
                IngestionMqTopology.DEAD_LETTER_EXCHANGE,
                IngestionMqTopology.DEAD_LETTER_ROUTING_KEY,
                PAYLOAD,
                Map.of(IngestionMqTopology.RETRY_COUNT_HEADER, 3),
                "message-1"
        );
    }
}
