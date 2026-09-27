import React from 'react';
import { View, Text, FlatList, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, Chip, ChipRow, Badge } from '../components/ui';
import { ROLES, ROLE_ORDER } from '../utils/permissions';

export default function UsersScreen() {
  const { users, user: me, setUserRole } = useApp();
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: colors.bg }}
      data={users}
      keyExtractor={(u) => u.id}
      contentContainerStyle={{ padding: 16 }}
      ListHeaderComponent={<Text style={{ color: colors.muted, marginBottom: 12 }}>Defina o papel de cada usuário. O backend oferece os papéis Administrador e Usuário; seu próprio papel não pode ser alterado por esta tela.</Text>}
      renderItem={({ item: u }) => {
        const isMe = u.id === me.id;
        return (
          <Card>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Avatar name={u.name} color={ROLES[u.role].color} size={42} />
              <View style={{ flex: 1, marginLeft: 12 }}>
                <Text style={{ fontWeight: '700', color: colors.text }}>{u.name}{isMe ? ' (você)' : ''}</Text>
                <Text style={{ color: colors.muted, fontSize: 12 }}>{u.email}</Text>
              </View>
            </View>
            <View style={{ marginTop: 12 }}>
              <Text style={{ fontSize: 12, color: colors.muted, marginBottom: 6 }}>Nível de acesso</Text>
              <ChipRow>
                {ROLE_ORDER.map((r) => (
                  <Chip key={r} label={ROLES[r].label} color={ROLES[r].color} active={u.role === r} disabled={isMe} onPress={() => setUserRole(u.id, r).catch((e) => Alert.alert('Não foi possível alterar o perfil', e.message))} />
                ))}
              </ChipRow>
            </View>
          </Card>
        );
      }}
    />
  );
}
