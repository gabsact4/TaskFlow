# TaskFlow Orchestrator

Sistema de gerenciamento de tarefas e projetos, desenvolvido como projeto acadêmico (tema **Gerenciamento**) para a FATEC São José dos Campos, curso de Desenvolvimento de Software Multiplataforma (DSM).

O produto foi planejado com um backlog completo de **45 itens** distribuídos em **8 épicos**, mas o desenvolvimento é executado em **3 sprints**, com escopo priorizado pelo método **MoSCoW** e estimativas em **Story Points** (escala Fibonacci). O objetivo é entregar um MVP funcional, coeso e defensável academicamente.

🎨 **Design das telas (Figma):** [TaskFlow Orchestrator Screens](https://www.figma.com/make/rdvZPOoZr6ScAwZOYeLyg0/TaskFlow-Orchestrator-Screens?fullscreen=1&t=VnR5Tk4KAj6K0GQr-1&code-node-id=0-6)

---

## Índice

- [Visão geral](#visão-geral)
- [Épicos do produto](#épicos-do-produto)
- [MVP — escopo das 3 sprints](#mvp--escopo-das-3-sprints)
- [Fluxo do MVP](#fluxo-do-mvp)
- [Plano de sprints](#plano-de-sprints)
- [Metodologia e organização no Jira](#metodologia-e-organização-no-jira)
- [Tecnologias](#tecnologias)
- [Critérios de conclusão das tasks](#critérios-de-conclusão-das-tasks)
- [Autor](#autor)

---

## Visão geral

O TaskFlow Orchestrator organiza o trabalho de desenvolvimento separando as atividades em três camadas técnicas, identificadas por prefixo nas tasks do Jira:

| Prefixo | Camada | Exemplo |
|---|---|---|
| `[BACK]` | Back-end | `[BACK] Criar API de tarefas` |
| `[FRONT]` | Front-end | `[FRONT] Criar tela de tarefas` |
| `[BANCO]` | Banco de Dados | `[BANCO] Criar tabela tarefas` |

Hierarquia adotada no Jira: **Épico → Item de Backlog/História → Tasks Técnicas → `[BACK]` / `[FRONT]` / `[BANCO]`**

## Épicos do produto

| Código | Épico | Cobertura |
|---|---|---|
| E1 | Identidade e Acesso | Autenticação, usuários, segurança de dados, biometria |
| E2 | Núcleo de Tarefas | Tarefas, projetos, prioridades, checklist, dependências |
| E3 | Organização e Visualização | Kanban, calendário, filtros, busca, linha do tempo |
| E4 | Notificações e Rotina | Notificações, lembretes, alertas, recorrência |
| E5 | Colaboração | Comentários, anexos, carga de equipe, conflitos |
| E6 | Produtividade e Analytics | Metas, modo foco, métricas, relatórios |
| E7 | Infraestrutura e Confiabilidade | Sincronização, offline, backup, exportação, histórico |
| E8 | Extensões | Recursos complementares de menor prioridade/maior risco |

## MVP — escopo das 3 sprints

O MVP contempla o fluxo essencial de gerenciamento de tarefas: autenticação e usuários, criação e organização de tarefas e projetos, visualização em Kanban e calendário, notificações e confiabilidade básica dos dados (segurança e backup). Os demais itens do backlog completo ficam documentados como trabalhos futuros.

## Fluxo do MVP

```
Login → Usuários → Projetos → Tarefas → Prioridades → Checklist
Kanban → Filtros → Histórico → Notificações → Calendário
Alertas críticos → Backup → Exportação
```

## Plano de sprints

| Sprint | Foco | Resultado |
|---|---|---|
| **Sprint 1** — Fundação | Acesso, usuários, projetos e tarefas | Login, usuários, projetos e tarefas funcionando |
| **Sprint 2** — Organização | Checklist, filtros, Kanban, histórico e notificações | Organização e acompanhamento do trabalho |
| **Sprint 3** — Fechamento do MVP | Calendário, alertas, backup e exportação | MVP completo e pronto para apresentação |

### Sprint 1 — Fundação

| Backlog | Funcionalidade | Back-end | Front-end | Banco |
|---|---|---|---|---|
| BACK-01 | Autenticação | Login, JWT e validação | Tela de login | Usuários/credenciais |
| BACK-03 | Usuários | CRUD | Cadastro/perfil | usuarios |
| BACK-44 | Segurança | Hash e proteção | Tratamento de erros | Dados protegidos |
| BACK-04 | Tarefas | CRUD/API | Tela e formulário | tarefas |
| BACK-05 | Projetos | CRUD/API | Tela de projetos | projetos |
| BACK-06 | Prioridades | API/campo | Seleção baixa/média/alta | prioridade |

### Sprint 2 — Organização

| Backlog | Funcionalidade | Back-end | Front-end | Banco |
|---|---|---|---|---|
| BACK-09 | Checklist | CRUD de subtarefas | Checklist na tarefa | checklists |
| BACK-11 | Filtros | Filtros/ordenação | Filtros e ordenação | Índices |
| BACK-27 | Kanban | Atualização de status | Quadro Kanban | status |
| BACK-20 | Histórico | Registro de alterações | Tela de histórico | historico |
| BACK-12 | Notificações | Criação/gerenciamento | Central de notificações | notificacoes |

### Sprint 3 — Fechamento do MVP

| Backlog | Funcionalidade | Back-end | Front-end | Banco |
|---|---|---|---|---|
| BACK-15 | Calendário | Tarefas por período | Visão semanal/mensal | Consultas por data |
| BACK-32 | Alertas | Serviço de vencimento | Destaques | Prazo/status |
| BACK-38 | Backup | Backup/restauração | Status, se necessário | Backup do banco |
| BACK-45 | Exportação | CSV/JSON | Botões de exportação | Consulta dos dados |

## Metodologia e organização no Jira

- **Metodologia:** Scrum adaptado, com 3 sprints.
- **Priorização:** MoSCoW (Must / Should / Could / Won't).
- **Estimativas:** Story Points, escala Fibonacci (1, 2, 3, 5, 8, 13).
- **Estrutura no Jira:** cada Épico agrupa Itens de Backlog/Histórias, que se desdobram em Tasks técnicas por camada (`[BACK]`, `[FRONT]`, `[BANCO]`), com Subtasks opcionais.
- Cada história de usuário implementada no MVP possui critérios de aceite documentados, guiando o desenvolvimento e a validação.

## Tecnologias

> Stack sugerida com base no perfil de desenvolvimento do projeto — ajuste conforme a decisão final da equipe.

- **Back-end:** Java + Spring Boot (API REST, autenticação JWT)
- **Front-end:** TypeScript + Next.js
- **Banco de Dados:** relacional (ex.: PostgreSQL/MySQL)
- **Design de telas:** Figma (link acima)
- **Gestão do projeto:** Jira (épicos, backlog e sprints)

## Critérios de conclusão das tasks

- Implementação realizada na camada correspondente.
- Integração entre Front-end, Back-end e Banco validada quando aplicável.
- Critérios de aceite atendidos.
- Testes básicos realizados.
- Sem erro crítico conhecido impedindo o uso.
- Status atualizado no Jira.

## Autor

**Gabriel Henrique Lazaro Almeida**
Estudante de Desenvolvimento de Software Multiplataforma (DSM) — FATEC São José dos Campos

- GitHub: [@gabsact4](https://github.com/gabsact4)
- LinkedIn: [gabriel-henrique-a1330a26a](https://linkedin.com/in/gabriel-henrique-a1330a26a)