import React, { useState, useMemo } from 'react';
import { View, FlatList, TextInput } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, STATUS, STATUS_ORDER, PRIORITY, PRIORITY_ORDER } from '../theme';
import { Chip, ChipRow, Fab, EmptyState } from '../components/ui';
import TaskCard from '../components/TaskCard';

export default function TasksScreen({ navigation }) {
  const { user, visibleTasks, can } = useApp();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState(null);
  const [priority, setPriority] = useState(null);
  const [onlyMine, setOnlyMine] = useState(false);

  const data = useMemo(
    () =>
      visibleTasks
        .filter((t) => (!status || t.status === status) && (!priority || t.priority === priority))
        .filter((t) => !onlyMine || t.assigneeId === user.id)
        .filter((t) => !q.trim() || t.title.toLowerCase().includes(q.trim().toLowerCase()))
        .sort((a, b) => (a.dueDate > b.dueDate ? 1 : -1)),
    [visibleTasks, status, priority, onlyMine, q, user]
  );

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <View style={{ padding: 16, paddingBottom: 4 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 12, paddingHorizontal: 12, borderWidth: 1, borderColor: colors.border, marginBottom: 10 }}>
          <Ionicons name="search" size={18} color={colors.muted} />
          <TextInput value={q} onChangeText={setQ} placeholder="Buscar tarefas" placeholderTextColor={colors.muted} style={{ flex: 1, paddingVertical: 10, paddingHorizontal: 8, color: colors.text }} />
        </View>
        <ChipRow>
          <Chip label="Minhas" active={onlyMine} onPress={() => setOnlyMine(!onlyMine)} />
          {STATUS_ORDER.map((s) => (
            <Chip key={s} label={STATUS[s].label} color={STATUS[s].color} active={status === s} onPress={() => setStatus(status === s ? null : s)} />
          ))}
        </ChipRow>
        <View style={{ height: 8 }} />
        <ChipRow>
          {PRIORITY_ORDER.map((p) => (
            <Chip key={p} label={PRIORITY[p].label} color={PRIORITY[p].color} active={priority === p} onPress={() => setPriority(priority === p ? null : p)} />
          ))}
        </ChipRow>
      </View>
      <FlatList
        data={data}
        keyExtractor={(t) => t.id}
        contentContainerStyle={{ padding: 16, paddingBottom: 90 }}
        ListEmptyComponent={<EmptyState text="Nenhuma tarefa encontrada com esses filtros." />}
        renderItem={({ item }) => <TaskCard task={item} onPress={() => navigation.navigate('TarefaDetalhe', { id: item.id })} />}
      />
      {can('createTask') ? <Fab onPress={() => navigation.navigate('NovaTarefa')} /> : null}
    </View>
  );
}
