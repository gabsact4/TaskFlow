import React, { useEffect, useState } from 'react';
import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { request } from '../api';
import { colors, PRIORITY, PRIORITY_ORDER, RECURRENCE, RECURRENCE_ORDER } from '../theme';
import { Button, Card, Chip, ChipRow, Field, SectionTitle } from '../components/ui';

export default function TemplatesScreen() {
  const { projects, instantiateTaskTemplate, instantiateProjectTemplate } = useApp();
  const [taskTemplates, setTaskTemplates] = useState([]);
  const [projectTemplates, setProjectTemplates] = useState([]);
  const [projectId, setProjectId] = useState(projects[0]?.id || null);
  const [projectKey, setProjectKey] = useState('');
  const [taskName, setTaskName] = useState('');
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');
  const [checklistItems, setChecklistItems] = useState('');
  const [priority, setPriority] = useState('medium');
  const [recurrence, setRecurrence] = useState('none');
  const [projectTemplateName, setProjectTemplateName] = useState('');
  const [projectName, setProjectName] = useState('');
  const [projectDescription, setProjectDescription] = useState('');
  const [starterTasks, setStarterTasks] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { if (!projectId && projects.length) setProjectId(projects[0].id); }, [projects, projectId]);
  useEffect(() => {
    Promise.all([request('/task-templates'), request('/project-templates')])
      .then(([tasks, projectRows]) => { setTaskTemplates(tasks); setProjectTemplates(projectRows); })
      .catch((e) => setError(e.message));
  }, []);

  const saveTaskTemplate = async () => {
    if (!taskName.trim() || !taskTitle.trim()) return Alert.alert('Modelo de tarefa', 'Informe o nome do modelo e o título da tarefa.');
    setBusy(true);
    try {
      const saved = await request('/task-templates', { method: 'POST', body: JSON.stringify({ name: taskName.trim(), title: taskTitle.trim(), description: taskDescription.trim(), priority: priority.toUpperCase(), recurrence: recurrence.toUpperCase(), checklistItems }) });
      setTaskTemplates((all) => [saved, ...all]);
      setTaskName(''); setTaskTitle(''); setTaskDescription(''); setChecklistItems('');
      Alert.alert('Pronto', 'Modelo de tarefa salvo.');
    } catch (e) { Alert.alert('Não foi possível salvar o modelo', e.message); }
    finally { setBusy(false); }
  };

  const saveProjectTemplate = async () => {
    if (!projectTemplateName.trim() || !projectName.trim()) return Alert.alert('Modelo de projeto', 'Informe o nome do modelo e o nome do projeto.');
    setBusy(true);
    try {
      const saved = await request('/project-templates', { method: 'POST', body: JSON.stringify({ name: projectTemplateName.trim(), projectName: projectName.trim(), description: projectDescription.trim(), starterTasks }) });
      setProjectTemplates((all) => [saved, ...all]);
      setProjectTemplateName(''); setProjectName(''); setProjectDescription(''); setStarterTasks('');
      Alert.alert('Pronto', 'Modelo de projeto salvo.');
    } catch (e) { Alert.alert('Não foi possível salvar o modelo', e.message); }
    finally { setBusy(false); }
  };

  const useTaskTemplate = async (template) => {
    if (!projectId) return Alert.alert('Selecione um projeto', 'Crie ou selecione um projeto antes de usar este modelo.');
    setBusy(true);
    try {
      await instantiateTaskTemplate(template.id, { projectId: Number(projectId), dueDate: null, assigneeId: null, parentTaskId: null });
      Alert.alert('Tarefa criada', `A tarefa “${template.title}” foi adicionada ao projeto.`);
    } catch (e) { Alert.alert('Não foi possível criar a tarefa', e.message); }
    finally { setBusy(false); }
  };

  const useProjectTemplate = async (template) => {
    const baseKey = template.projectName.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-|-$/g, '').slice(0, 13);
    const key = projectKey.trim() || `${baseKey}-${Date.now().toString(36).slice(-5)}`;
    if (!key) return Alert.alert('Chave necessária', 'Informe uma chave para o novo projeto.');
    setBusy(true);
    try {
      await instantiateProjectTemplate(template.id, key);
      setProjectKey('');
      Alert.alert('Projeto criado', `O projeto “${template.projectName}” e suas tarefas iniciais foram criados.`);
    } catch (e) { Alert.alert('Não foi possível criar o projeto', e.message); }
    finally { setBusy(false); }
  };

  const removeTemplate = async (kind, template) => {
    const path = kind === 'task' ? 'task-templates' : 'project-templates';
    try {
      await request(`/${path}/${template.id}`, { method: 'DELETE' });
      if (kind === 'task') setTaskTemplates((all) => all.filter((entry) => entry.id !== template.id));
      else setProjectTemplates((all) => all.filter((entry) => entry.id !== template.id));
    } catch (e) { Alert.alert('Não foi possível excluir o modelo', e.message); }
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      {error ? <Text style={{ color: colors.danger, marginBottom: 10 }}>{error}</Text> : null}
      <SectionTitle>Criar modelo de tarefa</SectionTitle>
      <Card>
        <Field label="Nome do modelo" value={taskName} onChangeText={setTaskName} placeholder="Ex.: Revisão semanal" />
        <Field label="Título da tarefa" value={taskTitle} onChangeText={setTaskTitle} placeholder="Ex.: Revisar indicadores" />
        <Field label="Descrição padrão" value={taskDescription} onChangeText={setTaskDescription} multiline placeholder="Detalhes reutilizados ao criar a tarefa" />
        <Text style={{ color: colors.text, fontWeight: '700', marginBottom: 6 }}>Prioridade padrão</Text>
        <ChipRow>{PRIORITY_ORDER.map((value) => <Chip key={value} label={PRIORITY[value].label} color={PRIORITY[value].color} active={priority === value} onPress={() => setPriority(value)} />)}</ChipRow>
        <Text style={{ color: colors.text, fontWeight: '700', marginTop: 12, marginBottom: 6 }}>Repetição padrão</Text>
        <ChipRow>{RECURRENCE_ORDER.map((value) => <Chip key={value} label={RECURRENCE[value].label} active={recurrence === value} onPress={() => setRecurrence(value)} />)}</ChipRow>
        <Field label="Itens do checklist (um por linha)" value={checklistItems} onChangeText={setChecklistItems} multiline placeholder={'Conferir dados\nValidar resultado'} />
        <Button title={busy ? 'Salvando…' : 'Salvar modelo de tarefa'} icon="save-outline" onPress={saveTaskTemplate} disabled={busy} />
      </Card>

      <SectionTitle>Meus modelos de tarefa</SectionTitle>
      <Text style={{ color: colors.muted, marginBottom: 8 }}>Projeto onde o modelo será aplicado:</Text>
      <ChipRow>{projects.map((project) => <Chip key={project.id} label={project.name} active={project.id === projectId} onPress={() => setProjectId(project.id)} />)}</ChipRow>
      {taskTemplates.map((template) => (
        <Card key={template.id}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={{ flex: 1 }}><Text style={{ color: colors.text, fontWeight: '800' }}>{template.name}</Text><Text style={{ color: colors.muted, marginTop: 3 }}>{template.title}</Text></View>
            <TouchableOpacity onPress={() => removeTemplate('task', template)} hitSlop={8}><Ionicons name="trash-outline" size={20} color={colors.danger} /></TouchableOpacity>
          </View>
          <Button title="Criar tarefa deste modelo" icon="add" onPress={() => useTaskTemplate(template)} disabled={busy || !projectId} style={{ marginTop: 12 }} />
        </Card>
      ))}

      <SectionTitle>Criar modelo de projeto</SectionTitle>
      <Card>
        <Field label="Nome do modelo" value={projectTemplateName} onChangeText={setProjectTemplateName} placeholder="Ex.: Lançamento de produto" />
        <Field label="Nome do projeto" value={projectName} onChangeText={setProjectName} placeholder="Ex.: Lançamento Q1" />
        <Field label="Descrição padrão" value={projectDescription} onChangeText={setProjectDescription} multiline placeholder="Objetivo do projeto" />
        <Field label="Tarefas iniciais (uma por linha; use 2 espaços para cada nível filho)" value={starterTasks} onChangeText={setStarterTasks} multiline placeholder={'Planejamento\n  Definir escopo\n  Aprovar cronograma\nExecução'} />
        <Button title={busy ? 'Salvando…' : 'Salvar modelo de projeto'} icon="save-outline" onPress={saveProjectTemplate} disabled={busy} />
      </Card>

      <SectionTitle>Meus modelos de projeto</SectionTitle>
      <Field label="Chave para o projeto criado (opcional)" value={projectKey} onChangeText={setProjectKey} autoCapitalize="characters" placeholder="Gerada automaticamente pelo nome" />
      {projectTemplates.map((template) => (
        <Card key={template.id}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <View style={{ flex: 1 }}><Text style={{ color: colors.text, fontWeight: '800' }}>{template.name}</Text><Text style={{ color: colors.muted, marginTop: 3 }}>{template.projectName}</Text></View>
            <TouchableOpacity onPress={() => removeTemplate('project', template)} hitSlop={8}><Ionicons name="trash-outline" size={20} color={colors.danger} /></TouchableOpacity>
          </View>
          <Button title="Criar projeto deste modelo" icon="add" onPress={() => useProjectTemplate(template)} disabled={busy} style={{ marginTop: 12 }} />
        </Card>
      ))}
    </ScrollView>
  );
}
