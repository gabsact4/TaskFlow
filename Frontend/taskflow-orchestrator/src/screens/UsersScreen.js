import React, { useState } from 'react';
import { View, Text, FlatList, Alert } from 'react-native';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, Chip, ChipRow, Button, Field } from '../components/ui';
import { ROLES, ROLE_ORDER } from '../utils/permissions';

export default function UsersScreen() {
  const { users, user: me, setUserRole, createManagedUser } = useApp();
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'dev' });
  const [saving, setSaving] = useState(false);
  const submit = async () => {
    setSaving(true);
    try {
      await createManagedUser(form);
      setForm({ name: '', email: '', password: '', role: 'dev' });
      Alert.alert('Conta criada', `A conta ${form.email} foi cadastrada como ${ROLES[form.role].label}.`);
    } catch (error) {
      Alert.alert('Não foi possível cadastrar a conta', error.message);
    } finally {
      setSaving(false);
    }
  };
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: colors.bg }}
      data={users}
      keyExtractor={(u) => u.id}
      contentContainerStyle={{ padding: 16 }}
      ListHeaderComponent={(
        <>
          <Card>
            <Text style={{ color: colors.text, fontWeight: '800', fontSize: 17 }}>Cadastrar pessoa</Text>
            <Text style={{ color: colors.muted, marginTop: 4, marginBottom: 14 }}>Contas novas são criadas com acesso definido pelo Master.</Text>
            <Field label="Nome" value={form.name} onChangeText={(name) => setForm((current) => ({ ...current, name }))} autoCapitalize="words" />
            <Field label="E-mail" value={form.email} onChangeText={(email) => setForm((current) => ({ ...current, email }))} keyboardType="email-address" autoCapitalize="none" />
            <Field label="Senha inicial (mínimo 8 caracteres)" value={form.password} onChangeText={(password) => setForm((current) => ({ ...current, password }))} secureTextEntry />
            <Text style={{ fontSize: 12, color: colors.muted, marginBottom: 7 }}>Papel de acesso</Text>
            <ChipRow>{ROLE_ORDER.map((role) => <Chip key={role} label={ROLES[role].label} color={ROLES[role].color} active={form.role === role} onPress={() => setForm((current) => ({ ...current, role }))} />)}</ChipRow>
            <Button title={saving ? 'Cadastrando…' : 'Cadastrar conta'} onPress={submit} disabled={saving || !form.name.trim() || !form.email.trim() || form.password.length < 8} style={{ marginTop: 12 }} />
          </Card>
          <Text style={{ color: colors.muted, marginBottom: 12 }}>Master supervisiona tudo; PO gerencia seus projetos; Dev trabalha apenas nas tarefas atribuídas. Seu próprio papel não pode ser alterado nesta tela.</Text>
        </>
      )}
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
