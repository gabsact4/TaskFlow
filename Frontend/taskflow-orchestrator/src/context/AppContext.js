import React, { createContext, useContext, useMemo, useState, useCallback, useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { request, setToken, normalizeUser, normalizeProject, normalizeTask, apiStatus } from '../api';
import { can as roleCan } from '../utils/permissions';
import { clearAccessToken, loadOfflineData, loadOfflineQueue, persistSession, restoreSession, saveOfflineData, saveOfflineQueue } from '../offlineStore';

const isNetworkFailure = (error) => !error?.status && String(error?.message || '').includes('Não foi possível conectar');
const localMutationId = () => `offline-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
const taskCreateBody = (data) => ({
  title: data.title, description: data.description || '', status: apiStatus(data.status || 'todo'), priority: (data.priority || 'medium').toUpperCase(),
  dueDate: data.dueDate || null, plannedStartDate: data.plannedStartDate || null,
  estimatedHours: data.estimatedHours == null || data.estimatedHours === '' ? null : Number(data.estimatedHours),
  projectId: Number(data.projectId), assigneeId: data.assigneeId ? Number(data.assigneeId) : null,
  parentTaskId: data.parentTaskId ? Number(data.parentTaskId) : null, recurrence: (data.recurrence || 'none').toUpperCase(),
  recurrenceEndDate: data.recurrenceEndDate || null, clientMutationId: data.clientMutationId || null,
});
const taskUpdateBody = (task, changes) => {
  const assigneeId = Object.prototype.hasOwnProperty.call(changes, 'assigneeId') ? changes.assigneeId : task.assigneeId;
  return {
    title: changes.title ?? task.title, description: changes.description ?? task.description ?? '',
    status: apiStatus(changes.status ?? task.status), priority: (changes.priority ?? task.priority).toUpperCase(),
    dueDate: changes.dueDate === undefined ? task.dueDate : changes.dueDate || null,
    plannedStartDate: changes.plannedStartDate === undefined ? task.plannedStartDate : changes.plannedStartDate || null,
    estimatedHours: changes.estimatedHours === undefined ? task.estimatedHours : changes.estimatedHours == null || changes.estimatedHours === '' ? null : Number(changes.estimatedHours),
    projectId: Number(changes.projectId ?? task.projectId), assigneeId: assigneeId ? Number(assigneeId) : null,
    recurrence: (changes.recurrence ?? task.recurrence ?? 'none').toUpperCase(),
    recurrenceEndDate: changes.recurrenceEndDate === undefined ? task.recurrenceEndDate || null : changes.recurrenceEndDate || null,
    version: Number(task.version || 0),
  };
};
const applyQueuedChanges = (task, changes) => normalizeTask({ ...task, ...changes,
  status: changes.status ?? task.status, priority: changes.priority ?? task.priority,
  recurrence: changes.recurrence ?? task.recurrence, updatedAt: new Date().toISOString(),
});
const changesAlreadyApplied = (task, changes) => {
  const checks = {
    title: changes.title, description: changes.description, status: changes.status,
    priority: changes.priority, dueDate: changes.dueDate, plannedStartDate: changes.plannedStartDate,
    estimatedHours: changes.estimatedHours, recurrence: changes.recurrence, recurrenceEndDate: changes.recurrenceEndDate,
    assigneeId: changes.assigneeId, projectId: changes.projectId,
  };
  return Object.entries(checks).every(([key, desired]) => {
    if (desired === undefined) return true;
    const actual = task[key];
    if (key === 'assigneeId') return String(actual ?? '') === String(desired ?? '');
    if (key === 'estimatedHours' && desired != null) return Number(actual) === Number(desired);
    return String(actual ?? '') === String(desired ?? '');
  });
};

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
  const [activeTimer, setActiveTimer] = useState(null);
  const [online, setOnline] = useState(false);
  const [offlineQueue, setOfflineQueue] = useState([]);
  const refreshInFlight = useRef(false);
  const syncInFlight = useRef(false);
  const taskCacheRef = useRef([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const can = useCallback((action) => !!user && roleCan(user.role, action), [user]);
  const getUser = useCallback((id) => users.find((u) => u.id === String(id)), [users]);
  const getProject = useCallback((id) => projects.find((p) => p.id === String(id)), [projects]);

  useEffect(() => { taskCacheRef.current = tasks; }, [tasks]);

  const syncQueuedMutations = useCallback(async (serverTasks, currentUserId = user?.id, cachedTasks = []) => {
    if (!currentUserId || syncInFlight.current) return serverTasks;
    syncInFlight.current = true;
    try {
      let queue = await loadOfflineQueue(currentUserId);
      if (!queue.length) { setOfflineQueue([]); setTasks(serverTasks); return serverTasks; }
      const localRows = [...cachedTasks, ...taskCacheRef.current].filter((task, index, all) => String(task.id).startsWith('local-') && all.findIndex((entry) => entry.id === task.id) === index);
      let rows = [...serverTasks, ...localRows];
      for (const queued of queue) {
        if (queued.type === 'TASK_DELETE' && queued.status === 'pending') rows = rows.filter((task) => task.id !== String(queued.taskId));
        if (queued.type === 'TASK_UPDATE') {
          const current = rows.find((task) => task.id === String(queued.taskId)) || queued.serverTask || queued.baseSnapshot;
          if (current) {
            const draft = applyQueuedChanges(current, queued.changes);
            rows = rows.some((task) => task.id === String(queued.taskId))
              ? rows.map((task) => task.id === String(queued.taskId) ? { ...draft, offlineConflict: queued.status !== 'pending' } : task)
              : [...rows, { ...draft, offlineConflict: queued.status !== 'pending' }];
          }
        }
      }
      const localIds = new Map();
      for (let index = 0; index < queue.length; index += 1) {
        let operation = queue[index];
        if (operation.status === 'conflict' || operation.status === 'blocked') continue;
        try {
          if (operation.type === 'TASK_CREATE') {
            const data = { ...operation.data };
            if (data.parentTaskId && localIds.has(String(data.parentTaskId))) data.parentTaskId = Number(localIds.get(String(data.parentTaskId)));
            if (String(data.parentTaskId || '').startsWith('local-')) throw Object.assign(new Error('Sincronize primeiro a tarefa pai.'), { status: 409 });
            const created = normalizeTask(await request('/tasks', { method: 'POST', body: JSON.stringify(taskCreateBody(data)) }));
            localIds.set(String(operation.localId), created.id);
            rows = rows.some((task) => task.id === String(operation.localId))
              ? rows.map((task) => task.id === String(operation.localId) ? created : task)
              : [created, ...rows];
            queue = queue.filter((item) => item.id !== operation.id);
            for (const later of queue) {
              if (String(later.taskId) === String(operation.localId)) later.taskId = created.id;
              if (later.data?.parentTaskId != null && String(later.data.parentTaskId) === String(operation.localId)) later.data.parentTaskId = Number(created.id);
            }
            index -= 1;
            continue;
          }

          const taskId = localIds.get(String(operation.taskId)) || operation.taskId;
          let remote;
          try { remote = normalizeTask(await request(`/tasks/${taskId}`)); }
          catch (lookupError) {
            if (operation.type === 'TASK_DELETE' && lookupError.status === 404) {
              queue = queue.filter((item) => item.id !== operation.id);
              rows = rows.filter((task) => task.id !== String(taskId) && task.parentTaskId !== String(taskId));
              index -= 1;
              continue;
            }
            throw lookupError;
          }
          if (Number(remote.version) !== Number(operation.baseVersion)) {
            if (operation.type === 'TASK_UPDATE' && changesAlreadyApplied(remote, operation.changes)) {
              queue = queue.filter((item) => item.id !== operation.id);
              rows = rows.map((task) => task.id === remote.id ? remote : task);
              index -= 1;
              continue;
            }
            operation = { ...operation, status: 'conflict', serverTask: remote, message: 'A tarefa mudou no servidor desde a última sincronização.' };
            queue[index] = operation;
            if (operation.type === 'TASK_DELETE') rows = [...rows, { ...remote, offlineConflict: true }];
            else rows = rows.map((task) => task.id === remote.id ? { ...applyQueuedChanges(remote, operation.changes), offlineConflict: true } : task);
            continue;
          }
          if (operation.type === 'TASK_UPDATE') {
            const saved = normalizeTask(await request(`/tasks/${taskId}`, { method: 'PUT', body: JSON.stringify(taskUpdateBody(remote, operation.changes)) }));
            rows = rows.map((task) => task.id === String(taskId) ? saved : task);
          } else if (operation.type === 'TASK_DELETE') {
            await request(`/tasks/${taskId}`, { method: 'DELETE' });
            rows = rows.filter((task) => task.id !== String(taskId) && task.parentTaskId !== String(taskId));
          }
          queue = queue.filter((item) => item.id !== operation.id);
          index -= 1;
        } catch (syncError) {
          if (isNetworkFailure(syncError)) { setOnline(false); break; }
          queue[index] = { ...operation, status: syncError.status === 403 ? 'blocked' : 'conflict', message: syncError.message || 'Não foi possível aplicar esta alteração.' };
        }
      }
      setTasks(rows);
      setOfflineQueue(queue);
      await saveOfflineQueue(currentUserId, queue);
      return rows;
    } finally { syncInFlight.current = false; }
  }, [user?.id]);



  const refresh = useCallback(async () => {
    if (!user || refreshInFlight.current) return;
    refreshInFlight.current = true;
    setLoading(true);
    setError('');
    try {
      const [projectData, me, reminder, notificationData, unreadData, timerData] = await Promise.all([request('/projects'), request('/users/me'), request('/users/me/reminder-days'), request('/notifications'), request('/notifications/unread-count'), request('/productivity/timer/active')]);
      setReminderDays(reminder);
      setMyNotifications(notificationData.map((n) => ({ ...n, id: String(n.id), body: n.message, date: n.createdAt, read: !!n.readAt })));
      setUnreadCount(unreadData.count);
      setActiveTimer(timerData ? { ...timerData, id: String(timerData.id), taskId: String(timerData.taskId), syncedAt: Date.now() } : null);
      const projectRows = projectData.map(normalizeProject);
      const currentUser = normalizeUser(me);
      setProjects(projectRows);
      setUser(currentUser);
      const userRows = (await request('/users/assignable')).map(normalizeUser);
      setUsers(userRows);
      const taskRows = await Promise.all(projectRows.map((p) => request(`/tasks/project/${p.id}`)));
      const normalizedTasks = taskRows.flat().map(normalizeTask);
      setTasks(normalizedTasks);
      setOnline(true);
      setError('');
      const syncedTasks = await syncQueuedMutations(normalizedTasks, currentUser.id);
      await saveOfflineData(currentUser.id, { user: currentUser, users: userRows, projects: projectRows, tasks: syncedTasks || normalizedTasks });
    } catch (e) { setOnline(false); setError(e.message); }
    finally { setLoading(false); refreshInFlight.current = false; }
  }, [user?.id, syncQueuedMutations]);

  const openSession = async (result) => {
    setToken(result.accessToken);
    await persistSession(result.accessToken, result.user);
    const restoredUser = normalizeUser(result.user);
    setUser(restoredUser);
    const [cachedData, queue] = await Promise.all([loadOfflineData(restoredUser.id), loadOfflineQueue(restoredUser.id)]);
    setOfflineQueue(queue);
    if (cachedData) {
      setProjects(cachedData.projects || []); setUsers(cachedData.users || []); setTasks(cachedData.tasks || []);
    }
    try {
    const [projectData, me, reminder, notificationData, unreadData, timerData] = await Promise.all([request('/projects'), request('/users/me'), request('/users/me/reminder-days'), request('/notifications'), request('/notifications/unread-count'), request('/productivity/timer/active')]);
    setReminderDays(reminder);
    setMyNotifications(notificationData.map((n) => ({ ...n, id: String(n.id), body: n.message, date: n.createdAt, read: !!n.readAt })));
    setUnreadCount(unreadData.count);
    setActiveTimer(timerData ? { ...timerData, id: String(timerData.id), taskId: String(timerData.taskId), syncedAt: Date.now() } : null);
    const projectRows = projectData.map(normalizeProject);
    const currentUser = normalizeUser(me);
    setProjects(projectRows);
    setUser(currentUser);
    const userRows = (await request('/users/assignable')).map(normalizeUser);
    setUsers(userRows);
    const taskRows = await Promise.all(projectRows.map((p) => request(`/tasks/project/${p.id}`)));
    const normalizedTasks = taskRows.flat().map(normalizeTask);
    setTasks(normalizedTasks);
    setOnline(true);
    const syncedTasks = await syncQueuedMutations(normalizedTasks, currentUser.id, cachedData?.tasks || []);
    await saveOfflineData(currentUser.id, { user: currentUser, users: userRows, projects: projectRows, tasks: syncedTasks || normalizedTasks });
    } catch (error) {
      if (isNetworkFailure(error) && cachedData) { setOnline(false); setError('Sem conexão. Os dados salvos neste dispositivo estão disponíveis offline.'); return; }
      throw error;
    }
  };



  const login = async (email, password) => {
    setBusy(true); setError('');
    try {
      const result = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email: email.trim(), password }) });
      await openSession(result);
      return { ok: true };
    } catch (e) { setToken(null); clearAccessToken().catch(() => {}); setUser(null); setOnline(false); setError(e.message); return { ok: false, error: e.message }; }
    finally { setBusy(false); }
  };
  const register = async (name, email, password, role = 'DEV') => {
    setBusy(true); setError('');
    try {
      const result = await request('/auth/register', { method: 'POST', body: JSON.stringify({ name: name.trim(), email: email.trim(), password, role }) });
      await openSession(result);
      return { ok: true };
    } catch (e) { setToken(null); clearAccessToken().catch(() => {}); setUser(null); setOnline(false); setError(e.message); return { ok: false, error: e.message }; }
    finally { setBusy(false); }
  };
  const logout = () => { setToken(null); clearAccessToken().catch(() => {}); setUser(null); setUsers([]); setProjects([]); setTasks([]); setMyNotifications([]); setUnreadCount(0); setActiveTimer(null); setOnline(false); setOfflineQueue([]); setError(''); };

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const saved = await restoreSession();
        if (!active || !saved) return;
        const restoredUser = normalizeUser(saved.user);
        setToken(saved.accessToken);
        const [snapshot, queue] = await Promise.all([loadOfflineData(restoredUser.id), loadOfflineQueue(restoredUser.id)]);
        if (!active) return;
        if (snapshot) {
          setUser(snapshot.user || restoredUser);
          setUsers(snapshot.users || []);
          setProjects(snapshot.projects || []);
          setTasks(snapshot.tasks || []);
        } else setUser(restoredUser);
        setOfflineQueue(queue);
        setOnline(false);
      } catch { if (active) setOnline(false); }
    })();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!user) return undefined;
    const timer = setInterval(() => refresh(), online ? 3000 : 15000);
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') refresh();
    });
    return () => { clearInterval(timer); subscription.remove(); };
  }, [user?.id, refresh, online]);

  const visibleProjects = useMemo(() => projects, [projects]);
  const visibleTasks = useMemo(() => tasks.filter((task) => !task.parentTaskId), [tasks]);
  const isProjectOwner = (task) => projects.find((project) => project.id === task.projectId)?.ownerId === user?.id;
  const canManageTask = (task) => !!user && (user.role === 'master' || (user.role === 'po' && isProjectOwner(task)));
  const canChangeStatus = (task) => canManageTask(task) || (user?.role === 'dev' && task.assigneeId === user.id);
  const persistQueue = async (queue) => {
    setOfflineQueue(queue);
    if (user?.id) await saveOfflineQueue(user.id, queue);
  };
  const applyOfflineTaskChanges = applyQueuedChanges;
  const queueTaskUpdate = async (task, changes) => {
    const queue = await loadOfflineQueue(user.id);
    const createOperation = queue.find((item) => item.type === 'TASK_CREATE' && item.localId === task.id && item.status !== 'conflict');
    if (createOperation) createOperation.data = { ...createOperation.data, ...changes };
    else {
      const existing = queue.find((item) => item.type === 'TASK_UPDATE' && item.taskId === task.id && item.status === 'pending');
      if (existing) existing.changes = { ...existing.changes, ...changes };
      else queue.push({ id: localMutationId(), type: 'TASK_UPDATE', taskId: task.id, baseVersion: Number(task.version || 0), baseSnapshot: task, changes, status: 'pending', createdAt: new Date().toISOString() });
    }
    await persistQueue(queue);
    const optimistic = applyOfflineTaskChanges(task, changes);
    const updatedTasks = tasks.map((entry) => entry.id === task.id ? optimistic : entry);
    setTasks(updatedTasks);
    await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
    return optimistic;
  };

  const addProject = async (data) => {
    const project = normalizeProject(await request('/projects', { method: 'POST', body: JSON.stringify({ name: data.name, projectKey: data.projectKey, description: data.description, memberIds: data.memberIds || [] }) }));
    setProjects((all) => [project, ...all]); return project;
  };
  const updateProject = async (project, changes) => {
    const saved = normalizeProject(await request(`/projects/${project.id}`, { method: 'PUT', body: JSON.stringify({ name: changes.name ?? project.name, description: changes.description ?? project.description ?? '', status: (changes.status ?? project.status).toUpperCase(), memberIds: changes.memberIds ?? project.memberIds.filter((id) => id !== project.ownerId).map(Number) }) }));
    setProjects((all) => all.map((item) => item.id === saved.id ? saved : item));
    return saved;
  };
  const addTask = async (data) => {
    const creationData = { ...data, clientMutationId: data.clientMutationId || localMutationId() };
    const body = taskCreateBody(creationData);
    try {
      const task = normalizeTask(await request('/tasks', { method: 'POST', body: JSON.stringify(body) }));
      const updatedTasks = [task, ...tasks]; setTasks(updatedTasks);
      if (user?.id) await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      return task;
    } catch (error) {
      if (!isNetworkFailure(error) || !user?.id) throw error;
      const project = projects.find((entry) => entry.id === String(data.projectId));
      if (!project || String(data.projectId).startsWith('local-')) throw new Error('Para criar tarefas offline, o projeto precisa estar sincronizado neste dispositivo.');
      const localId = `local-${localMutationId()}`;
      const localTask = normalizeTask({ id: localId, ...body, projectId: String(project.id), projectName: project.name,
        assigneeId: data.assigneeId || null, assigneeName: data.assigneeId ? getUser(data.assigneeId)?.name : null,
        creatorId: user.id, creatorName: user.name, parentTaskId: data.parentTaskId || null, checklist: [], version: 0,
        createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() });
      const queue = await loadOfflineQueue(user.id);
      queue.push({ id: localMutationId(), type: 'TASK_CREATE', localId, data: creationData, status: 'pending', createdAt: new Date().toISOString() });
      await persistQueue(queue);
      const updatedTasks = [localTask, ...tasks]; setTasks(updatedTasks); setOnline(false);
      await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      return localTask;
    }
  };
  const updateTask = async (task, changes) => {
    if (String(task.id).startsWith('local-')) return queueTaskUpdate(task, changes);
    const body = taskUpdateBody(task, changes);
    let response;
    try {
      response = await request(`/tasks/${task.id}`, { method: 'PUT', body: JSON.stringify(body) });
    } catch (error) {
      if (error.status === 409) await refresh();
      if (isNetworkFailure(error) && user?.id) { setOnline(false); return queueTaskUpdate(task, changes); }
      throw error;
    }
    const saved = normalizeTask(response);
    const updatedTasks = tasks.map((taskRow) => taskRow.id === saved.id ? saved : taskRow);
    setTasks(updatedTasks);
    if (user?.id) await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
    return saved;
  };
  const setTaskStatus = async (id, status) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { status }); };
  const setTaskAssignee = async (id, assigneeId) => { const task = tasks.find((t) => t.id === String(id)); if (task) return updateTask(task, { assigneeId }); };
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
  const deleteTask = async (id) => {
    const task = tasks.find((entry) => entry.id === String(id));
    if (!task) return;
    if (String(id).startsWith('local-')) {
      const queue = (await loadOfflineQueue(user.id)).filter((operation) => operation.localId !== String(id) && operation.taskId !== String(id) && String(operation.data?.parentTaskId) !== String(id));
      await persistQueue(queue);
      const updatedTasks = tasks.filter((entry) => entry.id !== String(id) && entry.parentTaskId !== String(id));
      setTasks(updatedTasks);
      await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      return;
    }
    try { await request(`/tasks/${id}`, { method: 'DELETE' }); }
    catch (error) {
      if (!isNetworkFailure(error) || !user?.id) throw error;
      const queue = (await loadOfflineQueue(user.id)).filter((operation) => !(operation.type === 'TASK_UPDATE' && operation.taskId === String(id)));
      queue.push({ id: localMutationId(), type: 'TASK_DELETE', taskId: String(id), baseVersion: Number(task.version || 0), baseSnapshot: task, status: 'pending', createdAt: new Date().toISOString() });
      await persistQueue(queue); setOnline(false);
    }
    const updatedTasks = tasks.filter((entry) => entry.id !== String(id) && entry.parentTaskId !== String(id));
    setTasks(updatedTasks);
    if (user?.id) await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
  };
  const resolveOfflineConflict = async (operationId, resolution) => {
    const queue = await loadOfflineQueue(user.id);
    const operation = queue.find((item) => item.id === operationId);
    if (!operation) return;
    if (resolution === 'server') {
      if (operation.status === 'pending') {
        const remaining = queue.filter((item) => item.id !== operationId);
        await persistQueue(remaining);
        const restoredTasks = operation.type === 'TASK_CREATE'
          ? tasks.filter((task) => task.id !== operation.localId && task.parentTaskId !== operation.localId)
          : operation.type === 'TASK_DELETE'
            ? [operation.baseSnapshot, ...tasks.filter((task) => task.id !== String(operation.taskId))]
            : tasks.map((task) => task.id === String(operation.taskId) ? operation.baseSnapshot : task);
        setTasks(restoredTasks);
        await saveOfflineData(user.id, { user, users, projects, tasks: restoredTasks });
        return;
      }
      let serverTask = operation.serverTask;
      if (!serverTask && (operation.type === 'TASK_UPDATE' || operation.type === 'TASK_DELETE')) {
        try { serverTask = normalizeTask(await request(`/tasks/${operation.taskId}`)); }
        catch (error) { if (error.status !== 404 && error.status !== 403) throw error; }
      }
      const remaining = queue.filter((item) => item.id !== operationId);
      await persistQueue(remaining);
      let reconciledTasks = tasks;
      if (operation.type === 'TASK_CREATE') reconciledTasks = tasks.filter((task) => task.id !== operation.localId);
      else if (serverTask) reconciledTasks = tasks.map((task) => task.id === String(operation.taskId) ? serverTask : task);
      else if (operation.type === 'TASK_DELETE' || operation.type === 'TASK_UPDATE') reconciledTasks = tasks.filter((task) => task.id !== String(operation.taskId));
      else if (operation.baseSnapshot) reconciledTasks = tasks.map((task) => task.id === String(operation.taskId) ? operation.baseSnapshot : task);
      setTasks(reconciledTasks);
      await saveOfflineData(user.id, { user, users, projects, tasks: reconciledTasks });
      return;
    }
    try {
      if (operation.type === 'TASK_UPDATE') {
        const latest = normalizeTask(await request(`/tasks/${operation.taskId}`));
        const saved = normalizeTask(await request(`/tasks/${operation.taskId}`, { method: 'PUT', body: JSON.stringify(taskUpdateBody(latest, operation.changes)) }));
        const updatedTasks = tasks.map((task) => task.id === saved.id ? saved : task);
        setTasks(updatedTasks);
        await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      } else if (operation.type === 'TASK_DELETE') {
        const latest = normalizeTask(await request(`/tasks/${operation.taskId}`));
        await request(`/tasks/${operation.taskId}`, { method: 'DELETE' });
        const updatedTasks = tasks.filter((task) => task.id !== latest.id && task.parentTaskId !== latest.id);
        setTasks(updatedTasks);
        await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      } else if (operation.type === 'TASK_CREATE') {
        const created = normalizeTask(await request('/tasks', { method: 'POST', body: JSON.stringify(taskCreateBody(operation.data)) }));
        const updatedTasks = [created, ...tasks.filter((task) => task.id !== operation.localId)];
        setTasks(updatedTasks);
        await saveOfflineData(user.id, { user, users, projects, tasks: updatedTasks });
      }
      await persistQueue(queue.filter((item) => item.id !== operationId));
      setOnline(true);
    } catch (error) {
      if (isNetworkFailure(error)) setOnline(false);
      throw error;
    }
  };
  const normalizeTimer = (timer) => timer ? { ...timer, id: String(timer.id), taskId: String(timer.taskId), syncedAt: Date.now() } : null;
  const startTaskTimer = async (taskId) => { const saved = normalizeTimer(await request('/productivity/timer/start', { method: 'POST', body: JSON.stringify({ taskId: Number(taskId) }) })); setActiveTimer(saved); return saved; };
  const pauseTaskTimer = async () => { const saved = normalizeTimer(await request('/productivity/timer/pause', { method: 'POST' })); setActiveTimer(saved); return saved; };
  const resumeTaskTimer = async () => { const saved = normalizeTimer(await request('/productivity/timer/resume', { method: 'POST' })); setActiveTimer(saved); return saved; };
  const stopTaskTimer = async (comment) => { const saved = await request(`/productivity/timer/stop${comment ? `?comment=${encodeURIComponent(comment)}` : ''}`, { method: 'POST' }); setActiveTimer(null); return saved; };
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
    user, users, projects, tasks, history: [], error, loading, busy, refresh, activeTimer, online, offlineQueue, resolveOfflineConflict,
    visibleProjects, visibleTasks, can, canChangeStatus, canManageTask, getUser, getProject,
    login, register, loginAs: noop, logout, addTask, setTaskStatus, setTaskAssignee, setTaskPriority, setTaskRecurrence,
    toggleChecklistItem, addChecklistItem, deleteChecklistItem, deleteTask, updateTask, addProject, updateProject,
    instantiateTaskTemplate, instantiateProjectTemplate,
    setUserRole, createManagedUser, toggleUserActive, myNotifications, reminderDays, setPersonalReminderDays, unreadCount, markRead, markAllRead,
    startTaskTimer, pauseTaskTimer, resumeTaskTimer, stopTaskTimer,
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
