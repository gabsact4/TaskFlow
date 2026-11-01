import React from 'react';
import { Alert, ScrollView, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Badge, Button, Card, EmptyState, SectionTitle } from '../components/ui';

export default function SyncScreen() {
  const { online, offlineQueue, refresh, resolveOfflineConflict, loading } = useApp();
  const conflicts = offlineQueue.filter((operation) => operation.status === 'conflict' || operation.status === 'blocked');
  const pending = offlineQueue.filter((operation) => operation.status === 'pending');
  const resolve = async (operation, choice) => {
    try { await resolveOfflineConflict(operation.id, choice); }
    catch (error) { Alert.alert('Não foi possível resolver a sincronização', error.message); }
  };
  const titleOf = (operation) => operation.type === 'TASK_CREATE'
    ? operation.data?.title || 'Nova tarefa'
    : operation.baseSnapshot?.title || operation.serverTask?.title || `Tarefa ${operation.taskId}`;
  const actionOf = (operation) => ({ TASK_CREATE: 'Criação', TASK_UPDATE: 'Edição', TASK_DELETE: 'Exclusão' })[operation.type] || 'Alteração';

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Ionicons name={online ? 'cloud-done-outline' : 'cloud-offline-outline'} size={26} color={online ? colors.success : colors.warning} />
          <View style={{ flex: 1, marginLeft: 10 }}>
            <Text style={{ color: colors.text, fontSize: 17, fontWeight: '800' }}>{online ? 'Conectado e sincronizado' : 'Modo offline'}</Text>
            <Text style={{ color: colors.muted, marginTop: 3 }}>{pending.length} alteração(ões) pendente(s) · {conflicts.length} conflito(s)</Text>
          </View>
          <Badge label={online ? 'Online' : 'Offline'} color={online ? colors.success : colors.warning} />
        </View>
        <Button title={loading ? 'Sincronizando…' : 'Sincronizar agora'} icon="sync-outline" onPress={refresh} disabled={loading} style={{ marginTop: 14 }} />
        <Text style={{ color: colors.muted, fontSize: 12, lineHeight: 18, marginTop: 10 }}>Tarefas e alterações ficam no dispositivo durante a falta de conexão. A sincronização é retomada automaticamente quando o servidor volta a responder.</Text>
      </Card>

      {conflicts.length ? <SectionTitle>Conflitos para revisar</SectionTitle> : null}
      {conflicts.map((operation) => <Card key={operation.id} style={{ borderColor: colors.danger + '55' }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}><Ionicons name="warning-outline" size={20} color={colors.danger} /><Text style={{ color: colors.text, fontWeight: '800', marginLeft: 8, flex: 1 }}>{titleOf(operation)}</Text><Badge label={operation.status === 'blocked' ? 'Permissão' : 'Conflito'} color={colors.danger} /></View>
        <Text style={{ color: colors.muted, marginTop: 8 }}>{operation.message || 'A versão do servidor mudou enquanto você estava offline.'}</Text>
        {operation.changes ? <Text style={{ color: colors.text, fontSize: 12, marginTop: 6 }}>Alterações locais: {Object.entries(operation.changes).map(([key, value]) => `${key}=${String(value)}`).join(' · ')}</Text> : null}
        <View style={{ flexDirection: 'row', marginTop: 12 }}>
          <Button title="Usar servidor" variant="outline" onPress={() => resolve(operation, 'server')} style={{ flex: 1, marginRight: 6 }} />
          <Button title={operation.type === 'TASK_DELETE' ? 'Aplicar exclusão' : operation.type === 'TASK_CREATE' ? 'Tentar novamente' : 'Reaplicar local'} onPress={() => resolve(operation, 'local')} style={{ flex: 1, marginLeft: 6 }} />
        </View>
      </Card>)}

      {pending.length ? <SectionTitle>Fila de sincronização</SectionTitle> : null}
      {pending.map((operation) => <Card key={operation.id} style={{ flexDirection: 'row', alignItems: 'center' }}>
        <Ionicons name="cloud-upload-outline" size={21} color={colors.primary} />
        <View style={{ flex: 1, marginLeft: 10 }}><Text style={{ color: colors.text, fontWeight: '700' }}>{titleOf(operation)}</Text><Text style={{ color: colors.muted, fontSize: 12 }}>{actionOf(operation)} · aguardando conexão</Text></View>
        <Button title="Cancelar" variant="outline" onPress={() => resolve(operation, 'server')} />
      </Card>)}
      {!offlineQueue.length ? <EmptyState icon="checkmark-circle-outline" text="Nenhuma alteração aguardando sincronização." /> : null}

      <Card>
        <Text style={{ color: colors.text, fontWeight: '800' }}>Resolução segura de conflitos</Text>
        <Text style={{ color: colors.muted, lineHeight: 20, marginTop: 6 }}>Alterações concorrentes não são sobrescritas silenciosamente. Você pode manter a versão do servidor ou reaplicar suas mudanças sobre a versão mais recente. O servidor continua validando permissões e versões.</Text>
      </Card>
    </ScrollView>
  );
}
