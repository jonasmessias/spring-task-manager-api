# 📋 Task Manager API# Task Manager API

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)Java · Spring Boot · Spring Security · JWT · Redis · PostgreSQL · Docker · AWS SES

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)

[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)---

[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)<!-- Add your architecture diagram image here -->

<!-- ![Diagram](diagram.png) -->

A production-ready **RESTful API** for task management built with Spring Boot. Features JWT + refresh token authentication, Google OAuth 2.0, workspace/board/list/card hierarchy, real-time member collaboration, HTML email notifications, and comprehensive audit logging.

---

---

## Setup

## 🏗️ Architecture

**1 — Install Docker**

````

┌──────────────┐       ┌──────────────┐       ┌──────────────┐https://www.docker.com

│   Angular    │◄─────►│  Spring Boot │◄─────►│  PostgreSQL  │

│  (Frontend)  │  REST │     API      │  JPA  │   Database   │**2 — Configure AWS SES (email sending)**

└──────────────┘       └──────┬───────┘       └──────────────┘

                              │Open `src/main/resources/application.properties` and fill in your AWS credentials:

                    ┌─────────┼─────────┐

                    │         │         │```properties

               ┌────▼───┐ ┌──▼───┐ ┌───▼────┐aws.ses.access-key=your-access-key

               │ Redis  │ │ AWS  │ │ Google │aws.ses.secret-key=your-secret-key

               │ Cache  │ │ SES  │ │ OAuth  │aws.ses.region=us-east-1

               └────────┘ └──────┘ └────────┘aws.ses.from=your-verified-email@example.com

````

### Module StructureTo get these credentials, see the [AWS SES setup guide](#aws-ses-setup) below.

````**3 — Run the application**

src/main/java/com/example/taskmanagerapi/

├── config/          # OpenAPI & Redis configurationOpen a terminal in the project root and run:

├── infra/

│   ├── cors/        # CORS policy```bash

│   ├── exception/   # Global exception handlerdocker-compose up -d

│   └── security/    # JWT filter, token service, security config```

└── modules/

    ├── auth/        # Authentication, users, email, auditAfter running, the following services will be available:

    ├── workspaces/  # Workspace CRUD + members

    ├── boards/      # Board CRUD + members| Service    | Port | URL                                   |

    ├── lists/       # List (column) CRUD| ---------- | ---- | ------------------------------------- |

    └── cards/       # Card CRUD + drag-and-drop| API        | 8080 | http://localhost:8080                 |

```| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |

| PostgreSQL | 5432 | localhost:5432                        |

---| Redis      | 6379 | localhost:6379                        |



## ✨ Features**PostgreSQL connection:**



| Category | Details || Field    | Value         |

|----------|---------|| -------- | ------------- |

| **Authentication** | JWT access tokens (4h) + refresh tokens (7d, Redis-cached) || Host     | `localhost`   |

| **Google OAuth** | One-click sign-in with automatic account creation || Port     | `5432`        |

| **Email Verification** | HTML email templates via AWS SES + Thymeleaf || Database | `taskmanager` |

| **Password Reset** | Secure token-based flow with 30-minute expiry || Username | `admin`       |

| **Workspaces** | Multi-workspace support with member roles (OWNER, ADMIN, MEMBER) || Password | `admin`       |

| **Boards** | Kanban boards within workspaces |

| **Lists** | Ordered columns with drag-and-drop reordering |---

| **Cards** | Cards with status (ACTIVE, ARCHIVED, COMPLETED) + cross-list moves |

| **Members** | Workspace & board-level access control |## Making requests

| **Pagination** | Optional pagination on listing endpoints (`?page=0&size=20`) |

| **Audit Logging** | All auth events persisted to database for security tracking |Open Swagger UI at http://localhost:8080/swagger-ui.html or use any HTTP client.

| **API Documentation** | Interactive Swagger UI with detailed schemas |

All protected endpoints require the header:

---

````

## 🛠️ Tech StackAuthorization: Bearer <access_token>

````

- **Runtime:** Java 17, Spring Boot 3.5

- **Database:** PostgreSQL 16 (JPA/Hibernate)---

- **Cache:** Redis 7 (refresh token caching via Jedis)

- **Auth:** JWT (`com.auth0:java-jwt`), Google OAuth (`google-api-client`)## Authentication flow

- **Email:** AWS SES + Thymeleaf HTML templates

- **Docs:** Springdoc OpenAPI 2.8 (Swagger UI)**1 — Register an account**

- **Build:** Maven with Lombok annotation processing

- **Container:** Docker + Docker Compose```

POST > http://localhost:8080/auth/register

---```



## 🚀 Getting Started```json

{

### Prerequisites  "name": "John Doe",

  "username": "johndoe",

- Java 17+  "email": "john@example.com",

- Docker & Docker Compose (for PostgreSQL + Redis)  "password": "secret123",

- AWS account with SES configured (for emails)  "confirmPassword": "secret123"

- Google Cloud project with OAuth 2.0 client ID (for Google login)}

````

### 1. Clone & configure

A verification email will be sent to the provided address.

```bash

git clone https://github.com/jonasmessias/spring-task-manager-api.git**2 — Verify your email**

cd spring-task-manager-api

```

# Copy the example config and fill in your secretsPOST > http://localhost:8080/auth/verify-email

cp src/main/resources/application-example.properties src/main/resources/application.properties```

````

```json

Edit `application.properties` with your credentials:{ "token": "<token_from_email>" }

````

- PostgreSQL connection

- Redis connection**3 — Login**

- JWT secret key

- AWS SES credentials```

- Google OAuth client IDPOST > http://localhost:8080/auth/login

- Frontend URLs```

### 2. Start infrastructure```json

{ "emailOrUsername": "johndoe", "password": "secret123" }

`bash`

docker-compose up -d # Starts PostgreSQL + Redis

````Response:



### 3. Run the API```json

{

```bash  "name": "John Doe",

# Using Maven Wrapper  "accessToken": "<access_token>",

./mvnw spring-boot:run  "refreshToken": "<refresh_token>"

}

# Or build and run the JAR```

./mvnw clean package -DskipTests

java -jar target/task-manager-api-1.0.0.jarAccess token expires in **4 hours**. Use the refresh token to get a new one:

````

```

The API will be available at `http://localhost:8080`.POST > http://localhost:8080/auth/refresh

```

### 4. Explore the API

```json

Open **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html){ "refreshToken": "<refresh_token>" }

```

---

---

## 📡 API Endpoints

## Usage example

### Authentication

**Create a workspace**

| Method | Endpoint | Description | Auth |

|--------|----------|-------------|------|```

| `POST` | `/auth/register` | Create account + verification email | ❌ |POST > http://localhost:8080/workspaces

| `POST` | `/auth/login` | Login with email/username + password | ❌ |Authorization: Bearer <access_token>

| `POST` | `/auth/google` | Login/register with Google ID token | ❌ |```

| `POST` | `/auth/refresh` | Get new access token | ❌ |

| `POST` | `/auth/verify-email` | Verify email with token | ❌ |```json

| `POST` | `/auth/resend-verification` | Resend verification email | ❌ |{ "name": "My Workspace" }

| `POST` | `/auth/forgot-password` | Send password reset email | ❌ |```

| `POST` | `/auth/reset-password` | Reset password with token | ❌ |

| `POST` | `/auth/logout` | Logout current device | ✅ |**Create a board inside the workspace**

| `POST` | `/auth/logout-all` | Logout all devices | ✅ |

````

### WorkspacesPOST > http://localhost:8080/boards?workspaceId=<workspace_id>

Authorization: Bearer <access_token>

| Method | Endpoint | Description | Auth |```

|--------|----------|-------------|------|

| `POST` | `/workspaces` | Create workspace | ✅ |```json

| `GET` | `/workspaces` | List user's workspaces | ✅ |{ "name": "My Project", "type": "BOARD", "description": "Main project board" }

| `GET` | `/workspaces/{id}` | Get workspace details | ✅ |```

| `PUT` | `/workspaces/{id}` | Update workspace | ✅ |

| `DELETE` | `/workspaces/{id}` | Delete workspace | ✅ |**Create a list inside the board**



### Workspace Members```

POST > http://localhost:8080/boards/<board_id>/lists

| Method | Endpoint | Description | Auth |Authorization: Bearer <access_token>

|--------|----------|-------------|------|```

| `POST` | `/workspaces/{id}/members` | Add member | ✅ |

| `GET` | `/workspaces/{id}/members` | List members | ✅ |```json

| `PUT` | `/workspaces/{id}/members/{memberId}` | Update role | ✅ |{ "name": "To Do" }

| `DELETE` | `/workspaces/{id}/members/{memberId}` | Remove member | ✅ |```



### Boards**Create a card inside the list**



| Method | Endpoint | Description | Auth |```

|--------|----------|-------------|------|POST > http://localhost:8080/boards/<board_id>/lists/<list_id>/cards

| `POST` | `/boards?workspaceId={id}` | Create board | ✅ |Authorization: Bearer <access_token>

| `GET` | `/boards?workspaceId={id}` | List boards (supports `?page=0&size=20`) | ✅ |```

| `GET` | `/boards/{id}` | Get board with lists & cards | ✅ |

| `PUT` | `/boards/{id}` | Update board | ✅ |```json

| `DELETE` | `/boards/{id}` | Delete board | ✅ |{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }

````

### Board Members

---

| Method | Endpoint | Description | Auth |

|--------|----------|-------------|------|## Endpoints

| `POST` | `/boards/{id}/members` | Add member | ✅ |

| `GET` | `/boards/{id}/members` | List members | ✅ |### Auth

| `PUT` | `/boards/{id}/members/{memberId}` | Update role | ✅ |

| `DELETE` | `/boards/{id}/members/{memberId}` | Remove member | ✅ || Method | Path | Auth | Status | Description |

| ------ | ------------------------- | ---- | ------ | ------------------------------------------ |

### Lists| POST | /auth/register | No | 201 | Create account and send verification email |

| POST | /auth/verify-email | No | 200 | Verify email with token from inbox |

| Method | Endpoint | Description | Auth || POST | /auth/resend-verification | No | 200 | Resend verification email |

|--------|----------|-------------|------|| POST | /auth/login | No | 200 | Login and receive access + refresh token |

| `POST` | `/boards/{boardId}/lists` | Create list | ✅ || POST | /auth/refresh | No | 200 | Get new access token using refresh token |

| `GET` | `/boards/{boardId}/lists` | Get all lists | ✅ || POST | /auth/logout | Yes | 200 | Invalidate current session |

| `PUT` | `/boards/{boardId}/lists/{id}` | Update list | ✅ || POST | /auth/logout-all | Yes | 200 | Invalidate all sessions |

| `DELETE` | `/boards/{boardId}/lists/{id}` | Delete list | ✅ || POST | /auth/forgot-password | No | 200 | Send password reset email |

| POST | /auth/reset-password | No | 200 | Reset password with token |

### Cards

### Users

| Method | Endpoint | Description | Auth |

|--------|----------|-------------|------|| Method | Path | Auth | Status | Description |

| `POST` | `/boards/{bId}/lists/{lId}/cards` | Create card | ✅ || ------ | ----------- | ---- | ------ | ------------------------------- |

| `GET` | `/boards/{bId}/lists/{lId}/cards` | List cards (supports `?page=0&size=50`) | ✅ || GET | /users/me | Yes | 200 | Get current authenticated user |

| `GET` | `/boards/{bId}/lists/{lId}/cards/{id}` | Get card | ✅ || PUT | /users/me | Yes | 200 | Update profile (name, username) |

| `PUT` | `/boards/{bId}/lists/{lId}/cards/{id}` | Update card | ✅ || DELETE | /users/me | Yes | 204 | Delete own account |

| `PATCH` | `/boards/{bId}/lists/{lId}/cards/{id}/move` | Move card to another list | ✅ || GET | /users/{id} | Yes | 200 | Get user by ID |

| `DELETE` | `/boards/{bId}/lists/{lId}/cards/{id}` | Delete card | ✅ |

### Workspaces

---

| Method | Path | Auth | Status | Description |

## 🔐 Authentication Flow| ------ | ---------------- | ---- | ------ | ------------------------------------ |

| POST | /workspaces | Yes | 201 | Create workspace |

````| GET    | /workspaces      | Yes  | 200    | List all workspaces                  |

┌────────┐                      ┌────────┐                    ┌───────┐| GET    | /workspaces/{id} | Yes  | 200    | Get workspace with boards            |

│ Client │                      │  API   │                    │ Redis │| PUT    | /workspaces/{id} | Yes  | 200    | Update workspace                     |

└───┬────┘                      └───┬────┘                    └───┬───┘| DELETE | /workspaces/{id} | Yes  | 204    | Delete workspace and all its content |

    │  POST /auth/login             │                             │

    │  {email, password}            │                             │### Boards

    │──────────────────────────────►│                             │

    │                               │  Create refresh token       │| Method | Path                     | Auth | Status | Description                      |

    │                               │────────────────────────────►│| ------ | ------------------------ | ---- | ------ | -------------------------------- |

    │  {accessToken, refreshToken}  │                             │| POST   | /boards?workspaceId={id} | Yes  | 201    | Create board in workspace        |

    │◄──────────────────────────────│                             │| GET    | /boards?workspaceId={id} | Yes  | 200    | List boards from workspace       |

    │                               │                             │| GET    | /boards/{id}             | Yes  | 200    | Get board with lists and cards   |

    │  GET /boards (Bearer token)   │                             │| PUT    | /boards/{id}             | Yes  | 200    | Update board                     |

    │──────────────────────────────►│                             │| DELETE | /boards/{id}             | Yes  | 204    | Delete board and all its content |

    │  200 OK                       │                             │

    │◄──────────────────────────────│                             │### Workspace Members

    │                               │                             │

    │  POST /auth/refresh           │                             │| Method | Path                              | Auth | Status | Description                        |

    │  {refreshToken}               │                             │| ------ | --------------------------------- | ---- | ------ | ---------------------------------- |

    │──────────────────────────────►│  Validate from cache        │| GET    | /workspaces/{id}/members          | Yes  | 200    | List all members of a workspace    |

    │                               │────────────────────────────►│| POST   | /workspaces/{id}/members          | Yes  | 201    | Invite a user by email or username |

    │  {newAccessToken}             │                             │| DELETE | /workspaces/{id}/members/{userId} | Yes  | 200    | Remove a member from workspace     |

    │◄──────────────────────────────│                             │

```### Board Members



---| Method | Path                          | Auth | Status | Description                             |

| ------ | ----------------------------- | ---- | ------ | --------------------------------------- |

## 🐳 Docker| GET    | /boards/{id}/members          | Yes  | 200    | List all members of a board             |

| POST   | /boards/{id}/members          | Yes  | 201    | Invite a workspace member to this board |

### Development (infrastructure only)| DELETE | /boards/{id}/members/{userId} | Yes  | 200    | Remove a member from board              |



```bash### Lists

docker-compose up -d    # PostgreSQL + Redis

./mvnw spring-boot:run  # API on host| Method | Path                             | Auth | Status | Description               |

```| ------ | -------------------------------- | ---- | ------ | ------------------------- |

| POST   | /boards/{boardId}/lists          | Yes  | 201    | Create list               |

### Production (full stack)| GET    | /boards/{boardId}/lists          | Yes  | 200    | Get all lists             |

| GET    | /boards/{boardId}/lists/{listId} | Yes  | 200    | Get list                  |

```bash| PUT    | /boards/{boardId}/lists/{listId} | Yes  | 200    | Update list               |

docker build -t task-manager-api .| DELETE | /boards/{boardId}/lists/{listId} | Yes  | 204    | Delete list and its cards |

docker run -p 8080:8080 --env-file .env task-manager-api

```### Cards



---| Method | Path                                                 | Auth | Status | Description                   |

| ------ | ---------------------------------------------------- | ---- | ------ | ----------------------------- |

## 📧 Email Templates| POST   | /boards/{boardId}/lists/{listId}/cards               | Yes  | 201    | Create card                   |

| GET    | /boards/{boardId}/lists/{listId}/cards               | Yes  | 200    | Get all cards                 |

The API sends HTML emails using Thymeleaf templates:| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200    | Get card                      |

| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200    | Update card                   |

| Template | Trigger | Expiry || PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Yes  | 200    | Move card to a different list |

|----------|---------|--------|| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 204    | Delete card                   |

| `email-verification.html` | Registration, resend verification | 24 hours |

| `password-reset.html` | Forgot password | 30 minutes |Card status values: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

| `member-invite.html` | Workspace/board member invitation | — |

---

Templates are located in `src/main/resources/templates/`.

## Error responses

---

All errors follow the format:

## 📝 Error Codes

```json

All error responses follow the format:{

  "code": "SNAKE_CASE_CODE",

```json  "message": "Human readable message.",

{  "statusCode": 400,

  "code": "ERROR_CODE",  "timestamp": "2026-03-06T12:00:00Z"

  "message": "Human-readable message",}

  "statusCode": 400```

}

```| Code                      | HTTP Status | Description                                  |

| ------------------------- | ----------- | -------------------------------------------- |

| Code | Status | Description || `PASSWORDS_DO_NOT_MATCH`  | 400         | Passwords do not match                       |

|------|--------|-------------|| `EMAIL_ALREADY_EXISTS`    | 400         | Email already registered                     |

| `INVALID_CREDENTIALS` | 401 | Wrong email/password || `USERNAME_ALREADY_EXISTS` | 400         | Username already taken                       |

| `EMAIL_NOT_VERIFIED` | 403 | Account not verified || `INVALID_TOKEN`           | 400         | Token not found in database                  |

| `USE_GOOGLE_LOGIN` | 401 | OAuth-only account || `EXPIRED_TOKEN`           | 400         | Token has passed its 24h expiry              |

| `INVALID_GOOGLE_TOKEN` | 401 | Bad Google ID token || `EMAIL_ALREADY_VERIFIED`  | 400         | Account already active                       |

| `EMAIL_ALREADY_EXISTS` | 400 | Duplicate email || `INVALID_CREDENTIALS`     | 401         | Wrong email/username or password             |

| `USERNAME_ALREADY_EXISTS` | 400 | Duplicate username || `EMAIL_NOT_VERIFIED`      | 403         | Account pending email verification           |

| `PASSWORDS_DO_NOT_MATCH` | 400 | Password mismatch || `EMAIL_NOT_FOUND`         | 404         | No account with that email                   |

| `INVALID_TOKEN` | 400/401 | Bad verification/reset token || `EMAIL_SEND_ERROR`        | 500         | SMTP failure                                 |

| `EXPIRED_TOKEN` | 400/401 | Token expired || `EMAIL_AUTH_ERROR`        | 500         | SMTP authentication failure                  |

| `EMAIL_NOT_FOUND` | 404 | Email not registered || `VALIDATION_ERROR`        | 400         | Bean validation failure (see `errors` array) |

| `EMAIL_SEND_ERROR` | 500 | AWS SES failure |

| `WORKSPACE_NOT_FOUND` | 404 | Workspace doesn't exist |Validation error format:

| `BOARD_NOT_FOUND` | 404 | Board doesn't exist |

| `LIST_NOT_FOUND` | 404 | List doesn't exist |```json

| `CARD_NOT_FOUND` | 404 | Card doesn't exist |{

| `FORBIDDEN` | 403 | Insufficient permissions |  "code": "VALIDATION_ERROR",

| `VALIDATION_ERROR` | 400 | Request body validation failed |  "message": "Invalid request data.",

  "statusCode": 400,

---  "errors": [{ "field": "email", "message": "must not be blank" }]

}

## ☁️ AWS SES Setup```



1. **Create an IAM user** at [AWS IAM Console](https://console.aws.amazon.com/iam)---

   - Name: `task-manager-ses`

   - Attach policy: `AmazonSESFullAccess`## AWS SES setup

   - Create access key → copy both keys

**1 — Create an IAM user**

2. **Verify sender email** at [AWS SES Console](https://console.aws.amazon.com/ses)

   - Region: `us-east-1`1. Go to https://console.aws.amazon.com/iam → **Users → Create user**

   - Verified Identities → Create Identity → Email address2. Name: `task-manager-api`

   - Confirm the verification email3. Attach policy: `AmazonSESFullAccess`

4. Go to the user → **Security credentials → Create access key**

3. **Fill in `application.properties`**5. Select **Application running outside AWS** — copy both keys



```properties**2 — Verify sender email**

aws.ses.access-key=AKIA...

aws.ses.secret-key=...1. Go to https://console.aws.amazon.com/ses → region `us-east-1`

aws.ses.region=us-east-12. **Verified Identities → Create Identity → Email address**

aws.ses.from=your-verified-email@example.com3. Enter your sender email and confirm the verification email AWS sends

````

**3 — Fill in `application.properties`**

> **Note:** AWS SES starts in **Sandbox mode** — you can only send to verified emails.

> Request production access in the SES console to send to anyone.```properties

aws.ses.access-key=AKIA...

---aws.ses.secret-key=...

aws.ses.region=us-east-1

## 📄 Licenseaws.ses.from=your-verified-email@example.com

```

This project is licensed under the MIT License.

> **Note:** By default AWS SES is in **Sandbox mode** — you can only send to verified emails.

---> To send to anyone, request production access in the SES console → **Account dashboard**.


**Built with ❤️ by [Jonas Messias](https://github.com/jonasmessias)**
```
