# Task Manager API — Technical Documentation

## Architecture

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Frontend   │◄─────►│  Spring Boot │◄─────►│  PostgreSQL  │
│   (Client)   │  REST │     API      │  JPA  │   Database   │
└──────────────┘       └──────┬───────┘       └──────────────┘
                              │
                    ┌─────────┼─────────┬──────────┐
                    │         │         │          │
               ┌────▼───┐ ┌──▼───┐ ┌───▼────┐ ┌───▼───┐
               │ Redis  │ │ AWS  │ │ Google │ │ AWS   │
               │ Cache  │ │ SES  │ │ OAuth  │ │  S3   │
               └────────┘ └──────┘ └────────┘ └───────┘
```

### Module Structure

```
src/main/java/com/example/taskmanagerapi/
├── config/          # OpenAPI, Redis, AWS S3 configuration
├── infra/
│   ├── cors/        # CORS policy (restricted origins)
│   ├── exception/   # Global exception handler
│   └── security/    # JWT filter, token service, security config
└── modules/
    ├── auth/        # Authentication, users, email, audit
    ├── workspaces/  # Workspace CRUD + members + covers
    ├── boards/      # Board CRUD + members + covers
    ├── lists/       # List (column) CRUD
    ├── cards/       # Card CRUD + drag-and-drop + attachments
    └── storage/     # AWS S3 file upload (direct + presigned URLs)
```

---

## Tech Stack

| Technology        | Purpose                                |
| ----------------- | -------------------------------------- |
| Java 17           | Language                               |
| Spring Boot 3.5   | Framework                              |
| Spring Security   | Authentication & authorization         |
| JWT (java-jwt)    | Access token (4h expiry)               |
| Redis 7           | Refresh token caching (7d)             |
| PostgreSQL 16     | Primary database                       |
| AWS SES           | Transactional email (HTML)             |
| AWS S3            | File storage (avatars, covers, files)  |
| Thymeleaf         | HTML email templates                   |
| Google OAuth 2.0  | Social login                           |
| Springdoc OpenAPI | Swagger UI documentation               |
| Docker Compose    | Infrastructure containerization        |
| Lombok            | Boilerplate reduction                  |

---

## Authentication Flow

```
┌────────┐                      ┌────────┐                    ┌───────┐
│ Client │                      │  API   │                    │ Redis │
└───┬────┘                      └───┬────┘                    └───┬───┘
    │  POST /auth/login             │                             │
    │  {email, password}            │                             │
    │──────────────────────────────►│                             │
    │                               │  Cache refresh token        │
    │                               │────────────────────────────►│
    │  {accessToken, refreshToken}  │                             │
    │◄──────────────────────────────│                             │
    │                               │                             │
    │  GET /boards (Bearer token)   │                             │
    │──────────────────────────────►│                             │
    │  200 OK                       │                             │
    │◄──────────────────────────────│                             │
    │                               │                             │
    │  POST /auth/refresh           │                             │
    │  {refreshToken}               │                             │
    │──────────────────────────────►│  Validate (cache-first)     │
    │                               │────────────────────────────►│
    │  {newAccessToken}             │                             │
    │◄──────────────────────────────│                             │
```

### Token Details

- **Access token:** JWT signed with HMAC256, 4-hour expiry, carries user email as subject
- **Refresh token:** UUID stored in PostgreSQL + cached in Redis with 7-day TTL
- **Google OAuth:** Frontend sends Google ID token → API verifies via Google public keys → creates/retrieves local user

### Security

- `SecurityFilter` intercepts every request, validates JWT, sets `SecurityContext`
- Public endpoints: `/auth/login`, `/auth/register`, `/auth/google`, `/auth/refresh`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/forgot-password`, `/auth/reset-password`
- All other endpoints require `Authorization: Bearer <token>`
- CORS restricted to configured frontend URLs with `allowCredentials(true)`
- Password reset invalidates **all** refresh tokens for the user

---

## Email Templates

HTML emails sent via AWS SES with Thymeleaf template engine:

| Template                  | Trigger                    | Token Expiry |
| ------------------------- | -------------------------- | ------------ |
| `email-verification.html` | Registration, resend       | 24 hours     |
| `password-reset.html`     | Forgot password            | 30 minutes   |
| `member-invite.html`      | Workspace/board invitation | —            |

Templates location: `src/main/resources/templates/`

---

## Audit Logging

All authentication events are persisted to the `audit_logs` table:

| Action           | Trigger                    | Severity |
| ---------------- | -------------------------- | -------- |
| `LOGIN`          | Successful login           | INFO     |
| `REGISTER`       | New account created        | INFO     |
| `LOGOUT`         | Single device logout       | INFO     |
| `LOGOUT_ALL`     | Global logout              | WARN     |
| `TOKEN_REFRESH`  | Access token refreshed     | INFO     |
| `PASSWORD_RESET` | Password changed via reset | WARN     |
| `EMAIL_VERIFIED` | Email verification success | INFO     |

Each record stores: action, email, IP address, user agent, details, timestamp.

---

## Pagination

Optional pagination on listing endpoints — backward compatible:

```
GET /boards?workspaceId=xxx                        → List<BoardResponseDTO>
GET /boards?workspaceId=xxx&page=0&size=20         → Page<BoardResponseDTO>

GET /boards/{id}/lists/{id}/cards                  → List<CardResponseDTO>
GET /boards/{id}/lists/{id}/cards?page=0&size=50   → Page<CardResponseDTO>
```

If `page` parameter is omitted, returns full list (no breaking change for existing clients).

---

## Member System

Two-level access control:

1. **Workspace members** — can see/create boards in the workspace
2. **Board members** — can see/create lists and cards in the board

Rules:

- To be invited to a **board**, user must already be a **workspace member**
- Only the **owner** can invite/remove members
- Owner cannot be removed
- Invitation sends an HTML email notification

---

## Authentication Examples

### Standard registration

**1 — Register an account**

```
POST /auth/register
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

Response `201 Created`:

```json
{
  "message": "Registration successful! Please check your email to verify your account."
}
```

**2 — Verify your email**

```
POST /auth/verify-email
```

```json
{ "token": "<token_from_email>" }
```

Response `200 OK`:

```json
{ "message": "Email verified successfully! You can now log in." }
```

**3 — Login**

```
POST /auth/login
```

```json
{ "emailOrUsername": "johndoe", "password": "secret123" }
```

Response `200 OK`:

```json
{
  "name": "John Doe",
  "accessToken": "<access_token>",
  "refreshToken": "<refresh_token>"
}
```

Access token expires in **4 hours**. Use the refresh token to get a new one:

```
POST /auth/refresh
```

```json
{ "refreshToken": "<refresh_token>" }
```

### Google OAuth login

```
POST /auth/google
```

```json
{ "token": "<google_id_token>" }
```

Response `200 OK`:

```json
{
  "name": "John Doe",
  "accessToken": "<access_token>",
  "refreshToken": "<refresh_token>"
}
```

If the user does not exist, a new account is created automatically with email already verified. If a user registered with email/password tries to log in via Google with the same email, they will receive a `USE_GOOGLE_LOGIN` or `INVALID_CREDENTIALS` error depending on direction.

---

## Endpoints

### Auth

| Method | Path                      | Auth | Status (success) | Description                                                |
| ------ | ------------------------- | ---- | ---------------- | ---------------------------------------------------------- |
| POST   | /auth/register            | No   | 201              | Create account and send verification email                 |
| POST   | /auth/verify-email        | No   | 200              | Verify email with token from inbox                         |
| POST   | /auth/resend-verification | No   | 200              | Resend verification email                                  |
| POST   | /auth/login               | No   | 200              | Login, returns access + refresh token                      |
| POST   | /auth/google              | No   | 200              | Login/register with Google ID token                        |
| POST   | /auth/refresh             | No   | 200              | Get new access token using refresh token                   |
| POST   | /auth/logout              | Yes  | 200              | Invalidate current session (requires refreshToken in body) |
| POST   | /auth/logout-all          | Yes  | 200              | Invalidate all sessions                                    |
| POST   | /auth/forgot-password     | No   | 200              | Send password reset email                                  |
| POST   | /auth/reset-password      | No   | 200              | Reset password with token                                  |

### Users

| Method | Path             | Auth | Status (success) | Description                     |
| ------ | ---------------- | ---- | ---------------- | ------------------------------- |
| GET    | /users/me        | Yes  | 200              | Get current authenticated user  |
| PUT    | /users/me        | Yes  | 200              | Update profile (name, username) |
| DELETE | /users/me        | Yes  | 204              | Delete own account              |
| PUT    | /users/me/avatar | Yes  | 200              | Upload or replace avatar        |
| DELETE | /users/me/avatar | Yes  | 204              | Remove avatar                   |
| GET    | /users/{id}      | Yes  | 200              | Get user by ID                  |

### Workspaces

| Method | Path                  | Auth | Status (success) | Description                          |
| ------ | --------------------- | ---- | ---------------- | ------------------------------------ |
| POST   | /workspaces           | Yes  | 201              | Create workspace                     |
| GET    | /workspaces           | Yes  | 200              | List all workspaces                  |
| GET    | /workspaces/{id}      | Yes  | 200              | Get workspace with boards            |
| PUT    | /workspaces/{id}      | Yes  | 200              | Update workspace                     |
| DELETE | /workspaces/{id}      | Yes  | 204              | Delete workspace and all its content |
| PUT    | /workspaces/{id}/cover | Yes | 200              | Upload or replace workspace cover    |
| DELETE | /workspaces/{id}/cover | Yes | 204              | Remove workspace cover               |

### Workspace Members

| Method | Path                              | Auth | Status (success) | Description                      |
| ------ | --------------------------------- | ---- | ---------------- | -------------------------------- |
| GET    | /workspaces/{id}/members          | Yes  | 200              | List all members                 |
| POST   | /workspaces/{id}/members          | Yes  | 201              | Invite user by email or username |
| DELETE | /workspaces/{id}/members/{userId} | Yes  | 200              | Remove member from workspace     |

### Boards

| Method | Path                     | Auth | Status (success) | Description                      |
| ------ | ------------------------ | ---- | ---------------- | -------------------------------- |
| POST   | /boards?workspaceId={id} | Yes  | 201              | Create board in workspace        |
| GET    | /boards?workspaceId={id} | Yes  | 200              | List boards from workspace       |
| GET    | /boards/{id}             | Yes  | 200              | Get board with lists and cards   |
| PUT    | /boards/{id}             | Yes  | 200              | Update board                     |
| DELETE | /boards/{id}             | Yes  | 204              | Delete board and all its content |
| PUT    | /boards/{id}/cover       | Yes  | 200              | Upload or replace board cover    |
| DELETE | /boards/{id}/cover       | Yes  | 204              | Remove board cover               |

### Board Members

| Method | Path                          | Auth | Status (success) | Description                           |
| ------ | ----------------------------- | ---- | ---------------- | ------------------------------------- |
| GET    | /boards/{id}/members          | Yes  | 200              | List all members                      |
| POST   | /boards/{id}/members          | Yes  | 201              | Invite workspace member to this board |
| DELETE | /boards/{id}/members/{userId} | Yes  | 200              | Remove member from board              |

### Lists

| Method | Path                             | Auth | Status (success) | Description               |
| ------ | -------------------------------- | ---- | ---------------- | ------------------------- |
| POST   | /boards/{boardId}/lists          | Yes  | 201              | Create list               |
| GET    | /boards/{boardId}/lists          | Yes  | 200              | Get all lists             |
| GET    | /boards/{boardId}/lists/{listId} | Yes  | 200              | Get list                  |
| PUT    | /boards/{boardId}/lists/{listId} | Yes  | 200              | Update list               |
| DELETE | /boards/{boardId}/lists/{listId} | Yes  | 204              | Delete list and its cards |

### Cards

| Method | Path                                                 | Auth | Status (success) | Description                   |
| ------ | ---------------------------------------------------- | ---- | ---------------- | ----------------------------- |
| POST   | /boards/{boardId}/lists/{listId}/cards               | Yes  | 201              | Create card                   |
| GET    | /boards/{boardId}/lists/{listId}/cards               | Yes  | 200              | Get all cards                 |
| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200              | Get card                      |
| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 200              | Update card                   |
| PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Yes  | 200              | Move card to a different list |
| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Yes  | 204              | Delete card                   |

Card status values: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

### Attachments

| Method | Path                                          | Auth | Status (success) | Description                            |
| ------ | --------------------------------------------- | ---- | ---------------- | -------------------------------------- |
| POST   | /cards/{cardId}/attachments/request-upload     | Yes  | 200              | Get presigned URL for direct S3 upload |
| POST   | /cards/{cardId}/attachments/confirm            | Yes  | 201              | Confirm upload and save metadata       |
| GET    | /cards/{cardId}/attachments                    | Yes  | 200              | List all attachments for a card        |
| DELETE | /cards/{cardId}/attachments/{attachmentId}     | Yes  | 204              | Delete attachment from S3 and database |

### Storage

| Method | Path                     | Auth | Status (success) | Description                              |
| ------ | ------------------------ | ---- | ---------------- | ---------------------------------------- |
| POST   | /storage/upload          | Yes  | 201              | Direct file upload (images up to 5MB)    |
| POST   | /storage/presigned-upload | Yes | 200              | Get presigned URL for large file upload  |
| DELETE | /storage?fileUrl=        | Yes  | 204              | Delete a file from S3 by URL or key      |

---

## File Storage (AWS S3)

The API uses AWS S3 for all file storage with two upload strategies:

### Upload Strategies

| Strategy       | Use Case                | Max Size | Flow                                |
| -------------- | ----------------------- | -------- | ----------------------------------- |
| Direct Upload  | Avatars, covers (small) | 5 MB     | Client → API → S3                   |
| Presigned URL  | Card attachments (large)| 50 MB    | API generates URL → Client → S3 directly |

### Allowed Image Types (Direct Upload)

- `image/jpeg`, `image/png`, `image/webp`

### Blocked Extensions (Attachments)

- `.exe`, `.bat`, `.cmd`, `.sh`, `.ps1`, `.msi`, `.dll`, `.com`

### S3 Folder Structure

```
bucket/
├── avatars/               # User profile images
├── covers/
│   ├── boards/            # Board cover images
│   └── workspaces/        # Workspace cover images
└── attachments/           # Card file attachments
```

### Default Images

All image fields (`avatarUrl`, `coverUrl`) are **nullable**. A `null` value means no custom image has been set — the frontend should render a default placeholder.

### Attachment Upload Flow (Presigned URL)

```
┌────────┐                    ┌────────┐                    ┌─────┐
│ Client │                    │  API   │                    │ S3  │
└───┬────┘                    └───┬────┘                    └──┬──┘
    │ POST /cards/{id}/           │                            │
    │   attachments/request-upload│                            │
    │ {fileName, contentType,     │                            │
    │  fileSize}                  │                            │
    │────────────────────────────►│                            │
    │                             │  Generate presigned URL    │
    │  {uploadUrl, fileKey,       │                            │
    │   fileUrl}                  │                            │
    │◄────────────────────────────│                            │
    │                             │                            │
    │  PUT uploadUrl              │                            │
    │  (binary file body)         │                            │
    │─────────────────────────────┼───────────────────────────►│
    │  200 OK                     │                            │
    │◄────────────────────────────┼────────────────────────────│
    │                             │                            │
    │ POST /cards/{id}/           │                            │
    │   attachments/confirm       │                            │
    │ {fileKey, fileName,         │                            │
    │  contentType, fileSize}     │                            │
    │────────────────────────────►│                            │
    │  201 Created                │                            │
    │  {id, fileName, fileUrl...} │                            │
    │◄────────────────────────────│                            │
```

### S3 Cleanup

All S3 files are automatically cleaned up when their parent resource is deleted:

| Deletion Trigger     | Files Cleaned                                       |
| -------------------- | --------------------------------------------------- |
| Delete user account  | User avatar                                         |
| Delete workspace     | Workspace cover + all board covers + all attachments |
| Delete board         | Board cover + all card attachments in the board      |
| Delete list          | All card attachments in the list                     |
| Delete card          | All card attachments                                 |
| Delete attachment    | Single file from S3                                  |

---

## Usage Examples

**Create a workspace**

```
POST /workspaces
Authorization: Bearer <access_token>
```

```json
{ "name": "My Workspace" }
```

**Invite a member to the workspace**

```
POST /workspaces/<workspace_id>/members
Authorization: Bearer <access_token>
```

```json
{ "emailOrUsername": "janedoe" }
```

**Create a board inside the workspace**

```
POST /boards?workspaceId=<workspace_id>
Authorization: Bearer <access_token>
```

```json
{ "name": "My Project", "type": "BOARD", "description": "Main project board" }
```

**Invite a workspace member to a specific board**

```
POST /boards/<board_id>/members
Authorization: Bearer <access_token>
```

```json
{ "emailOrUsername": "janedoe" }
```

**Create a list inside the board**

```
POST /boards/<board_id>/lists
Authorization: Bearer <access_token>
```

```json
{ "name": "To Do" }
```

**Create a card inside the list**

```
POST /boards/<board_id>/lists/<list_id>/cards
Authorization: Bearer <access_token>
```

```json
{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }
```

**Move a card to another list**

```
PATCH /boards/<board_id>/lists/<list_id>/cards/<card_id>/move
Authorization: Bearer <access_token>
```

```json
{ "targetListId": "<target_list_id>", "position": 0 }
```

**Upload a user avatar**

```
PUT /users/me/avatar
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Form data: `file` = JPG/PNG/WebP image (max 5MB)

**Upload a board cover**

```
PUT /boards/<board_id>/cover
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Form data: `file` = JPG/PNG/WebP image (max 5MB)

**Attach a file to a card (presigned URL flow)**

Step 1 — Request upload URL:

```
POST /cards/<card_id>/attachments/request-upload
Authorization: Bearer <access_token>
```

```json
{ "fileName": "report.pdf", "contentType": "application/pdf", "fileSize": 2048000 }
```

Response `200 OK`:

```json
{
  "uploadUrl": "https://bucket.s3.amazonaws.com/attachments/uuid.pdf?X-Amz-...",
  "fileKey": "attachments/uuid.pdf",
  "fileUrl": "https://bucket.s3.region.amazonaws.com/attachments/uuid.pdf"
}
```

Step 2 — Upload file directly to S3:

```
PUT <uploadUrl from step 1>
Content-Type: application/pdf
Body: <binary file>
```

Step 3 — Confirm upload:

```
POST /cards/<card_id>/attachments/confirm
Authorization: Bearer <access_token>
```

```json
{
  "fileKey": "attachments/uuid.pdf",
  "fileName": "report.pdf",
  "contentType": "application/pdf",
  "fileSize": 2048000
}
```

---

## Error Responses

All errors follow the format:

```json
{
  "code": "SNAKE_CASE_CODE",
  "message": "Human readable message",
  "statusCode": 400,
  "timestamp": "2026-03-06T12:00:00Z"
}
```

### Auth errors

| Code                      | HTTP Status | Endpoint(s)                                                      | Description                                  |
| ------------------------- | ----------- | ---------------------------------------------------------------- | -------------------------------------------- |
| `PASSWORDS_DO_NOT_MATCH`  | 400         | /auth/register                                                   | Passwords do not match                       |
| `EMAIL_ALREADY_EXISTS`    | 400         | /auth/register                                                   | Email already registered                     |
| `USERNAME_ALREADY_EXISTS` | 400         | /auth/register                                                   | Username already taken                       |
| `INVALID_TOKEN`           | 400         | /auth/verify-email                                               | Token not found in database                  |
| `EXPIRED_TOKEN`           | 400         | /auth/verify-email                                               | Token has passed its 24h expiry              |
| `EMAIL_ALREADY_VERIFIED`  | 400         | /auth/verify-email, /auth/resend-verification                    | Account already active                       |
| `EMAIL_NOT_FOUND`         | 404         | /auth/resend-verification, /auth/forgot-password                 | No account with that email                   |
| `INVALID_CREDENTIALS`     | 401         | /auth/login                                                      | Wrong email/username or password             |
| `USE_GOOGLE_LOGIN`        | 401         | /auth/login                                                      | Account uses Google Sign-In, no password set |
| `INVALID_GOOGLE_TOKEN`    | 401         | /auth/google                                                     | Invalid or expired Google ID token           |
| `EMAIL_NOT_VERIFIED`      | 403         | /auth/login                                                      | Account pending email verification           |
| `EMAIL_SEND_ERROR`        | 500         | /auth/register, /auth/resend-verification, /auth/forgot-password | AWS SES failure                              |
| `VALIDATION_ERROR`        | 400         | Any endpoint with @Valid body                                    | Bean validation failure (see `errors` array) |

### Members errors

| Code                    | HTTP Status | Endpoint(s)               | Description                              |
| ----------------------- | ----------- | ------------------------- | ---------------------------------------- |
| `USER_NOT_FOUND`        | 404         | POST .../members          | No user with that email or username      |
| `USER_ALREADY_MEMBER`   | 400         | POST .../members          | User is already a member                 |
| `USER_NOT_IN_WORKSPACE` | 400         | POST /boards/{id}/members | User must be a workspace member first    |
| `MEMBER_NOT_FOUND`      | 404         | DELETE .../members/{id}   | Member not found                         |
| `CANNOT_REMOVE_OWNER`   | 400         | DELETE .../members/{id}   | The owner cannot be removed              |
| `FORBIDDEN`             | 403         | Any members endpoint      | Not a member or insufficient permissions |

### Resource errors

| Code                  | HTTP Status | Description                          |
| --------------------- | ----------- | ------------------------------------ |
| `WORKSPACE_NOT_FOUND` | 404         | Workspace not found                  |
| `BOARD_NOT_FOUND`     | 404         | Board not found                      |
| `LIST_NOT_FOUND`      | 404         | List not found                       |
| `CARD_NOT_FOUND`      | 404         | Card not found                       |
| `FORBIDDEN`           | 403         | Insufficient access                  |
| `INVALID_MOVE`        | 400         | Invalid card move                    |
| `BAD_REQUEST`         | 400         | Invalid file type, size, or argument |

### Validation error format

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data.",
  "statusCode": 400,
  "errors": [{ "field": "email", "message": "must not be blank" }]
}
```

---

_Last updated: March 19, 2026_
