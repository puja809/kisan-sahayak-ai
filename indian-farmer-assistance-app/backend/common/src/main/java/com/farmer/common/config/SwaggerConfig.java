package com.farmer.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI farmerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Indian Farmer Assistance API")
                        .description("Backend APIs for Indian Farmer Assistance Application")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Farmer Assistance Team")
                                .email("support@farmer-assistance.in"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.farmer-assistance.in").description("Production")
                ));
    }
}