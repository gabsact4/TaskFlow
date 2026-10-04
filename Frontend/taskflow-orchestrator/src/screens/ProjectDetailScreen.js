import React, { useState } from 'react';
import { ScrollView, View, Text, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors, STATUS_ORDER, STATUS } from '../theme';
import { Card, Badge, ProgressBar, SectionTitle, Button, EmptyState, Chip } from '../components/ui';
import TaskCard from '../components/TaskCard';
import { projectProgress } from '../utils/format';

export default function ProjectDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const { getProject, visibleTasks, can, users, updateProject } = useApp();
  const project = getProject(id);
  const [memberIds, setMemberIds] = useState(null);
  const [savingMembers, setSavingMembers] = useState(false);
  if (!project) return <EmptyState text="Projeto não encontrado." />;

  const tasks = visibleTasks.filter((t) => t.projectId === id);
  const pr = projectProgress(tasks);
  const selectedMembers = memberIds ?? project.memberIds.filter((memberId) => memberId !== project.ownerId).map(Number);
  const saveMembers = async () => {
    setSavingMembers(true);
    try { await updateProject(project, { memberIds: selectedMembers }); setMemberIds(null); }
    catch (error) { Alert.alert('Não foi possível salvar a equipe', error.message); }
    finally { setSavingMembers(false); }
  };

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

      <Card>
        <SectionTitle>Equipe do projeto</SectionTitle>
        <Text style={{ color: colors.muted, marginBottom: 10 }}>Marque ou desmarque pessoas para adicionar ou remover do projeto.</Text>
        {can('createProject') ? <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          {users.filter((person) => person.id !== project.ownerId).map((person) => {
            const selected = selectedMembers.includes(Number(person.id));
            return <View key={person.id} style={{ marginRight: 8, marginBottom: 8 }}><Chip label={`${selected ? '✓ ' : ''}${person.name}`} active={selected} onPress={() => setMemberIds((ids) => {
              const current = ids ?? project.memberIds.filter((memberId) => memberId !== project.ownerId).map(Number);
              return selected ? current.filter((memberId) => memberId !== Number(person.id)) : [...current, Number(person.id)];
            })} /></View>;
          })}
          <Button title={savingMembers ? 'Salvando…' : 'Salvar equipe'} onPress={saveMembers} disabled={savingMembers || memberIds === null} />
        </View> : <Text style={{ color: colors.text }}>{project.memberIds.map((memberId) => users.find((person) => person.id === memberId)?.name).filter(Boolean).join(', ') || 'Nenhuma pessoa cadastrada.'}</Text>}
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
