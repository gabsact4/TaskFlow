# Autenticação por E-mail e Senha — TaskFlow Orchestrator

## Visão geral

A autenticação é **stateless**, baseada em **JWT (JSON Web Token)**. O usuário se
cadastra ou faz login uma vez e recebe um token que deve ser enviado em todas as
requisições subsequentes a endpoints protegidos.

```
Cliente                          API
   |  POST /api/auth/register       |
   |-------------------------------->|
   |   201 Created + accessToken     |
   |<--------------------------------|
   |                                 |
   |  GET /api/qualquer-recurso      |
   |  Authorization: Bearer <token>  |
   |-------------------------------->|
   |          200 OK                 |
   |<--------------------------------|
```

## Endpoints

### `POST /api/auth/register`

Cadastra um novo usuário e já retorna um token de acesso.

**Requisição**
```json
{
  "name": "Gabriel Almeida",
  "email": "gabriel@exemplo.com",
  "password": "senhaForte123"
}
```

**Resposta — 201 Created**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "Gabriel Almeida",
    "email": "gabriel@exemplo.com",
    "role": "DEV"
  }
}
```

**Erros**
| Status | Situação |
|---|---|
| 400 | Campos inválidos (nome/e-mail/senha em branco, e-mail mal formatado, senha curta) |
| 409 | Já existe uma conta com o e-mail informado |

### `POST /api/auth/login`

**Requisição**
```json
{
  "email": "gabriel@exemplo.com",
  "password": "senhaForte123"
}
```

**Resposta — 200 OK**: mesmo formato de `AuthResponse` do cadastro.

**Erros**
| Status | Situação |
|---|---|
| 400 | Campos inválidos |
| 401 | E-mail ou senha incorretos |

### Passkeys e autenticação biométrica

O backend usa WebAuthn/passkeys. A biometria é processada pelo autenticador do
dispositivo; a API recebe somente a chave pública e a resposta assinada. O
autenticador pode permitir PIN ou outro método local como alternativa à
biometria, conforme as configurações do dispositivo.

| Método e rota | Acesso | Resultado |
|---|---|---|
| `POST /api/auth/passkeys/registration/options` | JWT obrigatório | Opções WebAuthn para cadastrar uma passkey |
| `POST /api/auth/passkeys/registration/verify` | JWT obrigatório | Valida e salva a chave pública |
| `POST /api/auth/passkeys/login/options` | Público | Opções para login sem informar o e-mail |
| `POST /api/auth/passkeys/login/verify` | Público | Valida a assinatura e retorna o mesmo `AuthResponse` do login |

O endpoint `options` responde com `challengeId` e `options`. O cliente web deve
passar `options` para `navigator.credentials.create()` no cadastro ou
`navigator.credentials.get()` no login, usando um helper WebAuthn JSON para
codificar corretamente os campos binários. Depois envia:

```json
{
  "challengeId": "id-retornado-pelo-servidor",
  "credential": "json-WebAuthn-serializado-pelo-cliente"
}
```

Cada desafio expira em cinco minutos e só pode ser consumido uma vez. O servidor
valida desafio, origem, RP ID, assinatura e verificação do usuário antes de
emitir o JWT. O cadastro de passkey exige uma sessão autenticada; o login por
passkey não exige e-mail nem senha.

### Consumindo endpoints protegidos

Qualquer endpoint que não esteja na lista pública (`/api/auth/register`,
`/api/auth/login` e `/api/auth/passkeys/login/**`) exige o cabeçalho JWT. O
cadastro público aceita os papéis Dev, PO e Master; Masters têm acesso global.

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

## Testando com curl

```bash
# Cadastro
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Gabriel Almeida","email":"gabriel@exemplo.com","password":"senhaForte123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"gabriel@exemplo.com","password":"senhaForte123"}'

# Chamada autenticada (substitua <TOKEN> pelo accessToken recebido)
curl http://localhost:8080/api/algum-recurso \
  -H "Authorization: Bearer <TOKEN>"
```

## Decisões de implementação

- **Senhas**: nunca armazenadas em texto puro — são derivadas com `BCrypt`
  (`PasswordEncoder`), que já embute um salt por senha.
- **Nome e e-mail**: cifrados no banco com AES-256-GCM e nonce aleatório; o e-mail
  é localizado por um índice HMAC-SHA-256 com chave derivada, preservando a
  unicidade sem manter o endereço em texto puro.
- **Dados legados**: após a migration Flyway, a aplicação cifra nome/e-mail
  antigos e preenche o índice de busca no primeiro startup. Mantenha a mesma
  chave de dados entre instâncias e reinicializações; sem ela os dados não podem
  ser descriptografados.
- **Passkeys**: o banco guarda somente IDs, chave pública e contador. Chaves
  privadas e dados biométricos permanecem no autenticador do usuário.
- **E-mail único**: normalizado (trim + minúsculas) antes de gerar o índice HMAC,
  evitando duplicidade por diferença de caixa.
- **CORS/WebAuthn**: origens permitidas são configuradas explicitamente; use
  HTTPS em produção (WebAuthn só aceita HTTP em `localhost`).
- **Autenticação stateless**: sem sessão HTTP no servidor
  (`SessionCreationPolicy.STATELESS`); o token JWT carrega tudo o que é
  necessário para identificar o usuário a cada requisição.
- **Separação de camadas**: `AuthController` (HTTP) → `AuthService` (regra de
  negócio) → `UserRepository` (persistência), com um `GlobalExceptionHandler`
  centralizando o formato de erro.
- **Schema versionado via Flyway**: as migrations `V1__create_users_table.sql`
  e `V2__add_passkeys_and_encrypt_user_data.sql` controlam o schema; o Hibernate
  roda em modo `validate` (nunca gera DDL automaticamente).
- **Papéis (`Role`)**: no cadastro público, cada pessoa pode escolher `DEV`,
  `PO` ou `MASTER`, sem token ou aprovação de outro Master. O perfil Master
  tem acesso global de supervisão; Admin não pode ser criado publicamente.
  Masters também podem criar e promover contas pela gestão de usuários.

## Configuração necessária (variáveis de ambiente)

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | Recomendada | URL JDBC do MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | Recomendada | Credenciais do banco |
| `JWT_SECRET` | **Sim** | Chave aleatória com pelo menos 32 bytes, usada para assinar os tokens |
| `JWT_EXPIRATION_MS` | Opcional | Validade do token em milissegundos (padrão: 3600000 = 1h) |
| `DATA_ENCRYPTION_KEY` | **Sim** | Chave AES-256 em Base64; gere com `openssl rand -base64 32` e armazene em secret manager |
| `WEBAUTHN_RP_ID` | Produção | Domínio do cliente, sem esquema ou porta (ex.: `taskflow.exemplo.com`) |
| `WEBAUTHN_RP_NAME` | Opcional | Nome exibido no prompt de passkey (padrão: `TaskFlow`) |
| `WEBAUTHN_ORIGINS` | Produção | Origens WebAuthn exatas separadas por vírgula (ex.: `https://taskflow.exemplo.com`) |
| `CORS_ALLOWED_ORIGINS` | Produção | Origens do frontend autorizadas a chamar a API, separadas por vírgula |


> `JWT_SECRET` não possui fallback: sem essa variável, o backend não assina nem
> valida tokens. Proteja `DATA_ENCRYPTION_KEY` e mantenha-a no backup de
> segredos. Não a troque sem um procedimento de rotação que recifre os dados e
> regenere os índices HMAC.

## Próximos passos sugeridos

- Endpoint de *refresh token* para renovar o acesso sem exigir novo login.
- Fluxo de recuperação/redefinição de senha (envio de e-mail com token de reset).
- Confirmação de e-mail no cadastro (conta inativa até confirmação).
- Rate limiting no `/api/auth/login` para mitigar força bruta.
