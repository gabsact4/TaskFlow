import React, { useState } from 'react';
import { View, Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { Field, Button, Card } from '../components/ui';
import { colors } from '../theme';

export default function LoginScreen({ navigation }) {
  const { login, busy, error: apiError } = useApp();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = async () => {
    setError('');
    const r = await login(email, password);
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
          <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry placeholder="Sua senha" />
          {error || apiError ? <Text style={{ color: colors.danger, marginBottom: 10 }}>{error || apiError}</Text> : null}
          <Button title={busy ? 'Conectando…' : 'Entrar'} icon="log-in-outline" onPress={submit} disabled={busy} />
        </Card>

        <Text onPress={() => navigation.navigate('Cadastro')} style={{ color: colors.primary, fontSize: 14, fontWeight: '700', textAlign: 'center', marginTop: 16 }}>
          Ainda não tem conta? Criar conta
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  logo: { width: 72, height: 72, borderRadius: 20, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginBottom: 14 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text },
});
