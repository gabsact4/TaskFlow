import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const SESSION_KEY = 'taskflow.session.token.v1';
const SESSION_USER_KEY = 'taskflow.session.user.v1';
const dataKey = (userId) => `taskflow.offline.data.${userId}.v1`;
const queueKey = (userId) => `taskflow.offline.queue.${userId}.v1`;

export async function persistSession(token, user) {
  if (!token) return clearAccessToken();
  const cachedUser = user ? { id: user.id, name: user.name, email: user.email, role: user.role } : null;
  if (Platform.OS === 'web') {
    if (typeof localStorage !== 'undefined') localStorage.setItem(SESSION_KEY, token);
    if (typeof localStorage !== 'undefined') localStorage.setItem(SESSION_USER_KEY, JSON.stringify(cachedUser));
    return;
  }
  await SecureStore.setItemAsync(SESSION_KEY, token, { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY });
  await SecureStore.setItemAsync(SESSION_USER_KEY, JSON.stringify(cachedUser), { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY });
}

export async function restoreSession() {
  let token; let user;
  if (Platform.OS === 'web') {
    token = typeof localStorage !== 'undefined' ? localStorage.getItem(SESSION_KEY) : null;
    try { user = JSON.parse(localStorage.getItem(SESSION_USER_KEY) || 'null'); } catch { user = null; }
  } else {
    token = await SecureStore.getItemAsync(SESSION_KEY);
    try { user = JSON.parse((await SecureStore.getItemAsync(SESSION_USER_KEY)) || 'null'); } catch { user = null; }
  }
  return token && user ? { accessToken: token, user } : null;
}

export async function clearAccessToken() {
  if (Platform.OS === 'web') {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(SESSION_KEY);
    if (typeof localStorage !== 'undefined') localStorage.removeItem(SESSION_USER_KEY);
    return;
  }
  await SecureStore.deleteItemAsync(SESSION_KEY);
  await SecureStore.deleteItemAsync(SESSION_USER_KEY);
}

export async function loadOfflineData(userId) {
  try { const raw = await AsyncStorage.getItem(dataKey(userId)); return raw ? JSON.parse(raw) : null; }
  catch { return null; }
}

export async function saveOfflineData(userId, snapshot) {
  await AsyncStorage.setItem(dataKey(userId), JSON.stringify(snapshot));
}

export async function loadOfflineQueue(userId) {
  try { const raw = await AsyncStorage.getItem(queueKey(userId)); return raw ? JSON.parse(raw) : []; }
  catch { return []; }
}

export async function saveOfflineQueue(userId, queue) {
  await AsyncStorage.setItem(queueKey(userId), JSON.stringify(queue));
}

export async function clearOfflineData(userId) {
  await AsyncStorage.multiRemove([dataKey(userId), queueKey(userId)]);
}
