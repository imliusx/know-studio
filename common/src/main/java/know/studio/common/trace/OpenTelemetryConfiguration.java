package know.studio.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryConfiguration {

    @Bean(destroyMethod = "close")
    OpenTelemetrySdk openTelemetry(OpenTelemetryProperties properties) {
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"),
                properties.getServiceName()
        )));
        SdkTracerProviderBuilder tracerProvider = SdkTracerProvider.builder().setResource(resource);
        if (properties.isEnabled()) {
            OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(properties.getEndpoint())
                    .setTimeout(properties.getExportTimeout())
                    .build();
            tracerProvider.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
        }
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider.build())
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    RagTraceAspect ragTraceAspect(OpenTelemetry openTelemetry, io.micrometer.core.instrument.MeterRegistry registry) {
        return new RagTraceAspect(openTelemetry, registry);
    }
}
