package com.citizenbridge.citizenbridge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Swagger/OpenAPI documentation
 * Updated to be compatible with Spring Boot 3.4.x and SpringDoc 2.4.0
 */
@Configuration
public class SwaggerConfig {
    @Value("${openapi.dev-url}")
    private String devUrl;

    @Value("${openapi.prod-url}")
    private String prodUrl;

    @Bean
    public OpenAPI citizenBridgeOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Server URL in Production environment");

        Contact contact = new Contact();
        contact.setEmail("virgilendayambaje@gmail.com");
        contact.setName("CitizenBridge");
        contact.setUrl("https://citizenbridge.nvirgile.pro");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Citizen-Bridge System API")
                .version("1.0")
                .contact(contact)
                .description("This API exposes endpoints for citizenbridge System")
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createSecurityScheme()));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("Enter your bearer token in the format **Bearer <token>**\n\n" +
                        "1. Obtain a token by sending a POST request to /api/auth/login with your credentials\n" +
                        "2. Copy the token from the response\n" +
                        "3. Click 'Authorize' and paste the token with the prefix 'Bearer '\n" +
                        "4. Click 'Authorize' to save\n" +
                        "5. All subsequent requests will include this token");
    }
}