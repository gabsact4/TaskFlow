import React from 'react';
import { Alert, ScrollView, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { request } from '../api';
import { colors } from '../theme';
import { Button, Card, Chip, EmptyState, SectionTitle } from '../components/ui';

const DURATIONS = [15, 25, 50];
const BREAK_MINUTES = 5;

export default function FocusScreen() {
  const { visibleTasks, activeTimer, startTaskTimer, pauseTaskTimer, resumeTaskTimer } = useApp();
  const [focusSession, setFocusSession] = React.useState(null);
  const [selectedTaskId, setSelectedTaskId] = React.useState(null);
  const [minutes, setMinutes] = React.useState(25);
  const [phase, setPhase] = React.useState('ready');
  const [breakEnd, setBreakEnd] = React.useState(null);
  const [tick, setTick] = React.useState(Date.now());
  const [busy, setBusy] = React.useState(false);
  const handledSession = React.useRef(null);
  const availableTasks = visibleTasks.filter((task) => task.status !== 'done');

  React.useEffect(() => {
    let mounted = true;
    request('/productivity/focus/active').then((session) => {
      if (!mounted || !session) return;
      setFocusSession(session);
      setSelectedTaskId(session.taskId == null ? null : String(session.taskId));
      setMinutes(session.plannedMinutes);
      setPhase('work');
    }).catch(() => {});
    const ticker = setInterval(() => setTick(Date.now()), 1000);
    return () => { mounted = false; clearInterval(ticker); };
  }, []);

  const endFocus = async (completed) => {
    if (!focusSession || handledSession.current === focusSession.id) return;
    handledSession.current = focusSession.id;
    setBusy(true);
    try {
      const saved = await request(`/productivity/focus/${focusSession.id}/finish`, { method: 'POST', body: JSON.stringify({ completed }) });
      setFocusSession(null);
      if (saved.taskId && activeTimer?.taskId === String(saved.taskId) && activeTimer.status === 'RUNNING') await pauseTaskTimer();
      if (completed) {
        setPhase('break'); setBreakEnd(Date.now() + BREAK_MINUTES * 60 * 1000);
        Alert.alert('Pomodoro concluído', `Boa! Faça uma pausa de ${BREAK_MINUTES} minutos.`);
      } else setPhase('ready');
    } catch (error) {
      handledSession.current = null;
      Alert.alert('Não foi possível encerrar o foco', error.message);
    } finally { setBusy(false); }
  };

  React.useEffect(() => {
    if (phase === 'work' && focusSession) {
      const remaining = new Date(focusSession.startedAt).getTime() + focusSession.plannedMinutes * 60_000 - tick;
      if (remaining <= 0 && handledSession.current !== focusSession.id) endFocus(true);
    }
    if (phase === 'break' && breakEnd && tick >= breakEnd) setPhase('ready');
  }, [tick, phase, focusSession, breakEnd]);

  const startFocus = async () => {
    if (activeTimer?.status === 'RUNNING' && (!selectedTaskId || activeTimer.taskId !== selectedTaskId)) return Alert.alert('Cronômetro em outra tarefa', 'Selecione a tarefa do timer atual ou pause-o antes de iniciar este foco.');
    setBusy(true);
    let created;
    try {
      created = await request('/productivity/focus', { method: 'POST', body: JSON.stringify({ taskId: selectedTaskId ? Number(selectedTaskId) : null, plannedMinutes: minutes }) });
      if (selectedTaskId) {
        if (activeTimer?.taskId === selectedTaskId) {
          if (activeTimer.status === 'PAUSED') await resumeTaskTimer();
        } else await startTaskTimer(selectedTaskId);
      }
      handledSession.current = null;
      setFocusSession(created); setPhase('work'); setTick(Date.now());
    } catch (error) {
      if (created) {
        try { await request(`/productivity/focus/${created.id}/finish`, { method: 'POST', body: JSON.stringify({ completed: false }) }); } catch {}
      }
      Alert.alert('Não foi possível iniciar o foco', error.message);
    } finally { setBusy(false); }
  };

  const startBreak = () => { setBreakEnd(Date.now() + BREAK_MINUTES * 60 * 1000); setPhase('break'); setTick(Date.now()); };
  const remainingMs = phase === 'work' && focusSession
    ? Math.max(0, new Date(focusSession.startedAt).getTime() + focusSession.plannedMinutes * 60_000 - tick)
    : phase === 'break' && breakEnd ? Math.max(0, breakEnd - tick) : minutes * 60_000;
  const clock = `${String(Math.floor(remainingMs / 60_000)).padStart(2, '0')}:${String(Math.floor((remainingMs % 60_000) / 1000)).padStart(2, '0')}`;
  const selectedTask = availableTasks.find((task) => task.id === selectedTaskId);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card style={{ alignItems: 'center', paddingVertical: 24 }}>
        <Ionicons name={phase === 'break' ? 'cafe-outline' : 'flame-outline'} size={30} color={colors.primary} />
        <Text style={{ color: colors.text, fontSize: 17, fontWeight: '800', marginTop: 8 }}>
          {phase === 'work' ? 'Sessão de foco' : phase === 'break' ? 'Pausa' : 'Modo Foco'}
        </Text>
        <Text style={{ color: colors.primary, fontSize: 64, fontWeight: '900', marginVertical: 12, fontVariant: ['tabular-nums'] }}>{clock}</Text>
        <Text style={{ color: colors.muted, textAlign: 'center' }}>
          {phase === 'work' ? selectedTask?.title || 'Foco sem tarefa vinculada' : phase === 'break' ? 'Descanse os olhos e levante um pouco.' : 'Escolha uma duração e concentre-se em uma tarefa.'}
        </Text>
        {phase === 'ready' ? <>
          <View style={{ flexDirection: 'row', marginTop: 18 }}>
            {DURATIONS.map((duration) => <Chip key={duration} label={`${duration} min`} active={minutes === duration} onPress={() => setMinutes(duration)} />)}
          </View>
          <Button title={busy ? 'Iniciando…' : 'Iniciar foco'} icon="play" onPress={startFocus} disabled={busy} style={{ width: '100%', marginTop: 14 }} />
        </> : null}
        {phase === 'work' ? <Button title={busy ? 'Encerrando…' : 'Encerrar sessão'} variant="danger" icon="stop" onPress={() => endFocus(false)} disabled={busy} style={{ width: '100%', marginTop: 16 }} /> : null}
        {phase === 'break' ? <Button title="Pular pausa" variant="outline" onPress={() => setPhase('ready')} style={{ width: '100%', marginTop: 16 }} /> : null}
        {phase === 'ready' && !availableTasks.length ? <EmptyState icon="checkmark-circle-outline" text="Não há tarefas pendentes disponíveis para associar ao foco." /> : null}
      </Card>

      <SectionTitle>Tarefa em foco</SectionTitle>
      <Card>
        <Chip label="Sem tarefa" active={!selectedTaskId} onPress={() => setSelectedTaskId(null)} />
        {availableTasks.map((task) => <View key={task.id} style={{ marginTop: 8 }}>
          <Chip label={`${task.title}${task.assigneeName ? ` · ${task.assigneeName}` : ''}`} active={selectedTaskId === task.id} disabled={phase !== 'ready'} onPress={() => setSelectedTaskId(task.id)} />
        </View>)}
        {activeTimer ? <Text style={{ color: colors.muted, marginTop: 12 }}>Cronômetro vinculado a: {activeTimer.taskTitle} ({activeTimer.status === 'RUNNING' ? 'rodando' : 'pausado'}).</Text> : <Text style={{ color: colors.muted, marginTop: 12 }}>Ao iniciar o foco com uma tarefa, o tempo também será registrado no cronômetro da tarefa.</Text>}
      </Card>

      <Card>
        <Text style={{ color: colors.text, fontWeight: '800' }}>Como funciona</Text>
        <Text style={{ color: colors.muted, lineHeight: 20, marginTop: 6 }}>Cada sessão concluída conta para suas metas de foco. O cronômetro da tarefa pausa ao terminar o Pomodoro, evitando que os intervalos sejam somados às horas trabalhadas.</Text>
      </Card>
    </ScrollView>
  );
}
