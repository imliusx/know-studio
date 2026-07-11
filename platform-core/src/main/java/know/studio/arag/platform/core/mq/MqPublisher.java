package know.studio.arag.platform.core.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RabbitMQ 发送封装。统一走 publisher-confirm：发送后异步确认 broker 是否收妥，
 * 未确认则告警（业务可据此补偿）。各业务模块（如 knowledge 入库）通过它投递消息。
 *
 * <p>需配合 {@code spring.rabbitmq.publisher-confirm-type=correlated}。
 */
@Component
public class MqPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqPublisher.class);

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
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, new CorrelationData(correlationId));
        log.debug("[mq] sent exchange={} rk={} id={}", exchange, routingKey, correlationId);
        return correlationId;
    }
}
