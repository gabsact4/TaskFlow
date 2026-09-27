# Gerenciamento de Usuários — TaskFlow Orchestrator

Este módulo cobre as operações sobre usuários já cadastrados: consulta e
atualização do próprio perfil, troca de senha, e administração completa
(listagem, consulta, alteração de papel e exclusão) restrita a `ROLE_ADMIN`.

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
  "role": "USER"
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

## Administração (exige `ROLE_ADMIN`)

Chamadas por um usuário sem o papel `ADMIN` recebem `403 Forbidden`.

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
Altera o papel de um usuário (ex.: promover a `ADMIN`).

**Requisição**
```json
{ "role": "ADMIN" }
```

### `DELETE /api/users/{id}`
Exclui a conta de qualquer usuário. **Resposta**: `204 No Content`.

## Resumo de autorização

| Rota | Autenticado | ADMIN |
|---|---|---|
| `GET/PUT /api/users/me`, `PATCH /api/users/me/password`, `DELETE /api/users/me` | ✅ | — |
| `GET /api/users`, `GET /api/users/{id}` | — | ✅ |
| `PUT /api/users/{id}/role`, `DELETE /api/users/{id}` | — | ✅ |

## Decisões de implementação

- **Separação clara entre autoatendimento e administração**: rotas `/me` nunca
  recebem um `id` do cliente — o usuário é sempre resolvido a partir do token
  (`@AuthenticationPrincipal`), evitando que alguém manipule o próprio id na
  URL para acessar dados de terceiros.
- **Autorização em dois níveis**: `SecurityConfig` garante que toda a API
  exige autenticação; `@PreAuthorize("hasRole('ADMIN')")` no `UserController`
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
