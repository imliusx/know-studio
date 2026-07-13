package know.studio.knowledge.infra.mq;

import com.rabbitmq.client.Channel;
import know.studio.knowledge.domain.IngestionTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnBean(IngestionTaskHandler.class)
@RequiredArgsConstructor
@Slf4j
public class IngestionMessageListener {

    private final IngestionTaskHandler taskHandler;
    private final IngestionRetryDispatcher retryDispatcher;

    @RabbitListener(queues = IngestionMqTopology.QUEUE)
    public void consume(IngestionMessage payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        int retryCount = retryCount(message);
        try {
            taskHandler.process(payload.knowledgeBaseId(), payload.documentId());
            channel.basicAck(deliveryTag, false);
            log.info(
                    "Consumed ingestion task knowledgeBaseId={} documentId={} retryCount={}",
                    payload.knowledgeBaseId(),
                    payload.documentId(),
                    retryCount
            );
        } catch (Exception processingException) {
            try {
                retryDispatcher.dispatch(payload, retryCount, messageId);
                channel.basicAck(deliveryTag, false);
                log.warn(
                        "Ingestion task failed and was dispatched for retry knowledgeBaseId={} documentId={} retryCount={}",
                        payload.knowledgeBaseId(),
                        payload.documentId(),
                        retryCount,
                        processingException
                );
            } catch (RuntimeException dispatchException) {
                processingException.addSuppressed(dispatchException);
                channel.basicNack(deliveryTag, false, true);
                log.error(
                        "Failed to dispatch ingestion retry knowledgeBaseId={} documentId={}",
                        payload.knowledgeBaseId(),
                        payload.documentId(),
                        processingException
                );
            }
        }
    }

    private static int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeader(IngestionMqTopology.RETRY_COUNT_HEADER);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String text) {
            try {
                return Math.max(0, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
