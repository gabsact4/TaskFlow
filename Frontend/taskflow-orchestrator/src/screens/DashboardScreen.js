import React from 'react';
import { ScrollView, View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { ROLES } from '../utils/permissions';
import { colors } from '../theme';
import { Card, Badge, ProgressBar, SectionTitle, Button } from '../components/ui';
import TaskCard from '../components/TaskCard';
import { isOverdue, projectProgress } from '../utils/format';

export default function DashboardScreen({ navigation }) {
  const { user, can, visibleTasks, visibleProjects } = useApp();
  const role = ROLES[user.role];
  const mine = visibleTasks
    .filter((t) => t.assigneeId === user.id && t.status !== 'done')
    .sort((a, b) => (a.dueDate > b.dueDate ? 1 : -1));

  const stats = [
    { label: 'Tarefas', value: visibleTasks.length, icon: 'list', color: colors.primary },
    { label: 'Em andamento', value: visibleTasks.filter((t) => t.status === 'doing').length, icon: 'play-circle', color: colors.info },
    { label: 'Concluídas', value: visibleTasks.filter((t) => t.status === 'done').length, icon: 'checkmark-circle', color: colors.success },
    { label: 'Atrasadas', value: visibleTasks.filter(isOverdue).length, icon: 'alert-circle', color: colors.danger },
  ];

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 20, paddingTop: 24 }}>
      <View style={{ marginBottom: 18 }}>
        <Text style={{ fontSize: 28, fontWeight: '800', color: colors.text, letterSpacing: -0.4 }}>Olá, {user.name.split(' ')[0]} 👋</Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8, marginBottom: 4 }}>
          <Badge label={role.label} color={role.color} />
          <Text style={{ color: colors.muted, marginLeft: 8, fontSize: 12, fontWeight: '600' }}>{user.title}</Text>
        </View>
      </View>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginBottom: 6 }}>
        {stats.map((st) => (
          <Card key={st.label} style={{ width: '48.5%' }}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
              <Ionicons name={st.icon} size={22} color={st.color} />
              <Text style={{ color: colors.muted, fontSize: 12, fontWeight: '600' }}>{st.label}</Text>
            </View>
            <Text style={{ fontSize: 26, fontWeight: '800', color: colors.text, marginTop: 10 }}>{st.value}</Text>
          </Card>
        ))}
      </View>

      {can('createTask') ? (
        <Button title="Nova tarefa" icon="add-circle-outline" onPress={() => navigation.navigate('NovaTarefa')} style={{ marginBottom: 8 }} />
      ) : null}

      <SectionTitle right={<Text style={{ color: colors.primary, fontWeight: '700' }} onPress={() => navigation.navigate('Tarefas')}>Ver todas</Text>}>
        Minhas próximas tarefas
      </SectionTitle>
      {mine.length === 0 ? (
        <Card><Text style={{ color: colors.muted }}>Você não tem tarefas pendentes atribuídas.</Text></Card>
      ) : (
        mine.slice(0, 3).map((t) => (
          <TaskCard key={t.id} task={t} onPress={() => navigation.navigate('TarefaDetalhe', { id: t.id })} />
        ))
      )}

      <SectionTitle right={<Text style={{ color: colors.primary, fontWeight: '700' }} onPress={() => navigation.navigate('Projetos')}>Ver todos</Text>}>
        Progresso dos projetos
      </SectionTitle>
      {visibleProjects.map((p) => {
        const pr = projectProgress(visibleTasks.filter((t) => t.projectId === p.id));
        return (
          <Card key={p.id} onPress={() => navigation.navigate('ProjetoDetalhe', { id: p.id })}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10, alignItems: 'center' }}>
              <Text style={{ fontWeight: '700', color: colors.text, flex: 1 }}>{p.name}</Text>
              <Text style={{ color: colors.muted, fontWeight: '700' }}>{Math.round(pr * 100)}%</Text>
            </View>
            <ProgressBar value={pr} color={p.color} />
          </Card>
        );
      })}
    </ScrollView>
  );
}
