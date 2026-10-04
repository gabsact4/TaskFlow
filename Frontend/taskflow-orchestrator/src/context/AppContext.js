import React, { createContext, useContext, useMemo, useState, useCallback, useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { request, setToken, normalizeUser, normalizeProject, normalizeTask, apiStatus } from '../api';
import { can as roleCan } from '../utils/permissions';

const Ctx = createContext(null);
export const useApp = () => useContext(Ctx);

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [myNotifications, setMyNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [reminderDays, setReminderDays] = useState(2);
  const refreshInFlight = useRef(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const can = useCallback((action) => !!user && roleCan(user.role, action), [user]);
  const getUser = useCallback((id) => users.find((u) => u.id === String(id)), [users]);
  const getProject = useCallback((id) => projects.find((p) => p.id === String(id)), [projects]);

  const refresh = useCallback(async () => {
    if (!user || refreshInFlight.current) return;
    refreshInFlight.current = true;
    setLoading(true);
    setError('');
    try {
      const [projectData, me, reminder, notificationData, unreadData] = await Promise.all([request('/projects'), request('/users/me'), request('/users/me/reminder-days'), request('/notifications'), request('/notifications/unread-count')]);
      setReminderDays(reminder);
      setMyNotifications(notificationData.map((n) => ({ ...n, id: String(n.id), body: n.message, date: n.createdAt, read: !!n.readAt })));
      setUnreadCount(unreadData.count);
      const projectRows = projectData.map(normalizeProject);
      setProjects(projectRows);
      setUser(normalizeUser(me));
      if (['ADMIN', 'MASTER'].includes(me.role)) {
        const page = await request('/users?size=100');
        setUsers((page.content || []).map(normalizeUser));
      } else setUsers([normalizeUser(me)]);
      const taskRows = await Promise.all(projectRows.map((p) => request(`/tasks/project/${p.id}`)));
      setTasks(taskRows.flat().map(normalizeTask));
    } catch (e) { setError(e.message); }
    finally { setLoading(false); refreshInFlight.current = false; }
  }, [user?.id]);

  const openSession = async (result) => {
    setToken(result.accessToken);
    setUser(normalizeUser(result.user));
    const [projectData, me, reminder, notificationData, unreadData] = await Promise.all([request('/projects'), request('/users/me'), request('/users/me/reminder-days'), request('/notifications'), request('/notifications/unread-count')]);
    setReminderDays(reminder);
    setMyNotifications(notificationData.map((n) => ({ ...n, id: String(n.id), body: n.message, date: n.createdAt, read: !!n.readAt })));
    setUnreadCount(unreadData.count);
    const projectRows = projectData.map(normalizeProject);
    setProjects(projectRows);
    setUser(normalizeUser(me));
    if (['ADMIN', 'MASTER'].includes(me.role)) {
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
  const logout = () => { setToken(null); setUser(null); setUsers([]); setProjects([]); setTasks([]); setMyNotifications([]); setUnreadCount(0); setError(''); };

  useEffect(() => {
    if (!user) return undefined;
    const timer = setInterval(() => refresh(), 3000);
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });
    return () => { clearInterval(timer); subscription.remove(); };
  }, [user?.id, refresh]);

  const visibleProjects = useMemo(() => projects, [projects]);
  const visibleTasks = useMemo(() => tasks.filter((task) => !task.parentTaskId), [tasks]);
  const isProjectOwner = (task) => projects.find((project) => project.id === task.projectId)?.ownerId === user?.id;
  const canManageTask = (task) => !!user && (user.role === 'master' || (user.role === 'po' && isProjectOwner(task)));
  const canChangeStatus = (task) => canManageTask(task) || (user?.role === 'dev' && task.assigneeId === user.id);

  const addProject = async (data) => {
    const project = normalizeProject(await request('/projects', { method: 'POST', body: JSON.stringify({ name: data.name, projectKey: data.projectKey, description: data.description }) }));
    setProjects((all) => [project, ...all]); return project;
  };
  const addTask = async (data) => {
    const task = normalizeTask(await request('/tasks', { method: 'POST', body: JSON.stringify({ title: data.title, description: data.description, status: 'TODO', priority: (data.priority || 'medium').toUpperCase(), dueDate: data.dueDate || null, projectId: Number(data.projectId), assigneeId: data.assigneeId ? Number(data.assigneeId) : null, parentTaskId: data.parentTaskId ? Number(data.parentTaskId) : null, recurrence: (data.recurrence || 'none').toUpperCase(), recurrenceEndDate: data.recurrenceEndDate || null }) }));
    setTasks((all) => [task, ...all]); return task;
  };
  const updateTask = async (task, changes) => {
    const body = { title: changes.title ?? task.title, description: changes.description ?? task.description ?? '', status: apiStatus(changes.status ?? task.status), priority: (changes.priority ?? task.priority).toUpperCase(), dueDate: changes.dueDate === undefined ? task.dueDate : changes.dueDate || null, projectId: Number(changes.projectId ?? task.projectId), assigneeId: (changes.assigneeId ?? task.assigneeId) ? Number(changes.assigneeId ?? task.assigneeId) : null, recurrence: (changes.recurrence ?? task.recurrence ?? 'none').toUpperCase(), recurrenceEndDate: changes.recurrenceEndDate === undefined ? task.recurrenceEndDate || null : changes.recurrenceEndDate || null, version: Number(task.version || 0) };
    let response;
    try {
      response = await request(`/tasks/${task.id}`, { method: 'PUT', body: JSON.stringify(body) });
    } catch (error) {
      if (error.status === 409) await refresh();
      throw error;
    }
    const saved = normalizeTask(response);
    setTasks((all) => all.map((t) => t.id === saved.id ? saved : t)); return saved;
  };
  const setTaskStatus = async (id, status) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { status }); };
  const setTaskPriority = async (id, priority) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { priority }); };
  const setTaskRecurrence = async (id, recurrence, recurrenceEndDate) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { recurrence, recurrenceEndDate }); };
  const addChecklistItem = async (taskId, text) => {
    const item = await request(`/tasks/${taskId}/checklist`, { method: 'POST', body: JSON.stringify({ text }) });
    setTasks((all) => all.map((task) => task.id === String(taskId) ? { ...task, checklist: [...task.checklist, { ...item, id: String(item.id) }] } : task));
  };
  const toggleChecklistItem = async (taskId, itemId) => {
    const task = tasks.find((t) => t.id === String(taskId));
    const item = task?.checklist.find((entry) => entry.id === String(itemId));
    if (!item) return;
    const saved = await request(`/tasks/${taskId}/checklist/${itemId}`, { method: 'PATCH', body: JSON.stringify({ done: !item.done }) });
    setTasks((all) => all.map((t) => t.id === String(taskId) ? { ...t, checklist: t.checklist.map((entry) => entry.id === String(itemId) ? { ...entry, done: saved.done } : entry) } : t));
  };
  const deleteChecklistItem = async (taskId, itemId) => {
    await request(`/tasks/${taskId}/checklist/${itemId}`, { method: 'DELETE' });
    setTasks((all) => all.map((t) => t.id === String(taskId) ? { ...t, checklist: t.checklist.filter((entry) => entry.id !== String(itemId)) } : t));
  };
  const instantiateTaskTemplate = async (templateId, data) => {
    const task = normalizeTask(await request(`/task-templates/${templateId}/tasks`, { method: 'POST', body: JSON.stringify(data) }));
    setTasks((all) => [task, ...all]);
    return task;
  };
  const instantiateProjectTemplate = async (templateId, projectKey) => {
    const project = normalizeProject(await request(`/project-templates/${templateId}/projects`, { method: 'POST', body: JSON.stringify({ projectKey }) }));
    const taskRows = await request(`/tasks/project/${project.id}`);
    setProjects((all) => [project, ...all]);
    setTasks((all) => [...all, ...taskRows.map(normalizeTask)]);
    return project;
  };
  const deleteTask = async (id) => { await request(`/tasks/${id}`, { method: 'DELETE' }); setTasks((all) => all.filter((t) => t.id !== String(id) && t.parentTaskId !== String(id))); };
  const setUserRole = async (id, role) => { const saved = normalizeUser(await request(`/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role: ({ master: 'MASTER', po: 'PO', dev: 'DEV' })[role] }) })); setUsers((all) => all.map((u) => u.id === saved.id ? saved : u)); };
  const createManagedUser = async (data) => {
    const created = normalizeUser(await request('/users', { method: 'POST', body: JSON.stringify({ ...data, role: ({ master: 'MASTER', po: 'PO', dev: 'DEV' })[data.role] }) }));
    setUsers((all) => [created, ...all]);
    return created;
  };
  const toggleUserActive = () => {};
  const noop = () => {};
  const setPersonalReminderDays = async (days) => { const saved = await request(`/users/me/reminder-days/${days}`, { method: 'PUT' }); setReminderDays(saved); };
  const markRead = async (id) => {
    const existing = myNotifications.find((notification) => notification.id === String(id));
    const saved = await request(`/notifications/${id}/read`, { method: 'PATCH' });
    setMyNotifications((all) => all.map((n) => n.id === String(id) ? { ...n, read: !!saved.readAt } : n));
    if (existing && !existing.read && saved.readAt) setUnreadCount((count) => Math.max(0, count - 1));
  };
  const markAllRead = async () => {
    await request('/notifications/read-all', { method: 'POST' });
    setMyNotifications((all) => all.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
  };

  const value = {
    user, users, projects, tasks, history: [], error, loading, busy, refresh,
    visibleProjects, visibleTasks, can, canChangeStatus, canManageTask, getUser, getProject,
    login, register, loginAs: noop, logout, addTask, setTaskStatus, setTaskPriority, setTaskRecurrence,
    toggleChecklistItem, addChecklistItem, deleteChecklistItem, deleteTask, updateTask, addProject,
    instantiateTaskTemplate, instantiateProjectTemplate,
    setUserRole, createManagedUser, toggleUserActive, myNotifications, reminderDays, setPersonalReminderDays, unreadCount, markRead, markAllRead,
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
