import React, { createContext, useContext, useMemo, useState, useCallback } from 'react';
import { USERS, PROJECTS, TASKS, NOTIFICATIONS, HISTORY } from '../data/mock';
import { can as roleCan } from '../utils/permissions';
import { STATUS, PRIORITY } from '../theme';

const Ctx = createContext(null);
export const useApp = () => useContext(Ctx);

let seq = 1000;
const nid = (p) => `${p}${++seq}`;
const nowISO = () => new Date().toISOString();

export function AppProvider({ children }) {
  const [currentId, setCurrentId] = useState(null);
  const [users, setUsers] = useState(USERS);
  const [projects, setProjects] = useState(PROJECTS);
  const [tasks, setTasks] = useState(TASKS);
  const [notifications, setNotifications] = useState(NOTIFICATIONS);
  const [history, setHistory] = useState(HISTORY);

  const user = users.find((u) => u.id === currentId) || null;
  const can = useCallback((action) => !!user && roleCan(user.role, action), [user]);

  const getUser = useCallback((id) => users.find((u) => u.id === id), [users]);
  const getProject = useCallback((id) => projects.find((p) => p.id === id), [projects]);

  // ---------- Auth ----------
  const login = (email, password) => {
    const u = users.find((x) => x.email.toLowerCase() === email.trim().toLowerCase());
    if (!u) return { ok: false, error: 'Usuário não encontrado.' };
    if (!u.active) return { ok: false, error: 'Usuário desativado. Fale com um administrador.' };
    if (u.password !== password) return { ok: false, error: 'Senha incorreta.' };
    setCurrentId(u.id);
    return { ok: true };
  };
  const loginAs = (id) => setCurrentId(id);
  const logout = () => setCurrentId(null);

  // ---------- Helpers ----------
  const log = (text, extra = {}) =>
    setHistory((h) => [{ id: nid('h'), userId: currentId, text, date: nowISO(), ...extra }, ...h]);

  const pushNotif = (userId, title, body, type = 'task') =>
    setNotifications((n) => [{ id: nid('n'), userId, type, title, body, date: nowISO(), read: false }, ...n]);

  // ---------- Visibilidade por nível ----------
  const visibleProjects = useMemo(() => {
    if (!user) return [];
    if (roleCan(user.role, 'viewAllProjects')) return projects;
    return projects.filter((p) => p.memberIds.includes(user.id));
  }, [user, projects]);

  const visibleTasks = useMemo(() => {
    const ids = new Set(visibleProjects.map((p) => p.id));
    return tasks.filter((t) => ids.has(t.projectId));
  }, [tasks, visibleProjects]);

  const canChangeStatus = (task) =>
    can('changeStatusAny') || (can('changeStatusOwn') && task.assigneeId === user?.id);

  // ---------- Tarefas ----------
  const addTask = (data) => {
    const task = { id: nid('t'), status: 'todo', checklist: [], createdAt: nowISO(), ...data };
    setTasks((t) => [task, ...t]);
    log(`criou a tarefa "${task.title}"`, { taskId: task.id });
    if (task.assigneeId && task.assigneeId !== currentId) {
      pushNotif(task.assigneeId, 'Nova tarefa atribuída', `"${task.title}" foi atribuída a você.`);
    }
    return task;
  };

  const setTaskStatus = (id, status) => {
    const task = tasks.find((t) => t.id === id);
    if (!task || task.status === status) return;
    setTasks((all) => all.map((t) => (t.id === id ? { ...t, status } : t)));
    log(`moveu "${task.title}" de ${STATUS[task.status].label} para ${STATUS[status].label}`, { taskId: id });
    if (status === 'review' && task.assigneeId === currentId) {
      const project = projects.find((p) => p.id === task.projectId);
      if (project && project.ownerId !== currentId) {
        pushNotif(project.ownerId, 'Enviada para revisão', `"${task.title}" aguarda revisão.`);
      }
    }
  };

  const setTaskPriority = (id, priority) => {
    const task = tasks.find((t) => t.id === id);
    if (!task || task.priority === priority) return;
    setTasks((all) => all.map((t) => (t.id === id ? { ...t, priority } : t)));
    log(`alterou a prioridade de "${task.title}" para ${PRIORITY[priority].label}`, { taskId: id });
  };

  const toggleChecklistItem = (taskId, itemId) => {
    const task = tasks.find((t) => t.id === taskId);
    if (!task) return;
    const item = task.checklist.find((c) => c.id === itemId);
    setTasks((all) =>
      all.map((t) =>
        t.id === taskId
          ? { ...t, checklist: t.checklist.map((c) => (c.id === itemId ? { ...c, done: !c.done } : c)) }
          : t
      )
    );
    log(`${item.done ? 'desmarcou' : 'marcou'} "${item.text}" em "${task.title}"`, { taskId });
  };

  const addChecklistItem = (taskId, text) => {
    const task = tasks.find((t) => t.id === taskId);
    if (!task || !text.trim()) return;
    setTasks((all) =>
      all.map((t) =>
        t.id === taskId ? { ...t, checklist: [...t.checklist, { id: nid('c'), text: text.trim(), done: false }] } : t
      )
    );
    log(`adicionou "${text.trim()}" ao checklist de "${task.title}"`, { taskId });
  };

  const deleteTask = (id) => {
    const task = tasks.find((t) => t.id === id);
    setTasks((all) => all.filter((t) => t.id !== id));
    if (task) log(`excluiu a tarefa "${task.title}"`);
  };

  // ---------- Projetos ----------
  const addProject = (data) => {
    const project = { id: nid('p'), ownerId: currentId, color: '#4F46E5', ...data };
    setProjects((p) => [project, ...p]);
    log(`criou o projeto "${project.name}"`, { projectId: project.id });
    return project;
  };

  // ---------- Usuários ----------
  const setUserRole = (id, role) => {
    const target = users.find((u) => u.id === id);
    setUsers((all) => all.map((u) => (u.id === id ? { ...u, role } : u)));
    if (target) log(`alterou o nível de acesso de ${target.name}`);
  };

  const toggleUserActive = (id) => {
    const target = users.find((u) => u.id === id);
    setUsers((all) => all.map((u) => (u.id === id ? { ...u, active: !u.active } : u)));
    if (target) log(`${target.active ? 'desativou' : 'ativou'} o usuário ${target.name}`);
  };

  // ---------- Notificações ----------
  const myNotifications = useMemo(
    () => notifications.filter((n) => n.userId === currentId).sort((a, b) => (a.date < b.date ? 1 : -1)),
    [notifications, currentId]
  );
  const unreadCount = myNotifications.filter((n) => !n.read).length;
  const markRead = (id) => setNotifications((all) => all.map((n) => (n.id === id ? { ...n, read: true } : n)));
  const markAllRead = () =>
    setNotifications((all) => all.map((n) => (n.userId === currentId ? { ...n, read: true } : n)));

  const value = {
    user, users, projects, tasks, history,
    visibleProjects, visibleTasks,
    can, canChangeStatus, getUser, getProject,
    login, loginAs, logout,
    addTask, setTaskStatus, setTaskPriority, toggleChecklistItem, addChecklistItem, deleteTask,
    addProject, setUserRole, toggleUserActive,
    myNotifications, unreadCount, markRead, markAllRead,
  };

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
