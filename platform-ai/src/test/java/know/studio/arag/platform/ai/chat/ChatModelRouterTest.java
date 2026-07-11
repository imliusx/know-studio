package know.studio.arag.platform.ai.chat;

import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.observability.AiObservation;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.ai.routing.CircuitBreakerConfig;
import know.studio.arag.platform.ai.routing.CircuitState;
import know.studio.arag.platform.ai.routing.ProviderCircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelRouterTest {

    @Test
    void fallsBackWhenPrimaryFailsBeforeFirstChunk() {
        TestProvider primary = new TestProvider("primary", 0, Flux.error(new IllegalStateException("down")));
        TestProvider fallback = new TestProvider("fallback", 1, Flux.just(ChatChunk.token("ok")));
        ProviderCircuitBreakerRegistry breakers = registry();
        ChatModelRouter router = new ChatModelRouter(List.of(fallback, primary), breakers);

        StepVerifier.create(router.stream(ChatRequest.chat("system", "question")))
                .expectNext(ChatChunk.token("ok"))
                .verifyComplete();

        assertThat(primary.calls()).isEqualTo(1);
        assertThat(fallback.calls()).isEqualTo(1);
        assertThat(breakers.get("primary").state()).isEqualTo(CircuitState.OPEN);
    }

    @Test
    void doesNotReplayOnAnotherProviderAfterFirstChunk() {
        TestProvider primary = new TestProvider(
                "primary",
                0,
                Flux.concat(Flux.just(ChatChunk.token("partial")), Flux.error(new IllegalStateException("stream failed")))
        );
        TestProvider fallback = new TestProvider("fallback", 1, Flux.just(ChatChunk.token("duplicate")));
        ChatModelRouter router = new ChatModelRouter(List.of(primary, fallback), registry());

        StepVerifier.create(router.stream(ChatRequest.chat("system", "question")))
                .expectNext(ChatChunk.token("partial"))
                .expectErrorMessage("stream failed")
                .verify();

        assertThat(fallback.calls()).isZero();
    }

    @Test
    void recordsProviderObservationWithoutPromptContent() {
        TestProvider provider = new TestProvider("primary", 0, Flux.just(ChatChunk.token("answer")));
        AtomicReference<AiObservation> observation = new AtomicReference<>();
        ChatModelRouter router = new ChatModelRouter(List.of(provider), registry(), observation::set);

        StepVerifier.create(router.stream(ChatRequest.chat("secret-system", "secret-question")))
                .expectNext(ChatChunk.token("answer"))
                .verifyComplete();

        assertThat(observation.get().providerId()).isEqualTo("primary");
        assertThat(observation.get().outputCharacters()).isEqualTo(6);
        assertThat(observation.get().success()).isTrue();
    }

    private static ProviderCircuitBreakerRegistry registry() {
        return new ProviderCircuitBreakerRegistry(new CircuitBreakerConfig(1, Duration.ofSeconds(30)));
    }

    private static final class TestProvider implements AiProvider {

        private final String id;
        private final int priority;
        private final Flux<ChatChunk> response;
        private final AtomicInteger calls = new AtomicInteger();

        private TestProvider(String id, int priority, Flux<ChatChunk> response) {
            this.id = id;
            this.priority = priority;
            this.response = response;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public Set<AiCapability> capabilities() {
            return Set.of(AiCapability.CHAT);
        }

        @Override
        public Flux<ChatChunk> streamChat(ChatRequest request) {
            calls.incrementAndGet();
            return response;
        }

        private int calls() {
            return calls.get();
        }
    }
}
