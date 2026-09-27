import React from 'react';
import { TouchableOpacity, View, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { useApp } from '../context/AppContext';
import { colors } from '../theme';

import LoginScreen from '../screens/LoginScreen';
import RegisterScreen from '../screens/RegisterScreen';
import DashboardScreen from '../screens/DashboardScreen';
import ProjectsScreen from '../screens/ProjectsScreen';
import ProjectDetailScreen from '../screens/ProjectDetailScreen';
import NewProjectScreen from '../screens/NewProjectScreen';
import TasksScreen from '../screens/TasksScreen';
import TaskDetailScreen from '../screens/TaskDetailScreen';
import NewTaskScreen from '../screens/NewTaskScreen';
import KanbanScreen from '../screens/KanbanScreen';
import CalendarScreen from '../screens/CalendarScreen';
import NotificationsScreen from '../screens/NotificationsScreen';
import UsersScreen from '../screens/UsersScreen';
import HistoryScreen from '../screens/HistoryScreen';
import PermissionsScreen from '../screens/PermissionsScreen';
import MoreScreen from '../screens/MoreScreen';
import TemplatesScreen from '../screens/TemplatesScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

const ICONS = {
  Inicio: 'home',
  Projetos: 'folder',
  Tarefas: 'checkbox',
  Kanban: 'albums',
  Mais: 'menu',
};

function Tabs() {
  const { unreadCount } = useApp();
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.muted,
        headerTitleStyle: { fontWeight: '800' },
        headerStyle: { backgroundColor: colors.card, shadowOpacity: 0 },
        tabBarStyle: {
          backgroundColor: colors.card,
          borderTopWidth: 1,
          borderTopColor: colors.border,
          height: 64,
          paddingBottom: 8,
          paddingTop: 8,
        },
        tabBarLabelStyle: { fontSize: 11, fontWeight: '700' },
        tabBarIcon: ({ color, size }) => <Ionicons name={ICONS[route.name]} size={size} color={color} />,
      })}
    >
      <Tab.Screen
        name="Inicio"
        component={DashboardScreen}
        options={({ navigation }) => ({
          title: 'TaskFlow',
          headerRight: () => (
            <TouchableOpacity onPress={() => navigation.navigate('Notificacoes')} style={{ marginRight: 16 }}>
              <Ionicons name="notifications-outline" size={24} color={colors.text} />
              {unreadCount > 0 ? (
                <View style={{ position: 'absolute', top: -4, right: -6, backgroundColor: colors.danger, borderRadius: 9, minWidth: 18, height: 18, alignItems: 'center', justifyContent: 'center' }}>
                  <Text style={{ color: '#fff', fontSize: 10, fontWeight: '800' }}>{unreadCount}</Text>
                </View>
              ) : null}
            </TouchableOpacity>
          ),
        })}
      />
      <Tab.Screen name="Projetos" component={ProjectsScreen} />
      <Tab.Screen name="Tarefas" component={TasksScreen} />
      <Tab.Screen name="Kanban" component={KanbanScreen} />
      <Tab.Screen name="Mais" component={MoreScreen} options={{ title: 'Mais' }} />
    </Tab.Navigator>
  );
}

export default function Navigation() {
  const { user } = useApp();
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerTitleStyle: { fontWeight: '700' } }}>
        {!user ? (
          <>
            <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
            <Stack.Screen name="Cadastro" component={RegisterScreen} options={{ title: 'Criar conta' }} />
          </>
        ) : (
          <>
            <Stack.Screen name="Main" component={Tabs} options={{ headerShown: false }} />
            <Stack.Screen name="ProjetoDetalhe" component={ProjectDetailScreen} options={{ title: 'Projeto' }} />
            <Stack.Screen name="NovoProjeto" component={NewProjectScreen} options={{ title: 'Novo projeto' }} />
            <Stack.Screen name="TarefaDetalhe" component={TaskDetailScreen} options={{ title: 'Tarefa' }} />
            <Stack.Screen name="NovaTarefa" component={NewTaskScreen} options={{ title: 'Nova tarefa' }} />
            <Stack.Screen name="Modelos" component={TemplatesScreen} options={{ title: 'Modelos' }} />
            <Stack.Screen name="Calendario" component={CalendarScreen} options={{ title: 'Calendário' }} />
            <Stack.Screen name="Notificacoes" component={NotificationsScreen} options={{ title: 'Notificações' }} />
            <Stack.Screen name="Historico" component={HistoryScreen} options={{ title: 'Histórico' }} />
            <Stack.Screen name="Usuarios" component={UsersScreen} options={{ title: 'Usuários' }} />
            <Stack.Screen name="Permissoes" component={PermissionsScreen} options={{ title: 'Níveis de acesso' }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
