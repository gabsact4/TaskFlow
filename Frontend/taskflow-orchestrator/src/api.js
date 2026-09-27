import { Platform } from 'react-native';

const host = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
export const API_URL = (process.env.EXPO_PUBLIC_API_URL || `http://${host}:8080`).replace(/\/$/, '');

let token = null;
export const setToken = (value) => { token = value; };

export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_URL}/api${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error(`Não foi possível conectar ao backend em ${API_URL}.`);
  }
  if (!response.ok) {
    let message = `Erro ${response.status}.`;
    try {
      const body = await response.json();
      message = body.message || body.error || body.detail || message;
    } catch {}
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

export const normalizeUser = (u) => ({ ...u, id: String(u.id), role: u.role === 'ADMIN' ? 'admin' : 'colaborador', active: true });
export const normalizeProject = (p) => ({ ...p, id: String(p.id), ownerId: String(p.ownerId), memberIds: [String(p.ownerId)], color: '#4F46E5', deadline: null, status: p.status?.toLowerCase() });
export const normalizeTask = (t) => ({
  ...t,
  id: String(t.id), projectId: String(t.projectId), assigneeId: t.assigneeId == null ? null : String(t.assigneeId),
  parentTaskId: t.parentTaskId == null ? null : String(t.parentTaskId),
  creatorId: String(t.creatorId), status: ({ TODO: 'todo', IN_PROGRESS: 'doing', DONE: 'done' })[t.status] || 'todo',
  priority: (t.priority || 'MEDIUM').toLowerCase(),
  recurrence: (t.recurrence || 'NONE').toLowerCase(),
  checklist: (t.checklist || []).map((item) => ({ ...item, id: String(item.id) })),
});
export const apiStatus = (status) => ({ todo: 'TODO', doing: 'IN_PROGRESS', review: 'IN_PROGRESS', done: 'DONE' })[status] || 'TODO';
