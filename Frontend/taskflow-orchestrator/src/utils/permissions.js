export const ROLES = {
  admin: {
    label: 'Administrador',
    color: '#7C3AED',
    description: 'Acesso total: gerencia usuários, projetos, tarefas e vê todo o histórico.',
  },
  gerente: {
    label: 'Gerente',
    color: '#2563EB',
    description: 'Cria projetos e tarefas, atribui responsáveis, altera prioridades e status de qualquer tarefa.',
  },
  colaborador: {
    label: 'Colaborador',
    color: '#059669',
    description: 'Vê os projetos dos quais participa e atualiza status e checklist das próprias tarefas.',
  },
  visualizador: {
    label: 'Visualizador',
    color: '#6B7280',
    description: 'Somente leitura: acompanha projetos, tarefas, kanban e calendário.',
  },
};
export const ROLE_ORDER = ['admin', 'gerente', 'colaborador', 'visualizador'];

export const ACTIONS = {
  viewAllProjects: 'Ver todos os projetos',
  createProject: 'Criar projetos',
  createTask: 'Criar tarefas e atribuir responsáveis',
  editTask: 'Editar prioridade e checklist de qualquer tarefa',
  deleteTask: 'Excluir tarefas',
  changeStatusAny: 'Alterar status de qualquer tarefa',
  changeStatusOwn: 'Alterar status das próprias tarefas',
  viewHistory: 'Ver histórico de alterações',
  manageUsers: 'Gerenciar usuários e níveis de acesso',
};

const MATRIX = {
  admin: Object.keys(ACTIONS),
  gerente: ['viewAllProjects', 'createProject', 'createTask', 'editTask', 'deleteTask', 'changeStatusAny', 'changeStatusOwn', 'viewHistory'],
  colaborador: ['changeStatusOwn'],
  visualizador: ['viewAllProjects'],
};

export const can = (role, action) => !!(MATRIX[role] && MATRIX[role].includes(action));
