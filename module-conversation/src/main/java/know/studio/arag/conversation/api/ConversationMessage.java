package know.studio.arag.conversation.api;

import know.studio.arag.platform.core.json.JsonLongId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ConversationMessage(
        @JsonLongId long id,
        MessageRole role,
        String content,
        int tokens,
        Map<String, Object> metadata,
        Instant createdAt
) {

    public ConversationMessage {
        metadata = normalizeMetadata(metadata);
    }

    private static Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>(metadata);
        normalized.computeIfPresent("knowledgeBaseIds", (key, value) -> normalizeIdList(value));
        normalized.computeIfPresent("citations", (key, value) -> normalizeCitations(value));
        return Map.copyOf(normalized);
    }

    private static Object normalizeIdList(Object value) {
        if (!(value instanceof List<?> values)) {
            return value;
        }
        return values.stream().map(ConversationMessage::normalizeId).toList();
    }

    private static Object normalizeCitations(Object value) {
        if (!(value instanceof List<?> citations)) {
            return value;
        }
        List<Object> normalized = new ArrayList<>(citations.size());
        for (Object citation : citations) {
            if (!(citation instanceof Map<?, ?> source)) {
                normalized.add(citation);
                continue;
            }
            Map<String, Object> target = new LinkedHashMap<>();
            source.forEach((key, item) -> {
                String name = String.valueOf(key);
                target.put(name, isCitationId(name) ? normalizeId(item) : item);
            });
            normalized.add(Map.copyOf(target));
        }
        return List.copyOf(normalized);
    }

    private static boolean isCitationId(String name) {
        return "knowledgeBaseId".equals(name)
                || "documentId".equals(name)
                || "chunkId".equals(name);
    }

    private static Object normalizeId(Object value) {
        return value instanceof Number ? value.toString() : value;
    }
}
