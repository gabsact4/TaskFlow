# Gerenciamento de Usuários — TaskFlow Orchestrator

Este módulo cobre autoatendimento e administração de contas. O Master administra
usuários e papéis; PO administra apenas os próprios projetos; Dev trabalha
somente nas tarefas atribuídas a si. O cadastro público cria contas Dev e não
aceita papel informado pelo cliente. Para criar o primeiro Master, consulte
`docs/NOTIFICACOES.md` e configure as variáveis de bootstrap antes do primeiro
start com banco vazio.

Todas as rotas abaixo exigem o cabeçalho `Authorization: Bearer <token>`
(veja `docs/AUTENTICACAO.md` para como obter o token).

## Autoatendimento (qualquer usuário autenticado)

### `GET /api/users/me`
Retorna os dados do usuário autenticado.

**Resposta — 200 OK**
```json
{
  "id": 1,
  "name": "Gabriel Almeida",
  "email": "gabriel@exemplo.com",
  "role": "DEV"
}
```

### `PUT /api/users/me`
Atualiza nome e e-mail do próprio usuário.

**Requisição**
```json
{
  "name": "Gabriel H. Almeida",
  "email": "novo-email@exemplo.com"
}
```

**Erros**: `400` (dados inválidos), `409` (e-mail já em uso por outra conta).

### `PATCH /api/users/me/password`
Troca a senha do próprio usuário. Exige a senha atual.

**Requisição**
```json
{
  "currentPassword": "senhaForte123",
  "newPassword": "outraSenhaForte456"
}
```

**Resposta**: `204 No Content`.
**Erros**: `400` (senha atual incorreta ou nova senha inválida).

### `DELETE /api/users/me`
Exclui a própria conta. **Resposta**: `204 No Content`.

## Administração (exige `ROLE_MASTER` ou `ROLE_ADMIN` legado)

Chamadas por um usuário sem papel Master/Admin recebem `403 Forbidden`.

### `POST /api/users`
Cadastra uma conta diretamente como `DEV`, `PO` ou `MASTER`; exige sessão Master/Admin.
Senhas devem ter entre 8 e 72 caracteres.

**Requisição**
```json
{
  "name": "Ana Silva",
  "email": "ana@exemplo.com",
  "password": "senha-forte-123",
  "role": "PO"
}
```

### `GET /api/users?page=0&size=20&sort=id`
Lista usuários de forma paginada.

**Resposta — 200 OK**
```json
{
  "content": [
    { "id": 1, "name": "Gabriel Almeida", "email": "gabriel@exemplo.com", "role": "USER" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### `GET /api/users/{id}`
Consulta um usuário específico por id. **Erro**: `404` se não existir.

### `PUT /api/users/{id}/role`
Altera o papel de outro usuário para `DEV`, `PO` ou `MASTER`. Não é permitido
alterar o próprio papel nem rebaixar/remover o último Master.

**Requisição**
```json
{ "role": "DEV" }
```

### `DELETE /api/users/{id}`
Exclui a conta de qualquer usuário. **Resposta**: `204 No Content`.

## Resumo de autorização

| Rota | Autenticado | MASTER/ADMIN |
|---|---|---|
| `GET/PUT /api/users/me`, `PATCH /api/users/me/password`, `DELETE /api/users/me` | ✅ | — |
| `POST/GET /api/users`, `GET /api/users/{id}` | — | ✅ |
| `PUT /api/users/{id}/role`, `DELETE /api/users/{id}` | — | ✅ |

## Decisões de implementação

- **Separação clara entre autoatendimento e administração**: rotas `/me` nunca
  recebem um `id` do cliente — o usuário é sempre resolvido a partir do token
  (`@AuthenticationPrincipal`), evitando que alguém manipule o próprio id na
  URL para acessar dados de terceiros.
- **Autorização em dois níveis**: `SecurityConfig` garante que toda a API
  exige autenticação; `@PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")` no `UserController`
  refina o acesso às rotas administrativas, habilitado via `@EnableMethodSecurity`.
- **Troca de senha isolada do update de perfil**: evita que a senha seja
  alterada "de passagem" numa atualização de nome/e-mail, e sempre exige a
  senha atual como confirmação de identidade.
- **Paginação com contrato próprio (`PageResponse`)**: evita expor a
  serialização interna do `Page` do Spring Data diretamente na API pública.
- **Exclusão de conta**: por padrão, remove o registro do usuário
  (`DELETE`). Caso o negócio prefira manter histórico, uma evolução natural é
  trocar por *soft delete* (uma coluna `active`/`deleted_at`).

## Próximos passos sugeridos

- *Soft delete* de usuários, preservando histórico e vínculos com tarefas.
- Auditoria das ações administrativas (quem alterou o papel de quem, e quando).
- Endpoint de busca de usuários por nome/e-mail (`GET /api/users?query=...`).
