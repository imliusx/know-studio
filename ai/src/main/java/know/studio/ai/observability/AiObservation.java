package know.studio.ai.observability;

public record AiObservation(
        String providerId,
        boolean reasoning,
        boolean success,
        long latencyMillis,
        long outputCharacters,
        String errorType,
        String traceId,
        String generationProfile,
        String promptVersion
) {
}
