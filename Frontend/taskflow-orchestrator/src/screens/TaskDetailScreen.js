import React, { useState } from 'react';
import { ScrollView, View, Text, TouchableOpacity, TextInput, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, STATUS, STATUS_ORDER, PRIORITY, PRIORITY_ORDER } from '../theme';
import { Card, Badge, Chip, ChipRow, Avatar, SectionTitle, ProgressBar, Button, EmptyState } from '../components/ui';
import { formatDate, formatDateTime, isOverdue } from '../utils/format';

export default function TaskDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { tasks, getUser, getProject, can, canChangeStatus, setTaskStatus, setTaskPriority, toggleChecklistItem, addChecklistItem, deleteTask, history } = useApp();
  const [newItem, setNewItem] = useState('');
  const task = tasks.find((t) => t.id === id);
  if (!task) return <EmptyState text="Tarefa não encontrada." />;

  const assignee = getUser(task.assigneeId);
  const project = getProject(task.projectId);
  const canStatus = canChangeStatus(task);
  const canEdit = can('editTask');
  const done = task.checklist.filter((c) => c.done).length;
  const taskHistory = history.filter((h) => h.taskId === id);

  const confirmDelete = () =>
    Alert.alert('Excluir tarefa', `Deseja excluir "${task.title}"?`, [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Excluir', style: 'destructive', onPress: () => { deleteTask(id); navigation.goBack(); } },
    ]);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Card>
        <Text style={{ fontSize: 20, fontWeight: '800', color: colors.text }}>{task.title}</Text>
        <Text style={{ color: colors.muted, marginTop: 6 }}>{task.description || 'Sem descrição.'}</Text>
        <View style={{ height: 12 }} />
        <Row icon="folder-outline" label="Projeto" value={project ? project.name : '—'} />
        <Row icon="calendar-outline" label="Prazo" value={`${formatDate(task.dueDate)}${isOverdue(task) ? ' (atrasada)' : ''}`} danger={isOverdue(task)} />
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Ionicons name="person-outline" size={16} color={colors.muted} />
          <Text style={{ marginLeft: 8, color: colors.muted, width: 70 }}>Responsável</Text>
          {assignee ? (
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Avatar name={assignee.name} size={24} />
              <Text style={{ marginLeft: 6, color: colors.text, fontWeight: '600' }}>{assignee.name}</Text>
            </View>
          ) : <Text>—</Text>}
        </View>
      </Card>

      <SectionTitle>Status</SectionTitle>
      <ChipRow>
        {STATUS_ORDER.map((s) => (
          <Chip key={s} label={STATUS[s].label} color={STATUS[s].color} active={task.status === s} disabled={!canStatus} onPress={() => setTaskStatus(id, s)} />
        ))}
      </ChipRow>
      {!canStatus ? <Text style={{ color: colors.muted, fontSize: 12, marginTop: 6 }}>Seu nível de acesso não permite alterar o status desta tarefa.</Text> : null}

      <SectionTitle>Prioridade</SectionTitle>
      <ChipRow>
        {PRIORITY_ORDER.map((p) => (
          <Chip key={p} label={PRIORITY[p].label} color={PRIORITY[p].color} active={task.priority === p} disabled={!canEdit} onPress={() => setTaskPriority(id, p)} />
        ))}
      </ChipRow>

      <SectionTitle right={<Text style={{ color: colors.muted }}>{done}/{task.checklist.length}</Text>}>Checklist</SectionTitle>
      <Card>
        {task.checklist.length > 0 ? <ProgressBar value={done / task.checklist.length} color={colors.success} style={{ marginBottom: 12 }} /> : null}
        {task.checklist.length === 0 ? <Text style={{ color: colors.muted }}>Nenhum item no checklist.</Text> : null}
        {task.checklist.map((c) => (
          <TouchableOpacity key={c.id} disabled={!canStatus} onPress={() => toggleChecklistItem(id, c.id)} style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 8 }}>
            <Ionicons name={c.done ? 'checkbox' : 'square-outline'} size={22} color={c.done ? colors.success : colors.muted} />
            <Text style={{ marginLeft: 10, color: c.done ? colors.muted : colors.text, textDecorationLine: c.done ? 'line-through' : 'none', flex: 1 }}>{c.text}</Text>
          </TouchableOpacity>
        ))}
        {canEdit ? (
          <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
            <TextInput value={newItem} onChangeText={setNewItem} placeholder="Novo item" placeholderTextColor={colors.muted} style={{ flex: 1, borderWidth: 1, borderColor: colors.border, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 8, color: colors.text }} />
            <TouchableOpacity onPress={() => { addChecklistItem(id, newItem); setNewItem(''); }} style={{ marginLeft: 8, backgroundColor: colors.primary, borderRadius: 10, padding: 10 }}>
              <Ionicons name="add" size={20} color="#fff" />
            </TouchableOpacity>
          </View>
        ) : null}
      </Card>

      {can('viewHistory') ? (
        <>
          <SectionTitle>Histórico da tarefa</SectionTitle>
          <Card>
            {taskHistory.length === 0 ? <Text style={{ color: colors.muted }}>Sem alterações registradas.</Text> : null}
            {taskHistory.map((h) => {
              const u = getUser(h.userId);
              return (
                <View key={h.id} style={{ marginBottom: 10 }}>
                  <Text style={{ color: colors.text }}><Text style={{ fontWeight: '700' }}>{u ? u.name : 'Sistema'}</Text> {h.text}</Text>
                  <Text style={{ color: colors.muted, fontSize: 11 }}>{formatDateTime(h.date)}</Text>
                </View>
              );
            })}
          </Card>
        </>
      ) : null}

      {can('deleteTask') ? <Button title="Excluir tarefa" variant="danger" icon="trash-outline" onPress={confirmDelete} style={{ marginTop: 8 }} /> : null}
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
