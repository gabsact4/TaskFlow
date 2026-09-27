import React, { createContext, useContext, useMemo, useState, useCallback } from 'react';
import { request, setToken, normalizeUser, normalizeProject, normalizeTask, apiStatus } from '../api';
import { can as roleCan } from '../utils/permissions';

const Ctx = createContext(null);
export const useApp = () => useContext(Ctx);

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const can = useCallback((action) => !!user && roleCan(user.role, action), [user]);
  const getUser = useCallback((id) => users.find((u) => u.id === String(id)), [users]);
  const getProject = useCallback((id) => projects.find((p) => p.id === String(id)), [projects]);

  const refresh = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError('');
    try {
      const [projectData, me] = await Promise.all([request('/projects'), request('/users/me')]);
      const projectRows = projectData.map(normalizeProject);
      setProjects(projectRows);
      setUser(normalizeUser(me));
      if (me.role === 'ADMIN') {
        const page = await request('/users?size=100');
        setUsers((page.content || []).map(normalizeUser));
      } else setUsers([normalizeUser(me)]);
      const taskRows = await Promise.all(projectRows.map((p) => request(`/tasks/project/${p.id}`)));
      setTasks(taskRows.flat().map(normalizeTask));
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [user?.id]);

  const openSession = async (result) => {
    setToken(result.accessToken);
    setUser(normalizeUser(result.user));
    const [projectData, me] = await Promise.all([request('/projects'), request('/users/me')]);
    const projectRows = projectData.map(normalizeProject);
    setProjects(projectRows);
    setUser(normalizeUser(me));
    if (me.role === 'ADMIN') {
      const page = await request('/users?size=100');
      setUsers((page.content || []).map(normalizeUser));
    } else setUsers([normalizeUser(me)]);
    const taskRows = await Promise.all(projectRows.map((p) => request(`/tasks/project/${p.id}`)));
    setTasks(taskRows.flat().map(normalizeTask));
  };

  const login = async (email, password) => {
    setBusy(true); setError('');
    try {
      const result = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email: email.trim(), password }) });
      await openSession(result);
      return { ok: true };
    } catch (e) { setToken(null); setUser(null); setError(e.message); return { ok: false, error: e.message }; }
    finally { setBusy(false); }
  };
  const register = async (name, email, password) => {
    setBusy(true); setError('');
    try {
      const result = await request('/auth/register', { method: 'POST', body: JSON.stringify({ name: name.trim(), email: email.trim(), password }) });
      await openSession(result);
      return { ok: true };
    } catch (e) { setToken(null); setUser(null); setError(e.message); return { ok: false, error: e.message }; }
    finally { setBusy(false); }
  };
  const logout = () => { setToken(null); setUser(null); setUsers([]); setProjects([]); setTasks([]); setError(''); };

  const visibleProjects = useMemo(() => projects, [projects]);
  const visibleTasks = useMemo(() => tasks, [tasks]);
  const canManageTask = (task) => !!user && (task.creatorId === user.id || task.projectOwnerId === user.id || projects.find((p) => p.id === task.projectId)?.ownerId === user.id);
  const canChangeStatus = (task) => canManageTask(task);

  const addProject = async (data) => {
    const project = normalizeProject(await request('/projects', { method: 'POST', body: JSON.stringify({ name: data.name, projectKey: data.projectKey, description: data.description }) }));
    setProjects((all) => [project, ...all]); return project;
  };
  const addTask = async (data) => {
    const task = normalizeTask(await request('/tasks', { method: 'POST', body: JSON.stringify({ title: data.title, description: data.description, status: 'TODO', priority: (data.priority || 'medium').toUpperCase(), dueDate: data.dueDate || null, projectId: Number(data.projectId), assigneeId: data.assigneeId ? Number(data.assigneeId) : null }) }));
    setTasks((all) => [task, ...all]); return task;
  };
  const updateTask = async (task, changes) => {
    const body = { title: changes.title ?? task.title, description: changes.description ?? task.description ?? '', status: apiStatus(changes.status ?? task.status), priority: (changes.priority ?? task.priority).toUpperCase(), dueDate: changes.dueDate === undefined ? task.dueDate : changes.dueDate || null, projectId: Number(changes.projectId ?? task.projectId), assigneeId: (changes.assigneeId ?? task.assigneeId) ? Number(changes.assigneeId ?? task.assigneeId) : null };
    const saved = normalizeTask(await request(`/tasks/${task.id}`, { method: 'PUT', body: JSON.stringify(body) }));
    setTasks((all) => all.map((t) => t.id === saved.id ? saved : t)); return saved;
  };
  const setTaskStatus = async (id, status) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { status }); };
  const setTaskPriority = async (id, priority) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { priority }); };
  const deleteTask = async (id) => { await request(`/tasks/${id}`, { method: 'DELETE' }); setTasks((all) => all.filter((t) => t.id !== String(id))); };
  const setUserRole = async (id, role) => { const saved = normalizeUser(await request(`/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role: role === 'admin' ? 'ADMIN' : 'USER' }) })); setUsers((all) => all.map((u) => u.id === saved.id ? saved : u)); };
  const toggleUserActive = () => {};
  const noop = () => {};

  const value = {
    user, users, projects, tasks, history: [], error, loading, busy, refresh,
    visibleProjects, visibleTasks, can, canChangeStatus, canManageTask, getUser, getProject,
    login, register, loginAs: noop, logout, addTask, setTaskStatus, setTaskPriority,
    toggleChecklistItem: noop, addChecklistItem: noop, deleteTask, updateTask, addProject,
    setUserRole, toggleUserActive, myNotifications: [], unreadCount: 0, markRead: noop, markAllRead: noop,
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
