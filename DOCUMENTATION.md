# Task Manager API

RESTful API for task and board management built with Spring Boot 3.5.11, Java 17, H2, Redis 7 and JWT.

---

## Running

```bash
# Docker (recommended)
docker-compose up -d

# Local (requires Redis on port 6379)
mvn spring-boot:run
```

| Service    | URL                                   |
| ---------- | ------------------------------------- |
| API        | http://localhost:8080                 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console      |

H2 credentials: JDBC URL `jdbc:h2:mem:testdb`, username `admin`, password empty.

---

## Configuration

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=admin
spring.datasource.password=

spring.data.redis.host=localhost
spring.data.redis.port=6379

api.security.token.secret=your-secret-key
app.frontend.url=http://localhost:4200

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

Access token expires in **4 hours**. Refresh token expires in **7 days**.

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /auth/register | No | Register new account |
| POST | /auth/verify-email | No | Verify email with token from inbox |
| POST | /auth/resend-verification | No | Resend verification email |
| POST | /auth/login | No | Login, returns access + refresh token |
| POST | /auth/refresh | No | Get new access token |
| POST | /auth/logout | Yes | Invalidate current refresh token |
| POST | /auth/logout-all | Yes | Invalidate all refresh tokens |
| POST | /auth/forgot-password | No | Send password reset email |
| POST | /auth/reset-password | No | Reset password with token |

### Registration flow

```
POST /auth/register            -> account created, verification email sent
POST /auth/verify-email        -> account activated
POST /auth/login               -> returns tokens
```

Attempting to login before verifying returns:
```json
{ "code": "EMAIL_NOT_VERIFIED", "message": "..." }
```

### Payloads

**Register**
```json
{
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "confirmPassword": "secret123"
}
```

**Login** — accepts email or username in `emailOrUsername`
```json
{ "emailOrUsername": "johndoe", "password": "secret123" }
```

**Login response**
```json
{ "name": "John Doe", "token": "<access_token>", "refreshToken": "<refresh_token>" }
```

**Verify email / Refresh token**
```json
{ "token": "<token_from_email>" }
{ "refreshToken": "<refresh_token>" }
```

**Resend verification / Forgot password**
```json
{ "email": "john@example.com" }
```

**Reset password**
```json
{ "token": "<token_from_email>", "newPassword": "new123", "confirmNewPassword": "new123" }
```

---

## Users

| Method | Path | Description |
|--------|------|-------------|
| GET | /users/profile | Get current user profile |
| PUT | /users/profile | Update profile |
| DELETE | /users/profile | Delete account |

---

## Workspaces

| Method | Path | Description |
|--------|------|-------------|
| POST | /workspaces | Create workspace |
| GET | /workspaces | List all workspaces |
| GET | /workspaces/{id} | Get workspace with boards |
| PUT | /workspaces/{id} | Update workspace |
| DELETE | /workspaces/{id} | Delete workspace and all its content |

---

## Boards

| Method | Path | Description |
|--------|------|-------------|
| POST | /boards?workspaceId={id} | Create board in workspace |
| GET | /boards?workspaceId={id} | List boards from workspace |
| GET | /boards/{id} | Get board with lists and cards |
| PUT | /boards/{id} | Update board |
| DELETE | /boards/{id} | Delete board and all its content |

---

## Lists

| Method | Path | Description |
|--------|------|-------------|
| POST | /boards/{boardId}/lists | Create list |
| GET | /boards/{boardId}/lists | Get all lists |
| GET | /boards/{boardId}/lists/{listId} | Get list |
| PUT | /boards/{boardId}/lists/{listId} | Update list |
| DELETE | /boards/{boardId}/lists/{listId} | Delete list and its cards |

---

## Cards

| Method | Path | Description |
|--------|------|-------------|
| POST | /boards/{boardId}/lists/{listId}/cards | Create card |
| GET | /boards/{boardId}/lists/{listId}/cards | Get all cards |
| GET | /boards/{boardId}/lists/{listId}/cards/{cardId} | Get card |
| PUT | /boards/{boardId}/lists/{listId}/cards/{cardId} | Update card |
| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId} | Delete card |

Card status values: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

---

## Error responses

```json
{ "code": "ERROR_CODE", "message": "Human readable message." }
```

| Code | Status | Description |
|------|--------|-------------|
| EMAIL_NOT_VERIFIED | 403 | Account pending verification |
| INVALID_CREDENTIALS | 400 | Wrong email/username or password |
| EMAIL_NOT_FOUND | 400 | No account with that email |
| EMAIL_ALREADY_VERIFIED | 400 | Account already verified |

---

## Gmail setup

1. Enable 2FA on your Google account
2. Generate an App Password at https://myaccount.google.com/apppasswords
3. Use the 16-character password in `spring.mail.password`

---

*Last updated: March 6, 2026*
