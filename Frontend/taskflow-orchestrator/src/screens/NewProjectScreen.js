import React, { useState } from 'react';
import { ScrollView, Text, Alert, View } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Field, Button, Chip } from '../components/ui';

const PALETTE = ['#4F46E5', '#059669', '#F59E0B', '#DC2626', '#0EA5E9', '#7C3AED'];

export default function NewProjectScreen({ navigation }) {
  const { users, user, addProject } = useApp();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [deadline, setDeadline] = useState('');
  const [color, setColor] = useState(PALETTE[0]);
  const [members, setMembers] = useState([user.id]);

  const toggle = (id) => setMembers((m) => (m.includes(id) ? m.filter((x) => x !== id) : [...m, id]));

  const save = () => {
    if (!name.trim()) return Alert.alert('Atenção', 'Informe o nome do projeto.');
    if (deadline && !/^\d{4}-\d{2}-\d{2}$/.test(deadline)) return Alert.alert('Atenção', 'Use o prazo no formato AAAA-MM-DD.');
    addProject({ name: name.trim(), description: description.trim(), deadline: deadline || null, color, memberIds: members });
    navigation.goBack();
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Field label="Nome do projeto" value={name} onChangeText={setName} placeholder="Ex.: Portal do Aluno" />
      <Field label="Descrição" value={description} onChangeText={setDescription} multiline placeholder="Resumo do objetivo do projeto" />
      <Field label="Prazo (AAAA-MM-DD)" value={deadline} onChangeText={setDeadline} placeholder="2026-12-31" />
      <Text style={{ fontWeight: '600', marginBottom: 8, color: colors.text }}>Cor</Text>
      <View style={{ flexDirection: 'row', marginBottom: 16 }}>
        {PALETTE.map((c) => (
          <View key={c} style={{ marginRight: 10 }}>
            <Chip label={c === color ? '✓' : '  '} active={c === color} color={c} onPress={() => setColor(c)} />
          </View>
        ))}
      </View>
      <Text style={{ fontWeight: '600', marginBottom: 8, color: colors.text }}>Membros</Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 20 }}>
        {users.filter((u) => u.active).map((u) => (
          <View key={u.id} style={{ marginBottom: 8 }}>
            <Chip label={u.name.split(' ')[0]} active={members.includes(u.id)} onPress={() => toggle(u.id)} />
          </View>
        ))}
      </View>
      <Button title="Criar projeto" icon="checkmark" onPress={save} />
    </ScrollView>
  );
}
