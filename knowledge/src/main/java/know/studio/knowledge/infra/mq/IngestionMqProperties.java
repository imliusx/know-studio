package know.studio.knowledge.infra.mq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "arag.ingestion.mq")
@Getter
@Setter
public class IngestionMqProperties {

    private List<Duration> retryDelays = List.of(
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2)
    );
}
