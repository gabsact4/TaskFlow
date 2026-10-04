# Gerenciamento de Projetos e Tarefas

Esta implementação mantém a autenticação JWT e o gerenciamento de usuários já existentes e adiciona o núcleo do TaskFlow: projetos e tarefas.

## Projetos

`POST /api/projects` cria um projeto para o usuário autenticado.

Campos:
- `name`
- `projectKey` (única, usada como identificador curto do projeto)
- `description`

O projeto começa com status `ACTIVE`. Apenas o PO proprietário ou um Master pode editar ou excluir o projeto. Ao excluir um projeto, suas tarefas são excluídas pelo relacionamento do banco.

Rotas:
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/mine`
- `GET /api/projects/{id}`
- `PUT /api/projects/{id}`
- `DELETE /api/projects/{id}`

## Tarefas

Cada tarefa pertence a um projeto e possui:
- título
- descrição
- status: `TODO`, `IN_PROGRESS`, `DONE`
- prioridade: `LOW`, `MEDIUM`, `HIGH`
- prazo opcional
- responsável opcional
- criador
- projeto

Rotas:
- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `GET /api/tasks/project/{projectId}`
- `GET /api/tasks/project/{projectId}?status=IN_PROGRESS`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

O Master supervisiona e gerencia qualquer tarefa. O PO pode criar, editar e
excluir tarefas apenas nos próprios projetos. O Dev só pode consultar tarefas
atribuídas a si, alterar seu status e trabalhar no checklist; não pode mudar
campos de planejamento, reatribuir, mover ou excluir tarefas. A API aplica essas
regras no backend, além de ocultar projetos não atribuídos ao Dev.
As respostas incluem `version`. O cliente deve enviar essa versão no `PUT
/api/tasks/{id}`; se outro dispositivo já salvou uma versão mais nova, a API
responde `409 Conflict` e o cliente deve recarregar antes de repetir a edição.

## Exemplo de criação de projeto

```json
{
  "name": "TaskFlow",
  "projectKey": "TF",
  "description": "Projeto acadêmico de gerenciamento de tarefas"
}
```

## Exemplo de criação de tarefa

```json
{
  "title": "Criar tela de Kanban",
  "description": "Implementar quadro com colunas por status",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-10-10",
  "projectId": 1,
  "assigneeId": 1
}
```

## Relação com o backlog

- BACK-04 — CRUD de tarefas: implementado
- BACK-05 — CRUD de projetos: implementado
- BACK-06 — Prioridades: implementado
- Estrutura preparada para BACK-27 — Kanban, usando o status da tarefa
- Estrutura preparada para BACK-11 — filtros, com consulta por status

Checklist:
- API protegida por JWT
- validação dos dados
- regras de proprietário/criador
- persistência via JPA
- migração Flyway
- respostas sem expor senha ou outros dados sensíveis
