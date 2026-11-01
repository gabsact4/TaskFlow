import React from 'react';
import { Alert, Platform, ScrollView, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useApp } from '../context/AppContext';
import { requestRaw } from '../api';
import { colors } from '../theme';
import { Button, Card, Chip, SectionTitle } from '../components/ui';

export default function ExportScreen() {
  const { visibleProjects } = useApp();
  const [projectId, setProjectId] = React.useState(null);
  const [busy, setBusy] = React.useState('');
  const [error, setError] = React.useState('');

  const exportFile = async (format) => {
    setBusy(format); setError('');
    const filename = `taskflow-${new Date().toISOString().replace(/[:.]/g, '-')}.${format}`;
    try {
      const filter = projectId ? `&projectId=${encodeURIComponent(projectId)}` : '';
      const response = await requestRaw(`/exports?format=${format}${filter}`);
      if (Platform.OS === 'web') {
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a'); link.href = url; link.download = filename; link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      } else {
        const bytes = new Uint8Array(await response.arrayBuffer());
        let binary = '';
        for (let offset = 0; offset < bytes.length; offset += 0x8000) binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
        const uri = `${FileSystem.documentDirectory}${filename}`;
        await FileSystem.writeAsStringAsync(uri, btoa(binary), { encoding: FileSystem.EncodingType.Base64 });
        if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(uri, { mimeType: format === 'csv' ? 'text/csv' : 'application/json', dialogTitle: `Exportar ${filename}` });
        else Alert.alert('Arquivo exportado', uri);
      }
    } catch (e) { setError(e.message); }
    finally { setBusy(''); }
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 16 }}>
      <Card>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}><Ionicons name="download-outline" size={24} color={colors.primary} /><Text style={{ color: colors.text, fontSize: 18, fontWeight: '800', marginLeft: 8 }}>Exportar dados</Text></View>
        <Text style={{ color: colors.muted, marginTop: 8, lineHeight: 20 }}>Gere uma cópia dos projetos e tarefas acessíveis, incluindo comentários, worklogs e histórico de alterações. As permissões são verificadas no servidor.</Text>
        <SectionTitle>Escopo</SectionTitle>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
          <Chip label="Todos os projetos visíveis" active={!projectId} onPress={() => setProjectId(null)} />
          {visibleProjects.map((project) => <Chip key={project.id} label={project.name} active={projectId === project.id} onPress={() => setProjectId(project.id)} />)}
        </View>
        {error ? <Text style={{ color: colors.danger, marginTop: 12 }}>{error}</Text> : null}
        <Button title={busy === 'json' ? 'Gerando JSON…' : 'Baixar JSON'} icon="code-download-outline" onPress={() => exportFile('json')} disabled={!!busy} style={{ marginTop: 16 }} />
        <Button title={busy === 'csv' ? 'Gerando CSV…' : 'Baixar CSV'} icon="grid-outline" variant="outline" onPress={() => exportFile('csv')} disabled={!!busy} style={{ marginTop: 8 }} />
      </Card>
      <Card>
        <Text style={{ color: colors.text, fontWeight: '800' }}>Privacidade do arquivo</Text>
        <Text style={{ color: colors.muted, lineHeight: 20, marginTop: 6 }}>A exportação contém dados de negócio e pode incluir nomes, comentários e descrições. Guarde o arquivo em local apropriado. Senhas, tokens e conteúdo binário de anexos nunca são exportados.</Text>
      </Card>
    </ScrollView>
  );
}
