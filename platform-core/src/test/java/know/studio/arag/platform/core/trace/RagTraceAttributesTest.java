package know.studio.arag.platform.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagTraceAttributesTest {

    @Test
    void addsBoundedRetrievalAttributesToCurrentSpan() {
        SdkTracerProvider provider = SdkTracerProvider.builder().build();
        Span span = provider.get("test").spanBuilder("retrieval").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            RagTraceAttributes.put("retrieval.scope.strategy", "metadata");
            RagTraceAttributes.put("retrieval.scope.confidence", 0.8);
            RagTraceAttributes.put("retrieval.evidence.count", 5L);
            RagTraceAttributes.put("retrieval.refused", false);
        }

        io.opentelemetry.api.common.Attributes attributes =
                ((ReadableSpan) span).toSpanData().getAttributes();
        assertThat(attributes.get(AttributeKey.stringKey("rag.retrieval.scope.strategy")))
                .isEqualTo("metadata");
        assertThat(attributes.get(AttributeKey.doubleKey("rag.retrieval.scope.confidence")))
                .isEqualTo(0.8);
        assertThat(attributes.get(AttributeKey.longKey("rag.retrieval.evidence.count")))
                .isEqualTo(5L);
        assertThat(attributes.get(AttributeKey.booleanKey("rag.retrieval.refused")))
                .isFalse();
        span.end();
        provider.close();
    }
}
