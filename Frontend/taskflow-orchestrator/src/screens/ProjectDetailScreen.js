import React from 'react';
import { ScrollView, View, Text } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors, STATUS_ORDER, STATUS } from '../theme';
import { Card, Badge, ProgressBar, SectionTitle, Button, EmptyState } from '../components/ui';
import TaskCard from '../components/TaskCard';
import { projectProgress } from '../utils/format';

export default function ProjectDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { getProject, visibleTasks, can } = useApp();
  const project = getProject(id);
  if (!project) return <EmptyState text="Projeto não encontrado." />;

  const tasks = visibleTasks.filter((t) => t.projectId === id);
  const pr = projectProgress(tasks);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card style={{ borderTopWidth: 4, borderTopColor: project.color }}>
        <Text style={{ fontSize: 20, fontWeight: '800', color: colors.text }}>{project.name}</Text>
        <Text style={{ color: colors.muted, marginTop: 6 }}>{project.description}</Text>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 14, marginBottom: 6 }}>
          <Text style={{ color: colors.muted, fontSize: 12 }}>Chave: {project.projectKey}</Text>
          <Text style={{ color: colors.muted, fontSize: 12 }}>{Math.round(pr * 100)}% concluído</Text>
        </View>
        <ProgressBar value={pr} color={project.color} />
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: 12 }}>
          {STATUS_ORDER.map((st) => (
            <Badge key={st} label={`${STATUS[st].label}: ${tasks.filter((t) => t.status === st).length}`} color={STATUS[st].color} style={{ marginRight: 6, marginBottom: 6 }} />
          ))}
        </View>
      </Card>

      <Text style={{ color: colors.muted, marginHorizontal: 4, marginBottom: 12 }}>O backend ainda não oferece gerenciamento de membros nem prazo para projetos.</Text>

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
