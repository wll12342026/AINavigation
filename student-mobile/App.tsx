import { useEffect, useMemo, useState } from 'react'
import { SafeAreaView, StatusBar as NativeStatusBar, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { StatusBar } from 'expo-status-bar'
import { Ionicons } from '@expo/vector-icons'
import HomeScreen from './src/screens/HomeScreen'
import CampusScreen from './src/screens/CampusScreen'
import AssistantScreen from './src/screens/AssistantScreen'
import TasksScreen from './src/screens/TasksScreen'
import ProfileScreen from './src/screens/ProfileScreen'
import { colors, shadow } from './src/theme'
import type { TabKey } from './src/types'

const tabs: { key: TabKey; label: string; icon: keyof typeof Ionicons.glyphMap; activeIcon: keyof typeof Ionicons.glyphMap }[] = [
  { key: 'home', label: '首页', icon: 'home-outline', activeIcon: 'home' },
  { key: 'campus', label: '3D校园', icon: 'map-outline', activeIcon: 'map' },
  { key: 'assistant', label: '问AI', icon: 'sparkles-outline', activeIcon: 'sparkles' },
  { key: 'tasks', label: '任务', icon: 'checkbox-outline', activeIcon: 'checkbox' },
  { key: 'profile', label: '我的', icon: 'person-outline', activeIcon: 'person' },
]

export default function App() {
  const [active, setActive] = useState<TabKey>('home')
  const [toast, setToast] = useState('')
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(''), 2200)
    return () => clearTimeout(timer)
  }, [toast])

  const screen = useMemo(() => {
    const props = { goTo: setActive, notify: setToast }
    if (active === 'campus') return <CampusScreen {...props} />
    if (active === 'assistant') return <AssistantScreen {...props} />
    if (active === 'tasks') return <TasksScreen {...props} />
    if (active === 'profile') return <ProfileScreen {...props} />
    return <HomeScreen {...props} />
  }, [active])

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style={active === 'tasks' ? 'dark' : 'light'} />
      <NativeStatusBar backgroundColor={active === 'tasks' ? colors.canvas : colors.primaryDark} barStyle={active === 'tasks' ? 'dark-content' : 'light-content'} />
      <View style={styles.screen}>{screen}</View>
      <View style={styles.tabbar}>
        {tabs.map(tab => {
          const selected = active === tab.key
          const ai = tab.key === 'assistant'
          return (
            <TouchableOpacity key={tab.key} style={styles.tab} onPress={() => setActive(tab.key)} activeOpacity={0.75}>
              <View style={[styles.iconWrap, ai && styles.aiIconWrap, selected && !ai && styles.iconActive]}>
                <Ionicons name={selected ? tab.activeIcon : tab.icon} size={ai ? 23 : 21} color={ai ? 'white' : selected ? colors.primary : '#8996A9'} />
              </View>
              <Text style={[styles.tabLabel, selected && styles.tabLabelActive]}>{tab.label}</Text>
            </TouchableOpacity>
          )
        })}
      </View>
      {toast ? <View style={styles.toast}><Ionicons name="checkmark-circle" size={17} color="#54DAB2" /><Text style={styles.toastText}>{toast}</Text></View> : null}
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.primaryDark, paddingTop: NativeStatusBar.currentHeight ?? 0 },
  screen: { flex: 1, backgroundColor: colors.canvas },
  tabbar: { height: 68, flexDirection: 'row', alignItems: 'center', backgroundColor: 'white', borderTopWidth: 1, borderTopColor: colors.line, paddingHorizontal: 5, paddingBottom: 4 },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 2 },
  iconWrap: { width: 39, height: 31, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  iconActive: { backgroundColor: colors.primarySoft },
  aiIconWrap: { width: 45, height: 45, borderRadius: 16, backgroundColor: colors.violet, marginTop: -19, ...shadow },
  tabLabel: { color: '#8996A9', fontSize: 9, fontWeight: '700' },
  tabLabelActive: { color: colors.primary, fontWeight: '900' },
  toast: { position: 'absolute', left: 25, right: 25, bottom: 83, minHeight: 44, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7, backgroundColor: 'rgba(23,35,56,.96)', borderRadius: 14, ...shadow },
  toastText: { color: 'white', fontSize: 11, fontWeight: '700' },
})
