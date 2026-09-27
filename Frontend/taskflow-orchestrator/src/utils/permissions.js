export const ROLES = {
  admin: {
    label: 'Administrador',
    color: '#7C3AED',
    description: 'Administrador do backend: gerencia usuários e projetos.',
  },
  colaborador: {
    label: 'Colaborador',
    color: '#059669',
    description: 'Usuário padrão: cria e atualiza tarefas e projetos próprios.',
  },
};
export const ROLE_ORDER = ['admin', 'colaborador'];

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
  colaborador: ['createProject', 'createTask', 'editTask', 'deleteTask', 'changeStatusAny', 'changeStatusOwn'],
};

export const can = (role, action) => !!(MATRIX[role] && MATRIX[role].includes(action));
