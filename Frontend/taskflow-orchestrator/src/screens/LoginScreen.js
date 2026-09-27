import React, { useState } from 'react';
import { View, Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { Field, Button, Card } from '../components/ui';
import { colors } from '../theme';

export default function LoginScreen() {
  const { login, register, busy, error: apiError } = useApp();
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = async () => {
    setError('');
    if (creatingAccount && (!name.trim() || password.length < 8)) {
      setError('Informe seu nome e uma senha com pelo menos 8 caracteres.');
      return;
    }
    const r = creatingAccount ? await register(name, email, password) : await login(email, password);
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
          {creatingAccount ? <Field label="Nome" value={name} onChangeText={setName} placeholder="Seu nome" /> : null}
          <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="voce@empresa.com" />
          <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry placeholder={creatingAccount ? 'Mínimo de 8 caracteres' : 'Sua senha'} />
          {error || apiError ? <Text style={{ color: colors.danger, marginBottom: 10 }}>{error || apiError}</Text> : null}
          <Button title={busy ? 'Conectando…' : creatingAccount ? 'Criar conta' : 'Entrar'} icon={creatingAccount ? 'person-add-outline' : 'log-in-outline'} onPress={submit} disabled={busy} />
        </Card>

        <Text onPress={() => { setCreatingAccount(!creatingAccount); setError(''); }} style={{ color: colors.primary, fontSize: 14, fontWeight: '700', textAlign: 'center', marginTop: 16 }}>
          {creatingAccount ? 'Já tem uma conta? Entrar' : 'Ainda não tem conta? Criar conta'}
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  logo: { width: 72, height: 72, borderRadius: 20, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginBottom: 14 },
  title: { fontSize: 24, fontWeight: '800', color: colors.text },
});
