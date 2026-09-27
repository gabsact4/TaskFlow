import React from 'react';
import { View, Text, FlatList, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, EmptyState } from '../components/ui';
import { formatDateTime } from '../utils/format';

const ICONS = { task: 'checkbox-outline', alert: 'alarm-outline', user: 'person-add-outline', project: 'folder-open-outline' };

export default function NotificationsScreen() {
  const { myNotifications, markRead, markAllRead, unreadCount } = useApp();
  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      {unreadCount > 0 ? (
        <TouchableOpacity onPress={markAllRead} style={{ padding: 14, paddingBottom: 0, alignItems: 'flex-end' }}>
          <Text style={{ color: colors.primary, fontWeight: '700' }}>Marcar todas como lidas</Text>
        </TouchableOpacity>
      ) : null}
      <FlatList
        data={myNotifications}
        keyExtractor={(n) => n.id}
        contentContainerStyle={{ padding: 16 }}
        ListEmptyComponent={<EmptyState icon="notifications-off-outline" text="Você não tem notificações." />}
        renderItem={({ item: n }) => (
          <Card onPress={() => markRead(n.id)} style={{ flexDirection: 'row', backgroundColor: n.read ? '#fff' : '#EEF2FF' }}>
            <View style={{ width: 40, height: 40, borderRadius: 20, backgroundColor: colors.primary + '22', alignItems: 'center', justifyContent: 'center' }}>
              <Ionicons name={ICONS[n.type] || 'notifications-outline'} size={20} color={colors.primary} />
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
