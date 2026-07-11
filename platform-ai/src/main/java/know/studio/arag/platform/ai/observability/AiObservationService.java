package know.studio.arag.platform.ai.observability;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class AiObservationService implements AiObservationSink, AutoCloseable {

    private final MeterRegistry meterRegistry;
    private final LangfuseProperties properties;
    private final ExecutorService deliveryExecutor = new ThreadPoolExecutor(
            1,
            2,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Thread.ofVirtual().name("langfuse-delivery-", 0).factory(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    @Override
    public void record(AiObservation observation) {
        String status = observation.success() ? "success" : "error";
        meterRegistry.timer(
                        "arag.ai.request",
                        "provider", observation.providerId(),
                        "reasoning", Boolean.toString(observation.reasoning()),
                        "status", status
                )
                .record(observation.latencyMillis(), TimeUnit.MILLISECONDS);
        meterRegistry.summary("arag.ai.output.characters", "provider", observation.providerId())
                .record(observation.outputCharacters());
        if (properties.configured()) {
            deliveryExecutor.execute(() -> sendLangfuse(observation));
        }
    }

    private void sendLangfuse(AiObservation observation) {
        try {
            String eventId = UUID.randomUUID().toString();
            String timestamp = Instant.now().toString();
            Map<String, Object> body = Map.of(
                    "id", eventId,
                    "traceId", observation.traceId(),
                    "name", "arag.chat",
                    "startTime", timestamp,
                    "endTime", timestamp,
                    "model", observation.providerId(),
                    "level", observation.success() ? "DEFAULT" : "ERROR",
                    "statusMessage", observation.errorType(),
                    "metadata", Map.of(
                            "reasoning", observation.reasoning(),
                            "latencyMs", observation.latencyMillis(),
                            "outputCharacters", observation.outputCharacters(),
                            "environment", properties.getEnvironment()
                    )
            );
            Map<String, Object> event = Map.of(
                    "id", eventId,
                    "timestamp", timestamp,
                    "type", "generation-create",
                    "body", body
            );
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(properties.getTimeout())
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(properties.getTimeout());
            RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build()
                    .post()
                    .uri("/api/public/ingestion")
                    .headers(headers -> headers.setBasicAuth(properties.getPublicKey(), properties.getSecretKey()))
                    .body(Map.of("batch", List.of(event)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Langfuse observation delivery failed providerId={}", observation.providerId());
        }
    }

    @Override
    public void close() {
        deliveryExecutor.shutdown();
    }
}
