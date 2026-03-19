# TaskManager API

This project is a RESTful API built with **Spring Boot 3.5** and **Java 17** for managing tasks with workspaces, boards, lists, and cards — inspired by tools like Trello.

## Prerequisites

Make sure you have the following installed:

- [Java 17](https://adoptium.net/) (JDK)
- [Maven 3.9+](https://maven.apache.org/) (or use the included `mvnw` wrapper)
- [Docker & Docker Compose](https://www.docker.com/) (for infrastructure services)
- [PostgreSQL 16](https://www.postgresql.org/) (if running without Docker)
- [Redis 7](https://redis.io/) (if running without Docker)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/reazew/task-manager-api.git
cd task-manager-api
```

### 2. Configure environment variables

Copy the example properties file and fill in your values:

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Update the following properties in `application.properties`:

| Property                    | Description                        |
| --------------------------- | ---------------------------------- |
| `api.security.token.secret` | Strong random key for JWT signing  |
| `aws.ses.access-key`        | AWS SES access key                 |
| `aws.ses.secret-key`        | AWS SES secret key                 |
| `aws.ses.from`              | Verified sender email in AWS SES   |
| `google.client-id`          | Google OAuth 2.0 client ID         |
| `app.frontend.url`          | Frontend URL for CORS (dev)        |
| `app.frontend.prod-url`     | Frontend URL for CORS (production) |

### 3. Start infrastructure with Docker Compose

```bash
docker compose up -d postgres redis
```

This starts **PostgreSQL** on port `5432` and **Redis** on port `6379`.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Running with Docker (full stack)

To run the entire stack (API + PostgreSQL + Redis) in containers:

```bash
docker compose up -d --build
```

The API will be available at `http://localhost:8080`.

## API Documentation

Once the server is running, open your browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

The interactive Swagger UI lets you explore and test all available endpoints.

## Project Structure

```
src/main/java/com/example/taskmanagerapi/
├── config/              # OpenAPI, Redis, AWS SES configuration
├── infra/
│   ├── cors/            # CORS policy
│   ├── exception/       # Global exception handler
│   └── security/        # JWT filter, token service, security config
└── modules/
    ├── auth/            # Authentication, users, email verification
    ├── workspaces/      # Workspace CRUD + members
    ├── boards/          # Board CRUD + members
    ├── lists/           # List (column) CRUD
    └── cards/           # Card CRUD + drag-and-drop reordering
```

## Tech Stack

| Technology        | Purpose                         |
| ----------------- | ------------------------------- |
| Java 17           | Language                        |
| Spring Boot 3.5   | Framework                       |
| Spring Security   | Authentication & authorization  |
| JWT (java-jwt)    | Access token (4h expiry)        |
| PostgreSQL 16     | Primary database                |
| Redis 7           | Refresh token caching (7d TTL)  |
| AWS SES           | Transactional emails (HTML)     |
| Google OAuth 2.0  | Social login                    |
| Thymeleaf         | HTML email templates            |
| Springdoc OpenAPI | Swagger UI documentation        |
| Docker Compose    | Infrastructure containerization |
| Lombok            | Boilerplate reduction           |

## Running Tests

To execute the unit tests, use the following command:

```bash
./mvnw test
```

## Building

To build the project and generate the `.jar` artifact:

```bash
./mvnw clean package -DskipTests
```

The compiled JAR will be stored at `target/task-manager-api-1.0.0.jar`.

## Further Help

For more details about the architecture, authentication flow, endpoints, and business rules, see the [DOCUMENTATION.md](DOCUMENTATION.md) file.
