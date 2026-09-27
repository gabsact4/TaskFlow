import React from 'react';
import { ScrollView, View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Badge } from '../components/ui';
import { ROLES, ROLE_ORDER, ACTIONS, can } from '../utils/permissions';

export default function PermissionsScreen() {
  const { user } = useApp();
  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      {ROLE_ORDER.map((r) => (
        <Card key={r} style={{ borderWidth: user.role === r ? 2 : 0, borderColor: ROLES[r].color }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
            <Badge label={ROLES[r].label} color={ROLES[r].color} />
            {user.role === r ? <Text style={{ color: ROLES[r].color, fontWeight: '700', fontSize: 12 }}>Seu nível</Text> : null}
          </View>
          <Text style={{ color: colors.muted, marginVertical: 10 }}>{ROLES[r].description}</Text>
          {Object.keys(ACTIONS).map((a) => {
            const ok = can(r, a);
            return (
              <View key={a} style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 4 }}>
                <Ionicons name={ok ? 'checkmark-circle' : 'close-circle'} size={18} color={ok ? colors.success : colors.border} />
                <Text style={{ marginLeft: 8, color: ok ? colors.text : colors.muted, flex: 1 }}>{ACTIONS[a]}</Text>
              </View>
            );
          })}
        </Card>
      ))}
    </ScrollView>
  );
}
