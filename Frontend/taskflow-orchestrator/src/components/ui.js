import React from 'react';
import { View, Text, TouchableOpacity, TextInput, StyleSheet, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors, radius } from '../theme';

export const Card = ({ children, style, onPress }) =>
  onPress ? (
    <TouchableOpacity activeOpacity={0.9} onPress={onPress} style={[s.card, style]}>
      {children}
    </TouchableOpacity>
  ) : (
    <View style={[s.card, style]}>{children}</View>
  );

export const Badge = ({ label, color = colors.primary, style }) => (
  <View style={[s.badge, { backgroundColor: color + '18', borderColor: color + '50', borderWidth: 1 }, style]}>
    <Text style={[s.badgeText, { color }]}>{label}</Text>
  </View>
);

export const Button = ({ title, onPress, variant = 'primary', icon, disabled, style }) => {
  const bg = variant === 'primary' ? colors.primary : variant === 'danger' ? colors.danger : 'transparent';
  const fg = variant === 'outline' ? colors.primary : '#fff';
  return (
    <TouchableOpacity
      activeOpacity={0.9}
      disabled={disabled}
      onPress={onPress}
      style={[
        s.button,
        {
          backgroundColor: bg,
          borderColor: variant === 'outline' ? colors.primary : bg,
          opacity: disabled ? 0.5 : 1,
          shadowColor: bg === 'transparent' ? colors.primary : '#000',
          shadowOpacity: bg === 'transparent' ? 0.08 : 0.12,
          shadowRadius: 8,
          shadowOffset: { width: 0, height: 4 },
          elevation: 2,
        },
        style,
      ]}
    >
      {icon ? <Ionicons name={icon} size={18} color={fg} style={{ marginRight: 8 }} /> : null}
      <Text style={[s.buttonText, { color: fg }]}>{title}</Text>
    </TouchableOpacity>
  );
};

export const Avatar = ({ name = '?', size = 36, color = colors.primary }) => {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
  return (
    <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: color + '22', borderWidth: 1, borderColor: color + '33', alignItems: 'center', justifyContent: 'center' }}>
      <Text style={{ color, fontWeight: '800', fontSize: size * 0.38 }}>{initials}</Text>
    </View>
  );
};

export const ProgressBar = ({ value = 0, color = colors.primary, style }) => (
  <View style={[{ height: 10, borderRadius: 999, backgroundColor: colors.border, overflow: 'hidden' }, style]}>
    <View style={{ width: `${Math.round(value * 100)}%`, height: '100%', backgroundColor: color, borderRadius: 999 }} />
  </View>
);

export const Chip = ({ label, active, onPress, color = colors.primary, disabled }) => (
  <TouchableOpacity
    activeOpacity={0.8}
    onPress={onPress}
    disabled={disabled}
    style={[
      s.chip,
      { borderColor: active ? color : colors.border, backgroundColor: active ? color : colors.card, opacity: disabled && !active ? 0.5 : 1 },
    ]}
  >
    <Text style={{ color: active ? '#fff' : colors.text, fontSize: 13, fontWeight: '700' }}>{label}</Text>
  </TouchableOpacity>
);

export const ChipRow = ({ children }) => (
  <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingRight: 8, paddingVertical: 2 }} style={{ flexGrow: 0 }}>
    {children}
  </ScrollView>
);

export const SectionTitle = ({ children, right }) => (
  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12, marginBottom: 12 }}>
    <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{children}</Text>
    {right}
  </View>
);

export const EmptyState = ({ icon = 'file-tray-outline', text }) => (
  <View style={{ alignItems: 'center', padding: 32, borderRadius: radius.lg, borderWidth: 1, borderColor: colors.border, backgroundColor: colors.card }}>
    <Ionicons name={icon} size={40} color={colors.muted} />
    <Text style={{ color: colors.muted, marginTop: 8, textAlign: 'center' }}>{text}</Text>
  </View>
);

export const Field = ({ label, style, ...props }) => (
  <View style={{ marginBottom: 14 }}>
    {label ? <Text style={s.label}>{label}</Text> : null}
    <TextInput
      placeholderTextColor={colors.muted}
      {...props}
      style={[s.input, props.multiline && { height: 96, textAlignVertical: 'top' }, style]}
    />
  </View>
);

export const Fab = ({ onPress, icon = 'add' }) => (
  <TouchableOpacity onPress={onPress} activeOpacity={0.9} style={s.fab}>
    <Ionicons name={icon} size={28} color="#fff" />
  </TouchableOpacity>
);

const s = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderRadius: radius.md,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: colors.border,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
    elevation: 3,
  },
  badge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: radius.pill, alignSelf: 'flex-start' },
  badgeText: { fontSize: 11, fontWeight: '700' },
  button: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', minHeight: 48, paddingVertical: 13, paddingHorizontal: 18, borderRadius: radius.md, borderWidth: 1 },
  buttonText: { fontSize: 15, fontWeight: '700' },
  chip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: radius.pill, borderWidth: 1, marginRight: 8 },
  label: { fontSize: 13, fontWeight: '700', color: colors.text, marginBottom: 6 },
  input: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, paddingHorizontal: 14, paddingVertical: 12, fontSize: 15, color: colors.text },
  fab: { position: 'absolute', right: 20, bottom: 20, width: 56, height: 56, borderRadius: 28, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', elevation: 6, shadowColor: '#000', shadowOpacity: 0.2, shadowRadius: 8, shadowOffset: { width: 0, height: 4 } },
});
