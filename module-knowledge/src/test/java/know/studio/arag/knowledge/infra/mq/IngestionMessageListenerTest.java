package know.studio.arag.knowledge.infra.mq;

import com.rabbitmq.client.Channel;
import know.studio.arag.knowledge.domain.IngestionTaskHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionMessageListenerTest {

    private static final long DELIVERY_TAG = 91L;
    private static final IngestionMessage PAYLOAD = new IngestionMessage(11L, 22L);

    @Mock
    private IngestionTaskHandler taskHandler;
    @Mock
    private IngestionRetryDispatcher retryDispatcher;
    @Mock
    private Channel channel;

    private IngestionMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new IngestionMessageListener(taskHandler, retryDispatcher);
    }

    @Test
    void acknowledgesSuccessfullyProcessedTask() throws Exception {
        Message message = message(0);

        listener.consume(PAYLOAD, message, channel);

        verify(taskHandler).process(11L, 22L);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(retryDispatcher, never()).dispatch(PAYLOAD, 0, "message-1");
    }

    @Test
    void acknowledgesOriginalMessageAfterRetryWasConfirmed() throws Exception {
        Message message = message(2);
        doThrow(new IllegalStateException("processing failed")).when(taskHandler).process(11L, 22L);

        listener.consume(PAYLOAD, message, channel);

        verify(retryDispatcher).dispatch(PAYLOAD, 2, "message-1");
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(DELIVERY_TAG, false, true);
    }

    @Test
    void requeuesOriginalMessageWhenRetryDispatchFails() throws Exception {
        Message message = message(1);
        doThrow(new IllegalStateException("processing failed")).when(taskHandler).process(11L, 22L);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(retryDispatcher)
                .dispatch(PAYLOAD, 1, "message-1");

        listener.consume(PAYLOAD, message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, true);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }

    private static Message message(int retryCount) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(DELIVERY_TAG);
        properties.setMessageId("message-1");
        properties.setHeader(IngestionMqTopology.RETRY_COUNT_HEADER, retryCount);
        return new Message(new byte[0], properties);
    }
}
