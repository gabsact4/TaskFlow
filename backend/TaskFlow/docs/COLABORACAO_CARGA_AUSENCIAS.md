# Colaboração, capacidade e ausências

## Regras de negócio

### Comentários
- Membros do projeto, responsável da tarefa e Master podem consultar e publicar comentários.
- O autor pode excluir o próprio comentário; o PO proprietário do projeto e o Master podem moderar.
- Texto obrigatório, limitado a 2.000 caracteres. Comentários não substituem observações de horas trabalhadas.

### Anexos
- Upload/download autenticados, disponíveis aos participantes autorizados da tarefa.
- Tipos aceitos: JPG/JPEG, PNG, GIF, WebP, PDF, TXT, CSV, DOCX, XLSX e PPTX. Limite de 20 MB.
- O servidor gera chave de armazenamento aleatória, sanitiza o nome original e valida assinatura binária dos formatos suportados. Os arquivos não são expostos como conteúdo estático.
- O autor pode excluir o próprio anexo; PO proprietário e Master também podem removê-lo.
- Em Docker, os arquivos ficam no volume `taskflow-attachments`. Para produção multi-instância, use storage de objetos privado e adicione varredura antimalware.

### Estimativas, capacidade e conflitos
- Tarefa pode informar início planejado, prazo final e estimativa de esforço entre 0,25 e 10.000 horas. O início não pode ser posterior ao prazo.
- Capacidade padrão: 8 horas por dia útil, de segunda a sexta. Feriados não são descontados nesta versão.
- A estimativa é distribuída igualmente pelos dias úteis entre início e prazo. Sem início, considera-se apenas o prazo; sem datas, não há previsão de carga.
- Tarefas concluídas e subtarefas não entram na análise, evitando dupla contagem. Registros de horas são históricos e não substituem estimativas futuras.
- Ausências aprovadas descontam dias úteis. Solicitações pendentes ou rejeitadas não alteram capacidade.
- Conflito é um alerta não bloqueante quando duas tarefas abertas da mesma pessoa têm períodos sobrepostos em dias úteis. Como não há horários no modelo, é um sinal para revisão e não comprova simultaneidade.
- PO visualiza equipes dos projetos a que tem acesso; Master visualiza todas; Dev/User vê somente a própria carga e conflitos.

### Férias e ausências
- Qualquer usuário autenticado solicita ausência em dias completos futuros, com justificativa opcional.
- A data final precisa ser igual ou posterior à inicial. Solicitações pendentes/aprovadas não podem se sobrepor para a mesma pessoa.
- PO proprietário de projeto relacionado ao colaborador ou Master pode aprovar/rejeitar; ninguém aprova a própria solicitação.
- Apenas ausências aprovadas reduzem capacidade. Solicitante pode remover a solicitação pendente; ausência aprovada pode ser cancelada pelo PO responsável ou Master.

## API
| Método | Rota | Uso |
|---|---|---|
| `GET` / `POST` | `/api/tasks/{taskId}/comments` | Listar/publicar comentário |
| `DELETE` | `/api/tasks/{taskId}/comments/{commentId}` | Excluir comentário autorizado |
| `GET` | `/api/tasks/{taskId}/attachments` | Listar metadados |
| `POST` | `/api/tasks/{taskId}/attachments` | Multipart com campo `file` |
| `GET` | `/api/tasks/{taskId}/attachments/{id}/download` | Download autenticado |
| `DELETE` | `/api/tasks/{taskId}/attachments/{id}` | Excluir anexo autorizado |
| `GET` / `POST` | `/api/absences` | Consultar solicitações visíveis / solicitar ausência |
| `PATCH` | `/api/absences/{id}/review` | Revisar com `{ "approved": true/false, "note": "..." }` |
| `DELETE` | `/api/absences/{id}` | Cancelar/remover solicitação autorizada |
| `GET` | `/api/team/capacity?from=AAAA-MM-DD&to=AAAA-MM-DD` | Carga, capacidade, utilização e conflitos (máximo 366 dias) |

A migration Flyway V15 cria os campos e tabelas desta entrega. O Hibernate permanece em `validate`; não altere o schema manualmente.
