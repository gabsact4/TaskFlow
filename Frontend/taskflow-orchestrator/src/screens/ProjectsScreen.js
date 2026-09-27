import React from 'react';
import { View, Text, FlatList } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, ProgressBar, Avatar, Fab, EmptyState } from '../components/ui';
import { formatDate, projectProgress } from '../utils/format';

export default function ProjectsScreen({ navigation }) {
  const { visibleProjects, visibleTasks, getUser, can } = useApp();

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <FlatList
        data={visibleProjects}
        keyExtractor={(p) => p.id}
        contentContainerStyle={{ padding: 16, paddingBottom: 90 }}
        ListEmptyComponent={<EmptyState text="Você ainda não participa de nenhum projeto." />}
        renderItem={({ item: p }) => {
          const tasks = visibleTasks.filter((t) => t.projectId === p.id);
          const pr = projectProgress(tasks);
          return (
            <Card onPress={() => navigation.navigate('ProjetoDetalhe', { id: p.id })} style={{ borderTopWidth: 4, borderTopColor: p.color }}>
              <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{p.name}</Text>
              <Text style={{ color: colors.muted, marginTop: 4 }} numberOfLines={2}>{p.description}</Text>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 12, marginBottom: 6 }}>
                <Text style={{ color: colors.muted, fontSize: 12 }}>{tasks.length} tarefas</Text>
                <Text style={{ color: colors.muted, fontSize: 12 }}>{Math.round(pr * 100)}%</Text>
              </View>
              <ProgressBar value={pr} color={p.color} />
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
                <View style={{ flexDirection: 'row' }}>
                  {p.memberIds.slice(0, 4).map((id, i) => {
                    const u = getUser(id);
                    return u ? (
                      <View key={id} style={{ marginLeft: i === 0 ? 0 : -8, borderWidth: 2, borderColor: '#fff', borderRadius: 20 }}>
                        <Avatar name={u.name} size={28} />
                      </View>
                    ) : null;
                  })}
                </View>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <Ionicons name="flag-outline" size={14} color={colors.muted} />
                  <Text style={{ color: colors.muted, fontSize: 12, marginLeft: 4 }}>{formatDate(p.deadline)}</Text>
                </View>
              </View>
            </Card>
          );
        }}
      />
      {can('createProject') ? <Fab onPress={() => navigation.navigate('NovoProjeto')} /> : null}
    </View>
  );
}
