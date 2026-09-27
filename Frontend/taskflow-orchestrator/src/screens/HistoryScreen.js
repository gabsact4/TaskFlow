import React from 'react';
import { View, Text, FlatList } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, EmptyState } from '../components/ui';
import { formatDateTime } from '../utils/format';
import { ROLES } from '../utils/permissions';

export default function HistoryScreen() {
  const { history, getUser } = useApp();
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: colors.bg }}
      data={[...history].sort((a, b) => (a.date < b.date ? 1 : -1))}
      keyExtractor={(h) => h.id}
      contentContainerStyle={{ padding: 16 }}
      ListEmptyComponent={<EmptyState text="Nenhuma alteração registrada." />}
      renderItem={({ item: h }) => {
        const u = getUser(h.userId);
        return (
          <Card style={{ flexDirection: 'row' }}>
            <Avatar name={u ? u.name : 'S'} color={u ? ROLES[u.role].color : colors.muted} />
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={{ color: colors.text }}><Text style={{ fontWeight: '700' }}>{u ? u.name : 'Sistema'}</Text> {h.text}</Text>
              <Text style={{ color: colors.muted, fontSize: 11, marginTop: 4 }}>{formatDateTime(h.date)}</Text>
            </View>
          </Card>
        );
      }}
    />
  );
}
