import { toKey } from '../utils/format';

const d = (n) => {
  const x = new Date();
  x.setDate(x.getDate() + n);
  return toKey(x);
};
const dt = (n, h = 10) => {
  const x = new Date();
  x.setDate(x.getDate() + n);
  x.setHours(h, 0, 0, 0);
  return x.toISOString();
};

export const USERS = [
  { id: 'u1', name: 'Ana Souza', email: 'ana@taskflow.com', password: '123456', role: 'admin', active: true, title: 'Diretora de Projetos' },
  { id: 'u2', name: 'Bruno Lima', email: 'bruno@taskflow.com', password: '123456', role: 'gerente', active: true, title: 'Gerente de Projetos' },
  { id: 'u3', name: 'Carla Dias', email: 'carla@taskflow.com', password: '123456', role: 'colaborador', active: true, title: 'Desenvolvedora' },
  { id: 'u4', name: 'Diego Alves', email: 'diego@taskflow.com', password: '123456', role: 'colaborador', active: true, title: 'Designer UX' },
  { id: 'u5', name: 'Elisa Ramos', email: 'elisa@taskflow.com', password: '123456', role: 'visualizador', active: true, title: 'Cliente' },
  { id: 'u6', name: 'Felipe Costa', email: 'felipe@taskflow.com', password: '123456', role: 'colaborador', active: false, title: 'QA (inativo)' },
];

export const PROJECTS = [
  {
    id: 'p1',
    name: 'Site Institucional',
    description: 'Redesenho completo do site da empresa com novo CMS e blog.',
    ownerId: 'u2',
    memberIds: ['u2', 'u3', 'u4', 'u5'],
    deadline: d(30),
    color: '#4F46E5',
  },
  {
    id: 'p2',
    name: 'App Mobile SIGA',
    description: 'Aplicativo acadêmico em React Native para alunos e professores.',
    ownerId: 'u2',
    memberIds: ['u2', 'u3'],
    deadline: d(45),
    color: '#059669',
  },
  {
    id: 'p3',
    name: 'Migração para Cloud',
    description: 'Migração dos serviços on-premise para a nuvem com CI/CD.',
    ownerId: 'u1',
    memberIds: ['u1', 'u2', 'u4'],
    deadline: d(60),
    color: '#F59E0B',
  },
];

const cl = (...items) => items.map((t, i) => ({ id: `c${Math.random().toString(36).slice(2, 8)}${i}`, text: t[0], done: t[1] }));

export const TASKS = [
  { id: 't1', title: 'Definir identidade visual', description: 'Paleta, tipografia e guia de estilo para o novo site.', projectId: 'p1', assigneeId: 'u4', status: 'done', priority: 'high', dueDate: d(-5), createdAt: dt(-20), checklist: cl(['Moodboard', true], ['Paleta de cores', true], ['Tipografia', true]) },
  { id: 't2', title: 'Wireframes das páginas principais', description: 'Home, Sobre, Serviços e Contato em baixa fidelidade.', projectId: 'p1', assigneeId: 'u4', status: 'review', priority: 'medium', dueDate: d(2), createdAt: dt(-14), checklist: cl(['Home', true], ['Sobre', true], ['Serviços', false], ['Contato', false]) },
  { id: 't3', title: 'Implementar layout responsivo', description: 'Converter o design aprovado em componentes Next.js.', projectId: 'p1', assigneeId: 'u3', status: 'doing', priority: 'high', dueDate: d(6), createdAt: dt(-10), checklist: cl(['Header e footer', true], ['Home', false], ['Página de serviços', false]) },
  { id: 't4', title: 'Integrar CMS headless', description: 'Conectar o blog ao CMS e configurar preview.', projectId: 'p1', assigneeId: 'u3', status: 'todo', priority: 'medium', dueDate: d(14), createdAt: dt(-8), checklist: [] },
  { id: 't5', title: 'Corrigir bug de SEO no sitemap', description: 'Sitemap não inclui páginas dinâmicas.', projectId: 'p1', assigneeId: 'u3', status: 'todo', priority: 'urgent', dueDate: d(-2), createdAt: dt(-6), checklist: cl(['Reproduzir problema', true], ['Gerar sitemap dinâmico', false]) },
  { id: 't6', title: 'Tela de login e autenticação', description: 'Login com JWT e persistência de sessão.', projectId: 'p2', assigneeId: 'u3', status: 'doing', priority: 'urgent', dueDate: d(3), createdAt: dt(-9), checklist: cl(['Formulário', true], ['Validação', true], ['Persistir token', false]) },
  { id: 't7', title: 'Listagem de disciplinas', description: 'Consumir API do SIGA e listar disciplinas do aluno.', projectId: 'p2', assigneeId: 'u3', status: 'todo', priority: 'medium', dueDate: d(10), createdAt: dt(-5), checklist: [] },
  { id: 't8', title: 'Definir fluxo de notificações push', description: 'Escolher provedor e definir eventos.', projectId: 'p2', assigneeId: 'u2', status: 'review', priority: 'low', dueDate: d(20), createdAt: dt(-4), checklist: [] },
  { id: 't9', title: 'Mapear serviços legados', description: 'Inventário de serviços e dependências.', projectId: 'p3', assigneeId: 'u1', status: 'done', priority: 'high', dueDate: d(-10), createdAt: dt(-30), checklist: cl(['Levantar serviços', true], ['Mapear bancos', true]) },
  { id: 't10', title: 'Configurar pipeline CI/CD', description: 'GitHub Actions com deploy automático em staging.', projectId: 'p3', assigneeId: 'u2', status: 'doing', priority: 'high', dueDate: d(8), createdAt: dt(-12), checklist: cl(['Build', true], ['Testes', false], ['Deploy staging', false]) },
  { id: 't11', title: 'Protótipo do painel de monitoramento', description: 'Telas de métricas e alertas.', projectId: 'p3', assigneeId: 'u4', status: 'todo', priority: 'low', dueDate: d(25), createdAt: dt(-3), checklist: [] },
  { id: 't12', title: 'Migrar banco de dados de produção', description: 'Janela de migração planejada com rollback.', projectId: 'p3', assigneeId: 'u2', status: 'todo', priority: 'urgent', dueDate: d(40), createdAt: dt(-2), checklist: cl(['Plano de rollback', false], ['Backup completo', false]) },
];

export const NOTIFICATIONS = [
  { id: 'n1', userId: 'u1', type: 'task', title: 'Tarefa concluída', body: '"Mapear serviços legados" foi concluída.', date: dt(-1, 15), read: false },
  { id: 'n2', userId: 'u1', type: 'user', title: 'Novo usuário', body: 'Felipe Costa foi desativado.', date: dt(-2, 9), read: true },
  { id: 'n3', userId: 'u2', type: 'task', title: 'Tarefa atrasada', body: '"Corrigir bug de SEO no sitemap" passou do prazo.', date: dt(0, 8), read: false },
  { id: 'n4', userId: 'u2', type: 'task', title: 'Enviada para revisão', body: '"Wireframes das páginas principais" aguarda revisão.', date: dt(-1, 17), read: false },
  { id: 'n5', userId: 'u3', type: 'task', title: 'Nova tarefa atribuída', body: '"Tela de login e autenticação" foi atribuída a você.', date: dt(-3, 11), read: false },
  { id: 'n6', userId: 'u3', type: 'alert', title: 'Prazo se aproximando', body: '"Implementar layout responsivo" vence em 6 dias.', date: dt(0, 7), read: true },
  { id: 'n7', userId: 'u4', type: 'task', title: 'Comentário', body: 'Bruno pediu ajustes nos wireframes.', date: dt(-1, 13), read: false },
  { id: 'n8', userId: 'u5', type: 'project', title: 'Atualização de projeto', body: 'Site Institucional está 20% concluído.', date: dt(-2, 16), read: false },
];

export const HISTORY = [
  { id: 'h1', userId: 'u4', taskId: 't2', text: 'moveu "Wireframes das páginas principais" de Em andamento para Em revisão', date: dt(-1, 17) },
  { id: 'h2', userId: 'u3', taskId: 't6', text: 'marcou "Validação" como concluído em "Tela de login e autenticação"', date: dt(-1, 14) },
  { id: 'h3', userId: 'u2', taskId: 't5', text: 'alterou a prioridade de "Corrigir bug de SEO no sitemap" para Urgente', date: dt(-2, 10) },
  { id: 'h4', userId: 'u1', taskId: 't9', text: 'moveu "Mapear serviços legados" de Em revisão para Concluída', date: dt(-3, 16) },
  { id: 'h5', userId: 'u2', taskId: 't10', text: 'criou a tarefa "Configurar pipeline CI/CD"', date: dt(-12, 9) },
  { id: 'h6', userId: 'u1', projectId: 'p3', text: 'criou o projeto "Migração para Cloud"', date: dt(-32, 9) },
];
