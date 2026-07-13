package know.studio.common.id;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdGeneratorProperties.class)
public class IdGeneratorConfiguration {

    @Bean
    SnowflakeIdGenerator snowflakeIdGenerator(IdGeneratorProperties properties) {
        return new SnowflakeIdGenerator(properties.getWorkerId(), properties.getDatacenterId());
    }
}
