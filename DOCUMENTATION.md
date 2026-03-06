# Task Manager API

Java · Spring Boot · JWT · Redis · PostgreSQL · Docker

---

## Setup

**1 - Install Docker**
https://www.docker.com

**2 - Run the application**

Open a terminal in the project root and run:

```bash
docker-compose up -d
```

After running, the following services will be available:

| Service    | Port | URL                                   |
| ---------- | ---- | ------------------------------------- |
| API        | 8080 | http://localhost:8080                 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |
| PostgreSQL | 5432 | localhost:5432                        |
| Redis      | 6379 | localhost:6379                        |

**PostgreSQL connection:**

- Host: `localhost`
- Port: `5432`
- Database: `taskmanager`
- Username: `admin`
- Password: `admin`

**3 - Configure email sending**

Open `src/main/resources/application.properties` and set your Gmail credentials:

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

Generate a Gmail App Password at:
https://myaccount.google.com/apppasswords

---

## Making requests

Open Swagger UI at http://localhost:8080/swagger-ui.html or use any HTTP client.

All protected endpoints require the header:

```
Authorization: Bearer <access_token>
```

---

## Authentication flow

**1 - Register an account**

```
POST > http://localhost:8080/auth/register
```

```json
{
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "confirmPassword": "secret123"
}
```

A verification email will be sent to the provided address.

**2 - Verify your email**

```
POST > http://localhost:8080/auth/verify-email
```

```json
{ "token": "<token_from_email>" }
```

**3 - Login**

```
POST > http://localhost:8080/auth/login
```

```json
{ "emailOrUsername": "johndoe", "password": "secret123" }
```

Response:

```json
{
  "name": "John Doe",
  "accessToken": "<access_token>",
  "refreshToken": "<refresh_token>"
}
```

Access token expires in **4 hours**. Use the refresh token to get a new one:

```
POST > http://localhost:8080/auth/refresh
```

```json
{ "refreshToken": "<refresh_token>" }
```

---

## Endpoints

### Auth

| Method | Path                      | Auth | Description                                                |
| ------ | ------------------------- | ---- | ---------------------------------------------------------- |
| POST   | /auth/register            | No   | Register new account                                       |
| POST   | /auth/verify-email        | No   | Verify email with token from inbox                         |
| POST   | /auth/resend-verification | No   | Resend verification email                                  |
| POST   | /auth/login               | No   | Login, returns access + refresh token                      |
| POST   | /auth/refresh             | No   | Get new access token                                       |
| POST   | /auth/logout              | Yes  | Invalidate current session (requires refreshToken in body) |
| POST   | /auth/logout-all          | Yes  | Invalidate all sessions                                    |
| POST   | /auth/forgot-password     | No   | Send password reset email                                  |
| POST   | /auth/reset-password      | No   | Reset password with token                                  |

### Users

| Method | Path        | Auth | Description                     |
| ------ | ----------- | ---- | ------------------------------- |
| GET    | /users/me   | Yes  | Get current authenticated user  |
| PUT    | /users/me   | Yes  | Update profile (name, username) |
| DELETE | /users/me   | Yes  | Delete own account              |
| GET    | /users/{id} | Yes  | Get user by ID                  |

### Workspaces

| Method | Path             | Auth | Description                          |
| ------ | ---------------- | ---- | ------------------------------------ |
| POST   | /workspaces      | Yes  | Create workspace                     |
| GET    | /workspaces      | Yes  | List all workspaces                  |
| GET    | /workspaces/{id} | Yes  | Get workspace with boards            |
| PUT    | /workspaces/{id} | Yes  | Update workspace                     |
| DELETE | /workspaces/{id} | Yes  | Delete workspace and all its content |

### Boards

| Method | Path                     | Auth | Description                      |
| ------ | ------------------------ | ---- | -------------------------------- |
| POST   | /boards?workspaceId={id} | Yes  | Create board in workspace        |
| GET    | /boards?workspaceId={id} | Yes  | List boards from workspace       |
| GET    | /boards/{id}             | Yes  | Get board with lists and cards   |
| PUT    | /boards/{id}             | Yes  | Update board                     |
| DELETE | /boards/{id}             | Yes  | Delete board and all its content |

### Lists

| Method | Path                             | Auth | Description               |
| ------ | -------------------------------- | ---- | ------------------------- |
| POST   | /boards/{boardId}/lists          | Yes  | Create list               |
| GET    | /boards/{boardId}/lists          | Yes  | Get all lists             |
| GET    | /boards/{boardId}/lists/{listId} | Yes  | Get list                  |
| PUT    | /boards/{boardId}/lists/{listId} | Yes  | Update list               |
| DELETE | /boards/{boardId}/lists/{listId} | Yes  | Delete list and its cards |

### Cards

| Method | Path                                                 | Auth | Description                   |
| ------ | ---------------------------------------------------- | ---- | ----------------------------- |
| POST   | /boards/{boardId}/lists/{listId}/cards               | Yes  | Create card                   |
| GET    | /boards/{boardId}/lists/{listId}/cards               | Yes  | Get all cards                 |
| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | Get card                      |
| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | Update card                   |
| PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Yes  | Move card to a different list |
| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | Delete card                   |

Card status values: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

---

## Usage example

**Create a workspace**

```
POST > http://localhost:8080/workspaces
Authorization: Bearer <access_token>
```

```json
{ "name": "My Workspace" }
```

**Create a board inside the workspace**

```
POST > http://localhost:8080/boards?workspaceId=<workspace_id>
Authorization: Bearer <access_token>
```

```json
{ "name": "My Project", "type": "BOARD", "description": "Main project board" }
```

**Create a list inside the board**

```
POST > http://localhost:8080/boards/<board_id>/lists
Authorization: Bearer <access_token>
```

```json
{ "name": "To Do" }
```

**Create a card inside the list**

```
POST > http://localhost:8080/boards/<board_id>/lists/<list_id>/cards
Authorization: Bearer <access_token>
```

```json
{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }
```

---

## Error responses

All errors follow the format:

```json
{ "code": "ERROR_CODE", "message": "Human readable message." }
```

| Code                   | Status | Description                        |
| ---------------------- | ------ | ---------------------------------- |
| EMAIL_NOT_VERIFIED     | 403    | Account pending email verification |
| INVALID_CREDENTIALS    | 400    | Wrong email/username or password   |
| EMAIL_NOT_FOUND        | 400    | No account with that email         |
| EMAIL_ALREADY_VERIFIED | 400    | Account already verified           |

---

_Last updated: March 6, 2026_
