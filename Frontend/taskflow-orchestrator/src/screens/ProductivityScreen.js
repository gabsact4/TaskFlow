import React from 'react';
import { Alert, ScrollView, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { request } from '../api';
import { colors } from '../theme';
import { Badge, Button, Card, Chip, EmptyState, Field, ProgressBar, SectionTitle } from '../components/ui';
import DatePickerField from '../components/DatePickerField';

const todayIso = () => { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; };
const dateOffset = (days) => { const date = new Date(); date.setDate(date.getDate() + days); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; };
const METRICS = [
  { key: 'TRACKED_HOURS', label: 'Horas registradas', unit: 'h', icon: 'time-outline' },
  { key: 'COMPLETED_TASKS', label: 'Tarefas concluídas', unit: 'tarefas', icon: 'checkmark-done-outline' },
  { key: 'FOCUS_SESSIONS', label: 'Pomodoros concluídos', unit: 'sessões', icon: 'flame-outline' },
];

export default function ProductivityScreen() {
  const { visibleProjects, user } = useApp();
  const [from, setFrom] = React.useState(dateOffset(-6));
  const [to, setTo] = React.useState(todayIso());
  const [projectId, setProjectId] = React.useState(null);
  const [report, setReport] = React.useState(null);
  const [goals, setGoals] = React.useState([]);
  const [metric, setMetric] = React.useState('TRACKED_HOURS');
  const [periodType, setPeriodType] = React.useState('WEEKLY');
  const [targetValue, setTargetValue] = React.useState('5');
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');

  const loadGoals = React.useCallback(async () => {
    try { setGoals(await request('/productivity/goals')); } catch (e) { setError(e.message); }
  }, []);
  const loadReport = React.useCallback(async () => {
    if (!from || !to || from > to) { setError('Selecione um intervalo válido.'); return; }
    setLoading(true); setError('');
    try {
      const projectFilter = projectId ? `&projectId=${encodeURIComponent(projectId)}` : '';
      setReport(await request(`/productivity/report?from=${from}&to=${to}${projectFilter}`));
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [from, to, projectId]);
  React.useEffect(() => { loadGoals(); loadReport(); }, [loadGoals, loadReport]);

  const saveGoal = async () => {
    const target = Number(targetValue.trim().replace(',', '.'));
    if (!Number.isFinite(target) || target <= 0 || target > 100000) return Alert.alert('Meta inválida', 'Informe um valor acima de zero e até 100.000.');
    try {
      await request('/productivity/goals', { method: 'POST', body: JSON.stringify({ metric, periodType, targetValue: target }) });
      await loadGoals();
      Alert.alert('Meta salva', 'O progresso será atualizado automaticamente com os registros do período.');
    } catch (e) { Alert.alert('Não foi possível salvar a meta', e.message); }
  };
  const deleteGoal = (goal) => Alert.alert('Remover meta', 'Deseja excluir esta meta de produtividade?', [
    { text: 'Manter', style: 'cancel' },
    { text: 'Excluir', style: 'destructive', onPress: async () => {
      try { await request(`/productivity/goals/${goal.id}`, { method: 'DELETE' }); await loadGoals(); }
      catch (e) { Alert.alert('Não foi possível excluir a meta', e.message); }
    } },
  ]);
  const formatDate = (value) => value ? new Date(`${value}T12:00:00`).toLocaleDateString('pt-BR') : '—';
  const isIndividual = user?.role === 'dev';

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Card>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}><Ionicons name="analytics-outline" size={24} color={colors.primary} /><Text style={{ color: colors.text, fontSize: 18, fontWeight: '800', marginLeft: 9 }}>Relatórios de produtividade</Text></View>
        <Text style={{ color: colors.muted, marginTop: 6, lineHeight: 20 }}>{isIndividual ? 'Relatório individual das suas tarefas e horas.' : 'Resumo individual e da equipe nos projetos que você pode acessar.'} Horas vêm dos worklogs; tarefas concluídas contam somente após a mudança para Concluída.</Text>
        <DatePickerField label="De" value={from} onChange={setFrom} />
        <DatePickerField label="Até" value={to} onChange={setTo} minDate={from} />
        <Text style={{ color: colors.text, fontWeight: '700', marginBottom: 8 }}>Escopo do projeto</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 }}>
          <Chip label="Todos os projetos visíveis" active={!projectId} onPress={() => setProjectId(null)} />
          {visibleProjects.map((project) => <Chip key={project.id} label={project.name} active={projectId === project.id} onPress={() => setProjectId(project.id)} />)}
        </View>
        <Button title={loading ? 'Gerando…' : 'Atualizar relatório'} icon="refresh-outline" onPress={loadReport} disabled={loading} />
      </Card>
      {error ? <Card><Text style={{ color: colors.danger }}>{error}</Text></Card> : null}

      {report ? <>
        <SectionTitle>Resumo · {formatDate(report.from)} a {formatDate(report.to)}</SectionTitle>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' }}>
          <MetricCard label="Horas" value={`${report.trackedHours.toFixed(1)} h`} icon="time-outline" />
          <MetricCard label="Registros" value={report.workLogCount} icon="list-outline" />
          <MetricCard label="Concluídas" value={report.completedTasks} icon="checkmark-done-outline" />
          <MetricCard label="Focos" value={report.completedFocusSessions} icon="flame-outline" />
        </View>
        <SectionTitle>{isIndividual ? 'Meu resumo' : 'Desempenho por pessoa'}</SectionTitle>
        {report.members.map((member) => <Card key={member.userId}>
          <Text style={{ color: colors.text, fontWeight: '800' }}>{member.userName}</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: 8 }}>
            <Badge label={`${member.trackedHours.toFixed(1)} h`} color={colors.primary} style={{ marginRight: 6, marginBottom: 6 }} />
            <Badge label={`${member.workLogCount} registros`} color={colors.info} style={{ marginRight: 6, marginBottom: 6 }} />
            <Badge label={`${member.completedTasks} tarefas`} color={colors.success} style={{ marginRight: 6, marginBottom: 6 }} />
            <Badge label={`${member.focusSessions} focos`} color={colors.warning} style={{ marginRight: 6, marginBottom: 6 }} />
          </View>
        </Card>)}
        {!report.members.length ? <EmptyState icon="people-outline" text="Sem dados de produtividade neste período." /> : null}
        {!projectId ? <>
          <SectionTitle>Desempenho por projeto</SectionTitle>
          {report.projects.map((project) => <Card key={project.projectId}>
            <Text style={{ color: colors.text, fontWeight: '800' }}>{project.projectName}</Text>
            <Text style={{ color: colors.muted, marginTop: 6 }}>{project.trackedHours.toFixed(1)} h · {project.workLogCount} registros · {project.completedTasks} tarefas concluídas</Text>
          </Card>)}
        </> : null}
      </> : null}

      <SectionTitle>Minhas metas</SectionTitle>
      <Card>
        <Text style={{ color: colors.muted, lineHeight: 20, marginBottom: 12 }}>Crie uma meta semanal ou mensal. O progresso é calculado automaticamente com horas registradas, tarefas concluídas ou sessões Pomodoro completas.</Text>
        <Text style={{ color: colors.text, fontWeight: '700', marginBottom: 8 }}>Indicador</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 12 }}>
          {METRICS.map((item) => <Chip key={item.key} label={item.label} active={metric === item.key} onPress={() => setMetric(item.key)} />)}
        </View>
        <Text style={{ color: colors.text, fontWeight: '700', marginBottom: 8 }}>Período</Text>
        <View style={{ flexDirection: 'row', marginBottom: 12 }}>
          <Chip label="Semanal" active={periodType === 'WEEKLY'} onPress={() => setPeriodType('WEEKLY')} />
          <Chip label="Mensal" active={periodType === 'MONTHLY'} onPress={() => setPeriodType('MONTHLY')} />
        </View>
        <Field label={`Alvo (${METRICS.find((item) => item.key === metric)?.unit})`} value={targetValue} onChangeText={setTargetValue} keyboardType="decimal-pad" placeholder="Ex.: 5" />
        <Button title="Salvar meta" icon="flag-outline" onPress={saveGoal} />
      </Card>
      {goals.map((goal) => {
        const details = METRICS.find((item) => item.key === goal.metric);
        const progress = Math.min(goal.progressPercent / 100, 1);
        return <Card key={goal.id}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Ionicons name={details?.icon || 'flag-outline'} size={21} color={colors.primary} />
            <Text style={{ color: colors.text, fontWeight: '800', flex: 1, marginLeft: 8 }}>{details?.label || goal.metric} · {goal.periodType === 'WEEKLY' ? 'semanal' : 'mensal'}</Text>
            <Text onPress={() => deleteGoal(goal)} style={{ color: colors.danger, padding: 4 }}>Remover</Text>
          </View>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 12, marginBottom: 6 }}>
            <Text style={{ color: colors.muted }}>{goal.currentValue} / {goal.targetValue} {details?.unit}</Text>
            <Text style={{ color: colors.text, fontWeight: '800' }}>{Math.round(goal.progressPercent)}%</Text>
          </View>
          <ProgressBar value={progress} color={goal.progressPercent >= 100 ? colors.success : colors.primary} />
          <Text style={{ color: colors.muted, fontSize: 12, marginTop: 8 }}>{formatDate(goal.periodStart)} – {formatDate(goal.periodEnd)}</Text>
        </Card>;
      })}
      {!goals.length ? <EmptyState icon="flag-outline" text="Você ainda não criou metas de produtividade." /> : null}
    </ScrollView>
  );
}

function MetricCard({ label, value, icon }) {
  return <Card style={{ width: '48%' }}>
    <Ionicons name={icon} size={20} color={colors.primary} />
    <Text style={{ color: colors.muted, fontSize: 12, marginTop: 8 }}>{label}</Text>
    <Text style={{ color: colors.text, fontSize: 20, fontWeight: '900', marginTop: 2 }}>{value}</Text>
  </Card>;
}
