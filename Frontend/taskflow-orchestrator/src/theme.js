export const colors = {
  primary: '#4F46E5',
  bg: '#F3F4F6',
  card: '#FFFFFF',
  text: '#111827',
  muted: '#6B7280',
  border: '#E5E7EB',
  success: '#16A34A',
  warning: '#F59E0B',
  danger: '#DC2626',
  info: '#0EA5E9',
};

export const radius = { xs: 6, sm: 10, md: 14, lg: 18, pill: 999 };
export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 20, xxl: 24 };

export const shadows = {
  soft: {
    shadowColor: '#000',
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 4,
  },
  card: {
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 8 },
    elevation: 5,
  },
};

export const STATUS = {
  todo: { label: 'A fazer', color: '#6B7280' },
  doing: { label: 'Em andamento', color: '#0EA5E9' },
  review: { label: 'Em teste', color: '#8B5CF6' },
  done: { label: 'Concluída', color: '#16A34A' },
};
export const STATUS_ORDER = ['todo', 'doing', 'review', 'done'];

export const PRIORITY = {
  low: { label: 'Baixa', color: '#16A34A' },
  medium: { label: 'Média', color: '#F59E0B' },
  high: { label: 'Alta', color: '#EA580C' },
};
export const PRIORITY_ORDER = ['low', 'medium', 'high'];

export const RECURRENCE = {
  none: { label: 'Não se repete' },
  daily: { label: 'Diária' },
  weekly: { label: 'Semanal' },
  monthly: { label: 'Mensal' },
};
export const RECURRENCE_ORDER = ['none', 'daily', 'weekly', 'monthly'];
