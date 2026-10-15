import { useEffect, useState } from 'react'
import { ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import CampusScene from '../components/CampusScene'
import { Badge, Card, SectionTitle } from '../components/Ui'
import { colors } from '../theme'
import type { ScreenProps, TabKey } from '../types'
import { getDashboardOverview, getPlaces, type Place } from '../services/api'

const services: { icon: keyof typeof Ionicons.glyphMap; label: string; color: string; bg: string; tab?: TabKey }[] = [
  { icon: 'cube-outline', label: '3D校园', color: colors.primary, bg: colors.primarySoft, tab: 'campus' },
  { icon: 'scan-outline', label: '拍照问路', color: colors.teal, bg: colors.successSoft, tab: 'assistant' },
  { icon: 'checkbox-outline', label: '报到任务', color: colors.violet, bg: '#F0EDFF', tab: 'tasks' },
  { icon: 'walk-outline', label: '虚拟漫游', color: colors.orange, bg: colors.warningSoft, tab: 'campus' },
]

export default function HomeScreen({ goTo, notify }: ScreenProps) {
  const [query, setQuery] = useState('')
  const [taskProgress, setTaskProgress] = useState(0)
  const [taskCount, setTaskCount] = useState(0)
  const [backendOnline, setBackendOnline] = useState(false)
  const [recommendedPlace, setRecommendedPlace] = useState<Place | null>(null)
  const [recentKnowledge, setRecentKnowledge] = useState<string[]>([])
  useEffect(() => {
    Promise.all([getDashboardOverview(), getPlaces()]).then(([data, places]) => {
      if (data.tasks.length) setTaskProgress(Math.round(data.tasks.reduce((sum, item) => sum + item.progress, 0) / data.tasks.length))
      setTaskCount(data.tasks.length)
      setRecommendedPlace(places.find(item => item.status === 'OPEN') ?? places[0] ?? null)
      setRecentKnowledge(data.recentKnowledge.map(item => item.title))
      setBackendOnline(true)
    }).catch(() => setBackendOnline(false))
  }, [])
  const ask = (text = query) => {
    if (!text.trim()) return notify('先告诉AI你想去哪里或要办什么事')
    goTo('assistant')
  }

  return (
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
      <View style={styles.hero}>
        <View style={styles.orbA} /><View style={styles.orbB} />
        <View style={styles.topRow}>
          <View style={styles.userRow}><View style={styles.avatar}><Text style={styles.avatarText}>林</Text></View><View><Text style={styles.hello}>上午好，欢迎来到校园</Text><Text style={styles.name}>林知夏</Text></View></View>
          <View style={styles.weather}><Ionicons name="sunny" size={15} color="#FFD66B" /><Text style={styles.weatherText}>24℃ · 晴</Text></View>
        </View>
        <Text style={styles.heroTitle}>今天想去哪里？</Text>
        <Text style={styles.heroSub}>告诉 AI 你要办的事，不用知道具体地点</Text>
        <View style={styles.aiBox}>
          <View style={styles.aiHeading}><View style={styles.aiMark}><Text style={styles.aiMarkText}>AI</Text></View><Text style={styles.aiTitle}>校园任务智能体</Text><Badge tone="green">会规划 · 会导航</Badge></View>
          <View style={styles.searchRow}>
            <TextInput value={query} onChangeText={setQuery} placeholder="例：我想领校园卡并入住宿舍" placeholderTextColor="#8E9AB0" style={styles.input} returnKeyType="send" onSubmitEditing={() => ask()} />
            <TouchableOpacity style={styles.send} onPress={() => ask()}><Ionicons name="arrow-up" size={20} color="white" /></TouchableOpacity>
          </View>
          <View style={styles.promptRow}>
            <TouchableOpacity style={styles.prompt} onPress={() => ask('我刚到学校，先做什么？')}><Text style={styles.promptText}>刚到学校先做什么</Text></TouchableOpacity>
            <TouchableOpacity style={styles.prompt} onPress={() => ask('帮我规划今天的报到行程')}><Text style={styles.promptText}>规划报到行程</Text></TouchableOpacity>
          </View>
        </View>
      </View>

      <View style={styles.body}>
        <Card style={styles.progressCard}>
          <View style={styles.progressIcon}><Ionicons name="checkbox" size={23} color={colors.violet} /></View>
          <View style={{ flex: 1 }}><Text style={styles.cardTitle}>今日报到任务</Text><Text style={styles.cardSub}>{taskCount} 个流程节点 · {backendOnline ? '已与管理端同步' : '离线演示数据'}</Text></View>
          <View style={styles.progressText}><Text style={styles.progressNumber}>{taskProgress}%</Text><Text style={styles.progressLabel}>总体完成率</Text></View>
        </Card>

        <SectionTitle title="智能服务" action="全部服务  ›" onPress={() => notify('更多校园服务正在接入')} />
        <View style={styles.serviceRow}>{services.map(item => (
          <TouchableOpacity key={item.label} style={styles.service} onPress={() => item.tab && goTo(item.tab)} activeOpacity={0.72}>
            <View style={[styles.serviceIcon, { backgroundColor: item.bg }]}><Ionicons name={item.icon} size={23} color={item.color} /></View>
            <Text style={styles.serviceLabel}>{item.label}</Text>
          </TouchableOpacity>
        ))}</View>

        <SectionTitle title="推荐路线" action="打开3D校园  ›" onPress={() => goTo('campus')} />
        <Card style={styles.routeCard}>
          <CampusScene />
          <View style={styles.placeRow}>
            <View style={styles.placeIcon}><Ionicons name="card-outline" size={20} color={colors.primary} /></View>
            <View style={{ flex: 1 }}><Text style={styles.cardTitle}>{recommendedPlace?.name ?? '暂无推荐地点'}</Text><Text style={styles.cardSub}>{recommendedPlace ? `${recommendedPlace.building || '建筑未配置'} ${recommendedPlace.floor || ''} · ${recommendedPlace.status}` : '请先在管理端录入地点'}</Text></View>
            <Text style={styles.distance}>{backendOnline ? '数据库' : '离线'}</Text>
          </View>
        </Card>

        <SectionTitle title="知识库最近更新" action="刷新" onPress={() => notify('数据会随管理端知识库更新')} />
        <View style={styles.discoveryRow}>
          {(recentKnowledge.length ? recentKnowledge.slice(0, 2) : ['暂无真实知识更新']).map((title,index) => <Card key={title} style={styles.discovery}><Ionicons name={index ? 'navigate-circle-outline' : 'time-outline'} size={22} color={index ? colors.teal : colors.primary} /><Text style={styles.discoveryTitle}>{title}</Text><Text style={styles.discoveryText}>{backendOnline ? '来自管理端知识库' : '后端未连接'}</Text></Card>)}
        </View>
      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  content: { paddingBottom: 28, backgroundColor: colors.canvas },
  hero: { backgroundColor: colors.primaryDark, paddingHorizontal: 20, paddingTop: 16, paddingBottom: 62, overflow: 'hidden' },
  orbA: { position: 'absolute', width: 220, height: 220, borderRadius: 110, backgroundColor: 'rgba(72,118,255,.25)', right: -70, top: -90 },
  orbB: { position: 'absolute', width: 150, height: 150, borderRadius: 75, borderWidth: 24, borderColor: 'rgba(255,255,255,.05)', left: -78, bottom: -35 },
  topRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  userRow: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  avatar: { width: 38, height: 38, borderRadius: 13, backgroundColor: '#DCE6FF', alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.primaryDark, fontWeight: '900' },
  hello: { color: '#BFD0FF', fontSize: 10 },
  name: { color: 'white', fontSize: 14, fontWeight: '800', marginTop: 2 },
  weather: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 9, paddingVertical: 7, backgroundColor: 'rgba(255,255,255,.1)', borderRadius: 999 },
  weatherText: { color: 'white', fontSize: 10, fontWeight: '700' },
  heroTitle: { color: 'white', fontSize: 28, fontWeight: '900', marginTop: 27 },
  heroSub: { color: '#C8D5F9', fontSize: 12, marginTop: 6 },
  aiBox: { marginTop: 18, padding: 14, borderRadius: 20, backgroundColor: 'white' },
  aiHeading: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  aiMark: { width: 31, height: 31, borderRadius: 10, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  aiMarkText: { color: 'white', fontSize: 11, fontWeight: '900' },
  aiTitle: { flex: 1, color: colors.ink, fontSize: 14, fontWeight: '800' },
  searchRow: { marginTop: 12, height: 47, borderRadius: 14, backgroundColor: '#F2F5FA', paddingLeft: 13, paddingRight: 5, flexDirection: 'row', alignItems: 'center' },
  input: { flex: 1, color: colors.ink, fontSize: 12 },
  send: { width: 37, height: 37, borderRadius: 12, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
  promptRow: { flexDirection: 'row', gap: 7, marginTop: 9 },
  prompt: { paddingHorizontal: 10, paddingVertical: 7, borderRadius: 9, backgroundColor: colors.primarySoft },
  promptText: { color: colors.primary, fontSize: 10, fontWeight: '700' },
  body: { paddingHorizontal: 16, marginTop: -40, gap: 14 },
  progressCard: { padding: 14, flexDirection: 'row', alignItems: 'center', gap: 11 },
  progressIcon: { width: 42, height: 42, borderRadius: 13, backgroundColor: '#F0EDFF', alignItems: 'center', justifyContent: 'center' },
  cardTitle: { color: colors.ink, fontSize: 14, fontWeight: '800' },
  cardSub: { color: colors.muted, fontSize: 10, marginTop: 4 },
  progressText: { alignItems: 'flex-end' }, progressNumber: { color: colors.violet, fontSize: 18, fontWeight: '900' }, progressLabel: { color: colors.muted, fontSize: 9 },
  serviceRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  service: { width: '23%', alignItems: 'center', gap: 7 },
  serviceIcon: { width: 51, height: 51, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
  serviceLabel: { color: colors.text, fontSize: 11, fontWeight: '700' },
  routeCard: { padding: 7, overflow: 'hidden' },
  placeRow: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 10 },
  placeIcon: { width: 38, height: 38, borderRadius: 12, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  distance: { color: colors.primary, fontSize: 12, fontWeight: '800' },
  discoveryRow: { flexDirection: 'row', gap: 10 },
  discovery: { flex: 1, padding: 14, minHeight: 116 },
  discoveryTitle: { color: colors.ink, fontSize: 12, fontWeight: '800', marginTop: 9 },
  discoveryText: { color: colors.muted, fontSize: 10, lineHeight: 15, marginTop: 5 },
})
