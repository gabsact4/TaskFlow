import React from 'react';
import { ScrollView, View, Text, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, STATUS, STATUS_ORDER, PRIORITY, PRIORITY_ORDER } from '../theme';
import { Card, Chip, ChipRow, Avatar, SectionTitle, Button, EmptyState } from '../components/ui';
import { formatDate, isOverdue } from '../utils/format';

export default function TaskDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { tasks, getUser, getProject, canManageTask, setTaskStatus, setTaskPriority, deleteTask } = useApp();
  const task = tasks.find((t) => t.id === id);
  if (!task) return <EmptyState text="Tarefa não encontrada." />;

  const assignee = getUser(task.assigneeId);
  const project = getProject(task.projectId);
  const canEdit = canManageTask(task);
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
      </Card>

      <SectionTitle>Status</SectionTitle>
      <ChipRow>
        {STATUS_ORDER.map((s) => (
          <Chip key={s} label={STATUS[s].label} color={STATUS[s].color} active={task.status === s} disabled={!canEdit} onPress={() => setTaskStatus(id, s).catch((e) => Alert.alert('Não foi possível atualizar o status', e.message))} />
        ))}
      </ChipRow>
      {!canEdit ? <Text style={{ color: colors.muted, fontSize: 12, marginTop: 6 }}>Somente quem criou a tarefa ou é proprietário do projeto pode alterá-la.</Text> : null}

      <SectionTitle>Prioridade</SectionTitle>
      <ChipRow>
        {PRIORITY_ORDER.map((p) => (
          <Chip key={p} label={PRIORITY[p].label} color={PRIORITY[p].color} active={task.priority === p} disabled={!canEdit} onPress={() => setTaskPriority(id, p).catch((e) => Alert.alert('Não foi possível atualizar a prioridade', e.message))} />
        ))}
      </ChipRow>

      <Text style={{ color: colors.muted, fontSize: 12, marginTop: 12 }}>Checklist e histórico ainda não estão disponíveis no backend.</Text>

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
