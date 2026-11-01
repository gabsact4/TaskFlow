import React from 'react';
import { View, Text, FlatList, RefreshControl } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';
import { Card, Avatar, EmptyState, Chip, Button, SectionTitle } from '../components/ui';
import { formatDateTime } from '../utils/format';
import { request } from '../api';

export default function HistoryScreen() {
  const { user, visibleProjects } = useApp();
  const [history, setHistory] = React.useState([]);
  const [projectId, setProjectId] = React.useState(null);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');
  const load = React.useCallback(async () => {
    setLoading(true); setError('');
    const cacheKey = `taskflow.history.${user?.id}.${projectId || 'all'}.${page}.v1`;
    try {
      const cached = await AsyncStorage.getItem(cacheKey);
      if (cached) {
        const snapshot = JSON.parse(cached);
        setHistory(snapshot.content || []); setTotalPages(snapshot.totalPages || 0);
      }
    } catch {}
    try {
      const filter = projectId ? `&projectId=${encodeURIComponent(projectId)}` : '';
      const result = await request(`/history?page=${page}&size=30${filter}`);
      setHistory(result.content || []); setTotalPages(result.totalPages || 0);
      await AsyncStorage.setItem(cacheKey, JSON.stringify({ content: result.content || [], totalPages: result.totalPages || 0 }));
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [page, projectId, user?.id]);
  React.useEffect(() => { load(); }, [load]);
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: colors.bg }}
      data={history}
      keyExtractor={(h) => String(h.id)}
      contentContainerStyle={{ padding: 16 }}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
      ListHeaderComponent={<>
        <SectionTitle>Histórico de alterações</SectionTitle>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 10 }}>
          <Chip label="Todos os projetos" active={!projectId} onPress={() => { setProjectId(null); setPage(0); }} />
          {visibleProjects.map((project) => <Chip key={project.id} label={project.name} active={projectId === project.id} onPress={() => { setProjectId(project.id); setPage(0); }} />)}
        </View>
        {error ? <Card><Text style={{ color: colors.danger }}>{error}</Text></Card> : null}
      </>}
      ListEmptyComponent={!loading ? <EmptyState text={error ? 'Não foi possível carregar o histórico.' : 'Ainda não há alterações registradas para este escopo.'} /> : null}
      ListFooterComponent={totalPages > 1 ? <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 8 }}>
        <Button title="Anterior" variant="outline" disabled={page === 0 || loading} onPress={() => setPage((value) => Math.max(0, value - 1))} />
        <Text style={{ color: colors.muted }}>Página {page + 1} de {totalPages}</Text>
        <Button title="Próxima" variant="outline" disabled={page + 1 >= totalPages || loading} onPress={() => setPage((value) => value + 1)} />
      </View> : null}
      renderItem={({ item: h }) => {
        const role = colors.primary;
        let detail = '';
        try {
          const parsed = h.detailsJson ? JSON.parse(h.detailsJson) : null;
          if (parsed?.before && parsed?.after) detail = Object.keys(parsed.after).filter((key) => JSON.stringify(parsed.before[key]) !== JSON.stringify(parsed.after[key])).map((key) => `${key}: ${displayValue(parsed.before[key])} → ${displayValue(parsed.after[key])}`).join(' · ');
          else if (parsed?.title) detail = parsed.title;
          else if (parsed?.durationMinutes != null) detail = `${parsed.durationMinutes} minutos`;
          else if (parsed?.text) detail = parsed.text;
          else if (parsed?.content) detail = parsed.content;
        } catch { detail = ''; }
        return (
          <Card style={{ flexDirection: 'row' }}>
            <Avatar name={h.actorName || 'S'} color={role} />
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={{ color: colors.text }}><Text style={{ fontWeight: '700' }}>{h.actorName || 'Usuário'}</Text> {h.summary}</Text>
              {detail ? <Text style={{ color: colors.muted, fontSize: 12, marginTop: 5 }}>{detail}</Text> : null}
              <Text style={{ color: colors.primary, fontSize: 10, fontWeight: '700', marginTop: 5 }}>{h.entityType} · {h.action}</Text>
              <Text style={{ color: colors.muted, fontSize: 11, marginTop: 4 }}>{formatDateTime(h.createdAt)}</Text>
            </View>
          </Card>
        );
      }}
    />
  );
}

function displayValue(value) { return value == null || value === '' ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value); }
