import React, { useState } from 'react';
import { View, Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { Field, Button, Card, Badge, Avatar } from '../components/ui';
import { colors } from '../theme';
import { ROLES } from '../utils/permissions';

export default function LoginScreen() {
  const { login, loginAs, users } = useApp();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = () => {
    const r = login(email, password);
    if (!r.ok) setError(r.error);
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: 20, paddingTop: 70 }} keyboardShouldPersistTaps="handled">
        <View style={{ alignItems: 'center', marginBottom: 28 }}>
          <View style={s.logo}>
            <Ionicons name="git-network-outline" size={36} color="#fff" />
          </View>
          <Text style={s.title}>TaskFlow Orchestrator</Text>
          <Text style={{ color: colors.muted, marginTop: 4 }}>Gerencie projetos e tarefas em equipe</Text>
        </View>

        <Card>
          <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="voce@empresa.com" />
          <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry placeholder="••••••" />
          {error ? <Text style={{ color: colors.danger, marginBottom: 10 }}>{error}</Text> : null}
          <Button title="Entrar" icon="log-in-outline" onPress={submit} />
        </Card>

        <Text style={{ fontWeight: '700', color: colors.text, marginTop: 12, marginBottom: 4 }}>Acesso rápido (dados mock)</Text>
        <Text style={{ color: colors.muted, fontSize: 12, marginBottom: 10 }}>Toque em um perfil para entrar com aquele nível de acesso. Senha de todos: 123456</Text>
        {users.filter((u) => u.active).map((u) => (
          <Card key={u.id} onPress={() => loginAs(u.id)} style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Avatar name={u.name} color={ROLES[u.role].color} size={40} />
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={{ fontWeight: '700', color: colors.text }}>{u.name}</Text>
              <Text style={{ color: colors.muted, fontSize: 12 }}>{u.email}</Text>
            </View>
            <Badge label={ROLES[u.role].label} color={ROLES[u.role].color} />
          </Card>
        ))}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  logo: { width: 72, height: 72, borderRadius: 20, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginBottom: 14 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text },
});
