package know.studio.arag.platform.core.mq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractIdempotentMqConsumerTest {

    @Test
    void acknowledgesDuplicateWithoutHandlingAgain() throws Exception {
        RecordingChannel recording = new RecordingChannel();
        TestConsumer consumer = new TestConsumer(true, false);

        consumer.consume("payload", message("message-1", 7L), recording.channel());

        assertThat(consumer.handled).isFalse();
        assertThat(recording.ackTag).isEqualTo(7L);
        assertThat(recording.nackTag).isNull();
    }

    @Test
    void deadLettersFailedMessage() throws Exception {
        RecordingChannel recording = new RecordingChannel();
        TestConsumer consumer = new TestConsumer(false, true);

        assertThatThrownBy(() -> consumer.consume("payload", message("message-2", 9L), recording.channel()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed");

        assertThat(recording.nackTag).isEqualTo(9L);
        assertThat(recording.ackTag).isNull();
    }

    @Test
    void deadLettersMessageWithoutId() {
        RecordingChannel recording = new RecordingChannel();
        TestConsumer consumer = new TestConsumer(false, false);

        assertThatThrownBy(() -> consumer.consume("payload", message(null, 11L), recording.channel()))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("message_id");

        assertThat(recording.nackTag).isEqualTo(11L);
        assertThat(recording.ackTag).isNull();
    }

    private static Message message(String messageId, long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }

    private static final class TestConsumer extends AbstractIdempotentMqConsumer<String> {

        private final boolean processed;
        private final boolean fail;
        private boolean handled;

        private TestConsumer(boolean processed, boolean fail) {
            this.processed = processed;
            this.fail = fail;
        }

        @Override
        protected boolean isProcessed(String messageId) {
            return processed;
        }

        @Override
        protected void handle(String messageId, String payload) {
            handled = true;
            if (fail) {
                throw new IllegalStateException("failed");
            }
        }

        @Override
        protected void markProcessed(String messageId) {
        }
    }

    private static final class RecordingChannel {

        private Long ackTag;
        private Long nackTag;

        private Channel channel() {
            return (Channel) Proxy.newProxyInstance(
                    Channel.class.getClassLoader(),
                    new Class<?>[]{Channel.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("basicAck")) {
                            ackTag = (Long) args[0];
                        } else if (method.getName().equals("basicNack")) {
                            nackTag = (Long) args[0];
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0F;
            }
            if (type == double.class) {
                return 0D;
            }
            if (type == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
