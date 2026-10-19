import { useState } from 'react'
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import * as ImagePicker from 'expo-image-picker'
import { Badge, Card, PrimaryButton } from '../components/Ui'
import { colors } from '../theme'
import type { ScreenProps } from '../types'
import { analyzeOperation, recognizeCampusPhoto, uploadCampusPhoto, type RecognitionResult, type AiAnalysis } from '../services/api'

export default function AssistantScreen({ goTo, notify }: ScreenProps) {
  const [input, setInput] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('正在等待后端基于真实地点、路网、任务和知识库生成回答。')
  const [confidence, setConfidence] = useState(0)
  const [steps, setSteps] = useState<string[]>([])
  const [objects, setObjects] = useState<string[]>([])
  const [online, setOnline] = useState(false)
  const [loading, setLoading] = useState(false)
  const [recognition, setRecognition] = useState<RecognitionResult | null>(null)
  const [pickedImage, setPickedImage] = useState<ImagePicker.ImagePickerAsset | null>(null)
  const [visionLoading, setVisionLoading] = useState(false)
  const [sources, setSources] = useState<NonNullable<AiAnalysis['sources']>>([])
  const [routeResult, setRouteResult] = useState<AiAnalysis['route']>()

  const requestAnalysis = async (prompt: string, showToast = true) => {
    setLoading(true)
    setSources([]); setRouteResult(undefined)
    try {
      const result = await analyzeOperation(prompt)
      setAnswer(`${result.title}。${result.summary}`)
      setConfidence(Math.round(result.confidence * 100))
      setSteps(result.reasoningSteps)
      setObjects(result.affectedObjects)
      setSources(result.sources ?? [])
      setRouteResult(result.route)
      setOnline(true)
      if (showToast) notify('AI已根据后端路网和知识库完成分析')
    } catch (error) {
      setOnline(false)
      setAnswer('后端未连接，当前不显示本地演示答案。')
      setConfidence(0); setSteps([]); setObjects([])
      if (showToast) notify(error instanceof Error ? error.message : '校园服务连接失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const send = (value = input) => {
    if (!value.trim()) return
    setQuestion(value)
    setInput('')
    void requestAnalysis(value)
  }

  const pickAndRecognize = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync()
    if (!permission.granted) return notify('需要相册权限才能识别校园照片')
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.9 })
    if (result.canceled || !result.assets[0]) return
    const asset = result.assets[0]
    setPickedImage(asset); setRecognition(null); setVisionLoading(true)
    try {
      const matched = await recognizeCampusPhoto(asset)
      setRecognition(matched)
      notify(matched.sufficientEvidence ? '已匹配到可信校园地点' : '图库证据不足，已显示候选结果')
    } catch (error) {
      notify(error instanceof Error ? error.message : '图片识别失败')
    } finally { setVisionLoading(false) }
  }

  const saveAsReference = async () => {
    const candidate = recognition?.candidates[0]
    if (!pickedImage || !candidate) return notify('当前没有可绑定的地点候选')
    setVisionLoading(true)
    try {
      await uploadCampusPhoto(pickedImage, `手机采集-${candidate.placeName}`, candidate.placeId)
      notify('照片已上传、质检并加入地点素材库')
    } catch (error) {
      notify(error instanceof Error ? error.message : '素材上传失败')
    } finally { setVisionLoading(false) }
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
        <Text style={styles.date}>校园资料检索 · 当前为规则检索版本</Text>
        {question ? <View style={styles.userRow}><View style={styles.userBubble}><Text style={styles.userText}>{question}</Text></View></View> : null}
        <View style={styles.aiRow}><View style={styles.chatAvatar}><Text style={styles.chatAvatarText}>AI</Text></View><View style={styles.aiBubble}><Text style={styles.aiText}>{loading ? '正在联合分析任务、路网与知识证据…' : answer}</Text></View></View>
        <Card style={styles.planCard}>
          <View style={styles.planHead}><Text style={styles.planTitle}>数据库证据链</Text><Badge tone={online ? 'green' : 'orange'}>检索评分 {confidence}%</Badge></View>
          {!steps.length ? <Text style={styles.stepDetail}>暂无可核验步骤</Text> : steps.map((item, index) => <View key={`${item}-${index}`} style={styles.planStep}><View style={styles.stepNumber}><Text style={styles.stepNumberText}>{index + 1}</Text></View><View style={{ flex: 1 }}><Text style={styles.stepTitle}>{item}</Text><Text style={styles.stepDetail}>{objects[index] ?? '后端数据核验'}</Text></View></View>)}
          <View style={styles.evidence}><Ionicons name="shield-checkmark-outline" size={15} color={colors.primary} /><Text style={styles.evidenceText}>{online ? '依据：当前 MySQL 地点、路网、任务和知识记录' : '后端离线，未使用预设答案'}</Text></View>
          <PrimaryButton label="查看校园数据" icon="map-outline" onPress={() => goTo('campus')} />
        </Card>
        {routeResult ? <Card style={styles.planCard}>
          <Text style={styles.planTitle}>{routeResult.origin} → {routeResult.destination}</Text>
          <Text style={styles.stepTitle}>{routeResult.distanceMeters}米 · 预计{routeResult.estimatedMinutes}分钟</Text>
          {routeResult.steps.map((step, index) => <Text key={`${step.roadId}-${index}`} style={styles.aiText}>{index + 1}. {step.roadName}：{step.fromNode} → {step.toNode}，{step.lengthMeters}米</Text>)}
          {routeResult.evidence.map(item => <Text key={item} style={styles.stepDetail}>{item}</Text>)}
        </Card> : null}
        {sources.map(source => <Card key={source.id} style={styles.planCard}>
          <Text style={styles.planTitle}>{source.title}</Text>
          <Text style={styles.aiText}>{source.content}</Text>
          <Text style={styles.stepDetail}>来源：{source.source || '待补充'}</Text>
          <Text style={styles.stepDetail}>更新：{source.updatedAt?.replace('T', ' ').slice(0, 16) || '未记录'} · 有效期：{source.expiresAt || '未设置'}</Text>
        </Card>)}
        {pickedImage ? <Card style={styles.visionCard}>
          <View style={styles.visionHead}><View style={styles.visionIcon}><Ionicons name="scan" size={18} color={colors.violet} /></View><View style={{ flex: 1 }}><Text style={styles.planTitle}>校园视觉识别</Text><Text style={styles.stepDetail}>{visionLoading ? '正在提取视觉特征并检索地点图库…' : recognition?.message ?? '等待识别结果'}</Text></View></View>
          {recognition?.candidates.map(item => <View key={item.placeId} style={styles.candidate}><View style={{ flex: 1 }}><Text style={styles.stepTitle}>{item.placeName}</Text><Text style={styles.stepDetail}>{item.evidence}</Text></View><Text style={styles.confidence}>{Math.round(item.confidence * 100)}%</Text></View>)}
          {recognition?.candidates[0] ? <PrimaryButton label={visionLoading ? '处理中…' : '确认候选并加入地点图库'} icon="cloud-upload-outline" onPress={saveAsReference} /> : null}
        </Card> : null}
      </ScrollView>
      <View style={styles.composer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.suggestions}>
          {['我缺少什么材料？', '哪个地点快关门了？', '帮我重新排序'].map(item => <TouchableOpacity key={item} style={styles.suggestion} onPress={() => send(item)}><Text style={styles.suggestionText}>{item}</Text></TouchableOpacity>)}
        </ScrollView>
        <View style={styles.inputRow}>
          <TouchableOpacity disabled={visionLoading} onPress={pickAndRecognize}><Ionicons name="camera-outline" size={24} color={colors.text} /></TouchableOpacity>
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
  visionCard: { marginLeft: 38, padding: 14, gap: 9 },
  visionHead: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  visionIcon: { width: 34, height: 34, borderRadius: 11, backgroundColor: '#F0ECFF', alignItems: 'center', justifyContent: 'center' },
  candidate: { padding: 10, borderRadius: 11, backgroundColor: '#F5F7FB', flexDirection: 'row', alignItems: 'center', gap: 8 },
  confidence: { color: colors.violet, fontSize: 13, fontWeight: '900' },
})
