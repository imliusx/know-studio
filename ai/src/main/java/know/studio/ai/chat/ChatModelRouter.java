package know.studio.ai.chat;

import know.studio.ai.provider.AiCapability;
import know.studio.ai.observability.AiObservation;
import know.studio.ai.observability.AiObservationSink;
import know.studio.ai.provider.AiProvider;
import know.studio.ai.provider.ChatChunk;
import know.studio.ai.provider.ChatRequest;
import know.studio.ai.routing.AiRoutingException;
import know.studio.ai.routing.ProviderCircuitBreaker;
import know.studio.ai.routing.ProviderCircuitBreakerRegistry;
import know.studio.common.trace.TraceContext;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/** Routes streaming chat with circuit breaking and first-chunk failover. */
public class ChatModelRouter {

    private final List<AiProvider> providers;
    private final ProviderCircuitBreakerRegistry breakers;
    private final AiObservationSink observationSink;

    public ChatModelRouter(List<AiProvider> providers, ProviderCircuitBreakerRegistry breakers) {
        this(providers, breakers, AiObservationSink.NOOP);
    }

    public ChatModelRouter(
            List<AiProvider> providers,
            ProviderCircuitBreakerRegistry breakers,
            AiObservationSink observationSink
    ) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(AiProvider::priority))
                .toList();
        this.breakers = breakers;
        this.observationSink = observationSink;
    }

    public Flux<ChatChunk> stream(ChatRequest request) {
        List<AiProvider> candidates = providers.stream()
                .filter(provider -> supportsRequest(provider, request))
                .toList();
        return route(candidates, 0, request);
    }

    private static boolean supportsRequest(AiProvider provider, ChatRequest request) {
        if (!request.reasoning()) {
            return provider.supports(AiCapability.CHAT);
        }
        return provider.supports(AiCapability.REASONING) || provider.supports(AiCapability.CHAT);
    }

    private Flux<ChatChunk> route(List<AiProvider> candidates, int index, ChatRequest request) {
        if (index >= candidates.size()) {
            return Flux.error(new AiRoutingException("No healthy AI provider is available"));
        }

        AiProvider provider = candidates.get(index);
        ProviderCircuitBreaker breaker = breakers.get(provider.id());
        if (!breaker.tryAcquirePermission()) {
            return route(candidates, index + 1, request);
        }

        return observedProviderStream(provider, request)
                .switchOnFirst((signal, stream) -> {
                    if (signal.hasValue()) {
                        breaker.onSuccess();
                        return stream.doOnError(ignored -> breaker.onFailure());
                    }
                    breaker.onFailure();
                    return route(candidates, index + 1, request);
                });
    }

    private Flux<ChatChunk> observedProviderStream(AiProvider provider, ChatRequest request) {
        return Flux.defer(() -> {
            long start = System.nanoTime();
            String traceId = TraceContext.current();
            java.util.concurrent.atomic.AtomicLong outputCharacters = new java.util.concurrent.atomic.AtomicLong();
            return provider.streamChat(request)
                    .doOnNext(chunk -> outputCharacters.addAndGet(chunk.content().length()))
                    .doOnComplete(() -> observationSink.record(new AiObservation(
                            provider.id(),
                            request.reasoning(),
                            true,
                            elapsedMillis(start),
                            outputCharacters.get(),
                            "",
                            traceId == null ? "" : traceId,
                            request.profile().name(),
                            request.promptVersion()
                    )))
                    .doOnError(exception -> observationSink.record(new AiObservation(
                            provider.id(),
                            request.reasoning(),
                            false,
                            elapsedMillis(start),
                            outputCharacters.get(),
                            exception.getClass().getSimpleName(),
                            traceId == null ? "" : traceId,
                            request.profile().name(),
                            request.promptVersion()
                    )));
        });
    }

    private static long elapsedMillis(long start) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
