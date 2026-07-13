package know.studio.common.mq;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

import java.util.List;

/**
 * Reusable declaration for a direct business queue and its dead-letter queue.
 */
public record DeadLetterTopology(
        String exchange,
        String queue,
        String routingKey,
        String deadLetterExchange,
        String deadLetterQueue,
        String deadLetterRoutingKey
) {

    public Declarables declarations() {
        DirectExchange businessExchange = new DirectExchange(exchange, true, false);
        DirectExchange dlx = new DirectExchange(deadLetterExchange, true, false);
        Queue businessQueue = QueueBuilder.durable(queue)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
        Queue dlq = QueueBuilder.durable(deadLetterQueue).build();

        List<Declarable> declarables = List.of(
                businessExchange,
                dlx,
                businessQueue,
                dlq,
                BindingBuilder.bind(businessQueue).to(businessExchange).with(routingKey),
                BindingBuilder.bind(dlq).to(dlx).with(deadLetterRoutingKey)
        );
        return new Declarables(declarables);
    }
}
