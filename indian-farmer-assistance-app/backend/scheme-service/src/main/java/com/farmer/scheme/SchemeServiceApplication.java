package com.farmer.scheme;

import com.farmer.scheme.service.SchemeService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})

@OpenAPIDefinition(info = @Info(title = "Scheme Service API", version = "1.0.0", description = "Government schemes catalog and application tracking service", contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")))
public class SchemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemeServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadSchemes(SchemeService schemeService) {
        return args -> schemeService.loadSchemesFromCsv();
    }
}
