export const ROLES = {
  master: { label: 'Master', color: '#7C3AED', description: 'Supervisiona todos os projetos, tarefas e usuários.' },
  po: { label: 'PO', color: '#2563EB', description: 'Gerencia seus projetos e tarefas.' },
  dev: { label: 'Dev', color: '#059669', description: 'Atualiza tarefas atribuídas a você.' },
  admin: { label: 'Master', color: '#7C3AED', description: 'Papel administrativo legado.' },
  colaborador: { label: 'Dev', color: '#059669', description: 'Papel de colaborador legado.' },
};
export const ROLE_ORDER = ['master', 'po', 'dev'];
export const ACTIONS = { viewProjects: 'Ver projetos permitidos', createProject: 'Criar projetos', createTask: 'Criar tarefas', editTask: 'Editar tarefas', deleteTask: 'Excluir tarefas', changeStatusAny: 'Alterar tarefas dos próprios projetos', changeStatusOwn: 'Atualizar tarefas atribuídas', viewHistory: 'Ver histórico', manageUsers: 'Gerenciar usuários e papéis' };
const MATRIX = { master: Object.keys(ACTIONS), admin: Object.keys(ACTIONS), po: ['viewProjects', 'createProject', 'createTask', 'editTask', 'deleteTask', 'changeStatusAny', 'changeStatusOwn', 'viewHistory'], dev: ['viewProjects', 'changeStatusOwn'], colaborador: ['viewProjects', 'changeStatusOwn'] };
export const can = (role, action) => !!(MATRIX[role] && MATRIX[role].includes(action));
