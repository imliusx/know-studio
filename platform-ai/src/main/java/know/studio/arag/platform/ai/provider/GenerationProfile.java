package know.studio.arag.platform.ai.provider;

public enum GenerationProfile {
    CHAT(0.6, 1_200),
    KNOWLEDGE(0.15, 1_400),
    CLASSIFICATION(0.1, 80),
    PLANNING(0.1, 240),
    SUMMARY(0.1, 800);

    private final double temperature;
    private final int maxTokens;

    GenerationProfile(double temperature, int maxTokens) {
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public double temperature() {
        return temperature;
    }

    public int maxTokens() {
        return maxTokens;
    }
}
