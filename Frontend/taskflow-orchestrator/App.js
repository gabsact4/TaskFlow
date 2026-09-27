import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { AppProvider } from './src/context/AppContext';
import Navigation from './src/navigation';

export default function App() {
  return (
    <AppProvider>
      <StatusBar style="dark" />
      <Navigation />
    </AppProvider>
  );
}
