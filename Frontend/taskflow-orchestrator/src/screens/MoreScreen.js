import React from 'react';
import { ScrollView, View, Text, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, Badge } from '../components/ui';
import { ROLES } from '../utils/permissions';

export default function MoreScreen({ navigation }) {
  const { user, can, unreadCount, logout } = useApp();
  const items = [
    { icon: 'calendar-outline', label: 'Calendário', screen: 'Calendario', show: true },
    { icon: 'notifications-outline', label: 'Notificações (indisponível)', screen: 'Notificacoes', show: true, badge: unreadCount },
    { icon: 'time-outline', label: 'Histórico de alterações', screen: 'Historico', show: can('viewHistory') },
    { icon: 'people-outline', label: 'Usuários e níveis de acesso', screen: 'Usuarios', show: can('manageUsers') },
    { icon: 'shield-checkmark-outline', label: 'Permissões por nível', screen: 'Permissoes', show: true },
  ].filter((i) => i.show);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card style={{ alignItems: 'center', paddingVertical: 20 }}>
        <Avatar name={user.name} size={64} color={ROLES[user.role].color} />
        <Text style={{ fontSize: 18, fontWeight: '800', color: colors.text, marginTop: 10 }}>{user.name}</Text>
        <Text style={{ color: colors.muted }}>{user.email}</Text>
        <Badge label={ROLES[user.role].label} color={ROLES[user.role].color} style={{ marginTop: 8 }} />
      </Card>

      {items.map((i) => (
        <Card key={i.screen} onPress={() => navigation.navigate(i.screen)} style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Ionicons name={i.icon} size={22} color={colors.primary} />
          <Text style={{ flex: 1, marginLeft: 12, color: colors.text, fontWeight: '600' }}>{i.label}</Text>
          {i.badge ? <Badge label={String(i.badge)} color={colors.danger} style={{ marginRight: 8 }} /> : null}
          <Ionicons name="chevron-forward" size={18} color={colors.muted} />
        </Card>
      ))}

      <TouchableOpacity onPress={logout} style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', padding: 14, marginTop: 8 }}>
        <Ionicons name="log-out-outline" size={20} color={colors.danger} />
        <Text style={{ color: colors.danger, fontWeight: '700', marginLeft: 8 }}>Sair</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}
