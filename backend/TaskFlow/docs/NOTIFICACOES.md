# Notificações e sincronização — TaskFlow

As notificações são persistidas no MySQL e pertencem ao usuário autenticado. Uma varredura agendada verifica tarefas abertas com prazo e cria eventos idempotentes: alertas críticos para tarefas vencidas e para as que vencem hoje ou amanhã; lembretes configuráveis para prazos futuros dentro da antecedência escolhida pelo destinatário. Os registros não são recriados a cada varredura e o estado de leitura permanece sincronizado entre dispositivos.

## Preferência individual

- `GET /api/users/me/reminder-days`: lê a antecedência configurada.
- `PUT /api/users/me/reminder-days/{days}`: salva a antecedência, de 0 a 30 dias.
- `0` significa sem lembrete antecipado; os alertas críticos de hoje/amanhã e vencidos continuam ativos.

## Central de notificações

Todas as rotas abaixo exigem `Authorization: Bearer <token>`.

- `GET /api/notifications`: lista até 100 notificações mais recentes do usuário autenticado.
- `GET /api/notifications/unread-count`: retorna `{ "count": 3 }`.
- `PATCH /api/notifications/{id}/read`: marca uma notificação própria como lida. IDs de outros usuários retornam 404.
- `POST /api/notifications/read-all`: marca todas as notificações próprias como lidas.

Exemplo de item:

```json
{
  "id": 25,
  "type": "CRITICAL",
  "title": "Prazo crítico: vence amanhã",
  "message": "Preparar apresentação vence amanhã.",
  "taskId": 18,
  "createdAt": "2026-10-04T15:30:00Z",
  "readAt": null
}
```

Destinatários: responsável atribuído, criador da tarefa, proprietário PO do projeto e todos os Masters. Tarefas concluídas e subtarefas não geram alertas duplicados; a tarefa principal representa o prazo. A frequência padrão da verificação é 60 segundos e pode ser ajustada por `taskflow.notifications.scan-interval-ms`.

## Acesso por papel

- **Master**: supervisão e gerenciamento de todos os projetos/tarefas e usuários.
- **PO**: cria e gerencia projetos próprios e todas as tarefas desses projetos.
- **Dev**: só visualiza projetos com tarefas atribuídas a si, atualiza o status e marca itens de checklist como feitos/pendentes; não cria projetos/tarefas, altera planejamento ou a estrutura dos checklists, nem exclui tarefas.
- O cadastro público permite criar contas Dev, PO ou Master. Cada conta Master tem escopo global de supervisão, sem aprovação prévia.
- O cadastro público aceita diretamente Dev, PO e Master; contas Master podem supervisionar todos os projetos e usuários.

## Sincronização entre dispositivos

O banco compartilhado é a fonte de verdade. O cliente móvel recarrega projetos, tarefas, papéis e notificações a cada 3 segundos enquanto ativo e ao voltar para o primeiro plano. Uma versão otimista por tarefa faz atualizações concorrentes retornarem HTTP 409 em vez de sobrescrever silenciosamente uma alteração mais nova; após o conflito, atualize os dados e repita a operação desejada. Realtime push para notificações com o app totalmente fechado (APNs/FCM/Expo Push) ainda não está configurado.
