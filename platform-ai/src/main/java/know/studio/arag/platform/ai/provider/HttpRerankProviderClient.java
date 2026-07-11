package know.studio.arag.platform.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class HttpRerankProviderClient implements RerankProviderClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final Duration timeout;

    public HttpRerankProviderClient(ObjectMapper objectMapper, URI baseUrl, Duration timeout) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.objectMapper = objectMapper;
        this.endpoint = baseUrl.resolve("/rerank");
        this.timeout = timeout;
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        RequestBody payload = new RequestBody(query, documents.stream().map(RerankDocument::text).toList());
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Rerank service returned HTTP " + response.statusCode());
            }
            List<ResponseItem> items = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<ResponseItem>>() {
                    }
            );
            return items.stream()
                    .filter(item -> item.index() >= 0 && item.index() < documents.size())
                    .map(item -> new RerankResult(documents.get(item.index()).id(), item.score()))
                    .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Rerank request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Rerank request failed", exception);
        }
    }

    private String toJson(RequestBody payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize rerank request", exception);
        }
    }

    private record RequestBody(String query, List<String> texts) {
    }

    private record ResponseItem(int index, double score) {
    }
}
