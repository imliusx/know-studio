package know.studio.arag.agent.api;

public record IntentResult(IntentType intent, double confidence, String clarification) {

    public IntentResult {
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        clarification = clarification == null ? "" : clarification;
    }
}
