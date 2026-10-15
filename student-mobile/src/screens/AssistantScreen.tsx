import { useEffect, useState } from 'react'
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { Badge, Card, PrimaryButton } from '../components/Ui'
import { colors } from '../theme'
import type { ScreenProps } from '../types'
import { analyzeOperation } from '../services/api'

const plan = [
  { title: '学院报到', detail: '综合楼 A201 · 需录取通知书', time: '20分钟' },
  { title: '领取校园卡', detail: '综合服务中心 1 楼', time: '25分钟' },
  { title: '宿舍入住', detail: '学生公寓 A 区 · 领取钥匙', time: '35分钟' },
]

export default function AssistantScreen({ goTo, notify }: ScreenProps) {
  const [input, setInput] = useState('')
  const [question, setQuestion] = useState('我刚到学校，想先报到、领校园卡，然后去宿舍')
  const [answer, setAnswer] = useState('我已经理解你的目标。建议按“学院报到 → 校园卡 → 宿舍入住”的顺序办理，三个地点目前都在开放时间内。')
  const [confidence, setConfidence] = useState(96)
  const [online, setOnline] = useState(false)
  const [loading, setLoading] = useState(false)

  const requestAnalysis = async (prompt: string, showToast = true) => {
    setLoading(true)
    try {
      const result = await analyzeOperation(prompt)
      setAnswer(`${result.title}。${result.summary}`)
      setConfidence(Math.round(result.confidence * 100))
      setOnline(true)
      if (showToast) notify('AI已根据后端路网和知识库完成分析')
    } catch {
      setOnline(false)
      if (showToast) notify('后端暂未连接，当前显示本地演示方案')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void requestAnalysis(question, false)
  }, [])

  const send = (value = input) => {
    if (!value.trim()) return
    setQuestion(value)
    setInput('')
    void requestAnalysis(value)
  }

  return (
    <KeyboardAvoidingView style={styles.page} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={82}>
      <View style={styles.header}>
        <View style={styles.aiLogo}><Text style={styles.aiLogoText}>AI</Text></View>
        <View style={{ flex: 1 }}><Text style={styles.title}>校园任务智能体</Text><Text style={styles.subtitle}>理解任务，并安排地点、材料和路线</Text></View>
        <TouchableOpacity style={styles.more} onPress={() => notify('智能体设置已打开')}><Ionicons name="ellipsis-horizontal" size={20} color="white" /></TouchableOpacity>
        <View style={styles.state}><View style={[styles.stateDot, !online && styles.stateDotOffline]} /><Text style={styles.stateText}>{online ? '已连接后端知识库、任务系统与路网引擎' : '正在连接校园智能服务'}</Text></View>
      </View>
      <ScrollView style={styles.chat} contentContainerStyle={styles.chatContent} showsVerticalScrollIndicator={false}>
        <Text style={styles.date}>今天 09:41</Text>
        <View style={styles.userRow}><View style={styles.userBubble}><Text style={styles.userText}>{question}</Text></View></View>
        <View style={styles.aiRow}><View style={styles.chatAvatar}><Text style={styles.chatAvatarText}>AI</Text></View><View style={styles.aiBubble}><Text style={styles.aiText}>{loading ? '正在联合分析任务、路网与知识证据…' : answer}</Text></View></View>
        <Card style={styles.planCard}>
          <View style={styles.planHead}><Text style={styles.planTitle}>AI生成的报到行程</Text><Badge tone="green">置信度 {confidence}%</Badge></View>
          {plan.map((item, index) => <View key={item.title} style={styles.planStep}><View style={styles.stepNumber}><Text style={styles.stepNumberText}>{index + 1}</Text></View><View style={{ flex: 1 }}><Text style={styles.stepTitle}>{item.title}</Text><Text style={styles.stepDetail}>{item.detail}</Text></View><Text style={styles.stepTime}>{item.time}</Text></View>)}
          <View style={styles.evidence}><Ionicons name="shield-checkmark-outline" size={15} color={colors.primary} /><Text style={styles.evidenceText}>依据：新生入学指南、实时开放时间、当前导航路网</Text></View>
          <PrimaryButton label="查看3D任务路线" icon="map-outline" onPress={() => goTo('campus')} />
        </Card>
        <View style={styles.aiRow}><View style={styles.chatAvatar}><Text style={styles.chatAvatarText}>AI</Text></View><View style={styles.aiBubble}><Text style={styles.aiText}>请记得携带身份证和录取通知书。如果办事点排队或道路关闭，我会自动重新排序。</Text></View></View>
      </ScrollView>
      <View style={styles.composer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.suggestions}>
          {['我缺少什么材料？', '哪个地点快关门了？', '帮我重新排序'].map(item => <TouchableOpacity key={item} style={styles.suggestion} onPress={() => send(item)}><Text style={styles.suggestionText}>{item}</Text></TouchableOpacity>)}
        </ScrollView>
        <View style={styles.inputRow}>
          <TouchableOpacity onPress={() => notify('可以接入相册、相机和文件识别')}><Ionicons name="add-circle-outline" size={24} color={colors.text} /></TouchableOpacity>
          <TextInput value={input} onChangeText={setInput} style={styles.input} placeholder="继续询问或上传照片…" placeholderTextColor={colors.muted} returnKeyType="send" onSubmitEditing={() => send()} />
          <TouchableOpacity onPress={() => notify('语音输入已开启')}><Ionicons name="mic-outline" size={22} color={colors.violet} /></TouchableOpacity>
          <TouchableOpacity style={styles.send} onPress={() => send()}><Ionicons name="arrow-up" size={19} color="white" /></TouchableOpacity>
        </View>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas },
  header: { paddingHorizontal: 18, paddingTop: 13, paddingBottom: 15, backgroundColor: colors.primaryDark, flexDirection: 'row', alignItems: 'center', gap: 10, flexWrap: 'wrap' },
  aiLogo: { width: 42, height: 42, borderRadius: 14, alignItems: 'center', justifyContent: 'center', backgroundColor: '#577CF5' },
  aiLogoText: { color: 'white', fontSize: 13, fontWeight: '900' },
  title: { color: 'white', fontSize: 18, fontWeight: '900' },
  subtitle: { color: '#BED0FF', fontSize: 9, marginTop: 3 },
  more: { width: 37, height: 37, borderRadius: 12, backgroundColor: 'rgba(255,255,255,.12)', alignItems: 'center', justifyContent: 'center' },
  state: { width: '100%', flexDirection: 'row', alignItems: 'center', gap: 6, marginLeft: 53 },
  stateDot: { width: 6, height: 6, borderRadius: 4, backgroundColor: '#4DE2B5' },
  stateDotOffline: { backgroundColor: '#FFC56F' },
  stateText: { color: '#BED0FF', fontSize: 9 },
  chat: { flex: 1 },
  chatContent: { padding: 15, gap: 13 },
  date: { alignSelf: 'center', color: colors.muted, fontSize: 9 },
  userRow: { alignItems: 'flex-end' },
  userBubble: { maxWidth: '82%', paddingHorizontal: 13, paddingVertical: 10, backgroundColor: colors.primary, borderRadius: 16, borderBottomRightRadius: 5 },
  userText: { color: 'white', fontSize: 12, lineHeight: 18 },
  aiRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  chatAvatar: { width: 30, height: 30, borderRadius: 10, backgroundColor: colors.violet, alignItems: 'center', justifyContent: 'center' },
  chatAvatarText: { color: 'white', fontSize: 9, fontWeight: '900' },
  aiBubble: { maxWidth: '82%', paddingHorizontal: 13, paddingVertical: 11, backgroundColor: 'white', borderRadius: 16, borderTopLeftRadius: 5, borderWidth: 1, borderColor: colors.line },
  aiText: { color: colors.text, fontSize: 12, lineHeight: 19 },
  planCard: { marginLeft: 38, padding: 14, gap: 11 },
  planHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  planTitle: { color: colors.ink, fontSize: 14, fontWeight: '900' },
  planStep: { flexDirection: 'row', alignItems: 'center', gap: 9, paddingVertical: 4 },
  stepNumber: { width: 28, height: 28, borderRadius: 10, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  stepNumberText: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  stepTitle: { color: colors.ink, fontSize: 11, fontWeight: '800' },
  stepDetail: { color: colors.muted, fontSize: 9, marginTop: 3 },
  stepTime: { color: colors.text, fontSize: 9, fontWeight: '700' },
  evidence: { padding: 9, borderRadius: 11, backgroundColor: colors.primarySoft, flexDirection: 'row', gap: 6, alignItems: 'center' },
  evidenceText: { flex: 1, color: colors.primary, fontSize: 9, lineHeight: 14 },
  composer: { paddingTop: 8, paddingHorizontal: 12, paddingBottom: 8, backgroundColor: 'white', borderTopWidth: 1, borderTopColor: colors.line },
  suggestions: { gap: 7, paddingBottom: 8 },
  suggestion: { paddingHorizontal: 10, paddingVertical: 7, borderRadius: 10, backgroundColor: colors.primarySoft },
  suggestionText: { color: colors.primary, fontSize: 9, fontWeight: '700' },
  inputRow: { height: 48, borderRadius: 15, backgroundColor: '#F2F5FA', flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 10 },
  input: { flex: 1, color: colors.ink, fontSize: 11 },
  send: { width: 34, height: 34, borderRadius: 11, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
})
