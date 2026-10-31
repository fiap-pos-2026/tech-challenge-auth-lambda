# tech-challenge-auth-lambda

Função serverless que autentica o cliente da oficina pelo CPF e devolve um JWT para as rotas protegidas da API.

A aplicação principal fica em [tech-challenge-core](https://github.com/fiap-pos-2026/tech-challenge-core); o API Gateway e o EKS em [tech-challenge-k8s-infra](https://github.com/fiap-pos-2026/tech-challenge-k8s-infra); o banco em [tech-challenge-db-infra](https://github.com/fiap-pos-2026/tech-challenge-db-infra).

## Como funciona

```text
Cliente ──POST /auth {cpf}──► API Gateway ──► Lambda
                                               │ 1. valida formato e dígitos do CPF
                                               │ 2. busca o cliente na tabela customers (PostgreSQL)
                                               │ 3. confere se está ativo
                                               ▼ 4. assina o JWT (RS256)
Cliente ◄──── { access_token, expires_in } ────┘
```

| Situação | HTTP | `error` |
|---|---|---|
| Sucesso | 200 | — |
| Body ausente ou CPF inválido | 400 | `INVALID_CPF` |
| Cliente inexistente **ou** inativo | 401 | `INVALID_CREDENTIALS` |
| Falha interna / banco | 500 | `INTERNAL_ERROR` |

Cliente inexistente e inativo recebem a mesma resposta de propósito, para não revelar quais CPFs estão cadastrados.

```bash
curl -X POST "$GATEWAY_URL/auth" -H 'Content-Type: application/json' -d '{"cpf":"529.982.247-25"}'
```

```json
{ "access_token": "eyJ...", "token_type": "Bearer", "expires_in": 1800 }
```

### Token

| Claim | Valor |
|---|---|
| `iss` | `tech-challenge-auth-lambda` (ou a URL configurada em `JWT_ISSUER`) |
| `aud` | `tech-challenge-core` |
| `sub` | UUID do cliente |
| `role` | `CUSTOMER` |
| `name` | nome do cliente |
| `iat` / `exp` / `jti` | emissão, expiração (30 min) e id do token |

CPF, e-mail e telefone **não** vão no token.

A função também publica a chave pública para quem valida o token (core e authorizer do Gateway):

- `GET /auth/.well-known/jwks.json`
- `GET /auth/.well-known/openid-configuration`

## Stack

Java 21 (runtime gerenciado `java21`), Spring Security OAuth2 JOSE (mesmo `NimbusJwtEncoder` do core, sem subir contexto Spring Boot, para manter o cold start baixo), JDBC puro com driver PostgreSQL, Gradle. Testes com JUnit 5, Mockito e Testcontainers.

```text
src/main/java/.../auth
├── AuthHandler.java              entrada da Lambda (API Gateway HTTP API, payload 2.0)
├── application/                  AuthService + portas (CustomerRepository, TokenIssuer)
├── domain/                       Cpf, Customer
├── config/                       leitura das variáveis de ambiente
└── infrastructure/
    ├── persistence/              consulta JDBC na tabela customers
    └── security/                 carga da chave RSA e emissão do JWT
```

## Configuração

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JDBC_URL` | sim | `jdbc:postgresql://<host>:5432/<db>` |
| `JDBC_USERNAME` / `JDBC_PASSWORD` | sim | usuário de leitura do banco |
| `JWT_PRIVATE_KEY` | sim | PEM PKCS#8 (`BEGIN PRIVATE KEY`); a pública é derivada dela |
| `JWT_KEY_ID` | sim | `kid` do header, ex.: `auth-lambda-homolog-202610` |
| `JWT_ISSUER` | não | padrão `tech-challenge-auth-lambda` |
| `JWT_AUDIENCE` | não | padrão `tech-challenge-core` |
| `JWT_EXPIRY` | não | ISO-8601, padrão `PT30M` |

Handler: `br.com.fiap.pos.tech_challenge.auth.AuthHandler::handleRequest`

### Chaves

Cada ambiente tem **seu próprio** par RSA, diferente do par usado pelo login de funcionários do core. A chave privada fica só em secret (GitHub / Secrets Manager), nunca no repositório.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out auth-private.pem
openssl pkey -in auth-private.pem -pubout -out auth-public.pem   # vai para o core
```

O par em `src/test/resources/certs` existe só para os testes.

## Build e testes

Requer JDK 21+ (o Gradle baixa o toolchain 21 se necessário) e Docker para os testes de integração.

```bash
./gradlew build
```

Gera `build/distributions/auth-lambda.zip` (classes + `lib/`), pronto para upload na Lambda.

## Infraestrutura (Terraform)

`infra/` provisiona, em cima do que já existe nos outros repos (lidos via remote state):

- Lambda `java21` nas subnets privadas da VPC do `tech-challenge-db-infra`, com SG próprio (o SG do RDS já libera a VPC)
- Logs em JSON no CloudWatch
- Integração e rotas `POST /auth` + `GET /auth/.well-known/*` na HTTP API do `tech-challenge-k8s-infra`, com permissão de invoke

As rotas da Lambda ficam neste repo para evitar dependência circular: o `iss` do token é a URL do próprio Gateway, então o Gateway precisa existir antes. A role de execução é a `LabRole` do AWS Academy, onde criar role não é permitido.

**Ordem de apply:** `tech-challenge-db-infra` → `tech-challenge-k8s-infra` → `tech-challenge-auth-lambda`.

Local (com as credenciais do lab exportadas):

```bash
./gradlew build
cd infra
cp backend.hcl.example backend.hcl   # preencher bucket/tabela
terraform init -backend-config backend.hcl
TF_VAR_state_bucket=... TF_VAR_db_password=... TF_VAR_jwt_private_key="$(cat auth-private.pem)" terraform apply
```

## CI/CD

`.github/workflows/ci.yml`:

| Evento | Jobs |
|---|---|
| Pull request | build + testes + ZIP, `terraform fmt/validate`, `terraform plan` |
| Push na `main` / manual | build + testes + ZIP, `terraform validate`, `terraform apply` (ambiente `homolog`) + smoke test |

O smoke test confere o JWKS e se um CPF inválido devolve 400, sem depender de dados no banco.

Secrets usados: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (renovar a cada início do lab), `TF_STATE_BUCKET`, `TF_LOCK_TABLE`, `TF_VAR_DB_PASSWORD` e `AUTH_JWT_PRIVATE_KEY`.

Não commite chaves, senhas ou endpoints reais aqui.
