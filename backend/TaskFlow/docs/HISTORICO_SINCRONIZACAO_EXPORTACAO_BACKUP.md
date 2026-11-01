# Histórico, sincronização offline, exportação e backup

## Histórico de alterações

A API registra eventos append-only para criação/edição/exclusão de projetos e tarefas, alterações de checklist, comentários, worklogs (incluindo cronômetro) e sessões de foco associadas a tarefas. Cada evento armazena ator, instante UTC, projeto, entidade, ação, resumo e um diff permitido; credenciais, tokens, senhas e anexos binários nunca são auditados.

`GET /api/history?page=0&size=30&projectId=123` retorna páginas de até 100 eventos. Masters podem consultar o escopo global; o proprietário de projeto pode ver seu histórico completo; membros/Dev veem eventos de projeto e tarefas a que têm acesso. Eventos de projetos apagados continuam disponíveis ao PO proprietário e ao Master. A retenção é permanente nesta versão; a migration V17 não define expurgo automático.

## Offline e sincronização entre dispositivos

- O token fica no SecureStore do dispositivo (ou no armazenamento local do navegador na Web). Projetos, tarefas e fila ficam em armazenamento privado do app, associado ao ID da conta. Logout remove credenciais, mas preserva o cache/fila daquela conta para permitir login posterior.
- Após primeiro login online, o último snapshot fica disponível sem rede. É possível criar, editar e excluir tarefas offline em projetos já sincronizados; tarefas novas recebem IDs locais até sincronizarem. Worklogs, timers, comentários, anexos, mudanças em projetos, relatórios e exportações continuam exigindo conexão.
- A fila envia as mutações ao voltar a conectar. Criações usam `clientMutationId` idempotente para não duplicar uma tarefa se a resposta HTTP se perder.
- Edições usam a versão otimista da tarefa. Se a versão remota mudou, o app preserva o rascunho local e não sobrescreve silenciosamente; na tela **Sincronização offline**, a pessoa escolhe usar o servidor ou reaplicar as próprias alterações sobre a versão mais recente. Exclusões em conflito também exigem confirmação explícita.
- O backend continua sendo a autoridade: permissões e validações são rechecadas durante o replay. Alterações cuja permissão foi revogada ficam bloqueadas para resolução.
- O polling online existente (a cada poucos segundos e ao retornar ao app) sincroniza outros dispositivos; neste momento não há push quando o app está encerrado.

O cache contém dados de negócio, nomes, descrições e rascunhos. O armazenamento fica no sandbox da aplicação; habilite bloqueio/criptografia do dispositivo e encerre a sessão em dispositivos compartilhados. Para Web, cache fica no armazenamento local do navegador.

## Exportação CSV/JSON

`GET /api/exports?format=json` ou `format=csv` baixa os projetos/tarefas visíveis, worklogs, comentários e histórico. `projectId` pode restringir a um projeto autorizado. A autorização é aplicada no backend e também limita eventos e tarefas exportados para colaboradores/Dev. Senhas, tokens e conteúdo binário de anexos não são incluídos. CSV usa UTF-8 e protege células que poderiam ser interpretadas como fórmulas por planilhas.

## Backup automático local em Docker

O Compose executa um serviço `backup` que faz diariamente às **02:00 UTC** cópias criptografadas do banco MySQL e do volume de anexos. O volume Docker `taskflow-backups` mantém 14 dias por padrão (`BACKUP_RETENTION_DAYS`). Arquivos são `.sql.gz.enc` e `.tar.gz.enc`, protegidos por AES-256-CBC/PBKDF2; o serviço também grava checksums SHA-256.

Configure `BACKUP_ENCRYPTION_KEY` com uma chave própria e forte antes de produção. O valor fallback `local-only-change-backup-key` serve apenas para desenvolvimento e não protege backups contra alguém com acesso ao ambiente. Guarde a chave de backup fora do host/volume e teste a restauração. O backup local no mesmo host **não** protege contra perda ou comprometimento do host; produção precisa de cópia off-site (S3, Azure Blob, etc.), controle de acesso e monitoramento.

O serviço `restore` fica desativado no profile normal e requer confirmação explícita (`CONFIRM_RESTORE=YES`). A restauração substitui o schema e todo o conteúdo de anexos: pare primeiro o backend, escolha os arquivos DB/anexos com o mesmo timestamp e preserve a chave usada na criptografia. Liste arquivos com `docker compose run --rm --entrypoint ls backup -l /backups`; depois execute `docker compose --profile maintenance run --rm -e CONFIRM_RESTORE=YES restore /backups/taskflow-db-<timestamp>.sql.gz.enc /backups/taskflow-attachments-<timestamp>.tar.gz.enc`. Depois, inicie o backend e valide login, projetos e downloads. `docker compose down -v` remove também o volume local de backups.
