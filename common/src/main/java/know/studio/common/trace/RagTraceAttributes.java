package know.studio.common.trace;

import io.opentelemetry.api.trace.Span;

public final class RagTraceAttributes {

    private static final String PREFIX = "rag.";

    private RagTraceAttributes() {
    }

    public static void put(String name, String value) {
        if (value != null) {
            Span.current().setAttribute(key(name), value);
        }
    }

    public static void put(String name, long value) {
        Span.current().setAttribute(key(name), value);
    }

    public static void put(String name, double value) {
        Span.current().setAttribute(key(name), value);
    }

    public static void put(String name, boolean value) {
        Span.current().setAttribute(key(name), value);
    }

    private static String key(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("trace attribute name must not be blank");
        }
        return PREFIX + name;
    }
}
