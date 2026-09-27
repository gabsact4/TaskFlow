# TaskFlow Orchestrator — React Native (Expo)

Aplicativo Expo conectado à API Spring Boot do TaskFlow. Login e cadastro usam JWT; projetos, tarefas e usuários são carregados do backend.

## Como rodar

```bash
npm install
npx expo start
```

A API deve estar em execução em `http://localhost:8080`. O app usa `http://10.0.2.2:8080` por padrão no emulador Android e `http://localhost:8080` no iOS e web. Para aparelho físico ou outro endereço, defina `EXPO_PUBLIC_API_URL` antes de iniciar o Expo, por exemplo:

```bash
EXPO_PUBLIC_API_URL=http://192.168.1.20:8080 npx expo start
```

O backend precisa ter o banco, `JWT_SECRET` e as chaves de criptografia configurados. Para Expo Web, configure também `CORS_ALLOWED_ORIGINS` no backend com a origem usada pelo Expo.

## Funcionalidades conectadas

- Cadastro e login; o token JWT é enviado nas chamadas autenticadas.
- Listagem e criação de projetos.
- Listagem, criação, atualização de status/prioridade e exclusão de tarefas.
- Lista de usuários e alteração de papel por administradores.

Os status e prioridades exibidos correspondem aos enums da API. A edição/exclusão de tarefa respeita a regra do backend: apenas quem criou a tarefa ou quem é dono do projeto pode alterá-la.

## Limites atuais da API

O backend ainda não oferece checklist, histórico de alterações, notificações, membros/prazo de projeto ou ativação de usuários. Essas opções não são persistidas pelo app. Os papéis disponíveis na API são `ADMIN` e `USER`.

## Estrutura

- `src/api.js`: URL base, autenticação JWT e conversão dos DTOs.
- `src/context/AppContext.js`: chamadas e estado da sessão.
- `src/screens/`: telas do aplicativo.
