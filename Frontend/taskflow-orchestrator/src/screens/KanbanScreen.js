import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, STATUS, STATUS_ORDER, PRIORITY } from '../theme';
import { Chip, ChipRow, Avatar } from '../components/ui';
import { formatDate, isOverdue } from '../utils/format';

export default function KanbanScreen({ navigation }) {
  const { visibleTasks, visibleProjects, getUser, canChangeStatus, setTaskStatus } = useApp();
  const [projectId, setProjectId] = useState(null);
  const tasks = visibleTasks.filter((t) => !projectId || t.projectId === projectId);

  const move = (task, dir) => {
    const i = STATUS_ORDER.indexOf(task.status) + dir;
    if (i >= 0 && i < STATUS_ORDER.length) setTaskStatus(task.id, STATUS_ORDER[i]);
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <View style={{ padding: 16, paddingBottom: 8 }}>
        <ChipRow>
          <Chip label="Todos" active={!projectId} onPress={() => setProjectId(null)} />
          {visibleProjects.map((p) => (
            <Chip key={p.id} label={p.name} color={p.color} active={projectId === p.id} onPress={() => setProjectId(projectId === p.id ? null : p.id)} />
          ))}
        </ChipRow>
      </View>
      <ScrollView horizontal style={{ flex: 1 }} contentContainerStyle={{ padding: 12 }} showsHorizontalScrollIndicator={false}>
        {STATUS_ORDER.map((st) => {
          const col = tasks.filter((t) => t.status === st);
          return (
            <View key={st} style={{ width: 280, marginRight: 12, backgroundColor: '#E5E7EB', borderRadius: 14, padding: 10 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 10 }}>
                <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: STATUS[st].color, marginRight: 8 }} />
                <Text style={{ fontWeight: '800', color: colors.text, flex: 1 }}>{STATUS[st].label}</Text>
                <Text style={{ color: colors.muted, fontWeight: '700' }}>{col.length}</Text>
              </View>
              <ScrollView style={{ flex: 1 }} nestedScrollEnabled showsVerticalScrollIndicator={false}>
                {col.length === 0 ? <Text style={{ color: colors.muted, textAlign: 'center', padding: 16 }}>Vazio</Text> : null}
                {col.map((t) => {
                  const u = getUser(t.assigneeId);
                  const allowed = canChangeStatus(t);
                  const idx = STATUS_ORDER.indexOf(t.status);
                  return (
                    <TouchableOpacity key={t.id} activeOpacity={0.85} onPress={() => navigation.navigate('TarefaDetalhe', { id: t.id })} style={{ backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8, borderLeftWidth: 4, borderLeftColor: PRIORITY[t.priority].color }}>
                      <Text style={{ fontWeight: '700', color: colors.text }} numberOfLines={2}>{t.title}</Text>
                      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                        <Text style={{ fontSize: 12, color: isOverdue(t) ? colors.danger : colors.muted }}>{formatDate(t.dueDate)}</Text>
                        {u ? <Avatar name={u.name} size={24} /> : null}
                      </View>
                      {allowed ? (
                        <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 }}>
                          <TouchableOpacity disabled={idx === 0} onPress={() => move(t, -1)} style={{ opacity: idx === 0 ? 0.25 : 1, padding: 4 }}>
                            <Ionicons name="arrow-back-circle" size={26} color={colors.primary} />
                          </TouchableOpacity>
                          <TouchableOpacity disabled={idx === STATUS_ORDER.length - 1} onPress={() => move(t, 1)} style={{ opacity: idx === STATUS_ORDER.length - 1 ? 0.25 : 1, padding: 4 }}>
                            <Ionicons name="arrow-forward-circle" size={26} color={colors.primary} />
                          </TouchableOpacity>
                        </View>
                      ) : null}
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>
            </View>
          );
        })}
      </ScrollView>
    </View>
  );
}
