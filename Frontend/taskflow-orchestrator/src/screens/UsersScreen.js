import React from 'react';
import { View, Text, FlatList, Switch } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, Chip, ChipRow, Badge } from '../components/ui';
import { ROLES, ROLE_ORDER } from '../utils/permissions';

export default function UsersScreen() {
  const { users, user: me, setUserRole, toggleUserActive } = useApp();
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: colors.bg }}
      data={users}
      keyExtractor={(u) => u.id}
      contentContainerStyle={{ padding: 16 }}
      ListHeaderComponent={<Text style={{ color: colors.muted, marginBottom: 12 }}>Defina o nível de acesso de cada usuário. Você não pode alterar o seu próprio nível nem se desativar.</Text>}
      renderItem={({ item: u }) => {
        const isMe = u.id === me.id;
        return (
          <Card style={{ opacity: u.active ? 1 : 0.6 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Avatar name={u.name} color={ROLES[u.role].color} size={42} />
              <View style={{ flex: 1, marginLeft: 12 }}>
                <Text style={{ fontWeight: '700', color: colors.text }}>{u.name}{isMe ? ' (você)' : ''}</Text>
                <Text style={{ color: colors.muted, fontSize: 12 }}>{u.email}</Text>
              </View>
              <Switch value={u.active} disabled={isMe} onValueChange={() => toggleUserActive(u.id)} trackColor={{ true: colors.primary }} />
            </View>
            <View style={{ marginTop: 12 }}>
              <Text style={{ fontSize: 12, color: colors.muted, marginBottom: 6 }}>Nível de acesso</Text>
              <ChipRow>
                {ROLE_ORDER.map((r) => (
                  <Chip key={r} label={ROLES[r].label} color={ROLES[r].color} active={u.role === r} disabled={isMe} onPress={() => setUserRole(u.id, r)} />
                ))}
              </ChipRow>
            </View>
            {!u.active ? <Badge label="Desativado" color={colors.danger} style={{ marginTop: 10 }} /> : null}
          </Card>
        );
      }}
    />
  );
}
