import React, { useState } from 'react';
import { View, Text, ScrollView, KeyboardAvoidingView, Platform, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { Field, Button, Card } from '../components/ui';
import { colors } from '../theme';

export default function RegisterScreen({ navigation }) {
  const { register, busy, error } = useApp();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const submit = async () => {
    if (!name.trim()) return Alert.alert('Cadastro', 'Informe seu nome.');
    if (password.length < 8) return Alert.alert('Cadastro', 'A senha deve ter pelo menos 8 caracteres.');
    const result = await register(name, email, password);
    if (!result.ok) Alert.alert('Não foi possível criar a conta', result.error);
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ padding: 20, paddingTop: 32 }} keyboardShouldPersistTaps="handled">
        <View style={{ alignItems: 'center', marginBottom: 24 }}>
          <Text style={{ fontSize: 25, fontWeight: '800', color: colors.text }}>Criar conta</Text>
          <Text style={{ color: colors.muted, marginTop: 6 }}>Cadastre-se para começar a usar o TaskFlow.</Text>
        </View>
        <Card>
          <Field label="Nome" value={name} onChangeText={setName} autoCapitalize="words" placeholder="Seu nome" />
          <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" placeholder="voce@empresa.com" />
          <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry placeholder="Mínimo de 8 caracteres" />
          {error ? <Text style={{ color: colors.danger, marginBottom: 10 }}>{error}</Text> : null}
          <Button title={busy ? 'Criando conta…' : 'Criar conta'} icon="person-add-outline" onPress={submit} disabled={busy} />
        </Card>
        <Text onPress={() => navigation.navigate('Login')} style={{ color: colors.primary, fontSize: 14, fontWeight: '700', textAlign: 'center', marginTop: 16 }}>
          Já tem uma conta? Entrar
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
