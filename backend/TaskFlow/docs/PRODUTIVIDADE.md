# Controle de tempo, foco e produtividade

## Regras de negócio

### Cronômetro por tarefa
- Cada pessoa pode manter no máximo um cronômetro ativo ou pausado, em qualquer dispositivo.
- Iniciar exige permissão para trabalhar na tarefa e tarefa ainda aberta. A sessão fica persistida no servidor, então continua disponível ao reabrir o app.
- Pausar e retomar excluem o tempo pausado do total. Encerrar cria automaticamente um worklog; qualquer fração de minuto é arredondada para cima, com mínimo de 1 minuto e máximo de 24 horas por registro.
- Os worklogs manuais continuam disponíveis. O cronômetro é individual e não impede que outro usuário registre tempo em outra tarefa.

### Pomodoro / Modo Foco
- Durações de foco disponíveis na interface: 15, 25 e 50 minutos; pausa padrão de 5 minutos.
- Uma sessão é persistida e pode ser retomada ao reabrir o app. Só é contada como concluída depois da duração configurada; encerramento antecipado é registrado como incompleto.
- Se associada a uma tarefa, a sessão inicia ou retoma o cronômetro da tarefa. Ao concluir ou cancelar o foco, o cronômetro é pausado para que a pausa Pomodoro não seja contabilizada como trabalho.
- O fim da sessão não encerra o cronômetro; o usuário pode revisar e registrar o tempo posteriormente.

### Relatórios e metas
- Relatórios agregam worklogs, mudanças para Concluída feitas a partir desta versão e Pomodoros concluídos. Datas usam UTC, intervalo inclusivo e limite máximo de 366 dias.
- Dev/User consulta somente os próprios dados; PO consulta usuários/equipes e projetos visíveis; Master consulta o escopo global.
- Meta pessoal única por indicador e período (semanal ou mensal). Indicadores: horas registradas, tarefas concluídas e sessões de foco concluídas. Criar uma meta do mesmo tipo/periodicidade atualiza a existente.
- Metas exibem progresso do período atual: semana de segunda a domingo, mês do primeiro ao último dia.

### Previsão de conclusão do projeto
- Usa tarefas abertas de primeiro nível, estimativas menos worklogs já registrados e capacidade de 8 horas por colaborador participante em cada dia útil.
- Fins de semana e ausências aprovadas são descontados. Concluídas/subtarefas não são contadas para evitar duplicação.
- Sem estimativa em alguma tarefa aberta, a previsão é marcada como baixa confiança e não apresenta uma data final artificial. Confiança também considera se há histórico de horas registrado.
- O cálculo é uma projeção de planejamento; não representa garantia contratual.

## API

| Método | Rota | Uso |
|---|---|---|
| `GET` | `/api/productivity/timer/active` | Timer da pessoa autenticada; `204` se não houver |
| `POST` | `/api/productivity/timer/start` | Iniciar com `{ "taskId": 123 }` |
| `POST` | `/api/productivity/timer/pause` | Pausar timer atual |
| `POST` | `/api/productivity/timer/resume` | Retomar timer pausado |
| `POST` | `/api/productivity/timer/stop?comment=...` | Encerrar timer e criar worklog |
| `GET` | `/api/productivity/focus/active` | Pomodoro ativo; `204` se não houver |
| `GET` | `/api/productivity/focus/history` | Focos concluídos dos últimos 30 dias da pessoa |
| `POST` | `/api/productivity/focus` | Iniciar `{ "taskId": 123, "plannedMinutes": 25 }` (taskId opcional) |
| `POST` | `/api/productivity/focus/{id}/finish` | Encerrar com `{ "completed": true/false }` |
| `GET` | `/api/productivity/report?from=AAAA-MM-DD&to=AAAA-MM-DD&projectId=123&userId=4` | Relatório no escopo autorizado; filtros são opcionais |
| `GET` / `POST` | `/api/productivity/goals` | Listar / criar ou atualizar meta pessoal |
| `DELETE` | `/api/productivity/goals/{id}` | Excluir meta própria |
| `GET` | `/api/projects/{id}/forecast` | Previsão de esforço restante e conclusão autorizada |

A migration Flyway V16 adiciona timestamps de conclusão, sessões de timer e foco, e metas. Worklogs manuais continuam na V14. `ddl-auto` permanece `validate`.
