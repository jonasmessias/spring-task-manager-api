# TaskManager API

Este projeto é uma API RESTful construída com **Spring Boot 3.5** e **Java 17** para gerenciamento de tarefas com workspaces, boards, listas e cards — inspirado em ferramentas como o Trello.

## Pré-requisitos

Certifique-se de ter os seguintes itens instalados:

- [Java 17](https://adoptium.net/) (JDK)
- [Maven 3.9+](https://maven.apache.org/) (ou utilize o wrapper `mvnw` incluído)
- [Docker & Docker Compose](https://www.docker.com/) (para serviços de infraestrutura)
- [PostgreSQL 16](https://www.postgresql.org/) (se executar sem Docker)
- [Redis 7](https://redis.io/) (se executar sem Docker)

## Primeiros Passos

### 1. Clonar o repositório

```bash
git clone https://github.com/reazew/task-manager-api.git
cd task-manager-api
```

### 2. Configurar variáveis de ambiente

Copie o arquivo de propriedades de exemplo e preencha com seus valores:

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Atualize as seguintes propriedades em `application.properties`:

| Propriedade                 | Descrição                                  |
| --------------------------- | ------------------------------------------ |
| `api.security.token.secret` | Chave forte e aleatória para assinar JWTs  |
| `aws.ses.access-key`        | Chave de acesso AWS SES                    |
| `aws.ses.secret-key`        | Chave secreta AWS SES                      |
| `aws.ses.from`              | E-mail de remetente verificado no AWS SES  |
| `aws.s3.access-key`         | Chave de acesso AWS S3                     |
| `aws.s3.secret-key`         | Chave secreta AWS S3                       |
| `aws.s3.bucket-name`        | Nome do bucket S3 para armazenamento       |
| `aws.s3.region`             | Região AWS do bucket S3                    |
| `google.client-id`          | Client ID do Google OAuth 2.0              |
| `app.frontend.url`          | URL do frontend para CORS (desenvolvimento)|
| `app.frontend.prod-url`     | URL do frontend para CORS (produção)       |

### 3. Iniciar infraestrutura com Docker Compose

```bash
docker compose up -d postgres redis
```

Isso inicia o **PostgreSQL** na porta `5432` e o **Redis** na porta `6379`.

### 4. Executar a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

## Executando com Docker (stack completa)

Para executar a stack completa (API + PostgreSQL + Redis) em containers:

```bash
docker compose up -d --build
```

A API estará disponível em `http://localhost:8080`.

## Documentação da API

Com o servidor em execução, abra o navegador e acesse:

```
http://localhost:8080/swagger-ui.html
```

A interface interativa do Swagger UI permite explorar e testar todos os endpoints disponíveis.

## Estrutura do Projeto

```
src/main/java/com/example/taskmanagerapi/
├── config/              # Configuração OpenAPI, Redis, AWS SES, AWS S3
├── infra/
│   ├── cors/            # Política de CORS
│   ├── exception/       # Tratamento global de exceções
│   └── security/        # Filtro JWT, serviço de token, configuração de segurança
└── modules/
    ├── auth/            # Autenticação, usuários, verificação de e-mail
    ├── workspaces/      # CRUD de Workspaces + membros + capas
    ├── boards/          # CRUD de Boards + membros + capas
    ├── lists/           # CRUD de Listas (colunas)
    ├── cards/           # CRUD de Cards + drag-and-drop + anexos
    └── storage/         # Upload de arquivos AWS S3 (direto + URLs pré-assinadas)
```

## Stack Tecnológica

| Tecnologia        | Finalidade                                 |
| ----------------- | ------------------------------------------ |
| Java 17           | Linguagem                                  |
| Spring Boot 3.5   | Framework                                  |
| Spring Security   | Autenticação e autorização                 |
| JWT (java-jwt)    | Token de acesso (expiração de 4h)          |
| PostgreSQL 16     | Banco de dados principal                   |
| Redis 7           | Cache de refresh tokens (TTL de 7 dias)    |
| AWS SES           | E-mails transacionais (HTML)               |
| AWS S3            | Armazenamento de arquivos (avatares, capas)|
| Google OAuth 2.0  | Login social                               |
| Thymeleaf         | Templates de e-mail HTML                   |
| Springdoc OpenAPI | Documentação Swagger UI                    |
| Docker Compose    | Containerização da infraestrutura          |
| Lombok            | Redução de boilerplate                     |

## Executando Testes

Para executar os testes unitários, utilize o seguinte comando:

```bash
./mvnw test
```

## Build

Para compilar o projeto e gerar o artefato `.jar`:

```bash
./mvnw clean package -DskipTests
```

O JAR compilado será armazenado em `target/task-manager-api-1.0.0.jar`.

## Saiba Mais

Para mais detalhes sobre a arquitetura, fluxo de autenticação, endpoints e regras de negócio, consulte o arquivo [DOCUMENTATION.md](DOCUMENTATION.md).
