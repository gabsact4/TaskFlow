import React, { useState } from 'react';
import { ScrollView, Alert, Text, View } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Field, Button, Chip } from '../components/ui';

export default function NewProjectScreen({ navigation }) {
  const { addProject, users, user } = useApp();
  const [memberIds, setMemberIds] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [projectKey, setProjectKey] = useState('');

  const save = () => {
    if (!name.trim()) return Alert.alert('Atenção', 'Informe o nome do projeto.');
    const key = projectKey.trim() || name.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-|-$/g, '').slice(0, 20);
    if (!key) return Alert.alert('Atenção', 'Informe uma chave para o projeto.');
    addProject({ name: name.trim(), projectKey: key, description: description.trim(), memberIds })
      .then(() => navigation.goBack()).catch((e) => Alert.alert('Não foi possível criar o projeto', e.message));
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Field label="Nome do projeto" value={name} onChangeText={setName} placeholder="Ex.: Portal do Aluno" />
      <Field label="Descrição" value={description} onChangeText={setDescription} multiline placeholder="Resumo do objetivo do projeto" />
      <Field label="Chave do projeto" value={projectKey} onChangeText={setProjectKey} autoCapitalize="characters" placeholder="PORTAL-ALUNO (opcional: gerada pelo nome)" />
      <Text style={{ color: colors.text, fontWeight: '700', marginBottom: 8 }}>Pessoas do projeto</Text>
      <Text style={{ color: colors.muted, marginBottom: 10 }}>Selecione quem poderá participar. Você pode alterar a equipe depois.</Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 14 }}>
        {users.filter((person) => person.id !== user?.id).map((person) => { const selected = memberIds.includes(Number(person.id)); return <View key={person.id} style={{ marginRight: 8, marginBottom: 8 }}><Chip label={`${selected ? '✓ ' : ''}${person.name}`} active={selected} onPress={() => setMemberIds((ids) => selected ? ids.filter((id) => id !== Number(person.id)) : [...ids, Number(person.id)])} /></View>; })}
      </View>
      <Button title="Criar projeto" icon="checkmark" onPress={save} />
    </ScrollView>
  );
}
