# Task Manager API

Java · Spring Boot · Spring Security · JWT · Redis · PostgreSQL · Docker · AWS SES

---

<!-- Add your architecture diagram image here -->
<!-- ![Diagram](diagram.png) -->

---

## Setup

**1 — Install Docker**

https://www.docker.com

**2 — Configure AWS SES (email sending)**

Open `src/main/resources/application.properties` and fill in your AWS credentials:

```properties
aws.ses.access-key=your-access-key
aws.ses.secret-key=your-secret-key
aws.ses.region=us-east-1
aws.ses.from=your-verified-email@example.com
```

To get these credentials, see the [AWS SES setup guide](#aws-ses-setup) below.

**3 — Run the application**

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

| Field    | Value         |
| -------- | ------------- |
| Host     | `localhost`   |
| Port     | `5432`        |
| Database | `taskmanager` |
| Username | `admin`       |
| Password | `admin`       |

---

## Making requests

Open Swagger UI at http://localhost:8080/swagger-ui.html or use any HTTP client.

All protected endpoints require the header:

```
Authorization: Bearer <access_token>
```

---

## Authentication flow

**1 — Register an account**

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

**2 — Verify your email**

```
POST > http://localhost:8080/auth/verify-email
```

```json
{ "token": "<token_from_email>" }
```

**3 — Login**

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

## Endpoints

### Auth

| Method | Path                      | Auth | Status | Description                                |
| ------ | ------------------------- | ---- | ------ | ------------------------------------------ |
| POST   | /auth/register            | No   | 201    | Create account and send verification email |
| POST   | /auth/verify-email        | No   | 200    | Verify email with token from inbox         |
| POST   | /auth/resend-verification | No   | 200    | Resend verification email                  |
| POST   | /auth/login               | No   | 200    | Login and receive access + refresh token   |
| POST   | /auth/refresh             | No   | 200    | Get new access token using refresh token   |
| POST   | /auth/logout              | Yes  | 200    | Invalidate current session                 |
| POST   | /auth/logout-all          | Yes  | 200    | Invalidate all sessions                    |
| POST   | /auth/forgot-password     | No   | 200    | Send password reset email                  |
| POST   | /auth/reset-password      | No   | 200    | Reset password with token                  |

### Users

| Method | Path        | Auth | Status | Description                     |
| ------ | ----------- | ---- | ------ | ------------------------------- |
| GET    | /users/me   | Yes  | 200    | Get current authenticated user  |
| PUT    | /users/me   | Yes  | 200    | Update profile (name, username) |
| DELETE | /users/me   | Yes  | 204    | Delete own account              |
| GET    | /users/{id} | Yes  | 200    | Get user by ID                  |

### Workspaces

| Method | Path             | Auth | Status | Description                          |
| ------ | ---------------- | ---- | ------ | ------------------------------------ |
| POST   | /workspaces      | Yes  | 201    | Create workspace                     |
| GET    | /workspaces      | Yes  | 200    | List all workspaces                  |
| GET    | /workspaces/{id} | Yes  | 200    | Get workspace with boards            |
| PUT    | /workspaces/{id} | Yes  | 200    | Update workspace                     |
| DELETE | /workspaces/{id} | Yes  | 204    | Delete workspace and all its content |

### Boards

| Method | Path                     | Auth | Status | Description                      |
| ------ | ------------------------ | ---- | ------ | -------------------------------- |
| POST   | /boards?workspaceId={id} | Yes  | 201    | Create board in workspace        |
| GET    | /boards?workspaceId={id} | Yes  | 200    | List boards from workspace       |
| GET    | /boards/{id}             | Yes  | 200    | Get board with lists and cards   |
| PUT    | /boards/{id}             | Yes  | 200    | Update board                     |
| DELETE | /boards/{id}             | Yes  | 204    | Delete board and all its content |

### Workspace Members

| Method | Path                              | Auth | Status | Description                        |
| ------ | --------------------------------- | ---- | ------ | ---------------------------------- |
| GET    | /workspaces/{id}/members          | Yes  | 200    | List all members of a workspace    |
| POST   | /workspaces/{id}/members          | Yes  | 201    | Invite a user by email or username |
| DELETE | /workspaces/{id}/members/{userId} | Yes  | 200    | Remove a member from workspace     |

### Board Members

| Method | Path                          | Auth | Status | Description                             |
| ------ | ----------------------------- | ---- | ------ | --------------------------------------- |
| GET    | /boards/{id}/members          | Yes  | 200    | List all members of a board             |
| POST   | /boards/{id}/members          | Yes  | 201    | Invite a workspace member to this board |
| DELETE | /boards/{id}/members/{userId} | Yes  | 200    | Remove a member from board              |

### Lists

| Method | Path                             | Auth | Status | Description               |
| ------ | -------------------------------- | ---- | ------ | ------------------------- |
| POST   | /boards/{boardId}/lists          | Yes  | 201    | Create list               |
| GET    | /boards/{boardId}/lists          | Yes  | 200    | Get all lists             |
| GET    | /boards/{boardId}/lists/{listId} | Yes  | 200    | Get list                  |
| PUT    | /boards/{boardId}/lists/{listId} | Yes  | 200    | Update list               |
| DELETE | /boards/{boardId}/lists/{listId} | Yes  | 204    | Delete list and its cards |

### Cards

| Method | Path                                                 | Auth | Status | Description                   |
| ------ | ---------------------------------------------------- | ---- | ------ | ----------------------------- |
| POST   | /boards/{boardId}/lists/{listId}/cards               | Yes  | 201    | Create card                   |
| GET    | /boards/{boardId}/lists/{listId}/cards               | Yes  | 200    | Get all cards                 |
| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200    | Get card                      |
| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200    | Update card                   |
| PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Yes  | 200    | Move card to a different list |
| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 204    | Delete card                   |

Card status values: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

---

## Error responses

All errors follow the format:

```json
{
  "code": "SNAKE_CASE_CODE",
  "message": "Human readable message.",
  "statusCode": 400,
  "timestamp": "2026-03-06T12:00:00Z"
}
```

| Code                      | HTTP Status | Description                                  |
| ------------------------- | ----------- | -------------------------------------------- |
| `PASSWORDS_DO_NOT_MATCH`  | 400         | Passwords do not match                       |
| `EMAIL_ALREADY_EXISTS`    | 400         | Email already registered                     |
| `USERNAME_ALREADY_EXISTS` | 400         | Username already taken                       |
| `INVALID_TOKEN`           | 400         | Token not found in database                  |
| `EXPIRED_TOKEN`           | 400         | Token has passed its 24h expiry              |
| `EMAIL_ALREADY_VERIFIED`  | 400         | Account already active                       |
| `INVALID_CREDENTIALS`     | 401         | Wrong email/username or password             |
| `EMAIL_NOT_VERIFIED`      | 403         | Account pending email verification           |
| `EMAIL_NOT_FOUND`         | 404         | No account with that email                   |
| `EMAIL_SEND_ERROR`        | 500         | SMTP failure                                 |
| `EMAIL_AUTH_ERROR`        | 500         | SMTP authentication failure                  |
| `VALIDATION_ERROR`        | 400         | Bean validation failure (see `errors` array) |

Validation error format:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data.",
  "statusCode": 400,
  "errors": [{ "field": "email", "message": "must not be blank" }]
}
```

---

## AWS SES setup

**1 — Create an IAM user**

1. Go to https://console.aws.amazon.com/iam → **Users → Create user**
2. Name: `task-manager-api`
3. Attach policy: `AmazonSESFullAccess`
4. Go to the user → **Security credentials → Create access key**
5. Select **Application running outside AWS** — copy both keys

**2 — Verify sender email**

1. Go to https://console.aws.amazon.com/ses → region `us-east-1`
2. **Verified Identities → Create Identity → Email address**
3. Enter your sender email and confirm the verification email AWS sends

**3 — Fill in `application.properties`**

```properties
aws.ses.access-key=AKIA...
aws.ses.secret-key=...
aws.ses.region=us-east-1
aws.ses.from=your-verified-email@example.com
```

> **Note:** By default AWS SES is in **Sandbox mode** — you can only send to verified emails.
> To send to anyone, request production access in the SES console → **Account dashboard**.
