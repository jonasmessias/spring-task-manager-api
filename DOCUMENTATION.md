# Task Manager API — Documentação Técnica

## Arquitetura

### Desenvolvimento (Local)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Frontend   │◄─────►│  Spring Boot │◄─────►│  PostgreSQL  │
│   (Client)   │  REST │     API      │  JPA  │  (Docker)    │
└──────────────┘       └──────┬───────┘       └──────────────┘
                              │
                    ┌─────────┼─────────┬──────────┐
                    │         │         │          │
               ┌────▼───┐ ┌──▼───┐ ┌───▼────┐ ┌───▼───┐
               │ Redis  │ │ AWS  │ │ Google │ │ AWS   │
               │(Docker)│ │ SES  │ │ OAuth  │ │  S3   │
               └────────┘ └──────┘ └────────┘ └───────┘
```

### Produção (AWS)

```
                    ┌─── Internet ───┐
                    │                │
                    ▼                ▼
            ┌──────────────┐  ┌──────────────┐
            │   Frontend   │  │  DNS (A)     │
            │   (Vercel)   │  │  api.domain  │
            └──────────────┘  └──────┬───────┘
                                     │
                    ┌────── EC2 ──────┴──────────────┐
                    │                                 │
                    │  Nginx (reverse proxy + SSL)    │
                    │  :443 → :8080                   │
                    │                                 │
                    │  ┌───────────────────────────┐  │
                    │  │  Spring Boot (container)  │  │
                    │  │  porta 8080               │  │
                    │  └──────────┬────────────────┘  │
                    │             │ rede Docker        │
                    │  ┌──────────┴────────────────┐  │
                    │  │  Redis 7 (container)       │  │
                    │  │  ~10MB RAM                 │  │
                    │  └───────────────────────────┘  │
                    │                                 │
                    └────────────────┬────────────────┘
                                    │ rede VPC
                             ┌──────┴──────┐
                             │  AWS RDS     │
                             │  PostgreSQL  │
                             └─────────────┘
```

### Estrutura dos Módulos

```
src/main/java/com/example/taskmanagerapi/
├── config/          # Configuração OpenAPI, Redis, AWS S3
├── infra/
│   ├── cors/        # Política de CORS (origens restritas)
│   ├── exception/   # Tratamento global de exceções
│   └── security/    # Filtro JWT, serviço de token, configuração de segurança
└── modules/
    ├── auth/        # Autenticação, usuários, e-mail, auditoria
    ├── workspaces/  # CRUD de Workspaces + membros + capas
    ├── boards/      # CRUD de Boards + membros + capas
    ├── lists/       # CRUD de Listas (colunas)
    ├── cards/       # CRUD de Cards + drag-and-drop + anexos
    └── storage/     # Upload de arquivos AWS S3 (direto + URLs pré-assinadas)
```

---

## Stack Tecnológica

| Tecnologia        | Finalidade                                  |
| ----------------- | ------------------------------------------- |
| Java 17           | Linguagem                                   |
| Spring Boot 3.5   | Framework                                   |
| Spring Security   | Autenticação e autorização                  |
| JWT (java-jwt)    | Token de acesso (expiração de 4h)           |
| Redis 7           | Cache de refresh tokens (7 dias)            |
| PostgreSQL 16     | Banco de dados principal                    |
| Flyway            | Migrações de banco de dados                 |
| Bucket4j          | Rate limiting por IP (100 req/min)          |
| Spring Actuator   | Health check e métricas                     |
| AWS SES           | E-mails transacionais (HTML)                |
| AWS S3            | Armazenamento de arquivos (avatares, capas) |
| Thymeleaf         | Templates de e-mail HTML                    |
| Google OAuth 2.0  | Login social                                |
| Springdoc OpenAPI | Documentação Swagger UI                     |
| Docker Compose    | Containerização da infraestrutura           |
| GitHub Actions    | CI/CD — deploy automático                   |
| Nginx + Certbot   | Reverse proxy + HTTPS (Let's Encrypt)       |
| Lombok            | Redução de boilerplate                      |

---

## Deploy em Produção

### Infraestrutura

| Serviço  | Tecnologia             | Descrição                      |
| -------- | ---------------------- | ------------------------------ |
| Servidor | AWS EC2 (t2.micro)     | Hospeda API + Redis via Docker |
| Banco    | AWS RDS PostgreSQL 16  | Banco de dados gerenciado      |
| Cache    | Redis 7 (Docker local) | Refresh tokens (dentro da EC2) |
| Email    | AWS SES (sa-east-1)    | E-mails transacionais          |
| Storage  | AWS S3 (sa-east-1)     | Armazenamento de arquivos      |
| SSL      | Nginx + Certbot        | HTTPS com Let's Encrypt        |
| CI/CD    | GitHub Actions         | Deploy automático a cada push  |

### Spring Profiles

| Profile   | Arquivo                       | Uso                                 |
| --------- | ----------------------------- | ----------------------------------- |
| (default) | `application.properties`      | Desenvolvimento local               |
| `prod`    | `application-prod.properties` | Produção (credenciais via env vars) |

Em produção, o profile é ativado via `SPRING_PROFILES_ACTIVE=prod` no Docker Compose.

### CI/CD (GitHub Actions)

A cada push na branch `master`, o workflow `.github/workflows/deploy.yml` executa:

1. Conecta via SSH na EC2
2. `git pull origin master`
3. `docker compose -f docker-compose.prod.yml up -d --build`
4. `docker image prune -f` (limpa imagens antigas)

Secrets necessários no GitHub (Settings → Secrets → Actions):

| Secret         | Descrição                    |
| -------------- | ---------------------------- |
| `EC2_HOST`     | IP público da instância EC2  |
| `EC2_USERNAME` | Usuário SSH (ex: `ec2-user`) |
| `EC2_SSH_KEY`  | Conteúdo do arquivo `.pem`   |

### Variáveis de Ambiente (Produção)

Configuradas em um arquivo `.env` na EC2 (não versionado):

| Variável            | Descrição                          |
| ------------------- | ---------------------------------- |
| `DATABASE_URL`      | JDBC URL do RDS PostgreSQL         |
| `DATABASE_USERNAME` | Usuário do banco                   |
| `DATABASE_PASSWORD` | Senha do banco                     |
| `JWT_SECRET`        | Chave para assinar JWTs (base64)   |
| `AWS_ACCESS_KEY`    | Chave de acesso AWS                |
| `AWS_SECRET_KEY`    | Chave secreta AWS                  |
| `AWS_REGION`        | Região AWS (sa-east-1)             |
| `S3_BUCKET_NAME`    | Nome do bucket S3                  |
| `AWS_SES_FROM`      | E-mail remetente verificado no SES |
| `GOOGLE_CLIENT_ID`  | Client ID do Google OAuth 2.0      |
| `FRONTEND_URL`      | URL do frontend (CORS)             |

### Segurança em Produção

- `application.properties` e `.env` estão no `.gitignore` — nunca vão para o git
- RDS acessível apenas dentro da VPC (Security Group restrito)
- Redis roda localmente no Docker — sem exposição externa
- Swagger UI desabilitado em produção
- HTTPS obrigatório via Nginx + Certbot
- Rate limiting: 100 requisições/minuto por IP

---

## Fluxo de Autenticação

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

### Detalhes dos Tokens

- **Access token:** JWT assinado com HMAC256, expiração de 4 horas, carrega o e-mail do usuário como subject
- **Refresh token:** UUID armazenado no PostgreSQL + cacheado no Redis com TTL de 7 dias
- **Google OAuth:** Frontend envia Google ID token → API verifica via chaves públicas do Google → cria/recupera usuário local

### Segurança

- `SecurityFilter` intercepta todas as requisições, valida o JWT e configura o `SecurityContext`
- `RateLimitFilter` limita a 100 requisições/minuto por IP usando Bucket4j
- Endpoints públicos: `/auth/login`, `/auth/register`, `/auth/google`, `/auth/refresh`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/forgot-password`, `/auth/reset-password`, `/actuator/health`
- Todos os outros endpoints requerem `Authorization: Bearer <token>`
- CORS restrito às URLs de frontend configuradas com `allowCredentials(true)`
- Redefinição de senha invalida **todos** os refresh tokens do usuário
- Validação com `@Valid` em todos os endpoints que recebem request body

---

## Templates de E-mail

E-mails HTML enviados via AWS SES com motor de templates Thymeleaf:

| Template                  | Gatilho                      | Expiração do Token |
| ------------------------- | ---------------------------- | ------------------ |
| `email-verification.html` | Registro, reenvio            | 24 horas           |
| `password-reset.html`     | Esqueceu a senha             | 30 minutos         |
| `member-invite.html`      | Convite para workspace/board | —                  |

Localização dos templates: `src/main/resources/templates/`

---

## Log de Auditoria

Todos os eventos de autenticação são persistidos na tabela `audit_logs`:

| Ação             | Gatilho                           | Severidade |
| ---------------- | --------------------------------- | ---------- |
| `LOGIN`          | Login bem-sucedido                | INFO       |
| `REGISTER`       | Nova conta criada                 | INFO       |
| `LOGOUT`         | Logout de dispositivo único       | INFO       |
| `LOGOUT_ALL`     | Logout global                     | WARN       |
| `TOKEN_REFRESH`  | Access token renovado             | INFO       |
| `PASSWORD_RESET` | Senha alterada via redefinição    | WARN       |
| `EMAIL_VERIFIED` | Verificação de e-mail com sucesso | INFO       |

Cada registro armazena: ação, e-mail, endereço IP, user agent, detalhes, timestamp.

---

## Paginação

Paginação opcional nos endpoints de listagem — compatível com versões anteriores:

```
GET /boards?workspaceId=xxx                        → List<BoardResponseDTO>
GET /boards?workspaceId=xxx&page=0&size=20         → Page<BoardResponseDTO>

GET /boards/{id}/lists/{id}/cards                  → List<CardResponseDTO>
GET /boards/{id}/lists/{id}/cards?page=0&size=50   → Page<CardResponseDTO>
```

Se o parâmetro `page` for omitido, retorna a lista completa (sem quebrar clientes existentes).

---

## Sistema de Membros

Controle de acesso em dois níveis:

1. **Membros do workspace** — podem visualizar/criar boards no workspace
2. **Membros do board** — podem visualizar/criar listas e cards no board

Regras:

- Para ser convidado a um **board**, o usuário já deve ser **membro do workspace**
- Apenas o **proprietário** pode convidar/remover membros
- O proprietário não pode ser removido
- O convite envia uma notificação por e-mail HTML

---

## Exemplos de Autenticação

### Registro padrão

**1 — Criar uma conta**

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
  "message": "Registro realizado com sucesso! Verifique seu e-mail para ativar sua conta."
}
```

**2 — Verificar seu e-mail**

```
POST /auth/verify-email
```

```json
{ "token": "<token_from_email>" }
```

Response `200 OK`:

```json
{ "message": "E-mail verificado com sucesso! Agora você pode fazer login." }
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

Access token expira em **4 horas**. Use o refresh token para obter um novo:

```
POST /auth/refresh
```

```json
{ "refreshToken": "<refresh_token>" }
```

### Login com Google OAuth

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

Se o usuário não existir, uma nova conta é criada automaticamente com e-mail já verificado. Se um usuário registrado com e-mail/senha tentar fazer login via Google com o mesmo e-mail, receberá um erro `USE_GOOGLE_LOGIN` ou `INVALID_CREDENTIALS` dependendo da direção.

---

## Endpoints

### Auth

| Método | Caminho                   | Auth | Status (sucesso) | Descrição                                               |
| ------ | ------------------------- | ---- | ---------------- | ------------------------------------------------------- |
| POST   | /auth/register            | Não  | 201              | Criar conta e enviar e-mail de verificação              |
| POST   | /auth/verify-email        | Não  | 200              | Verificar e-mail com token recebido na caixa de entrada |
| POST   | /auth/resend-verification | Não  | 200              | Reenviar e-mail de verificação                          |
| POST   | /auth/login               | Não  | 200              | Login, retorna access + refresh token                   |
| POST   | /auth/google              | Não  | 200              | Login/registro com Google ID token                      |
| POST   | /auth/refresh             | Não  | 200              | Obter novo access token usando refresh token            |
| POST   | /auth/logout              | Sim  | 200              | Invalidar sessão atual (requer refreshToken no body)    |
| POST   | /auth/logout-all          | Sim  | 200              | Invalidar todas as sessões                              |
| POST   | /auth/forgot-password     | Não  | 200              | Enviar e-mail de redefinição de senha                   |
| POST   | /auth/reset-password      | Não  | 200              | Redefinir senha com token                               |

### Users (Usuários)

| Método | Caminho          | Auth | Status (sucesso) | Descrição                         |
| ------ | ---------------- | ---- | ---------------- | --------------------------------- |
| GET    | /users/me        | Sim  | 200              | Obter usuário autenticado atual   |
| PUT    | /users/me        | Sim  | 200              | Atualizar perfil (nome, username) |
| DELETE | /users/me        | Sim  | 204              | Excluir própria conta             |
| PUT    | /users/me/avatar | Sim  | 200              | Enviar ou substituir avatar       |
| DELETE | /users/me/avatar | Sim  | 204              | Remover avatar                    |
| GET    | /users/{id}      | Sim  | 200              | Obter usuário por ID              |

### Workspaces

| Método | Caminho                | Auth | Status (sucesso) | Descrição                              |
| ------ | ---------------------- | ---- | ---------------- | -------------------------------------- |
| POST   | /workspaces            | Sim  | 201              | Criar workspace                        |
| GET    | /workspaces            | Sim  | 200              | Listar todos os workspaces             |
| GET    | /workspaces/{id}       | Sim  | 200              | Obter workspace com boards             |
| PUT    | /workspaces/{id}       | Sim  | 200              | Atualizar workspace                    |
| DELETE | /workspaces/{id}       | Sim  | 204              | Excluir workspace e todo seu conteúdo  |
| PUT    | /workspaces/{id}/cover | Sim  | 200              | Enviar ou substituir capa do workspace |
| DELETE | /workspaces/{id}/cover | Sim  | 204              | Remover capa do workspace              |

### Membros do Workspace

| Método | Caminho                           | Auth | Status (sucesso) | Descrição                               |
| ------ | --------------------------------- | ---- | ---------------- | --------------------------------------- |
| GET    | /workspaces/{id}/members          | Sim  | 200              | Listar todos os membros                 |
| POST   | /workspaces/{id}/members          | Sim  | 201              | Convidar usuário por e-mail ou username |
| DELETE | /workspaces/{id}/members/{userId} | Sim  | 200              | Remover membro do workspace             |

### Boards

| Método | Caminho                  | Auth | Status (sucesso) | Descrição                          |
| ------ | ------------------------ | ---- | ---------------- | ---------------------------------- |
| POST   | /boards?workspaceId={id} | Sim  | 201              | Criar board no workspace           |
| GET    | /boards?workspaceId={id} | Sim  | 200              | Listar boards do workspace         |
| GET    | /boards/{id}             | Sim  | 200              | Obter board com listas e cards     |
| PUT    | /boards/{id}             | Sim  | 200              | Atualizar board                    |
| DELETE | /boards/{id}             | Sim  | 204              | Excluir board e todo seu conteúdo  |
| PUT    | /boards/{id}/cover       | Sim  | 200              | Enviar ou substituir capa do board |
| DELETE | /boards/{id}/cover       | Sim  | 204              | Remover capa do board              |

### Membros do Board

| Método | Caminho                       | Auth | Status (sucesso) | Descrição                                    |
| ------ | ----------------------------- | ---- | ---------------- | -------------------------------------------- |
| GET    | /boards/{id}/members          | Sim  | 200              | Listar todos os membros                      |
| POST   | /boards/{id}/members          | Sim  | 201              | Convidar membro do workspace para este board |
| DELETE | /boards/{id}/members/{userId} | Sim  | 200              | Remover membro do board                      |

### Listas

| Método | Caminho                          | Auth | Status (sucesso) | Descrição                  |
| ------ | -------------------------------- | ---- | ---------------- | -------------------------- |
| POST   | /boards/{boardId}/lists          | Sim  | 201              | Criar lista                |
| GET    | /boards/{boardId}/lists          | Sim  | 200              | Obter todas as listas      |
| GET    | /boards/{boardId}/lists/{listId} | Sim  | 200              | Obter lista                |
| PUT    | /boards/{boardId}/lists/{listId} | Sim  | 200              | Atualizar lista            |
| DELETE | /boards/{boardId}/lists/{listId} | Sim  | 204              | Excluir lista e seus cards |

### Cards

| Método | Caminho                                              | Auth | Status (sucesso) | Descrição                   |
| ------ | ---------------------------------------------------- | ---- | ---------------- | --------------------------- |
| POST   | /boards/{boardId}/lists/{listId}/cards               | Sim  | 201              | Criar card                  |
| GET    | /boards/{boardId}/lists/{listId}/cards               | Sim  | 200              | Obter todos os cards        |
| GET    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 200              | Obter card                  |
| PUT    | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 200              | Atualizar card              |
| PATCH  | /boards/{boardId}/lists/{listId}/cards/{cardId}/move | Sim  | 200              | Mover card para outra lista |
| DELETE | /boards/{boardId}/lists/{listId}/cards/{cardId}      | Sim  | 204              | Excluir card                |

Valores de status do card: `ACTIVE`, `ARCHIVED`, `COMPLETED`.

### Anexos (Attachments)

| Método | Caminho                                    | Auth | Status (sucesso) | Descrição                                       |
| ------ | ------------------------------------------ | ---- | ---------------- | ----------------------------------------------- |
| POST   | /cards/{cardId}/attachments/request-upload | Sim  | 200              | Obter URL pré-assinada para upload direto no S3 |
| POST   | /cards/{cardId}/attachments/confirm        | Sim  | 201              | Confirmar upload e salvar metadados             |
| GET    | /cards/{cardId}/attachments                | Sim  | 200              | Listar todos os anexos de um card               |
| DELETE | /cards/{cardId}/attachments/{attachmentId} | Sim  | 204              | Excluir anexo do S3 e do banco de dados         |

### Armazenamento (Storage)

| Método | Caminho                   | Auth | Status (sucesso) | Descrição                                      |
| ------ | ------------------------- | ---- | ---------------- | ---------------------------------------------- |
| POST   | /storage/upload           | Sim  | 201              | Upload direto de arquivo (imagens até 5MB)     |
| POST   | /storage/presigned-upload | Sim  | 200              | Obter URL pré-assinada para upload de arquivos |
| DELETE | /storage?fileUrl=         | Sim  | 204              | Excluir arquivo do S3 por URL ou key           |

---

## Armazenamento de Arquivos (AWS S3)

A API utiliza AWS S3 para todo armazenamento de arquivos com duas estratégias de upload:

### Estratégias de Upload

| Estratégia       | Caso de Uso                | Tamanho Máx. | Fluxo                                  |
| ---------------- | -------------------------- | ------------ | -------------------------------------- |
| Upload Direto    | Avatares, capas (pequenos) | 5 MB         | Client → API → S3                      |
| URL Pré-assinada | Anexos de cards (grandes)  | 50 MB        | API gera URL → Client → S3 diretamente |

### Tipos de Imagem Permitidos (Upload Direto)

- `image/jpeg`, `image/png`, `image/webp`

### Extensões Bloqueadas (Anexos)

- `.exe`, `.bat`, `.cmd`, `.sh`, `.ps1`, `.msi`, `.dll`, `.com`

### Estrutura de Pastas no S3

```
bucket/
├── avatars/               # Imagens de perfil dos usuários
├── covers/
│   ├── boards/            # Imagens de capa dos boards
│   └── workspaces/        # Imagens de capa dos workspaces
└── attachments/           # Anexos de arquivos dos cards
```

### Imagens Padrão

Todos os campos de imagem (`avatarUrl`, `coverUrl`) são **anuláveis**. Um valor `null` significa que nenhuma imagem personalizada foi definida — o frontend deve renderizar um placeholder padrão.

### Fluxo de Upload de Anexos (URL Pré-assinada)

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

### Limpeza do S3

Todos os arquivos no S3 são automaticamente removidos quando o recurso pai é excluído:

| Gatilho de Exclusão      | Arquivos Removidos                                             |
| ------------------------ | -------------------------------------------------------------- |
| Excluir conta de usuário | Avatar do usuário                                              |
| Excluir workspace        | Capa do workspace + todas as capas de boards + todos os anexos |
| Excluir board            | Capa do board + todos os anexos de cards do board              |
| Excluir lista            | Todos os anexos de cards da lista                              |
| Excluir card             | Todos os anexos do card                                        |
| Excluir anexo            | Arquivo único do S3                                            |

---

## Exemplos de Uso

**Criar um workspace**

```
POST /workspaces
Authorization: Bearer <access_token>
```

```json
{ "name": "My Workspace" }
```

**Convidar um membro para o workspace**

```
POST /workspaces/<workspace_id>/members
Authorization: Bearer <access_token>
```

```json
{ "emailOrUsername": "janedoe" }
```

**Criar um board dentro do workspace**

```
POST /boards?workspaceId=<workspace_id>
Authorization: Bearer <access_token>
```

```json
{ "name": "My Project", "type": "BOARD", "description": "Main project board" }
```

**Convidar um membro do workspace para um board específico**

```
POST /boards/<board_id>/members
Authorization: Bearer <access_token>
```

```json
{ "emailOrUsername": "janedoe" }
```

**Criar uma lista dentro do board**

```
POST /boards/<board_id>/lists
Authorization: Bearer <access_token>
```

```json
{ "name": "To Do" }
```

**Criar um card dentro da lista**

```
POST /boards/<board_id>/lists/<list_id>/cards
Authorization: Bearer <access_token>
```

```json
{ "name": "Task 1", "description": "Task description", "status": "ACTIVE" }
```

**Mover um card para outra lista**

```
PATCH /boards/<board_id>/lists/<list_id>/cards/<card_id>/move
Authorization: Bearer <access_token>
```

```json
{ "targetListId": "<target_list_id>", "position": 0 }
```

**Enviar um avatar de usuário**

```
PUT /users/me/avatar
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Form data: `file` = imagem JPG/PNG/WebP (máx. 5MB)

**Enviar uma capa de board**

```
PUT /boards/<board_id>/cover
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Form data: `file` = imagem JPG/PNG/WebP (máx. 5MB)

**Anexar um arquivo a um card (fluxo de URL pré-assinada)**

Passo 1 — Solicitar URL de upload:

```
POST /cards/<card_id>/attachments/request-upload
Authorization: Bearer <access_token>
```

```json
{
  "fileName": "report.pdf",
  "contentType": "application/pdf",
  "fileSize": 2048000
}
```

Response `200 OK`:

```json
{
  "uploadUrl": "https://bucket.s3.amazonaws.com/attachments/uuid.pdf?X-Amz-...",
  "fileKey": "attachments/uuid.pdf",
  "fileUrl": "https://bucket.s3.region.amazonaws.com/attachments/uuid.pdf"
}
```

Passo 2 — Enviar arquivo diretamente para o S3:

```
PUT <uploadUrl from step 1>
Content-Type: application/pdf
Body: <binary file>
```

Passo 3 — Confirmar upload:

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

## Respostas de Erro

Todos os erros seguem o formato:

```json
{
  "code": "SNAKE_CASE_CODE",
  "message": "Human readable message",
  "statusCode": 400,
  "timestamp": "2026-03-06T12:00:00Z"
}
```

### Erros de autenticação

| Código                    | Status HTTP | Endpoint(s)                                                      | Descrição                                    |
| ------------------------- | ----------- | ---------------------------------------------------------------- | -------------------------------------------- |
| `PASSWORDS_DO_NOT_MATCH`  | 400         | /auth/register                                                   | As senhas não coincidem                      |
| `EMAIL_ALREADY_EXISTS`    | 400         | /auth/register                                                   | E-mail já registrado                         |
| `USERNAME_ALREADY_EXISTS` | 400         | /auth/register                                                   | Username já em uso                           |
| `INVALID_TOKEN`           | 400         | /auth/verify-email                                               | Token não encontrado no banco de dados       |
| `EXPIRED_TOKEN`           | 400         | /auth/verify-email                                               | Token expirou (24h)                          |
| `EMAIL_ALREADY_VERIFIED`  | 400         | /auth/verify-email, /auth/resend-verification                    | Conta já ativa                               |
| `EMAIL_NOT_FOUND`         | 404         | /auth/resend-verification, /auth/forgot-password                 | Nenhuma conta com esse e-mail                |
| `INVALID_CREDENTIALS`     | 401         | /auth/login                                                      | E-mail/username ou senha incorretos          |
| `USE_GOOGLE_LOGIN`        | 401         | /auth/login                                                      | Conta usa Google Sign-In, sem senha definida |
| `INVALID_GOOGLE_TOKEN`    | 401         | /auth/google                                                     | Google ID token inválido ou expirado         |
| `EMAIL_NOT_VERIFIED`      | 403         | /auth/login                                                      | Conta pendente de verificação de e-mail      |
| `EMAIL_SEND_ERROR`        | 500         | /auth/register, /auth/resend-verification, /auth/forgot-password | Falha no AWS SES                             |
| `VALIDATION_ERROR`        | 400         | Qualquer endpoint com @Valid body                                | Falha de validação (veja array `errors`)     |

### Erros de membros

| Código                  | Status HTTP | Endpoint(s)                  | Descrição                                     |
| ----------------------- | ----------- | ---------------------------- | --------------------------------------------- |
| `USER_NOT_FOUND`        | 404         | POST .../members             | Nenhum usuário com esse e-mail ou username    |
| `USER_ALREADY_MEMBER`   | 400         | POST .../members             | Usuário já é membro                           |
| `USER_NOT_IN_WORKSPACE` | 400         | POST /boards/{id}/members    | Usuário deve ser membro do workspace primeiro |
| `MEMBER_NOT_FOUND`      | 404         | DELETE .../members/{id}      | Membro não encontrado                         |
| `CANNOT_REMOVE_OWNER`   | 400         | DELETE .../members/{id}      | O proprietário não pode ser removido          |
| `FORBIDDEN`             | 403         | Qualquer endpoint de membros | Não é membro ou permissões insuficientes      |

### Erros de recursos

| Código                | Status HTTP | Descrição                                      |
| --------------------- | ----------- | ---------------------------------------------- |
| `WORKSPACE_NOT_FOUND` | 404         | Workspace não encontrado                       |
| `BOARD_NOT_FOUND`     | 404         | Board não encontrado                           |
| `LIST_NOT_FOUND`      | 404         | Lista não encontrada                           |
| `CARD_NOT_FOUND`      | 404         | Card não encontrado                            |
| `FORBIDDEN`           | 403         | Acesso insuficiente                            |
| `INVALID_MOVE`        | 400         | Movimentação de card inválida                  |
| `BAD_REQUEST`         | 400         | Tipo de arquivo, tamanho ou argumento inválido |

### Formato de erro de validação

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request data.",
  "statusCode": 400,
  "errors": [{ "field": "email", "message": "must not be blank" }]
}
```

---

_Última atualização: 24 de março de 2026_
