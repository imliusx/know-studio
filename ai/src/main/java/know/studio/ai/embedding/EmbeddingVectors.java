package know.studio.ai.embedding;

public final class EmbeddingVectors {

    private EmbeddingVectors() {
    }

    public static String toPgVectorLiteral(float[] embedding) {
        StringBuilder value = new StringBuilder(embedding.length * 10).append('[');
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(embedding[index]);
        }
        return value.append(']').toString();
    }
}
