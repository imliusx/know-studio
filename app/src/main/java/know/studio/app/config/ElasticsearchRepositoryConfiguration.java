package know.studio.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Registers Elasticsearch repositories owned by backend modules outside the app package.
 */
@Configuration(proxyBeanMethods = false)
@EnableElasticsearchRepositories(basePackages = "know.studio.knowledge.infra.search")
public class ElasticsearchRepositoryConfiguration {
}
