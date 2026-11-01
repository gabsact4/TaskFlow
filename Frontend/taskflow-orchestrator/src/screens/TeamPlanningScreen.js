import React from 'react';
import { Alert, ScrollView, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { request } from '../api';
import { colors } from '../theme';
import { Badge, Button, Card, EmptyState, Field, ProgressBar, SectionTitle } from '../components/ui';
import DatePickerField from '../components/DatePickerField';

const iso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const today = iso(new Date());
const afterDays = (count) => { const date = new Date(); date.setDate(date.getDate() + count); return iso(date); };

export default function TeamPlanningScreen() {
  const { user } = useApp();
  const [from, setFrom] = React.useState(today);
  const [to, setTo] = React.useState(afterDays(13));
  const [capacity, setCapacity] = React.useState(null);
  const [absences, setAbsences] = React.useState([]);
  const [startDate, setStartDate] = React.useState('');
  const [endDate, setEndDate] = React.useState('');
  const [reason, setReason] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');

  const load = React.useCallback(async () => {
    if (!from || !to || from > to) { setError('Selecione um intervalo de análise válido.'); setCapacity(null); return; }
    setLoading(true); setError('');
    try {
      const [report, absenceRows] = await Promise.all([
        request(`/team/capacity?from=${from}&to=${to}`), request('/absences'),
      ]);
      setCapacity(report); setAbsences(absenceRows);
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [from, to]);
  React.useEffect(() => { load(); }, [load]);

  const submitAbsence = async () => {
    if (!startDate || !endDate) return Alert.alert('Período obrigatório', 'Selecione a data inicial e final.');
    if (startDate > endDate) return Alert.alert('Período inválido', 'A data final deve ser igual ou posterior à inicial.');
    try {
      await request('/absences', { method: 'POST', body: JSON.stringify({ startDate, endDate, reason: reason.trim() || null }) });
      setStartDate(''); setEndDate(''); setReason(''); await load();
      Alert.alert('Solicitação enviada', 'A ausência ficará pendente até a aprovação do responsável.');
    } catch (e) { Alert.alert('Não foi possível solicitar a ausência', e.message); }
  };

  const reviewAbsence = (absence, approved) => Alert.alert(approved ? 'Aprovar ausência' : 'Rejeitar ausência', `${absence.userName}: ${formatDate(absence.startDate)} a ${formatDate(absence.endDate)}?`, [
    { text: 'Cancelar', style: 'cancel' },
    { text: approved ? 'Aprovar' : 'Rejeitar', style: approved ? 'default' : 'destructive', onPress: async () => {
      try { await request(`/absences/${absence.id}/review`, { method: 'PATCH', body: JSON.stringify({ approved, note: null }) }); await load(); }
      catch (e) { Alert.alert('Não foi possível revisar a solicitação', e.message); }
    } },
  ]);
  const cancelAbsence = (absence) => Alert.alert('Cancelar solicitação', 'Deseja remover esta solicitação de ausência?', [
    { text: 'Manter', style: 'cancel' },
    { text: 'Cancelar ausência', style: 'destructive', onPress: async () => {
      try { await request(`/absences/${absence.id}`, { method: 'DELETE' }); await load(); }
      catch (e) { Alert.alert('Não foi possível cancelar', e.message); }
    } },
  ]);

  const canReview = user?.role === 'master' || user?.role === 'po';
  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }} keyboardShouldPersistTaps="handled">
      <Card>
        <Text style={{ color: colors.text, fontSize: 18, fontWeight: '800' }}>Planejamento da equipe</Text>
        <Text style={{ color: colors.muted, marginTop: 6, lineHeight: 20 }}>Capacidade calculada com 8 horas por dia útil. A previsão usa estimativas das tarefas e ausências aprovadas; conflitos são alertas informativos.</Text>
        <View style={{ marginTop: 14 }}>
          <DatePickerField label="Início da análise" value={from} onChange={setFrom} />
          <DatePickerField label="Fim da análise" value={to} onChange={setTo} minDate={from} />
          <Button title={loading ? 'Atualizando…' : 'Atualizar análise'} icon="analytics-outline" onPress={load} disabled={loading} />
        </View>
      </Card>
      {error ? <Card><Text style={{ color: colors.danger }}>{error}</Text></Card> : null}
      <SectionTitle>Membros e carga</SectionTitle>
      {capacity?.members?.map((member) => {
        const overCapacity = member.utilizationPercent > 100;
        const fill = Math.min(member.utilizationPercent / 100, 1);
        return <Card key={member.userId}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text style={{ color: colors.text, fontWeight: '800', flex: 1 }}>{member.userName}</Text>
            <Badge label={`${Math.round(member.utilizationPercent)}%`} color={overCapacity ? colors.danger : colors.primary} />
          </View>
          <ProgressBar value={fill} color={overCapacity ? colors.danger : colors.primary} style={{ marginTop: 12, marginBottom: 8 }} />
          <Text style={{ color: colors.muted }}>{member.plannedHours.toFixed(1)} h planejadas · {member.availableHours.toFixed(1)} h disponíveis</Text>
          <Text style={{ color: colors.muted, fontSize: 12, marginTop: 4 }}>{member.openTasks} tarefas abertas · {member.approvedAbsenceDays} dias úteis de ausência aprovada</Text>
          {overCapacity ? <Text style={{ color: colors.danger, fontWeight: '700', marginTop: 8 }}>A carga estimada excede a capacidade disponível.</Text> : null}
        </Card>;
      })}
      {capacity && capacity.members.length === 0 ? <EmptyState icon="people-outline" text="Não há membros ou tarefas atribuídas neste escopo." /> : null}

      <SectionTitle right={<Badge label={String(capacity?.conflicts?.length || 0)} color={capacity?.conflicts?.length ? colors.danger : colors.success} />}>Conflitos de agenda</SectionTitle>
      {capacity?.conflicts?.map((conflict, index) => <Card key={`${conflict.firstTaskId}-${conflict.secondTaskId}`} style={{ borderColor: colors.danger + '55' }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}><Ionicons name="warning-outline" size={20} color={colors.danger} /><Text style={{ color: colors.text, fontWeight: '800', marginLeft: 8 }}>{conflict.userName}</Text></View>
        <Text style={{ color: colors.text, marginTop: 8 }}>{conflict.firstTaskTitle} ↔ {conflict.secondTaskTitle}</Text>
        <Text style={{ color: colors.muted, fontSize: 12, marginTop: 4 }}>Sobreposição: {formatDate(conflict.overlapStart)} – {formatDate(conflict.overlapEnd)}</Text>
        <Text style={{ color: colors.muted, fontSize: 11, marginTop: 5 }}>Alerta baseado nas datas planejadas; não bloqueia a edição das tarefas.</Text>
      </Card>)}
      {capacity && capacity.conflicts.length === 0 ? <EmptyState icon="checkmark-circle-outline" text="Nenhum conflito de período identificado neste intervalo." /> : null}

      <SectionTitle>Solicitar férias ou ausência</SectionTitle>
      <Card>
        <Text style={{ color: colors.muted, marginBottom: 12 }}>Solicitações pendentes não reduzem a capacidade da equipe. Após aprovação, os dias úteis serão descontados.</Text>
        <DatePickerField label="Primeiro dia" value={startDate} onChange={setStartDate} minDate={today} />
        <DatePickerField label="Último dia" value={endDate} onChange={setEndDate} minDate={startDate || today} />
        <Field label="Observação (opcional)" value={reason} onChangeText={setReason} placeholder="Férias, compromisso, licença…" maxLength={500} />
        <Button title="Enviar solicitação" icon="paper-plane-outline" onPress={submitAbsence} />
      </Card>

      <SectionTitle>Solicitações de ausência</SectionTitle>
      {absences.map((absence) => {
        const own = absence.userId === user?.id;
        const canAct = canReview && !own && absence.status === 'PENDING';
        const statusColors = { PENDING: colors.warning, APPROVED: colors.success, REJECTED: colors.danger };
        const labels = { PENDING: 'Pendente', APPROVED: 'Aprovada', REJECTED: 'Rejeitada' };
        return <Card key={absence.id}>
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
            <Text style={{ color: colors.text, fontWeight: '800', flex: 1 }}>{own ? 'Minha solicitação' : absence.userName}</Text>
            <Badge label={labels[absence.status] || absence.status} color={statusColors[absence.status] || colors.muted} />
          </View>
          <Text style={{ color: colors.text, marginTop: 8 }}>{formatDate(absence.startDate)} – {formatDate(absence.endDate)}</Text>
          {absence.reason ? <Text style={{ color: colors.muted, marginTop: 4 }}>{absence.reason}</Text> : null}
          {absence.reviewNote ? <Text style={{ color: colors.muted, marginTop: 4 }}>Nota da revisão: {absence.reviewNote}</Text> : null}
          {canAct ? <View style={{ flexDirection: 'row', marginTop: 12 }}>
            <Button title="Aprovar" icon="checkmark" onPress={() => reviewAbsence(absence, true)} style={{ flex: 1, marginRight: 6 }} />
            <Button title="Rejeitar" icon="close" variant="danger" onPress={() => reviewAbsence(absence, false)} style={{ flex: 1, marginLeft: 6 }} />
          </View> : null}
          {(own || canReview) && absence.status !== 'REJECTED' ? <Button title="Cancelar" variant="outline" onPress={() => cancelAbsence(absence)} style={{ marginTop: 8 }} /> : null}
        </Card>;
      })}
      {!absences.length ? <EmptyState icon="calendar-outline" text="Nenhuma solicitação de ausência registrada." /> : null}
    </ScrollView>
  );
}

function formatDate(value) { return value ? new Date(`${value}T12:00:00`).toLocaleDateString('pt-BR') : '—'; }