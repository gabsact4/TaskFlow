import React, { useState } from 'react';
import { ScrollView, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Field, Button } from '../components/ui';

export default function NewProjectScreen({ navigation }) {
  const { addProject } = useApp();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [projectKey, setProjectKey] = useState('');

  const save = () => {
    if (!name.trim()) return Alert.alert('Atenção', 'Informe o nome do projeto.');
    const key = projectKey.trim() || name.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-|-$/g, '').slice(0, 20);
    if (!key) return Alert.alert('Atenção', 'Informe uma chave para o projeto.');
    addProject({ name: name.trim(), projectKey: key, description: description.trim() })
      .then(() => navigation.goBack()).catch((e) => Alert.alert('Não foi possível criar o projeto', e.message));
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Field label="Nome do projeto" value={name} onChangeText={setName} placeholder="Ex.: Portal do Aluno" />
      <Field label="Descrição" value={description} onChangeText={setDescription} multiline placeholder="Resumo do objetivo do projeto" />
      <Field label="Chave do projeto" value={projectKey} onChangeText={setProjectKey} autoCapitalize="characters" placeholder="PORTAL-ALUNO (opcional: gerada pelo nome)" />
      <Button title="Criar projeto" icon="checkmark" onPress={save} />
    </ScrollView>
  );
}
