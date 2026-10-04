import React, { useMemo, useState } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme';

const toIsoDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const fromIsoDate = (value) => {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
};

export default function DatePickerField({ label, value, onChange, placeholder = 'Selecionar data', minDate = null }) {
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() => {
    const selected = fromIsoDate(value);
    return selected ? new Date(selected.getFullYear(), selected.getMonth(), 1) : new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  });
  const selectedDate = fromIsoDate(value);
  const firstWeekday = new Date(month.getFullYear(), month.getMonth(), 1).getDay();
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const cells = useMemo(() => [...Array(firstWeekday).fill(null), ...Array.from({ length: daysInMonth }, (_, index) => index + 1)], [firstWeekday, daysInMonth]);
  const monthLabel = month.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  const displayValue = selectedDate ? selectedDate.toLocaleDateString('pt-BR') : placeholder;

  const openCalendar = () => {
    const selected = fromIsoDate(value);
    setMonth(selected ? new Date(selected.getFullYear(), selected.getMonth(), 1) : new Date(new Date().getFullYear(), new Date().getMonth(), 1));
    setOpen(true);
  };

  return (
    <View style={{ marginBottom: 14 }}>
      <Text style={{ color: colors.text, fontSize: 13, fontWeight: '600', marginBottom: 6 }}>{label}</Text>
      <View style={{ flexDirection: 'row', alignItems: 'center' }}>
        <Pressable onPress={openCalendar} style={{ flex: 1, minHeight: 46, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: 10, backgroundColor: colors.card, paddingHorizontal: 12 }}>
          <Ionicons name="calendar-outline" size={19} color={colors.primary} />
          <Text style={{ color: selectedDate ? colors.text : colors.muted, marginLeft: 9 }}>{displayValue}</Text>
        </Pressable>
        {value ? <Pressable onPress={() => onChange(null)} accessibilityRole="button" accessibilityLabel="Remover data" style={{ padding: 10 }}><Ionicons name="close-circle" size={21} color={colors.muted} /></Pressable> : null}
      </View>
      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable onPress={() => setOpen(false)} style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#0007' }}>
          <Pressable onPress={() => {}} style={{ borderRadius: 16, padding: 16, backgroundColor: colors.card, alignSelf: 'center', width: '100%', maxWidth: 380 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
              <Pressable onPress={() => setMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))} hitSlop={12} accessibilityLabel="Mês anterior"><Ionicons name="chevron-back" size={23} color={colors.primary} /></Pressable>
              <Text style={{ color: colors.text, fontSize: 17, fontWeight: '700', textTransform: 'capitalize' }}>{monthLabel}</Text>
              <Pressable onPress={() => setMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))} hitSlop={12} accessibilityLabel="Próximo mês"><Ionicons name="chevron-forward" size={23} color={colors.primary} /></Pressable>
            </View>
            <View style={{ flexDirection: 'row', marginBottom: 6 }}>{['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((day, index) => <Text key={`${day}${index}`} style={{ width: '14.285%', textAlign: 'center', color: colors.muted, fontSize: 12, fontWeight: '700', paddingVertical: 8 }}>{day}</Text>)}</View>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>{cells.map((day, index) => {
              const iso = day ? toIsoDate(new Date(month.getFullYear(), month.getMonth(), day)) : null;
              const active = iso === value;
              const beforeMinimum = !!(iso && minDate && iso < minDate);
              return <Pressable key={`${day || 'blank'}-${index}`} disabled={!day || beforeMinimum} onPress={() => { onChange(iso); setOpen(false); }} style={{ width: '14.285%', height: 42, alignItems: 'center', justifyContent: 'center' }}>
                {day ? <Text style={{ minWidth: 34, textAlign: 'center', overflow: 'hidden', borderRadius: 18, paddingVertical: 8, backgroundColor: active ? colors.primary : 'transparent', color: active ? '#fff' : beforeMinimum ? colors.border : colors.text, fontWeight: active ? '800' : '500' }}>{day}</Text> : null}
              </Pressable>;
            })}</View>
            <Pressable onPress={() => setOpen(false)} style={{ alignSelf: 'flex-end', paddingHorizontal: 12, paddingVertical: 10, marginTop: 8 }}><Text style={{ color: colors.primary, fontWeight: '700' }}>Fechar</Text></Pressable>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}
