# Task Manager API# 📋 Task Manager API# 📋 Task Manager API# Task Manager API



Java · Spring Boot · Spring Security · JWT · Redis · PostgreSQL · Docker · AWS SES · Google OAuth[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)Java · Spring Boot · Spring Security · JWT · Redis · PostgreSQL · Docker · AWS SES



---[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)



## Passo a passo[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)



**1 — Instale o Docker**[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)



https://www.docker.com[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)---



**2 — Configure o ambiente**A production-ready **RESTful API** for task management built with Spring Boot. Features JWT + refresh token authentication, Google OAuth 2.0, workspace/board/list/card hierarchy, real-time member collaboration, HTML email notifications, and comprehensive audit logging.[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)



Copie o arquivo de exemplo e preencha com seus valores:---[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)<!-- Add your architecture diagram image here -->



```bash## 🏗️ Architecture<!-- ![Diagram](diagram.png) -->

cp src/main/resources/application-example.properties src/main/resources/application.properties

``````A production-ready **RESTful API** for task management built with Spring Boot. Features JWT + refresh token authentication, Google OAuth 2.0, workspace/board/list/card hierarchy, real-time member collaboration, HTML email notifications, and comprehensive audit logging.



Edite `application.properties` com suas credenciais (JWT secret, AWS SES keys, Google Client ID).┌──────────────┐       ┌──────────────┐       ┌──────────────┐



**3 — Suba a infraestrutura**│   Angular    │◄─────►│  Spring Boot │◄─────►│  PostgreSQL  │---



```bash│  (Frontend)  │  REST │     API      │  JPA  │   Database   │

docker-compose up -d

```└──────────────┘       └──────┬───────┘       └──────────────┘---



Após executar, os seguintes serviços estarão disponíveis:                              │



| Serviço    | Porta | URL                                   |                    ┌─────────┼─────────┐## Setup

| ---------- | ----- | ------------------------------------- |

| API        | 8080  | http://localhost:8080                 |                    │         │         │

| Swagger UI | 8080  | http://localhost:8080/swagger-ui.html |

| PostgreSQL | 5432  | localhost:5432                        |               ┌────▼───┐ ┌──▼───┐ ┌───▼────┐## 🏗️ Architecture

| Redis      | 6379  | localhost:6379                        |

               │ Redis  │ │ AWS  │ │ Google │

**Conexão PostgreSQL:**

               │ Cache  │ │ SES  │ │ OAuth  │**1 — Install Docker**

| Campo    | Valor         |

| -------- | ------------- |               └────────┘ └──────┘ └────────┘

| Host     | `localhost`   |

| Porta    | `5432`        |```

| Database | `taskmanager` |

| Usuário  | `admin`       |### Module Structure┌──────────────┐ ┌──────────────┐ ┌──────────────┐https://www.docker.com

| Senha    | `admin`       |

````│ Angular    │◄─────►│  Spring Boot │◄─────►│  PostgreSQL  │

**4 — Inicie a API**

src/main/java/com/example/taskmanagerapi/

```bash

./mvnw spring-boot:run├── config/          # OpenAPI & Redis configuration│  (Frontend)  │  REST │     API      │  JPA  │   Database   │**2 — Configure AWS SES (email sending)**

```

├── infra/

Ou build + run:

│   ├── cors/        # CORS policy└──────────────┘       └──────┬───────┘       └──────────────┘

```bash

./mvnw clean package -DskipTests│   ├── exception/   # Global exception handler

java -jar target/task-manager-api-1.0.0.jar

```│   └── security/    # JWT filter, token service, security config                              │Open `src/main/resources/application.properties` and fill in your AWS credentials:



---└── modules/



## AWS SES (envio de e-mails)    ├── auth/        # Authentication, users, email, audit                    ┌─────────┼─────────┐



1. Crie um IAM user em https://console.aws.amazon.com/iam com a policy `AmazonSESFullAccess`    ├── workspaces/  # Workspace CRUD + members

2. Gere uma access key em **Security credentials → Create access key**

3. Verifique o e-mail remetente em https://console.aws.amazon.com/ses → **Verified Identities**    ├── boards/      # Board CRUD + members                    │         │         │```properties

4. Preencha em `application.properties`:

    ├── lists/       # List (column) CRUD

```properties

aws.ses.access-key=your-access-key    └── cards/       # Card CRUD + drag-and-drop               ┌────▼───┐ ┌──▼───┐ ┌───▼────┐aws.ses.access-key=your-access-key

aws.ses.secret-key=your-secret-key

aws.ses.region=us-east-1````

aws.ses.from=your-verified-email@example.com

```               │ Redis  │ │ AWS  │ │ Google │aws.ses.secret-key=your-secret-key



> **Nota:** AWS SES inicia em **Sandbox mode** — só envia para e-mails verificados. Solicite acesso de produção no console SES para enviar para qualquer destinatário.---



---               │ Cache  │ │ SES  │ │ OAuth  │aws.ses.region=us-east-1



## Google OAuth (opcional)## ✨ Features



1. Acesse https://console.cloud.google.com/apis/credentials               └────────┘ └──────┘ └────────┘aws.ses.from=your-verified-email@example.com

2. Crie um OAuth 2.0 Client ID (Web application)

3. Copie o Client ID e preencha `google.client-id` em `application.properties`| Category | Details |



---|----------|---------|````



## Fazendo requisições| **Authentication** | JWT access tokens (4h) + refresh tokens (7d, Redis-cached) |



Abra o Swagger UI em http://localhost:8080/swagger-ui.html ou use qualquer cliente HTTP.| **Google OAuth** | One-click sign-in with automatic account creation |### Module StructureTo get these credentials, see the [AWS SES setup guide](#aws-ses-setup) below.



Todos os endpoints protegidos exigem o header:| **Email Verification** | HTML email templates via AWS SES + Thymeleaf |



```| **Password Reset** | Secure token-based flow with 30-minute expiry |````**3 — Run the application**

Authorization: Bearer <access_token>

```| **Workspaces** | Multi-workspace support with member roles (OWNER, MEMBER) |



---| **Boards** | Kanban boards within workspaces |src/main/java/com/example/taskmanagerapi/



## Fluxo de autenticação| **Lists** | Ordered columns with drag-and-drop reordering |



**1 — Registre uma conta**| **Cards** | Cards with status (ACTIVE, ARCHIVED, COMPLETED) + cross-list moves |├── config/ # OpenAPI & Redis configurationOpen a terminal in the project root and run:



```| **Members** | Workspace & board-level access control with HTML invite emails |

POST > http://localhost:8080/auth/register

```| **Pagination** | Optional pagination on listing endpoints (`?page=0&size=20`) |├── infra/



```json| **Audit Logging** | All auth events persisted to database for security tracking |

{

  "name": "John Doe",| **API Documentation** | Interactive Swagger UI with detailed schemas |│ ├── cors/ # CORS policy```bash

  "username": "johndoe",

  "email": "john@example.com",---│ ├── exception/ # Global exception handlerdocker-compose up -d

  "password": "secret123",

  "confirmPassword": "secret123"## 🛠️ Tech Stack│ └── security/ # JWT filter, token service, security config```

}

```- **Runtime:** Java 17, Spring Boot 3.5└── modules/



Um e-mail de verificação será enviado para o endereço informado.- **Database:** PostgreSQL 16 (JPA/Hibernate)



**2 — Verifique o e-mail**- **Cache:** Redis 7 (refresh token caching via Jedis) ├── auth/ # Authentication, users, email, auditAfter running, the following services will be available:



```- **Auth:** JWT (`com.auth0:java-jwt`), Google OAuth (`google-api-client`)

POST > http://localhost:8080/auth/verify-email

```- **Email:** AWS SES + Thymeleaf HTML templates ├── workspaces/ # Workspace CRUD + members



```json- **Docs:** Springdoc OpenAPI 2.8 (Swagger UI)

{ "token": "<token_do_email>" }

```- **Build:** Maven with Lombok annotation processing ├── boards/ # Board CRUD + members| Service | Port | URL |



**3 — Faça login**- **Container:** Docker + Docker Compose



```  ├── lists/ # List (column) CRUD| ---------- | ---- | ------------------------------------- |

POST > http://localhost:8080/auth/login

```---



```json    └── cards/       # Card CRUD + drag-and-drop| API        | 8080 | http://localhost:8080                 |

{ "emailOrUsername": "johndoe", "password": "secret123" }

```## 🚀 Getting Started



Response:````| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |



```json### Prerequisites

{

  "name": "John Doe",| PostgreSQL | 5432 | localhost:5432                        |

  "accessToken": "<access_token>",

  "refreshToken": "<refresh_token>"- Java 17+

}

```- Docker & Docker Compose (for PostgreSQL + Redis)---| Redis      | 6379 | localhost:6379                        |



O access token expira em **4 horas**. Use o refresh token para obter um novo:- AWS account with SES configured (for emails)



```- Google Cloud project with OAuth 2.0 client ID (for Google login)

POST > http://localhost:8080/auth/refresh

```



```json### 1. Clone & configure## ✨ Features**PostgreSQL connection:**

{ "refreshToken": "<refresh_token>" }

```



---```bash



## Exemplo de usogit clone https://github.com/jonasmessias/spring-task-manager-api.git



**Crie um workspace**cd spring-task-manager-api| Category | Details || Field    | Value         |



```

POST > http://localhost:8080/workspaces

Authorization: Bearer <access_token># Copy the example config and fill in your secrets|----------|---------|| -------- | ------------- |

```

cp src/main/resources/application-example.properties src/main/resources/application.properties

```json

{ "name": "My Workspace" }```| **Authentication** | JWT access tokens (4h) + refresh tokens (7d, Redis-cached) || Host     | `localhost`   |

```



**Crie um board dentro do workspace**

Edit `application.properties` with your credentials:| **Google OAuth** | One-click sign-in with automatic account creation || Port     | `5432`        |

```

POST > http://localhost:8080/boards?workspaceId=<workspace_id>

Authorization: Bearer <access_token>

```- PostgreSQL connection| **Email Verification** | HTML email templates via AWS SES + Thymeleaf || Database | `taskmanager` |



```json- Redis connection

{ "name": "My Project", "type": "BOARD", "description": "Main project board" }

```- JWT secret key| **Password Reset** | Secure token-based flow with 30-minute expiry || Username | `admin`       |



**Crie uma lista dentro do board**- AWS SES credentials



```- Google OAuth client ID| **Workspaces** | Multi-workspace support with member roles (OWNER, ADMIN, MEMBER) || Password | `admin`       |

POST > http://localhost:8080/boards/<board_id>/lists

Authorization: Bearer <access_token>- Frontend URLs

```

| **Boards** | Kanban boards within workspaces |

```json

{ "name": "To Do" }### 2. Start infrastructure

```

| **Lists** | Ordered columns with drag-and-drop reordering |---

**Crie um card dentro da lista**

```bash

```

POST > http://localhost:8080/boards/<board_id>/lists/<list_id>/cardsdocker-compose up -d    # Starts PostgreSQL + Redis| **Cards** | Cards with status (ACTIVE, ARCHIVED, COMPLETED) + cross-list moves |

Authorization: Bearer <access_token>

```````



```json| **Members** | Workspace & board-level access control |## Making requests

{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }

```| Service | Port | URL |



**Mova um card para outra lista**|------------|------|---------------------------------------|| **Pagination** | Optional pagination on listing endpoints (`?page=0&size=20`) |



```| PostgreSQL | 5432 | `localhost:5432` (db: `taskmanager`) |

PATCH > http://localhost:8080/boards/<board_id>/lists/<list_id>/cards/<card_id>/move

Authorization: Bearer <access_token>| Redis | 6379 | `localhost:6379` || **Audit Logging** | All auth events persisted to database for security tracking |Open Swagger UI at http://localhost:8080/swagger-ui.html or use any HTTP client.

```

### 3. Run the API| **API Documentation** | Interactive Swagger UI with detailed schemas |

```json

{ "targetListId": "<target_list_id>", "position": 0 }`````bashAll protected endpoints require the header:

```

# Using Maven Wrapper

---

./mvnw spring-boot:run---

## Endpoints



### Auth

# Or build and run the JAR````

| Método | Path                      | Auth | Status | Descrição                                    |

| ------ | ------------------------- | ---- | ------ | -------------------------------------------- |./mvnw clean package -DskipTests

| POST   | /auth/register            | Não  | 201    | Criar conta e enviar e-mail de verificação   |

| POST   | /auth/verify-email        | Não  | 200    | Verificar e-mail com token                   |java -jar target/task-manager-api-1.0.0.jar## 🛠️ Tech StackAuthorization: Bearer <access_token>

| POST   | /auth/resend-verification | Não  | 200    | Reenviar e-mail de verificação               |

| POST   | /auth/login               | Não  | 200    | Login, retorna access + refresh token        |`````

| POST   | /auth/google              | Não  | 200    | Login/registro via Google ID token           |

| POST   | /auth/refresh             | Não  | 200    | Obter novo access token                      |````````

| POST   | /auth/logout              | Sim  | 200    | Invalidar sessão atual                       |

| POST   | /auth/logout-all          | Sim  | 200    | Invalidar todas as sessões                   |The API will be available at `http://localhost:8080`.

| POST   | /auth/forgot-password     | Não  | 200    | Enviar e-mail de reset de senha              |

| POST   | /auth/reset-password      | Não  | 200    | Resetar senha com token                      |- **Runtime:** Java 17, Spring Boot 3.5



### Users### 4. Explore the API



| Método | Path        | Auth | Status | Descrição                    |- **Database:** PostgreSQL 16 (JPA/Hibernate)---

| ------ | ----------- | ---- | ------ | ---------------------------- |

| GET    | /users/me   | Sim  | 200    | Obter usuário autenticado    |Open **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

| PUT    | /users/me   | Sim  | 200    | Atualizar perfil             |

| DELETE | /users/me   | Sim  | 204    | Deletar conta própria        |- **Cache:** Redis 7 (refresh token caching via Jedis)

| GET    | /users/{id} | Sim  | 200    | Obter usuário por ID         |

---

### Workspaces

- **Auth:** JWT (`com.auth0:java-jwt`), Google OAuth (`google-api-client`)## Authentication flow

| Método | Path             | Auth | Status | Descrição                         |

| ------ | ---------------- | ---- | ------ | --------------------------------- |## 📡 API Endpoints

| POST   | /workspaces      | Sim  | 201    | Criar workspace                   |

| GET    | /workspaces      | Sim  | 200    | Listar workspaces do usuário      |- **Email:** AWS SES + Thymeleaf HTML templates

| GET    | /workspaces/{id} | Sim  | 200    | Obter workspace com boards        |

| PUT    | /workspaces/{id} | Sim  | 200    | Atualizar workspace               |### Authentication

| DELETE | /workspaces/{id} | Sim  | 204    | Deletar workspace e todo conteúdo |

- **Docs:** Springdoc OpenAPI 2.8 (Swagger UI)**1 — Register an account**

### Workspace Members

| Method | Endpoint | Description | Auth |

| Método | Path                              | Auth | Status | Descrição                         |

| ------ | --------------------------------- | ---- | ------ | --------------------------------- ||--------|----------|-------------|------|- **Build:** Maven with Lombok annotation processing

| POST   | /workspaces/{id}/members          | Sim  | 201    | Convidar membro (envia e-mail)    |

| GET    | /workspaces/{id}/members          | Sim  | 200    | Listar membros                    || `POST` | `/auth/register` | Create account + verification email | ❌ |

| DELETE | /workspaces/{id}/members/{userId} | Sim  | 200    | Remover membro                    |

| `POST` | `/auth/login` | Login with email/username + password | ❌ |- **Container:** Docker + Docker Compose```

### Boards

| `POST` | `/auth/google` | Login/register with Google ID token | ❌ |

| Método | Path                     | Auth | Status | Descrição                                  |

| ------ | ------------------------ | ---- | ------ | ------------------------------------------ || `POST` | `/auth/refresh` | Get new access token | ❌ |POST > http://localhost:8080/auth/register

| POST   | /boards?workspaceId={id} | Sim  | 201    | Criar board no workspace                   |

| GET    | /boards?workspaceId={id} | Sim  | 200    | Listar boards (suporta `?page=0&size=20`)  || `POST` | `/auth/verify-email` | Verify email with token | ❌ |

| GET    | /boards/{id}             | Sim  | 200    | Obter board com listas e cards             |

| PUT    | /boards/{id}             | Sim  | 200    | Atualizar board                            || `POST` | `/auth/resend-verification` | Resend verification email | ❌ |---```

| DELETE | /boards/{id}             | Sim  | 204    | Deletar board e todo conteúdo              |

| `POST` | `/auth/forgot-password` | Send password reset email | ❌ |

### Board Members

| `POST` | `/auth/reset-password` | Reset password with token | ❌ |

| Método | Path                          | Auth | Status | Descrição                         |

| ------ | ----------------------------- | ---- | ------ | --------------------------------- || `POST` | `/auth/logout` | Logout current device | ✅ |

| POST   | /boards/{id}/members          | Sim  | 201    | Convidar membro (envia e-mail)    |

| GET    | /boards/{id}/members          | Sim  | 200    | Listar membros                    || `POST` | `/auth/logout-all` | Logout all devices | ✅ |## 🚀 Getting Started```json

| DELETE | /boards/{id}/members/{userId} | Sim  | 200    | Remover membro                    |



### Lists

### Workspaces{

| Método | Path                             | Auth | Status | Descrição               |

| ------ | -------------------------------- | ---- | ------ | ----------------------- |

| POST   | /boards/{boardId}/lists          | Sim  | 201    | Criar lista             |

| GET    | /boards/{boardId}/lists          | Sim  | 200    | Listar todas as listas  || Method | Endpoint | Description | Auth |### Prerequisites  "name": "John Doe",

| GET    | /boards/{boardId}/lists/{listId} | Sim  | 200    | Obter lista             |

| PUT    | /boards/{boardId}/lists/{listId} | Sim  | 200    | Atualizar lista         ||--------|----------|-------------|------|

| DELETE | /boards/{boardId}/lists/{listId} | Sim  | 204    | Deletar lista e cards   |

| `POST` | `/workspaces` | Create workspace | ✅ |  "username": "johndoe",

### Cards

| `GET` | `/workspaces` | List user's workspaces | ✅ |

| Método | Path                                                 | Auth | Status | Descrição                    |

| ------ | ---------------------------------------------------- | ---- | ------ | ---------------------------- || `GET` | `/workspaces/{id}` | Get workspace details | ✅ |- Java 17+  "email": "john@example.com",

| POST   | /boards/{boardId}/lists/{listId}/cards               | Sim  | 201    | Criar card                   |

| GET    | /boards/{boardId}/lists/{listId}/cards               | Sim  | 200    | Listar cards (`?page=0&size=50`) || `PUT` | `/workspaces/{id}` | Update workspace | ✅ |

| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 200    | Obter card                   |

| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 200    | Atualizar card               || `DELETE` | `/workspaces/{id}` | Delete workspace | ✅ |- Docker & Docker Compose (for PostgreSQL + Redis)  "password": "secret123",

| PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Sim  | 200    | Mover card para outra lista  |

| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 204    | Deletar card                 |



Status do card: `ACTIVE`, `ARCHIVED`, `COMPLETED`.### Workspace Members- AWS account with SES configured (for emails)  "confirmPassword": "secret123"



---



## Error responses| Method | Endpoint | Description | Auth |- Google Cloud project with OAuth 2.0 client ID (for Google login)}



Todos os erros seguem o formato:|--------|----------|-------------|------|



```json| `POST` | `/workspaces/{id}/members` | Invite member (sends HTML email) | ✅ |````

{

  "code": "ERROR_CODE",| `GET` | `/workspaces/{id}/members` | List members | ✅ |

  "message": "Human readable message.",

  "statusCode": 400| `DELETE` | `/workspaces/{id}/members/{userId}` | Remove member | ✅ |### 1. Clone & configure

}

```



### Auth errors### BoardsA verification email will be sent to the provided address.



| Code                      | Status | Descrição                            |

| ------------------------- | ------ | ------------------------------------ |

| `INVALID_CREDENTIALS`     | 401    | Credenciais inválidas                || Method | Endpoint | Description | Auth |```bash

| `EMAIL_NOT_VERIFIED`      | 403    | E-mail não verificado                |

| `USE_GOOGLE_LOGIN`        | 401    | Conta OAuth, sem senha               ||--------|----------|-------------|------|

| `INVALID_GOOGLE_TOKEN`    | 401    | Token Google inválido                |

| `EMAIL_ALREADY_EXISTS`    | 400    | E-mail já registrado                 || `POST` | `/boards?workspaceId={id}` | Create board | ✅ |git clone https://github.com/jonasmessias/spring-task-manager-api.git**2 — Verify your email**

| `USERNAME_ALREADY_EXISTS` | 400    | Username já em uso                   |

| `PASSWORDS_DO_NOT_MATCH`  | 400    | Senhas não conferem                  || `GET` | `/boards?workspaceId={id}` | List boards (supports `?page=0&size=20`) | ✅ |

| `INVALID_TOKEN`           | 400    | Token de verificação/reset inválido  |

| `EXPIRED_TOKEN`           | 400    | Token expirado                       || `GET` | `/boards/{id}` | Get board with lists & cards | ✅ |cd spring-task-manager-api

| `EMAIL_NOT_FOUND`         | 404    | E-mail não encontrado                |

| `EMAIL_SEND_ERROR`        | 500    | Falha no AWS SES                     || `PUT` | `/boards/{id}` | Update board | ✅ |



### Resource errors| `DELETE` | `/boards/{id}` | Delete board | ✅ |```



| Code                  | Status | Descrição            |

| --------------------- | ------ | -------------------- |

| `WORKSPACE_NOT_FOUND` | 404    | Workspace não existe |### Board Members# Copy the example config and fill in your secretsPOST > http://localhost:8080/auth/verify-email

| `BOARD_NOT_FOUND`     | 404    | Board não existe     |

| `LIST_NOT_FOUND`      | 404    | Lista não existe     |

| `CARD_NOT_FOUND`      | 404    | Card não existe      |

| `FORBIDDEN`           | 403    | Sem permissão        || Method | Endpoint | Description | Auth |cp src/main/resources/application-example.properties src/main/resources/application.properties```

| `VALIDATION_ERROR`    | 400    | Falha de validação   |

|--------|----------|-------------|------|

### Members errors

| `POST` | `/boards/{id}/members` | Invite member (sends HTML email) | ✅ |````

| Code                    | Status | Descrição                          |

| ----------------------- | ------ | ---------------------------------- || `GET` | `/boards/{id}/members` | List members | ✅ |

| `USER_NOT_FOUND`        | 404    | Usuário não encontrado             |

| `USER_ALREADY_MEMBER`   | 400    | Já é membro                        || `DELETE` | `/boards/{id}/members/{userId}` | Remove member | ✅ |```json

| `USER_NOT_IN_WORKSPACE` | 400    | Precisa ser membro do workspace    |

| `MEMBER_NOT_FOUND`      | 404    | Membro não encontrado              |

| `CANNOT_REMOVE_OWNER`   | 400    | Owner não pode ser removido        |

### ListsEdit `application.properties` with your credentials:{ "token": "<token_from_email>" }



| Method | Endpoint | Description | Auth |````

|--------|----------|-------------|------|

| `POST` | `/boards/{boardId}/lists` | Create list | ✅ |- PostgreSQL connection

| `GET` | `/boards/{boardId}/lists` | Get all lists | ✅ |

| `PUT` | `/boards/{boardId}/lists/{id}` | Update list | ✅ |- Redis connection**3 — Login**

| `DELETE` | `/boards/{boardId}/lists/{id}` | Delete list | ✅ |

- JWT secret key

### Cards

- AWS SES credentials```

| Method | Endpoint | Description | Auth |

|--------|----------|-------------|------|- Google OAuth client IDPOST > http://localhost:8080/auth/login

| `POST` | `/boards/{bId}/lists/{lId}/cards` | Create card | ✅ |

| `GET` | `/boards/{bId}/lists/{lId}/cards` | List cards (supports `?page=0&size=50`) | ✅ |- Frontend URLs```

| `GET` | `/boards/{bId}/lists/{lId}/cards/{id}` | Get card | ✅ |

| `PUT` | `/boards/{bId}/lists/{lId}/cards/{id}` | Update card | ✅ |### 2. Start infrastructure```json

| `PATCH` | `/boards/{bId}/lists/{lId}/cards/{id}/move` | Move card to another list | ✅ |

| `DELETE` | `/boards/{bId}/lists/{lId}/cards/{id}` | Delete card | ✅ |{ "emailOrUsername": "johndoe", "password": "secret123" }



---`bash`



## 🔐 Authentication Flowdocker-compose up -d # Starts PostgreSQL + Redis



```````Response:

┌────────┐                      ┌────────┐                    ┌───────┐

│ Client │                      │  API   │                    │ Redis │

└───┬────┘                      └───┬────┘                    └───┬───┘

    │  POST /auth/login             │                             │### 3. Run the API```json

    │  {email, password}            │                             │

    │──────────────────────────────►│                             │{

    │                               │  Create refresh token       │

    │                               │────────────────────────────►│```bash  "name": "John Doe",

    │  {accessToken, refreshToken}  │                             │

    │◄──────────────────────────────│                             │# Using Maven Wrapper  "accessToken": "<access_token>",

    │                               │                             │

    │  GET /boards (Bearer token)   │                             │./mvnw spring-boot:run  "refreshToken": "<refresh_token>"

    │──────────────────────────────►│                             │

    │  200 OK                       │                             │}

    │◄──────────────────────────────│                             │

    │                               │                             │# Or build and run the JAR```

    │  POST /auth/refresh           │                             │

    │  {refreshToken}               │                             │./mvnw clean package -DskipTests

    │──────────────────────────────►│  Validate from cache        │

    │                               │────────────────────────────►│java -jar target/task-manager-api-1.0.0.jarAccess token expires in **4 hours**. Use the refresh token to get a new one:

    │  {newAccessToken}             │                             │

    │◄──────────────────────────────│                             │````

```

```

---

The API will be available at `http://localhost:8080`.POST > http://localhost:8080/auth/refresh

## 📧 Email Templates

```

The API sends HTML emails using Thymeleaf templates via AWS SES:

### 4. Explore the API

| Template | Trigger | Expiry |

|----------|---------|--------|```json

| `email-verification.html` | Registration, resend verification | 24 hours |

| `password-reset.html` | Forgot password | 30 minutes |Open **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html){ "refreshToken": "<refresh_token>" }

| `member-invite.html` | Workspace/board member invitation | — |

```

Templates are located in `src/main/resources/templates/`.

---

---

---

## 📝 Error Codes

## 📡 API Endpoints

All error responses follow the format:

## Usage example

```json

{### Authentication

  "code": "ERROR_CODE",

  "message": "Human-readable message",**Create a workspace**

  "statusCode": 400

}| Method | Endpoint | Description | Auth |

```

|--------|----------|-------------|------|```

| Code | Status | Description |

|------|--------|-------------|| `POST` | `/auth/register` | Create account + verification email | ❌ |POST > http://localhost:8080/workspaces

| `INVALID_CREDENTIALS` | 401 | Wrong email/password |

| `EMAIL_NOT_VERIFIED` | 403 | Account not verified || `POST` | `/auth/login` | Login with email/username + password | ❌ |Authorization: Bearer <access_token>

| `USE_GOOGLE_LOGIN` | 401 | OAuth-only account |

| `INVALID_GOOGLE_TOKEN` | 401 | Bad Google ID token || `POST` | `/auth/google` | Login/register with Google ID token | ❌ |```

| `EMAIL_ALREADY_EXISTS` | 400 | Duplicate email |

| `USERNAME_ALREADY_EXISTS` | 400 | Duplicate username || `POST` | `/auth/refresh` | Get new access token | ❌ |

| `PASSWORDS_DO_NOT_MATCH` | 400 | Password mismatch |

| `INVALID_TOKEN` | 400/401 | Bad verification/reset token || `POST` | `/auth/verify-email` | Verify email with token | ❌ |```json

| `EXPIRED_TOKEN` | 400/401 | Token expired |

| `EMAIL_NOT_FOUND` | 404 | Email not registered || `POST` | `/auth/resend-verification` | Resend verification email | ❌ |{ "name": "My Workspace" }

| `EMAIL_SEND_ERROR` | 500 | AWS SES failure |

| `WORKSPACE_NOT_FOUND` | 404 | Workspace doesn't exist || `POST` | `/auth/forgot-password` | Send password reset email | ❌ |```

| `BOARD_NOT_FOUND` | 404 | Board doesn't exist |

| `LIST_NOT_FOUND` | 404 | List doesn't exist || `POST` | `/auth/reset-password` | Reset password with token | ❌ |

| `CARD_NOT_FOUND` | 404 | Card doesn't exist |

| `FORBIDDEN` | 403 | Insufficient permissions || `POST` | `/auth/logout` | Logout current device | ✅ |**Create a board inside the workspace**

| `VALIDATION_ERROR` | 400 | Request body validation failed |

| `POST` | `/auth/logout-all` | Logout all devices | ✅ |

---

````````

## 🐳 Docker

### WorkspacesPOST > http://localhost:8080/boards?workspaceId=<workspace_id>

### Development (infrastructure only)

Authorization: Bearer <access_token>

````bash

docker-compose up -d    # PostgreSQL + Redis| Method | Endpoint | Description | Auth |```

./mvnw spring-boot:run  # API on host

```|--------|----------|-------------|------|



### Production (full stack)| `POST` | `/workspaces` | Create workspace | ✅ |```json



```bash| `GET` | `/workspaces` | List user's workspaces | ✅ |{ "name": "My Project", "type": "BOARD", "description": "Main project board" }

docker build -t task-manager-api .

docker run -p 8080:8080 --env-file .env task-manager-api| `GET` | `/workspaces/{id}` | Get workspace details | ✅ |```

````

| `PUT` | `/workspaces/{id}` | Update workspace | ✅ |

---

| `DELETE` | `/workspaces/{id}` | Delete workspace | ✅ |**Create a list inside the board**

## ☁️ AWS SES Setup

1. **Create an IAM user** at [AWS IAM Console](https://console.aws.amazon.com/iam)
   - Name: `task-manager-ses`### Workspace Members```

   - Attach policy: `AmazonSESFullAccess`

   - Create access key → copy both keysPOST > http://localhost:8080/boards/<board_id>/lists

2. **Verify sender email** at [AWS SES Console](https://console.aws.amazon.com/ses)| Method | Endpoint | Description | Auth |Authorization: Bearer <access_token>
   - Region: `us-east-1`

   - Verified Identities → Create Identity → Email address|--------|----------|-------------|------|```

   - Confirm the verification email

| `POST` | `/workspaces/{id}/members` | Add member | ✅ |

3. **Fill in `application.properties`**

| `GET` | `/workspaces/{id}/members` | List members | ✅ |```json

````properties

aws.ses.access-key=AKIA...| `PUT` | `/workspaces/{id}/members/{memberId}` | Update role | ✅ |{ "name": "To Do" }

aws.ses.secret-key=...

aws.ses.region=us-east-1| `DELETE` | `/workspaces/{id}/members/{memberId}` | Remove member | ✅ |```

aws.ses.from=your-verified-email@example.com

````

> **Note:** AWS SES starts in **Sandbox mode** — you can only send to verified emails.### Boards**Create a card inside the list**

> Request production access in the SES console to send to anyone.

---

| Method | Endpoint | Description | Auth |```

## 📄 License

|--------|----------|-------------|------|POST > http://localhost:8080/boards/<board_id>/lists/<list_id>/cards

This project is licensed under the MIT License.

| `POST` | `/boards?workspaceId={id}` | Create board | ✅ |Authorization: Bearer <access_token>

---

| `GET` | `/boards?workspaceId={id}` | List boards (supports `?page=0&size=20`) | ✅ |```

**Built with ❤️ by [Jonas Messias](https://github.com/jonasmessias)**

| `GET` | `/boards/{id}` | Get board with lists & cards | ✅ |

| `PUT` | `/boards/{id}` | Update board | ✅ |```json

| `DELETE` | `/boards/{id}` | Delete board | ✅ |{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }

`````

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

`````

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
