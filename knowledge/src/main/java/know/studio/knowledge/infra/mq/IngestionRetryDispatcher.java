package know.studio.knowledge.infra.mq;

import know.studio.common.mq.MqPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class IngestionRetryDispatcher {

    private final MqPublisher publisher;
    private final IngestionMqProperties properties;

    public void dispatch(IngestionMessage payload, int retryCount, String messageId) {
        int nextRetryCount = retryCount + 1;
        Map<String, Object> headers = Map.of(IngestionMqTopology.RETRY_COUNT_HEADER, nextRetryCount);
        if (retryCount < properties.getRetryDelays().size()) {
            publisher.sendConfirmed(
                    IngestionMqTopology.RETRY_EXCHANGE,
                    IngestionMqTopology.retryRoutingKey(nextRetryCount),
                    payload,
                    headers,
                    messageId
            );
            return;
        }
        publisher.sendConfirmed(
                IngestionMqTopology.DEAD_LETTER_EXCHANGE,
                IngestionMqTopology.DEAD_LETTER_ROUTING_KEY,
                payload,
                headers,
                messageId
        );
    }
}
