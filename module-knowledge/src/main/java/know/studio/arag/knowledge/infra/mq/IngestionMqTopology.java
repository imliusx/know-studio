package know.studio.arag.knowledge.infra.mq;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IngestionMqProperties.class)
public class IngestionMqTopology {

    public static final String EXCHANGE = "arag.ingestion.exchange";
    public static final String QUEUE = "arag.ingestion.queue";
    public static final String ROUTING_KEY = "ingestion";
    public static final String RETRY_EXCHANGE = "arag.ingestion.retry.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "arag.ingestion.dlx";
    public static final String DEAD_LETTER_QUEUE = "arag.ingestion.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "ingestion.dead";
    public static final String RETRY_COUNT_HEADER = "x-arag-retry-count";

    @Bean
    Declarables ingestionDeclarables(IngestionMqProperties properties) {
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        DirectExchange retryExchange = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();

        List<Declarable> declarations = new ArrayList<>();
        declarations.add(exchange);
        declarations.add(retryExchange);
        declarations.add(deadLetterExchange);
        declarations.add(queue);
        declarations.add(deadLetterQueue);
        declarations.add(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
        declarations.add(BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY));

        List<Duration> retryDelays = properties.getRetryDelays();
        for (int index = 0; index < retryDelays.size(); index++) {
            int attempt = index + 1;
            Queue retryQueue = QueueBuilder.durable(retryQueue(attempt))
                    .ttl(Math.toIntExact(retryDelays.get(index).toMillis()))
                    .deadLetterExchange(EXCHANGE)
                    .deadLetterRoutingKey(ROUTING_KEY)
                    .build();
            declarations.add(retryQueue);
            declarations.add(BindingBuilder.bind(retryQueue)
                    .to(retryExchange)
                    .with(retryRoutingKey(attempt)));
        }
        return new Declarables(declarations);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    public static String retryQueue(int attempt) {
        return QUEUE + ".retry." + attempt;
    }

    public static String retryRoutingKey(int attempt) {
        return "ingestion.retry." + attempt;
    }
}
