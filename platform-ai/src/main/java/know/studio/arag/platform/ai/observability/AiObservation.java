package know.studio.arag.platform.ai.observability;

public record AiObservation(
        String providerId,
        boolean reasoning,
        boolean success,
        long latencyMillis,
        long outputCharacters,
        String errorType,
        String traceId
) {
}
