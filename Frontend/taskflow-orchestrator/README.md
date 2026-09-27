# TaskFlow Orchestrator — Telas em React Native (Expo)

Protótipo navegável com **dados mock** e **níveis de usuário**.

## Como rodar
```bash
npm install
npx expo install --fix   # alinha as versões ao SDK do seu Expo Go
npx expo start
```
Escaneie o QR code com o Expo Go (ou pressione `a` / `i` / `w`).

## Níveis de usuário (login rápido na tela inicial — senha 123456)
| Nível | Usuário mock | O que pode |
|---|---|---|
| Administrador | ana@taskflow.com | Tudo: usuários/níveis, projetos, tarefas, histórico |
| Gerente | bruno@taskflow.com | Cria projetos e tarefas, altera prioridade/status de qualquer tarefa, vê histórico |
| Colaborador | carla@taskflow.com / diego@taskflow.com | Vê só seus projetos; altera status e checklist das próprias tarefas |
| Visualizador | elisa@taskflow.com | Somente leitura |

As regras ficam em `src/utils/permissions.js` (matriz `MATRIX`).

## Telas
Login · Início (dashboard) · Projetos · Detalhe do projeto · Novo projeto · Tarefas (busca + filtros por status/prioridade) · Detalhe da tarefa (status, prioridade, checklist, histórico) · Nova tarefa · Kanban · Calendário · Notificações · Histórico · Usuários e níveis · Permissões por nível · Perfil/Mais

## Estrutura
```
App.js
src/
  context/AppContext.js   estado global mock (usuários, projetos, tarefas...)
  data/mock.js            dados de exemplo
  utils/permissions.js    níveis de acesso
  components/             ui.js, TaskCard.js
  navigation/index.js     Stack + Bottom Tabs
  screens/                todas as telas
```
Os dados são mantidos em memória (reiniciar o app restaura o mock).
Para trocar pelo backend depois, substitua as ações do `AppContext` por chamadas à API.
