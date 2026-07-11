package know.studio.arag.platform.core.mq;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/**
 * Base contract for manually acknowledged, idempotent RabbitMQ consumers.
 * Failed messages are rejected without requeue so the queue's DLX policy can
 * route them to a retry queue or final dead-letter queue.
 */
public abstract class AbstractIdempotentMqConsumer<T> {

    public final void consume(T payload, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId;
        try {
            messageId = requireMessageId(message);
        } catch (Exception exception) {
            channel.basicNack(deliveryTag, false, false);
            throw exception;
        }

        if (isProcessed(messageId)) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            handle(messageId, payload);
            markProcessed(messageId);
        } catch (Exception exception) {
            channel.basicNack(deliveryTag, false, false);
            throw exception;
        }

        channel.basicAck(deliveryTag, false);
    }

    protected abstract boolean isProcessed(String messageId);

    protected abstract void handle(String messageId, T payload) throws Exception;

    protected abstract void markProcessed(String messageId);

    private static String requireMessageId(Message message) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new IOException("RabbitMQ message_id is required for idempotent consumption");
        }
        return messageId;
    }
}
