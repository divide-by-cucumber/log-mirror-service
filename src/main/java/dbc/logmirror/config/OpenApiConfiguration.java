package dbc.logmirror.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Log Mirror Service API")
                        .version("0.1.0")
                        .description("Configuration-driven log mirroring and processing service with SSH connectivity, " +
                                "rotation/retention policies, processing pipelines, and comprehensive monitoring.")
                        .contact(new Contact()
                                .name("Development Team")
                                .url("https://example.com")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server"))
                .addServersItem(new Server()
                        .url("http://prod.example.com:8080")
                        .description("Production server"));
    }
}
