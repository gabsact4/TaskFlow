import React from 'react';
import { ScrollView, View, Text, Alert, TextInput, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, STATUS, STATUS_ORDER, PRIORITY, PRIORITY_ORDER, RECURRENCE, RECURRENCE_ORDER } from '../theme';
import { Card, Chip, ChipRow, Avatar, SectionTitle, Button, EmptyState, ProgressBar, Field } from '../components/ui';
import { formatDate, isOverdue } from '../utils/format';
import DatePickerField from '../components/DatePickerField';
import { request } from '../api';

export default function TaskDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { tasks, users, getUser, getProject, canManageTask, canChangeStatus, setTaskStatus, setTaskAssignee, setTaskPriority, setTaskRecurrence, deleteTask, addChecklistItem, toggleChecklistItem, deleteChecklistItem, addTask } = useApp();
  const [newItem, setNewItem] = React.useState('');
  const [newSubtask, setNewSubtask] = React.useState('');
  const [recurrenceEndDate, setRecurrenceEndDate] = React.useState('');
  const [workLogs, setWorkLogs] = React.useState([]);
  const [workHours, setWorkHours] = React.useState('');
  const [workComment, setWorkComment] = React.useState('');
  const [workLogsError, setWorkLogsError] = React.useState('');
  const [savingWorkLog, setSavingWorkLog] = React.useState(false);
  const task = tasks.find((t) => t.id === id);
  React.useEffect(() => setRecurrenceEndDate(task?.recurrenceEndDate || ''), [task?.id, task?.recurrenceEndDate]);
  React.useEffect(() => {
    let active = true;
    if (!task) return () => { active = false; };
    request(`/tasks/${id}/work-logs`).then((rows) => { if (active) setWorkLogs(rows); }).catch((error) => { if (active) setWorkLogsError(error.message); });
    return () => { active = false; };
  }, [id, !!task]);
  if (!task) return <EmptyState text="Tarefa não encontrada." />;

  const assignee = getUser(task.assigneeId);
  const project = getProject(task.projectId);
  const parentTask = task.parentTaskId ? tasks.find((entry) => entry.id === task.parentTaskId) : null;
  const canEdit = canManageTask(task);
  const canWork = canChangeStatus(task);
  const subtasks = tasks.filter((entry) => entry.parentTaskId === id);
  const checklistDone = task.checklist.filter((item) => item.done).length;
  const totalWorkMinutes = workLogs.reduce((total, entry) => total + entry.durationMinutes, 0);
  const formatWorkedTime = (minutes) => `${Math.floor(minutes / 60)}h${minutes % 60 ? ` ${minutes % 60}min` : ''}`;
  const saveWorkLog = async () => {
    const parsedHours = Number(workHours.trim().replace(',', '.'));
    if (!Number.isFinite(parsedHours) || parsedHours <= 0 || parsedHours > 24) return Alert.alert('Horas inválidas', 'Informe um valor maior que zero e de até 24 horas.');
    const durationMinutes = Math.round(parsedHours * 60);
    if (durationMinutes < 1) return Alert.alert('Horas inválidas', 'O lançamento precisa ter pelo menos um minuto.');
    setSavingWorkLog(true);
    try {
      const saved = await request(`/tasks/${id}/work-logs`, { method: 'POST', body: JSON.stringify({ durationMinutes, comment: workComment.trim() || null }) });
      setWorkLogs((entries) => [saved, ...entries]);
      setWorkHours(''); setWorkComment(''); setWorkLogsError('');
    } catch (error) { Alert.alert('Não foi possível registrar as horas', error.message); }
    finally { setSavingWorkLog(false); }
  };
  const confirmDelete = () =>
    Alert.alert('Excluir tarefa', `Deseja excluir "${task.title}"?`, [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Excluir', style: 'destructive', onPress: () => deleteTask(id).then(() => navigation.goBack()).catch((e) => Alert.alert('Não foi possível excluir a tarefa', e.message)) },
    ]);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Card>
        <Text style={{ fontSize: 20, fontWeight: '800', color: colors.text }}>{task.title}</Text>
        <Text style={{ color: colors.muted, marginTop: 6 }}>{task.description || 'Sem descrição.'}</Text>
        <View style={{ height: 12 }} />
        <Row icon="folder-outline" label="Projeto" value={project ? project.name : '—'} />
        {parentTask ? <Row icon="git-branch-outline" label="Tarefa pai" value={parentTask.title} /> : null}
        <Row icon="calendar-outline" label="Prazo" value={`${formatDate(task.dueDate)}${isOverdue(task) ? ' (atrasada)' : ''}`} danger={isOverdue(task)} />
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Ionicons name="person-outline" size={16} color={colors.muted} />
          <Text style={{ marginLeft: 8, color: colors.muted, width: 70 }}>Responsável</Text>
          {assignee || task.assigneeName ? (
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Avatar name={assignee?.name || task.assigneeName} size={24} />
              <Text style={{ marginLeft: 6, color: colors.text, fontWeight: '600' }}>{assignee?.name || task.assigneeName}</Text>
            </View>
          ) : <Text>—</Text>}
        </View>
        {canEdit && project ? <ChipRow style={{ marginTop: 8 }}>
          <Chip label="Sem responsável" active={!task.assigneeId} onPress={() => setTaskAssignee(id, null).catch((e) => Alert.alert('Não foi possível alterar o responsável', e.message))} />
          {users.filter((person) => project.memberIds.includes(person.id)).map((person) => <Chip key={person.id} label={person.name} active={task.assigneeId === person.id} onPress={() => setTaskAssignee(id, person.id).catch((e) => Alert.alert('Não foi possível alterar o responsável', e.message))} />)}
        </ChipRow> : null}
      </Card>

      <SectionTitle right={<Text style={{ color: colors.primary, fontWeight: '800' }}>{formatWorkedTime(totalWorkMinutes)}</Text>}>Horas trabalhadas</SectionTitle>
      <Card>
        <Text style={{ color: colors.muted, marginBottom: 10 }}>Total registrado: {formatWorkedTime(totalWorkMinutes)}</Text>
        {canWork || canEdit ? <>
          <Field label="Adicionar horas" value={workHours} onChangeText={setWorkHours} keyboardType="decimal-pad" placeholder="Ex.: 1,5" />
          <Field label="Observação (opcional)" value={workComment} onChangeText={setWorkComment} placeholder="O que foi feito?" maxLength={500} />
          <Button title={savingWorkLog ? 'Registrando…' : 'Registrar horas'} icon="time-outline" onPress={saveWorkLog} disabled={savingWorkLog || !workHours.trim()} />
        </> : null}
        {workLogsError ? <Text style={{ color: colors.danger, marginTop: 8 }}>{workLogsError}</Text> : null}
        {workLogs.map((entry) => <View key={entry.id} style={{ borderTopWidth: 1, borderTopColor: colors.border, paddingTop: 10, marginTop: 10 }}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Text style={{ color: colors.text, fontWeight: '700' }}>{formatWorkedTime(entry.durationMinutes)}</Text>
            <Text style={{ color: colors.muted, fontSize: 12 }}>{new Date(entry.createdAt).toLocaleString('pt-BR')}</Text>
          </View>
          <Text style={{ color: colors.muted, marginTop: 3 }}>{entry.userName}{entry.comment ? ` · ${entry.comment}` : ''}</Text>
        </View>)}
        {!workLogs.length && !workLogsError ? <Text style={{ color: colors.muted }}>Nenhum lançamento de horas ainda.</Text> : null}
      </Card>

      <SectionTitle>Status</SectionTitle>
      <ChipRow>
        {STATUS_ORDER.map((s) => (
          <Chip key={s} label={STATUS[s].label} color={STATUS[s].color} active={task.status === s} disabled={!canWork} onPress={() => setTaskStatus(id, s).catch((e) => Alert.alert('Não foi possível atualizar o status', e.message))} />
        ))}
      </ChipRow>
      {!canWork ? <Text style={{ color: colors.muted, fontSize: 12, marginTop: 6 }}>Dev só pode atualizar tarefas atribuídas a si; PO e Master gerenciam as tarefas conforme seu acesso.</Text> : null}

      <SectionTitle>Prioridade</SectionTitle>
      <ChipRow>
        {PRIORITY_ORDER.map((p) => (
          <Chip key={p} label={PRIORITY[p].label} color={PRIORITY[p].color} active={task.priority === p} disabled={!canEdit} onPress={() => setTaskPriority(id, p).catch((e) => Alert.alert('Não foi possível atualizar a prioridade', e.message))} />
        ))}
      </ChipRow>

      <SectionTitle>Repetição</SectionTitle>
      <ChipRow>
        {RECURRENCE_ORDER.map((value) => (
          <Chip key={value} label={RECURRENCE[value].label} active={task.recurrence === value} disabled={!canEdit} onPress={() => setTaskRecurrence(id, value, value === 'none' ? null : recurrenceEndDate || null).catch((e) => Alert.alert('Não foi possível atualizar a repetição', e.message))} />
        ))}
      </ChipRow>
      {task.recurrence !== 'none' ? (
        <Card>
          <Text style={{ color: colors.muted, marginBottom: 8 }}>{task.nextOccurrenceDate ? `Próxima ocorrência: ${formatDate(task.nextOccurrenceDate)}` : 'Próxima ocorrência será agendada automaticamente.'}</Text>
          {canEdit ? <>
            <DatePickerField label="Repetir até (opcional)" value={recurrenceEndDate} onChange={setRecurrenceEndDate} placeholder="Sem data final" />
            <Button title="Salvar data final" variant="outline" onPress={() => setTaskRecurrence(id, task.recurrence, recurrenceEndDate || null).catch((e) => Alert.alert('Não foi possível salvar', e.message))} />
          </> : null}
        </Card>
      ) : null}

      <SectionTitle right={<Text style={{ color: colors.muted }}>{checklistDone}/{task.checklist.length}</Text>}>Checklist</SectionTitle>
      <Card>
        {task.checklist.length > 0 ? <ProgressBar value={checklistDone / task.checklist.length} color={colors.success} style={{ marginBottom: 10 }} /> : null}
        {task.checklist.map((item) => (
          <View key={item.id} style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 7 }}>
            <TouchableOpacity disabled={!canWork} onPress={() => toggleChecklistItem(id, item.id).catch((e) => Alert.alert('Não foi possível atualizar o item', e.message))} style={{ flexDirection: 'row', alignItems: 'center', flex: 1 }}>
              <Ionicons name={item.done ? 'checkbox' : 'square-outline'} size={22} color={item.done ? colors.success : colors.muted} />
              <Text style={{ marginLeft: 9, color: item.done ? colors.muted : colors.text, textDecorationLine: item.done ? 'line-through' : 'none', flex: 1 }}>{item.text}</Text>
            </TouchableOpacity>
            {canEdit ? <TouchableOpacity onPress={() => deleteChecklistItem(id, item.id).catch((e) => Alert.alert('Não foi possível remover o item', e.message))} hitSlop={8}>
              <Ionicons name="trash-outline" size={18} color={colors.muted} />
            </TouchableOpacity> : null}
          </View>
        ))}
        {task.checklist.length === 0 ? <Text style={{ color: colors.muted }}>Adicione itens para acompanhar esta tarefa.</Text> : null}
        {canEdit ? (
          <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 10 }}>
            <TextInput value={newItem} onChangeText={setNewItem} placeholder="Novo item do checklist" placeholderTextColor={colors.muted} maxLength={300} onSubmitEditing={() => {
              const text = newItem.trim(); if (!text) return;
              addChecklistItem(id, text).then(() => setNewItem('')).catch((e) => Alert.alert('Não foi possível adicionar o item', e.message));
            }} style={{ flex: 1, borderWidth: 1, borderColor: colors.border, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 9, color: colors.text }} />
            <TouchableOpacity onPress={() => {
              const text = newItem.trim(); if (!text) return;
              addChecklistItem(id, text).then(() => setNewItem('')).catch((e) => Alert.alert('Não foi possível adicionar o item', e.message));
            }} style={{ marginLeft: 8, backgroundColor: colors.primary, borderRadius: 10, padding: 10 }}>
              <Ionicons name="add" size={20} color="#fff" />
            </TouchableOpacity>
          </View>
        ) : null}
      </Card>

      <SectionTitle>Subtarefas ({subtasks.length})</SectionTitle>
      {subtasks.map((subtask) => (
            <Card key={subtask.id} onPress={() => navigation.push('TarefaDetalhe', { id: subtask.id })} style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Ionicons name={subtask.status === 'done' ? 'checkmark-circle' : 'radio-button-off-outline'} size={20} color={subtask.status === 'done' ? colors.success : colors.muted} />
              <Text style={{ color: colors.text, fontWeight: '600', marginLeft: 10, flex: 1 }}>{subtask.title}</Text>
              <Text style={{ color: colors.muted, fontSize: 12 }}>{PRIORITY[subtask.priority]?.label}</Text>
            </Card>
          ))}
      {canEdit ? (
            <Card>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <TextInput value={newSubtask} onChangeText={setNewSubtask} placeholder="Título da subtarefa" placeholderTextColor={colors.muted} maxLength={180} style={{ flex: 1, borderWidth: 1, borderColor: colors.border, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 9, color: colors.text }} />
                <TouchableOpacity onPress={() => {
                  const title = newSubtask.trim(); if (!title) return;
                  addTask({ title, description: '', projectId: task.projectId, parentTaskId: task.id, priority: task.priority, dueDate: null, assigneeId: null })
                    .then(() => setNewSubtask('')).catch((e) => Alert.alert('Não foi possível criar a subtarefa', e.message));
                }} style={{ marginLeft: 8, backgroundColor: colors.primary, borderRadius: 10, padding: 10 }}>
                  <Ionicons name="add" size={20} color="#fff" />
                </TouchableOpacity>
              </View>
            </Card>
      ) : null}

      {canEdit ? <Button title="Excluir tarefa" variant="danger" icon="trash-outline" onPress={confirmDelete} style={{ marginTop: 8 }} /> : null}
    </ScrollView>
  );
}

const Row = ({ icon, label, value, danger }) => (
  <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
    <Ionicons name={icon} size={16} color={colors.muted} />
    <Text style={{ marginLeft: 8, color: colors.muted, width: 70 }}>{label}</Text>
    <Text style={{ color: danger ? colors.danger : colors.text, fontWeight: '600', flex: 1 }}>{value}</Text>
  </View>
);
