import React, { useState, useMemo } from 'react';
import { ScrollView, View, Text, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors, PRIORITY } from '../theme';
import { SectionTitle, EmptyState } from '../components/ui';
import TaskCard from '../components/TaskCard';
import { todayKey } from '../utils/format';

const MONTHS = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
const WEEK = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];

export default function CalendarScreen({ navigation }) {
  const { visibleTasks } = useApp();
  const [cursor, setCursor] = useState(() => { const n = new Date(); return new Date(n.getFullYear(), n.getMonth(), 1); });
  const [selected, setSelected] = useState(todayKey());

  const year = cursor.getFullYear();
  const month = cursor.getMonth();
  const first = new Date(year, month, 1).getDay();
  const total = new Date(year, month + 1, 0).getDate();
  const cells = [...Array(first).fill(null), ...Array.from({ length: total }, (_, i) => i + 1)];
  while (cells.length % 7 !== 0) cells.push(null);

  const key = (day) => `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  const byDate = useMemo(() => {
    const m = {};
    visibleTasks.forEach((t) => { if (t.dueDate) (m[t.dueDate] = m[t.dueDate] || []).push(t); });
    return m;
  }, [visibleTasks]);

  const dayTasks = byDate[selected] || [];
  const change = (n) => setCursor(new Date(year, month + n, 1));

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <View style={{ backgroundColor: '#fff', borderRadius: 14, padding: 12 }}>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <TouchableOpacity onPress={() => change(-1)} style={{ padding: 6 }}><Ionicons name="chevron-back" size={22} color={colors.text} /></TouchableOpacity>
          <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{MONTHS[month]} {year}</Text>
          <TouchableOpacity onPress={() => change(1)} style={{ padding: 6 }}><Ionicons name="chevron-forward" size={22} color={colors.text} /></TouchableOpacity>
        </View>
        <View style={{ flexDirection: 'row' }}>
          {WEEK.map((w, i) => (
            <Text key={i} style={{ width: '14.285%', textAlign: 'center', color: colors.muted, fontWeight: '700', fontSize: 12, marginBottom: 6 }}>{w}</Text>
          ))}
        </View>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          {cells.map((day, i) => {
            if (!day) return <View key={i} style={{ width: '14.285%', height: 46 }} />;
            const k = key(day);
            const list = byDate[k] || [];
            const isSel = k === selected;
            const isToday = k === todayKey();
            return (
              <TouchableOpacity key={i} onPress={() => setSelected(k)} style={{ width: '14.285%', height: 46, alignItems: 'center', justifyContent: 'center' }}>
                <View style={{ width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center', backgroundColor: isSel ? colors.primary : 'transparent', borderWidth: isToday && !isSel ? 1.5 : 0, borderColor: colors.primary }}>
                  <Text style={{ color: isSel ? '#fff' : colors.text, fontWeight: isToday ? '800' : '500' }}>{day}</Text>
                </View>
                <View style={{ flexDirection: 'row', height: 5, marginTop: 1 }}>
                  {list.slice(0, 3).map((t) => (
                    <View key={t.id} style={{ width: 5, height: 5, borderRadius: 3, marginHorizontal: 1, backgroundColor: PRIORITY[t.priority].color }} />
                  ))}
                </View>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

      <SectionTitle>Tarefas do dia {selected.split('-').reverse().join('/')}</SectionTitle>
      {dayTasks.length === 0 ? <EmptyState icon="calendar-outline" text="Nenhuma tarefa com prazo neste dia." /> : null}
      {dayTasks.map((t) => (
        <TaskCard key={t.id} task={t} onPress={() => navigation.navigate('TarefaDetalhe', { id: t.id })} />
      ))}
    </ScrollView>
  );
}
