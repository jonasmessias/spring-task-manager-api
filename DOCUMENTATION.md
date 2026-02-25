# Task Manager API - Documentation

## Overview
RESTful API for task management with JWT authentication, email notifications, and Redis-based token caching.

**Tech Stack:** Spring Boot 3.5.11 | Java 17 | H2 Database | Redis 7 | JWT | Docker

---

## Features

### Authentication System
- **User Registration & Login** with JWT tokens
- **Access Token:** 4-hour validity
- **Refresh Token:** 7-day validity with Redis caching
- **Password Reset:** Email-based token system
- **User Profile Management**

### Task Management
- CRUD operations for tasks
- Task status tracking (PENDING, IN_PROGRESS, COMPLETED)
- Priority levels (LOW, MEDIUM, HIGH)
- User-specific task filtering
- Due date management

### Performance Optimization
- **Redis Cache Layer:** 99% cache hit rate for token validation
- **Reduced DB Load:** From ~666 queries/min to ~7 queries/min (10k users scenario)
- **Cache-Aside Pattern:** Automatic cache warming and invalidation

### API Documentation
- **Swagger UI:** Interactive API documentation
- **Ordered Endpoints:** Grouped by Authentication → Users → Tasks
- **OpenAPI 3.0 Specification**

---

## Architecture

### Project Structure
```
src/main/java/com/example/taskmanagerapi/
├── TaskManagerApiApplication.java
├── config/
│   ├── OpenApiConfig.java         # Swagger configuration
│   └── RedisConfig.java           # Redis with Jackson serialization
├── infra/
│   ├── cors/CorsConfig.java       # CORS configuration
│   └── security/                  # Security layer
│       ├── SecurityConfig.java    # JWT security setup
│       ├── SecurityFilter.java    # Request filter
│       ├── TokenService.java      # JWT generation/validation
│       └── CustomUserDetailsService.java
└── modules/
    ├── auth/                      # Authentication module
    │   ├── controllers/           # AuthController, UserController
    │   ├── domain/                # User, RefreshToken, PasswordResetToken
    │   ├── dto/                   # Request/Response DTOs
    │   ├── repositories/          # JPA repositories
    │   └── services/              # Business logic
    └── tasks/                     # Task management module
        ├── controllers/           # TaskController
        ├── domain/                # Task, TaskStatus
        ├── dto/                   # Task DTOs
        └── repositories/          # TaskRepository
```

### Database Schema

#### User
- `id` (UUID), `name`, `email`, `password` (BCrypt), `createdAt`, `updatedAt`

#### RefreshToken
- `id` (Long), `token` (UUID), `user` (FK), `expiresAt`, `createdAt`
- **Redis Cache:** Key format `refresh:user:{userId}`, 7-day TTL

#### PasswordResetToken
- `id` (Long), `token` (UUID), `user` (FK), `expiresAt`, `createdAt`
- **Validity:** 1 hour

#### Task
- `id` (UUID), `user` (FK), `title`, `description`, `status`, `priority`, `dueDate`, `createdAt`, `updatedAt`

---

## API Endpoints

### Authentication
```
POST   /api/auth/register          # Register new user
POST   /api/auth/login             # Login (returns access + refresh tokens)
POST   /api/auth/refresh           # Get new access token using refresh token
POST   /api/auth/logout            # Invalidate refresh token
POST   /api/auth/forgot-password   # Request password reset email
POST   /api/auth/reset-password    # Reset password with token
```

### User Management
```
GET    /api/users/profile          # Get current user profile
PUT    /api/users/profile          # Update user profile
DELETE /api/users/profile          # Delete user account
```

### Tasks
```
GET    /api/tasks                  # List all tasks (user-specific)
POST   /api/tasks                  # Create new task
GET    /api/tasks/{id}             # Get task by ID
PUT    /api/tasks/{id}             # Update task
DELETE /api/tasks/{id}             # Delete task
GET    /api/tasks/status/{status}  # Filter tasks by status
GET    /api/tasks/priority/{priority} # Filter tasks by priority
```

---

## Configuration

### application.properties
```properties
# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=admin
spring.datasource.password=

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Secret
api.security.token.secret=my-secret-key-from-task-manager

# Frontend URL (CORS)
app.frontend.url=http://localhost:4200

# Email (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### Environment Variables (Production)
Override any property using environment variables:
```bash
export API_SECURITY_TOKEN_SECRET="strong-production-key"
export SPRING_MAIL_USERNAME="noreply@yourdomain.com"
export SPRING_MAIL_PASSWORD="your-password"
export APP_FRONTEND_URL="https://yourdomain.com"
```

---

## Running the Application

### Option 1: Docker Compose (Recommended)
```bash
# Start Redis + Application
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

### Option 2: Local Development
```bash
# Terminal 1: Start Redis
docker run -d -p 6379:6379 redis:7-alpine

# Terminal 2: Run application
mvn spring-boot:run
```

### Option 3: Production JAR
```bash
mvn clean package -DskipTests
java -jar target/task-manager-api-1.0.0.jar
```

---

## Access Points

| Service | URL |
|---------|-----|
| API Base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |
| Redis | localhost:6379 |

**H2 Console Credentials:**
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `admin`
- Password: _(empty)_

---

## Authentication Flow

### 1. User Registration
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 14400
}
```

### 2. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

### 3. Access Protected Endpoints
```http
GET /api/tasks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Refresh Access Token (Before Expiration)
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 14400
}
```

### 5. Logout
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Redis Caching Strategy

### Cache Key Format
- **Refresh Tokens:** `refresh:user:{userId}`
- **TTL:** 7 days (matches refresh token expiration)

### Cache Operations

#### Read (Token Validation)
1. Check Redis cache (~5ms)
2. If cache miss → Query database (~50ms)
3. Warm cache with result
4. Return token

#### Write (Token Creation)
1. Save to database
2. Cache in Redis with TTL
3. Return token

#### Update (Token Refresh)
1. Validate existing token (from cache)
2. Invalidate old cache entry
3. Create new tokens
4. Cache new refresh token

#### Delete (Logout)
1. Remove from Redis cache
2. Delete from database

### Performance Impact
- **Before:** 666 DB queries/min (10k users, 15-min tokens)
- **After:** ~7 DB queries/min (10k users, 4-hour tokens + cache)
- **Improvement:** 99% reduction in database load

---

## Email System

### Password Reset Flow
1. User requests reset: `POST /api/auth/forgot-password`
2. System generates token (1-hour validity)
3. Email sent with reset link: `{FRONTEND_URL}/reset-password?token={token}`
4. User submits new password: `POST /api/auth/reset-password`
5. Token invalidated, password updated

### Gmail Configuration
1. Enable 2FA: https://myaccount.google.com/security
2. Generate App Password: https://myaccount.google.com/apppasswords
3. Use 16-character password in `application.properties`

---

## Security

### Password Hashing
- **Algorithm:** BCrypt
- **Strength:** 10 rounds
- **Implementation:** Spring Security `BCryptPasswordEncoder`

### JWT Configuration
- **Algorithm:** HMAC SHA-256
- **Access Token:** 4 hours
- **Refresh Token:** 7 days
- **Secret:** Configurable via `api.security.token.secret`

### CORS
- **Allowed Origin:** Configured via `app.frontend.url`
- **Allowed Methods:** GET, POST, PUT, DELETE
- **Allowed Headers:** Authorization, Content-Type
- **Credentials:** Enabled

### Security Headers
- Stateless session management
- CSRF disabled (JWT-based auth)
- HTTP Basic disabled

---

## Testing

### Manual Testing with Swagger
1. Start application: `docker-compose up -d`
2. Open Swagger: http://localhost:8080/swagger-ui.html
3. Register user via `/api/auth/register`
4. Copy `token` from response
5. Click "Authorize" → Paste token → Authorize
6. Test protected endpoints

### cURL Examples

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"test123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

**Create Task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"My Task","description":"Task description","status":"PENDING","priority":"HIGH","dueDate":"2026-12-31T23:59:59"}'
```

---

## Monitoring

### Redis CLI
```bash
# Connect to Redis
docker exec -it taskmanager-redis redis-cli

# Check cache stats
INFO stats

# List all cached tokens
KEYS refresh:*

# Check token TTL
TTL refresh:user:550e8400-e29b-41d4-a716-446655440000

# View token data
GET refresh:user:550e8400-e29b-41d4-a716-446655440000
```

### Application Logs
```bash
# Follow logs
docker-compose logs -f app

# Filter security logs
docker-compose logs app | grep Security

# Filter Redis operations
docker-compose logs app | grep Redis
```

---

## Dependencies

### Core
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-security` - Authentication/Authorization
- `spring-boot-starter-validation` - DTO validation

### Database
- `h2` - In-memory database (development)
- `spring-boot-starter-data-redis` - Redis integration
- `jedis` - Redis client

### Security
- `java-jwt` (Auth0 4.5.0) - JWT token handling

### Email
- `spring-boot-starter-mail` - SMTP integration

### Documentation
- `springdoc-openapi-starter-webmvc-ui` (2.3.0) - Swagger UI

### Utilities
- `lombok` - Reduce boilerplate code

---

## Build Information

**Maven:**
- Group: `com.example`
- Artifact: `task-manager-api`
- Version: `1.0.0`
- Java: `17`

**Build Output:**
- JAR: `target/task-manager-api-1.0.0.jar`
- Size: ~75 MB (includes dependencies)

---

## Production Deployment

### Docker Compose (VPS)
1. Clone repository
2. Set environment variables:
   ```bash
   export API_SECURITY_TOKEN_SECRET="strong-key"
   export SPRING_MAIL_USERNAME="noreply@domain.com"
   export SPRING_MAIL_PASSWORD="password"
   ```
3. Deploy: `docker-compose up -d --build`

### Kubernetes
1. Create secrets:
   ```bash
   kubectl create secret generic taskmanager-secrets \
     --from-literal=jwt-secret='your-key' \
     --from-literal=mail-password='your-password'
   ```
2. Apply deployment manifests
3. Expose via LoadBalancer/Ingress

### AWS ECS
1. Push image to ECR
2. Create task definition with secrets from AWS Secrets Manager
3. Create ECS service with Redis from ElastiCache

---

## Troubleshooting

### Redis Connection Failed
```bash
# Check if Redis is running
docker ps | grep redis

# Test connection
docker exec -it taskmanager-redis redis-cli ping
# Should return: PONG
```

### Email Not Sending
- Verify Gmail App Password (not account password)
- Check `spring.mail.username` and `spring.mail.password`
- Ensure 2FA enabled on Gmail account
- Check firewall allows outbound port 587

### JWT Token Invalid
- Verify `api.security.token.secret` matches between runs
- Check token expiration time
- Ensure Bearer token format: `Authorization: Bearer <token>`

### H2 Console Not Accessible
- Verify `spring.h2.console.enabled=true`
- Access via: http://localhost:8080/h2-console
- Use JDBC URL: `jdbc:h2:mem:testdb`

---

## Migration Notes

### From 15-minute to 4-hour Tokens
- **Old:** Users refreshed every 15 minutes
- **New:** Users refresh every ~3.5 hours
- **Impact:** 16x reduction in refresh requests

### Redis Cache Integration
- **Backward Compatible:** Works with existing database
- **Migration:** No schema changes required
- **Behavior:** First request hits DB, subsequent requests use cache

---

## License
This project is for educational/demonstration purposes.

---

## Contact
Repository: github.com/reazew/task-manager-api
