import React from 'react';
import { View, Text, FlatList, TouchableOpacity, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, EmptyState } from '../components/ui';
import { formatDateTime } from '../utils/format';

const ICONS = { task: 'checkbox-outline', alert: 'alarm-outline', critical: 'warning-outline', reminder: 'time-outline', user: 'person-add-outline', project: 'folder-open-outline' };

export default function NotificationsScreen({ navigation }) {
  const { myNotifications, markRead, markAllRead, unreadCount, reminderDays, setPersonalReminderDays } = useApp();
  const changeReminder = (days) => setPersonalReminderDays(days).catch((error) => Alert.alert('Não foi possível salvar o lembrete', error.message));
  const openNotification = async (notification) => {
    try {
      if (!notification.read) await markRead(notification.id);
      if (notification.taskId) navigation.navigate('TarefaDetalhe', { id: String(notification.taskId) });
    } catch (error) {
      Alert.alert('Não foi possível abrir a notificação', error.message);
    }
  };
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <View style={{ padding: 16, paddingBottom: 0 }}>
        <Text style={{ color: colors.text, fontWeight: '700' }}>Lembrete personalizado</Text>
        <Text style={{ color: colors.muted, marginTop: 3 }}>Escolha com quantos dias de antecedência receber lembretes.</Text>
        <View style={{ flexDirection: 'row', gap: 8, marginTop: 8 }}>
          {[0, 1, 2, 3, 7, 14, 21, 30].map((days) => <TouchableOpacity key={days} accessibilityRole="button" accessibilityState={{ selected: reminderDays === days }} onPress={() => changeReminder(days)} style={{ paddingHorizontal: 11, paddingVertical: 7, borderRadius: 16, backgroundColor: reminderDays === days ? colors.primary : '#E5E7EB' }}><Text style={{ color: reminderDays === days ? '#fff' : colors.text, fontWeight: '600' }}>{days === 0 ? '0d' : `${days}d`}</Text></TouchableOpacity>)}
        </View>
        <Text style={{ color: colors.muted, fontSize: 12, marginTop: 8 }}>Os alertas críticos de vencimento hoje, amanhã e tarefas atrasadas permanecem ativos mesmo com antecedência 0.</Text>
      </View>
      {unreadCount > 0 ? (
        <TouchableOpacity onPress={() => markAllRead().catch((error) => Alert.alert('Não foi possível atualizar as notificações', error.message))} style={{ padding: 14, paddingBottom: 0, alignItems: 'flex-end' }}>
          <Text style={{ color: colors.primary, fontWeight: '700' }}>Marcar todas como lidas</Text>
        </TouchableOpacity>
      ) : null}
      <FlatList
        data={myNotifications}
        keyExtractor={(n) => n.id}
        contentContainerStyle={{ padding: 16 }}
        ListEmptyComponent={<EmptyState icon="notifications-off-outline" text="Nenhum prazo crítico no momento." />}
        renderItem={({ item: n }) => (
          <Card onPress={() => openNotification(n)} style={{ flexDirection: 'row', backgroundColor: n.read ? '#fff' : '#EEF2FF' }}>
            <View style={{ width: 40, height: 40, borderRadius: 20, backgroundColor: (n.type === 'CRITICAL' ? colors.danger : colors.primary) + '22', alignItems: 'center', justifyContent: 'center' }}>
              <Ionicons name={ICONS[n.type?.toLowerCase()] || 'notifications-outline'} size={20} color={n.type === 'CRITICAL' ? colors.danger : colors.primary} />
            </View>
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={{ fontWeight: '700', color: colors.text }}>{n.title}</Text>
              <Text style={{ color: colors.text, marginTop: 2 }}>{n.body}</Text>
              <Text style={{ color: colors.muted, fontSize: 11, marginTop: 4 }}>{formatDateTime(n.date)}</Text>
            </View>
            {!n.read ? <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: colors.primary, marginTop: 4 }} /> : null}
          </Card>
        )}
      />
    </View>
  );
}
