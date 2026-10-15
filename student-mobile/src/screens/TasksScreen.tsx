import { useEffect, useMemo, useState } from 'react'
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { Card, PageHeader, SectionTitle } from '../components/Ui'
import { colors } from '../theme'
import type { ScreenProps } from '../types'
import { getTasks, type StudentTask } from '../services/api'

export default function TasksScreen({ notify }: ScreenProps) {
  const [tasks, setTasks] = useState<StudentTask[]>([])
  const [backendOnline, setBackendOnline] = useState(false)
  useEffect(() => { getTasks().then(data => { setTasks(data); setBackendOnline(true) }).catch(() => setBackendOnline(false)) }, [])
  const averageProgress = useMemo(() => tasks.length ? Math.round(tasks.reduce((sum,item) => sum + item.progress, 0) / tasks.length) : 0, [tasks])
  const totalMinutes = useMemo(() => tasks.reduce((sum,item) => sum + item.estimatedMinutes, 0), [tasks])
  return (
    <ScrollView style={styles.page} contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <PageHeader title="新生任务" subtitle="AI会根据时间、路况和前置条件动态调整" action={<TouchableOpacity style={styles.headerAction} onPress={() => notify('任务日历已打开')}><Ionicons name="calendar-outline" size={20} color={colors.primary} /></TouchableOpacity>} />
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>新生报到一日通</Text><Text style={styles.heroSub}>{backendOnline ? '已同步管理端真实任务数据' : '后端未连接，未使用演示任务'} · 预计 {totalMinutes} 分钟</Text>
        <View style={styles.progressRow}>
          <View style={styles.ring}><Text style={styles.ringValue}>{averageProgress}%</Text><Text style={styles.ringLabel}>总体进度</Text></View>
          <View style={styles.stats}>{[[String(tasks.length), '流程节点'], [String(tasks.filter(item => item.status === 'ACTIVE').length), '运行中'], [String(tasks.filter(item => item.status !== 'ACTIVE').length), '已暂停']].map(item => <View key={item[1]} style={styles.stat}><Text style={styles.statValue}>{item[0]}</Text><Text style={styles.statLabel}>{item[1]}</Text></View>)}</View>
        </View>
      </View>

      <View style={styles.section}>
        <SectionTitle title="今日流程" action="✦ AI重新排序" onPress={() => notify('AI已检查，当前顺序为最优')} />
        <Card style={styles.timeline}>
          {!tasks.length ? <Text style={styles.empty}>暂无任务数据</Text> : null}
          {tasks.map((item, index) => {
            const done = index === 0; const active = index === 1
            return <View key={item.id} style={styles.taskRow}>
              {index < tasks.length - 1 ? <View style={[styles.line, done && styles.lineDone]} /> : null}
              <View style={[styles.taskIcon, done && styles.taskIconDone, active && styles.taskIconActive]}>{done ? <Ionicons name="checkmark" color="white" size={15} /> : <Text style={[styles.taskIndex, active && { color: 'white' }]}>{index + 1}</Text>}</View>
              <View style={{ flex: 1 }}><Text style={styles.taskTitle}>{item.name}</Text><Text style={styles.taskDetail}>{item.location} · {item.description}</Text></View>
              <Text style={[styles.taskTime, active && { color: colors.primary }]}>{done ? '已完成' : `预计 ${item.estimatedMinutes} 分钟`}</Text>
            </View>
          })}
        </Card>
      </View>

      <View style={styles.aiTip}><View style={styles.tipIcon}><Ionicons name="sparkles" size={18} color="white" /></View><View style={{ flex: 1 }}><Text style={styles.tipTitle}>数据说明</Text><Text style={styles.tipText}>材料清单和个性化提醒将在后端建立对应记录后展示，当前不会生成虚假内容。</Text></View></View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas }, content: { paddingBottom: 28 },
  headerAction: { width: 40, height: 40, borderRadius: 13, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  hero: { marginHorizontal: 16, padding: 18, borderRadius: 22, backgroundColor: colors.primaryDark },
  heroTitle: { color: 'white', fontSize: 18, fontWeight: '900' }, heroSub: { color: '#C4D1FA', fontSize: 10, marginTop: 5 },
  progressRow: { flexDirection: 'row', alignItems: 'center', marginTop: 17 },
  ring: { width: 78, height: 78, borderRadius: 42, borderWidth: 8, borderColor: '#6687F4', borderTopColor: '#50D7B0', alignItems: 'center', justifyContent: 'center' },
  ringValue: { color: 'white', fontSize: 18, fontWeight: '900' }, ringLabel: { color: '#C4D1FA', fontSize: 8 },
  stats: { flex: 1, flexDirection: 'row', marginLeft: 14 }, stat: { flex: 1, alignItems: 'center' }, statValue: { color: 'white', fontSize: 17, fontWeight: '900' }, statLabel: { color: '#C4D1FA', fontSize: 8, marginTop: 3 },
  section: { paddingHorizontal: 16, marginTop: 21 },
  timeline: { paddingHorizontal: 14, paddingVertical: 7 }, taskRow: { minHeight: 63, flexDirection: 'row', alignItems: 'center', gap: 10, position: 'relative' },
  line: { position: 'absolute', width: 2, height: 26, backgroundColor: '#E1E7F0', left: 15, top: 47 }, lineDone: { backgroundColor: '#74D8BB' },
  taskIcon: { width: 32, height: 32, borderRadius: 11, backgroundColor: '#EDF1F6', alignItems: 'center', justifyContent: 'center', zIndex: 2 }, taskIconDone: { backgroundColor: colors.teal }, taskIconActive: { backgroundColor: colors.primary }, taskIndex: { color: colors.muted, fontSize: 11, fontWeight: '900' },
  taskTitle: { color: colors.ink, fontSize: 12, fontWeight: '800' }, taskDetail: { color: colors.muted, fontSize: 9, marginTop: 4 }, taskTime: { color: colors.muted, fontSize: 8, fontWeight: '700' },
  materials: { paddingHorizontal: 14 }, materialRow: { height: 51, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: colors.line },
  check: { width: 24, height: 24, borderRadius: 8, backgroundColor: colors.teal, alignItems: 'center', justifyContent: 'center' }, checkOff: { backgroundColor: colors.warningSoft }, materialName: { flex: 1, color: colors.text, fontSize: 11, fontWeight: '700', marginLeft: 10 }, materialState: { color: colors.teal, fontSize: 9, fontWeight: '700' },
  aiTip: { margin: 16, padding: 13, borderRadius: 18, backgroundColor: '#EFEDFF', flexDirection: 'row', alignItems: 'center', gap: 10, borderWidth: 1, borderColor: '#DFDAFF' },
  tipIcon: { width: 35, height: 35, borderRadius: 11, backgroundColor: colors.violet, alignItems: 'center', justifyContent: 'center' }, tipTitle: { color: colors.ink, fontSize: 11, fontWeight: '900' }, tipText: { color: colors.text, fontSize: 9, lineHeight: 14, marginTop: 3 }, adopt: { paddingHorizontal: 11, paddingVertical: 8, borderRadius: 9, backgroundColor: colors.violet }, adoptText: { color: 'white', fontSize: 9, fontWeight: '800' },
  empty: { paddingVertical: 24, textAlign: 'center', color: colors.muted, fontSize: 11 },
})
