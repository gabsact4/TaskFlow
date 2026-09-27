export const toKey = (x) =>
  `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`;

export const todayKey = () => toKey(new Date());

export const formatDate = (key) => {
  if (!key) return '—';
  const [y, m, d] = key.split('-');
  return `${d}/${m}/${y}`;
};

export const formatDateTime = (iso) => {
  const x = new Date(iso);
  const hh = String(x.getHours()).padStart(2, '0');
  const mm = String(x.getMinutes()).padStart(2, '0');
  return `${formatDate(toKey(x))} às ${hh}:${mm}`;
};

export const isOverdue = (task) => !!task.dueDate && task.status !== 'done' && task.dueDate < todayKey();

export const projectProgress = (tasks) => {
  if (!tasks.length) return 0;
  return tasks.filter((t) => t.status === 'done').length / tasks.length;
};

export const checklistProgress = (task) => {
  if (!task.checklist || !task.checklist.length) return null;
  const done = task.checklist.filter((c) => c.done).length;
  return { done, total: task.checklist.length };
};
