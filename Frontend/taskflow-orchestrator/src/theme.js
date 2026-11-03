import { Platform } from 'react-native';

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

export const shadow = (color = '#000', opacity = 0.1, blur = 8, x = 0, y = 4, elevation = 3) => {
  if (Platform.OS === 'web') {
    const hex = color.replace('#', '');
    const rgb = hex.length === 3
      ? hex.split('').map((part) => parseInt(part + part, 16))
      : [0, 2, 4].map((index) => parseInt(hex.slice(index, index + 2), 16));
    return { boxShadow: `${x}px ${y}px ${blur}px rgba(${rgb.join(', ')}, ${opacity})` };
  }
  return {
    shadowColor: color,
    shadowOpacity: opacity,
    shadowRadius: blur,
    shadowOffset: { width: x, height: y },
    elevation,
  };
};

export const shadows = {
  soft: shadow('#000', 0.06, 12, 0, 6, 4),
  card: shadow('#000', 0.08, 14, 0, 8, 5),
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
