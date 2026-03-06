package com.example.taskmanagerapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        // ── Shared error response examples ──────────────────────────────────
        Example errorInvalidCredentials = new Example().value("""
                {
                  "code": "INVALID_CREDENTIALS",
                  "message": "Invalid credentials.",
                  "statusCode": 401,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorEmailNotVerified = new Example().value("""
                {
                  "code": "EMAIL_NOT_VERIFIED",
                  "message": "Email not verified. Please check your inbox and verify your account.",
                  "statusCode": 403,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorEmailAlreadyExists = new Example().value("""
                {
                  "code": "EMAIL_ALREADY_EXISTS",
                  "message": "Email already registered.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorUsernameAlreadyExists = new Example().value("""
                {
                  "code": "USERNAME_ALREADY_EXISTS",
                  "message": "Username already taken.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorPasswordsDoNotMatch = new Example().value("""
                {
                  "code": "PASSWORDS_DO_NOT_MATCH",
                  "message": "Passwords do not match.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorInvalidToken = new Example().value("""
                {
                  "code": "INVALID_TOKEN",
                  "message": "Invalid verification token.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorExpiredToken = new Example().value("""
                {
                  "code": "EXPIRED_TOKEN",
                  "message": "Verification token has expired. Please request a new one.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorEmailAlreadyVerified = new Example().value("""
                {
                  "code": "EMAIL_ALREADY_VERIFIED",
                  "message": "This email is already verified. You can log in.",
                  "statusCode": 400,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorEmailNotFound = new Example().value("""
                {
                  "code": "EMAIL_NOT_FOUND",
                  "message": "E-mail not found.",
                  "statusCode": 404,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        Example errorEmailSendError = new Example().value("""
                {
                  "code": "EMAIL_SEND_ERROR",
                  "message": "Failed to send email. Please try again later.",
                  "statusCode": 500,
                  "timestamp": "2026-03-06T12:00:00Z"
                }""");

        // ── Shared response definitions ──────────────────────────────────────
        ApiResponse response400 = new ApiResponse()
                .description("Bad Request")
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .addExamples("PASSWORDS_DO_NOT_MATCH", errorPasswordsDoNotMatch)
                                .addExamples("EMAIL_ALREADY_EXISTS", errorEmailAlreadyExists)
                                .addExamples("USERNAME_ALREADY_EXISTS", errorUsernameAlreadyExists)
                                .addExamples("INVALID_TOKEN", errorInvalidToken)
                                .addExamples("EXPIRED_TOKEN", errorExpiredToken)
                                .addExamples("EMAIL_ALREADY_VERIFIED", errorEmailAlreadyVerified)));

        ApiResponse response401 = new ApiResponse()
                .description("Unauthorized")
                .content(new Content().addMediaType("application/json",
                        new MediaType().addExamples("INVALID_CREDENTIALS", errorInvalidCredentials)));

        ApiResponse response403 = new ApiResponse()
                .description("Forbidden")
                .content(new Content().addMediaType("application/json",
                        new MediaType().addExamples("EMAIL_NOT_VERIFIED", errorEmailNotVerified)));

        ApiResponse response404 = new ApiResponse()
                .description("Not Found")
                .content(new Content().addMediaType("application/json",
                        new MediaType().addExamples("EMAIL_NOT_FOUND", errorEmailNotFound)));

        ApiResponse response500 = new ApiResponse()
                .description("Internal Server Error")
                .content(new Content().addMediaType("application/json",
                        new MediaType().addExamples("EMAIL_SEND_ERROR", errorEmailSendError)));

        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing workspaces, boards, lists and cards.

                                **Hierarchical structure:** Workspace → Boards → Lists → Cards

                                ## Authentication
                                All protected endpoints require a `Bearer` JWT token in the `Authorization` header.
                                Obtain the token via `POST /auth/login`.

                                ## Error format
                                All errors follow the pattern:
                                ```json
                                {
                                  "code": "SNAKE_CASE_CODE",
                                  "message": "Human readable message",
                                  "statusCode": 400,
                                  "timestamp": "2026-03-06T12:00:00Z"
                                }
                                ```

                                ## Registration flow
                                1. `POST /auth/register` → receives verification email
                                2. `POST /auth/verify-email` with the token from the email
                                3. `POST /auth/login` → receives `accessToken` + `refreshToken`
                                4. Use `POST /auth/refresh` when the access token expires (4h)
                                """)
                        .contact(new Contact()
                                .name("Task Manager Team")
                                .email("support@taskmanager.com")
                                .url("https://github.com/jonasmessias/task-manager-api"))
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
                                        .description("JWT token obtained from `POST /auth/login`"))
                        .addResponses("400", response400)
                        .addResponses("401", response401)
                        .addResponses("403", response403)
                        .addResponses("404", response404)
                        .addResponses("500", response500))
                .addTagsItem(new Tag().name("Authentication")
                        .description("Register, login, email verification and password reset"))
                .addTagsItem(new Tag().name("Users")
                        .description("Profile management"))
                .addTagsItem(new Tag().name("Workspaces")
                        .description("Top-level containers for organizing work"))
                .addTagsItem(new Tag().name("Boards")
                        .description("Boards inside a workspace"))
                .addTagsItem(new Tag().name("Board Lists")
                        .description("Lists (columns) inside a board"))
                .addTagsItem(new Tag().name("Cards")
                        .description("Cards (tasks) inside a list"));
    }
}