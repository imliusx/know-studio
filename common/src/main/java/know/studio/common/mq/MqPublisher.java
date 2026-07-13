package know.studio.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 发送封装。统一走 publisher-confirm：发送后异步确认 broker 是否收妥，
 * 未确认则告警（业务可据此补偿）。各业务模块（如 knowledge 入库）通过它投递消息。
 *
 * <p>需配合 {@code spring.rabbitmq.publisher-confirm-type=correlated}。
 */
@Component
@Slf4j
public class MqPublisher {

    private static final Duration DEFAULT_CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private final RabbitTemplate rabbitTemplate;

    public MqPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            String id = correlation == null ? "-" : correlation.getId();
            if (ack) {
                log.debug("[mq] confirm OK id={}", id);
            } else {
                log.error("[mq] confirm FAILED id={} cause={}", id, cause);
            }
        });
    }

    /** 发送消息；返回本次投递的关联 ID，便于日志/追踪。 */
    public String send(String exchange, String routingKey, Object payload) {
        String correlationId = UUID.randomUUID().toString().replace("-", "");
        send(exchange, routingKey, payload, Map.of(), correlationId, new CorrelationData(correlationId));
        log.debug("[mq] sent exchange={} rk={} id={}", exchange, routingKey, correlationId);
        return correlationId;
    }

    public String sendConfirmed(String exchange, String routingKey, Object payload) {
        return sendConfirmed(exchange, routingKey, payload, Map.of(), null);
    }

    public String sendConfirmed(
            String exchange,
            String routingKey,
            Object payload,
            Map<String, Object> headers,
            String messageId
    ) {
        String correlationId = messageId == null || messageId.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : messageId;
        CorrelationData correlationData = new CorrelationData(correlationId);
        send(exchange, routingKey, payload, headers, correlationId, correlationData);
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(DEFAULT_CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected message: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned unroutable message: " + correlationData.getReturned().getReplyText()
                );
            }
            log.debug("[mq] confirmed exchange={} rk={} id={}", exchange, routingKey, correlationId);
            return correlationId;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for RabbitMQ confirm", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to confirm RabbitMQ message " + correlationId, exception);
        }
    }

    private void send(
            String exchange,
            String routingKey,
            Object payload,
            Map<String, Object> headers,
            String messageId,
            CorrelationData correlationData
    ) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setMessageId(messageId);
            message.getMessageProperties().setCorrelationId(messageId);
            headers.forEach(message.getMessageProperties()::setHeader);
            return message;
        }, correlationData);
    }
}
