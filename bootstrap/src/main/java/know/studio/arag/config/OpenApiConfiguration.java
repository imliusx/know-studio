package know.studio.arag.config;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.properties.SpringDocConfigProperties;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI aragOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARAG Agentic RAG API")
                        .version("1.0.0")
                        .description("Identity, ingestion, retrieval, Agent SSE, conversation and evaluation APIs"))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Sa-Token")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    Knife4jOpenApiCustomizer compatibleKnife4jCustomizer(
            Knife4jProperties knife4jProperties,
            SpringDocConfigProperties springDocProperties
    ) {
        return new Knife4jOpenApiCustomizer(knife4jProperties, springDocProperties) {
            @Override
            public void customise(OpenAPI openApi) {
                // Knife4j 4.5 ordering extensions target Springdoc 2.3 APIs.
            }
        };
    }
}
