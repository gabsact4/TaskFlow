import React from 'react';
import { View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { Card, Badge, Avatar } from './ui';
import { colors, STATUS, PRIORITY, RECURRENCE } from '../theme';
import { formatDate, isOverdue, checklistProgress } from '../utils/format';

export default function TaskCard({ task, onPress, compact, showStatus = true }) {
  const { getUser, getProject, tasks } = useApp();
  const assignee = getUser(task.assigneeId);
  const project = getProject(task.projectId);
  const overdue = isOverdue(task);
  const cp = checklistProgress(task);
  const subtaskCount = tasks.filter((entry) => entry.parentTaskId === task.id).length;

  return (
    <Card onPress={onPress} style={{ borderLeftWidth: 4, borderLeftColor: PRIORITY[task.priority].color }}>
      <Text style={{ fontSize: 15, fontWeight: '700', color: colors.text }} numberOfLines={2}>{task.title}</Text>
      {!compact && project ? (
        <Text style={{ fontSize: 12, color: colors.muted, marginTop: 2 }}>{project.name}</Text>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginTop: 8 }}>
        <Badge label={PRIORITY[task.priority].label} color={PRIORITY[task.priority].color} style={{ marginRight: 6, marginBottom: 4 }} />
        {task.recurrence && task.recurrence !== 'none' ? <Badge label={`↻ ${RECURRENCE[task.recurrence]?.label || 'Recorrente'}`} color={colors.info} style={{ marginRight: 6, marginBottom: 4 }} /> : null}
        {subtaskCount > 0 ? <Badge label={`↳ ${subtaskCount} subtarefas`} color={colors.primary} style={{ marginRight: 6, marginBottom: 4 }} /> : null}
        {showStatus ? <Badge label={STATUS[task.status].label} color={STATUS[task.status].color} style={{ marginBottom: 4 }} /> : null}
      </View>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 6 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Ionicons name="calendar-outline" size={14} color={overdue ? colors.danger : colors.muted} />
          <Text style={{ marginLeft: 4, fontSize: 12, color: overdue ? colors.danger : colors.muted, fontWeight: overdue ? '700' : '400' }}>
            {formatDate(task.dueDate)}{overdue ? ' • atrasada' : ''}
          </Text>
          {cp ? (
            <View style={{ flexDirection: 'row', alignItems: 'center', marginLeft: 12 }}>
              <Ionicons name="checkbox-outline" size={14} color={colors.muted} />
              <Text style={{ marginLeft: 4, fontSize: 12, color: colors.muted }}>{cp.done}/{cp.total}</Text>
            </View>
          ) : null}
        </View>
        {assignee ? <Avatar name={assignee.name} size={26} /> : null}
      </View>
    </Card>
  );
}
