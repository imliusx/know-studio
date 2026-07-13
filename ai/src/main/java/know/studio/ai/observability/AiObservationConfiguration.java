package know.studio.ai.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LangfuseProperties.class)
public class AiObservationConfiguration {

    @Bean(destroyMethod = "close")
    AiObservationService aiObservationSink(MeterRegistry meterRegistry, LangfuseProperties properties) {
        return new AiObservationService(meterRegistry, properties);
    }
}
