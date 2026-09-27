import React from 'react';
import { ScrollView, View, Text } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors, STATUS_ORDER, STATUS } from '../theme';
import { Card, Badge, ProgressBar, Avatar, SectionTitle, Button, EmptyState } from '../components/ui';
import TaskCard from '../components/TaskCard';
import { ROLES } from '../utils/permissions';
import { formatDate, projectProgress } from '../utils/format';

export default function ProjectDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { getProject, getUser, visibleTasks, can } = useApp();
  const project = getProject(id);
  if (!project) return <EmptyState text="Projeto não encontrado." />;

  const tasks = visibleTasks.filter((t) => t.projectId === id);
  const pr = projectProgress(tasks);
  const owner = getUser(project.ownerId);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card style={{ borderTopWidth: 4, borderTopColor: project.color }}>
        <Text style={{ fontSize: 20, fontWeight: '800', color: colors.text }}>{project.name}</Text>
        <Text style={{ color: colors.muted, marginTop: 6 }}>{project.description}</Text>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 14, marginBottom: 6 }}>
          <Text style={{ color: colors.muted, fontSize: 12 }}>Prazo: {formatDate(project.deadline)}</Text>
          <Text style={{ color: colors.muted, fontSize: 12 }}>{Math.round(pr * 100)}% concluído</Text>
        </View>
        <ProgressBar value={pr} color={project.color} />
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: 12 }}>
          {STATUS_ORDER.map((st) => (
            <Badge key={st} label={`${STATUS[st].label}: ${tasks.filter((t) => t.status === st).length}`} color={STATUS[st].color} style={{ marginRight: 6, marginBottom: 6 }} />
          ))}
        </View>
      </Card>

      <SectionTitle>Equipe</SectionTitle>
      <Card>
        {project.memberIds.map((mid) => {
          const u = getUser(mid);
          if (!u) return null;
          return (
            <View key={mid} style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 10 }}>
              <Avatar name={u.name} color={ROLES[u.role].color} />
              <View style={{ flex: 1, marginLeft: 10 }}>
                <Text style={{ fontWeight: '600', color: colors.text }}>{u.name}{owner && owner.id === u.id ? ' • responsável' : ''}</Text>
                <Text style={{ color: colors.muted, fontSize: 12 }}>{u.title}</Text>
              </View>
              <Badge label={ROLES[u.role].label} color={ROLES[u.role].color} />
            </View>
          );
        })}
      </Card>

      <SectionTitle>Tarefas ({tasks.length})</SectionTitle>
      {can('createTask') ? (
        <Button title="Nova tarefa neste projeto" icon="add" variant="outline" onPress={() => navigation.navigate('NovaTarefa', { projectId: id })} style={{ marginBottom: 12 }} />
      ) : null}
      {tasks.length === 0 ? <EmptyState text="Nenhuma tarefa neste projeto." /> : null}
      {tasks.map((t) => (
        <TaskCard key={t.id} task={t} compact onPress={() => navigation.navigate('TarefaDetalhe', { id: t.id })} />
      ))}
    </ScrollView>
  );
}
