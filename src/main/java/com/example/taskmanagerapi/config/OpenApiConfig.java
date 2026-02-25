package com.example.taskmanagerapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager API")
                        .version("1.0.0")
                        .description("API for managing tasks with user authentication. " +
                                "This API allows users to create, read, update, and delete tasks, " +
                                "with built-in authentication and authorization.")
                        .contact(new Contact()
                                .name("Task Manager Team")
                                .email("support@taskmanager.com")
                                .url("https://github.com/reazew/task-manager-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /auth/login or /auth/register")))
                // Define a ordem das tags (grupos) no Swagger UI
                .addTagsItem(new Tag().name("Authentication").description("Endpoints de autenticação e registro"))
                .addTagsItem(new Tag().name("Users").description("Endpoints de gerenciamento de usuários"))
                .addTagsItem(new Tag().name("Tasks").description("Endpoints de gerenciamento de tarefas"));
    }
}