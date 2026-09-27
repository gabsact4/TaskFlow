import React, { useState } from 'react';
import { ScrollView, Text, View, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors, PRIORITY, PRIORITY_ORDER } from '../theme';
import { Field, Button, Chip } from '../components/ui';

export default function NewTaskScreen({ route, navigation }) {
  const { visibleProjects, users, addTask } = useApp();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [projectId, setProjectId] = useState(route.params?.projectId || (visibleProjects[0] && visibleProjects[0].id));
  const project = visibleProjects.find((p) => p.id === projectId);
  const [assigneeId, setAssigneeId] = useState(null);
  const [priority, setPriority] = useState('medium');
  const [dueDate, setDueDate] = useState('');

  const candidates = users.filter((u) => u.active && u.role !== 'visualizador' && (!project || project.memberIds.includes(u.id)));

  const save = () => {
    if (!title.trim()) return Alert.alert('Atenção', 'Informe o título da tarefa.');
    if (!projectId) return Alert.alert('Atenção', 'Selecione um projeto.');
    if (dueDate && !/^\d{4}-\d{2}-\d{2}$/.test(dueDate)) return Alert.alert('Atenção', 'Use o prazo no formato AAAA-MM-DD.');
    addTask({ title: title.trim(), description: description.trim(), projectId, assigneeId, priority, dueDate: dueDate || null });
    navigation.goBack();
  };

  const Label = ({ children }) => <Text style={{ fontWeight: '600', marginBottom: 8, color: colors.text }}>{children}</Text>;
  const Wrap = ({ children }) => <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 }}>{children}</View>;

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Field label="Título" value={title} onChangeText={setTitle} placeholder="Ex.: Criar tela de perfil" />
      <Field label="Descrição" value={description} onChangeText={setDescription} multiline placeholder="Detalhes da tarefa" />

      <Label>Projeto</Label>
      <Wrap>
        {visibleProjects.map((p) => (
          <View key={p.id} style={{ marginBottom: 8 }}>
            <Chip label={p.name} active={projectId === p.id} color={p.color} onPress={() => { setProjectId(p.id); setAssigneeId(null); }} />
          </View>
        ))}
      </Wrap>

      <Label>Responsável</Label>
      <Wrap>
        {candidates.map((u) => (
          <View key={u.id} style={{ marginBottom: 8 }}>
            <Chip label={u.name.split(' ')[0]} active={assigneeId === u.id} onPress={() => setAssigneeId(assigneeId === u.id ? null : u.id)} />
          </View>
        ))}
      </Wrap>

      <Label>Prioridade</Label>
      <Wrap>
        {PRIORITY_ORDER.map((p) => (
          <View key={p} style={{ marginBottom: 8 }}>
            <Chip label={PRIORITY[p].label} color={PRIORITY[p].color} active={priority === p} onPress={() => setPriority(p)} />
          </View>
        ))}
      </Wrap>

      <Field label="Prazo (AAAA-MM-DD)" value={dueDate} onChangeText={setDueDate} placeholder="2026-10-15" />
      <Button title="Criar tarefa" icon="checkmark" onPress={save} />
    </ScrollView>
  );
}
