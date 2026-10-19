import { useEffect, useMemo, useState } from 'react'
import { Image, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import RoadTopology from '../components/RoadTopology'
import { PrimaryButton } from '../components/Ui'
import { colors, shadow } from '../theme'
import type { ScreenProps } from '../types'
import { getMedia, getPlaces, getRoads, planAiRoute, planRoute, resolveMediaUrl, type MediaAsset, type Place, type RoadSegment, type RoutePlan } from '../services/api'

const cleanName = (value: string) => value.replace(/^【[^】]+】\s*/, '')

export default function CampusScreen({ notify }: ScreenProps) {
  const [places, setPlaces] = useState<Place[]>([])
  const [roads, setRoads] = useState<RoadSegment[]>([])
  const [media, setMedia] = useState<MediaAsset[]>([])
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [accessibleOnly, setAccessibleOnly] = useState(false)
  const [avoidRestricted, setAvoidRestricted] = useState(true)
  const [optimization, setOptimization] = useState('DISTANCE')
  const [query, setQuery] = useState('')
  const [plan, setPlan] = useState<RoutePlan | null>(null)
  const [online, setOnline] = useState(false)
  const [planning, setPlanning] = useState(false)

  const refreshData = () => {
    setPlan(null)
    return Promise.all([getPlaces(), getRoads(), getMedia()])
      .then(([placeRows, roadRows, mediaRows]) => {
        setPlaces(placeRows); setRoads(roadRows); setMedia(mediaRows); setOnline(true)
      })
      .catch(() => { setOnline(false); setPlaces([]); setRoads([]); setMedia([]); notify('数据刷新失败，请检查网络后重试') })
  }
  useEffect(() => { void refreshData() }, [])

  const nodes = useMemo(() => Array.from(new Set(roads.flatMap(item => [item.fromNode, item.toNode]).filter(Boolean))), [roads])
  useEffect(() => {
    if (!origin && nodes.length) setOrigin(nodes[0] ?? '')
    if (!destination && nodes.length > 1) setDestination(nodes[nodes.length - 1] ?? '')
  }, [nodes, origin, destination])

  const destinationMedia = useMemo(() => media.find(item => item.placeName === plan?.destination && item.url && item.reviewStatus === 'APPROVED'), [media, plan])
  const destinationPlace = places.find(item => item.name === plan?.destination)

  const calculate = async () => {
    if (planning) return
    if (!origin || !destination) return notify('请选择起点和终点')
    setPlanning(true)
    setPlan(null)
    try {
      const result = await planRoute(origin, destination, accessibleOnly, avoidRestricted, optimization)
      setPlan(result); setOnline(true); notify('路线已由后端实时计算')
    } catch (error) {
      notify(error instanceof Error ? error.message : '路线规划失败')
    } finally { setPlanning(false) }
  }

  const calculateByAi = async () => {
    if (planning) return
    if (!query.trim()) return notify('请说出起点和终点')
    setPlanning(true)
    setPlan(null)
    try {
      const result = await planAiRoute(query, origin || undefined)
      setPlan(result); setOrigin(result.origin); setDestination(result.destination)
      setAccessibleOnly(result.accessibleOnly); setAvoidRestricted(result.avoidRestricted)
      setOptimization(result.optimization ?? 'DISTANCE')
      setOnline(true); notify('AI已理解需求并调用真实路网')
    } catch (error) {
      notify(error instanceof Error ? error.message : 'AI未能完成路线规划')
    } finally { setPlanning(false) }
  }

  return (
    <View style={styles.page}>
      <View style={styles.mapArea}>
        <RoadTopology roads={roads} plan={plan} />
        <View style={styles.search}>
          <Ionicons name="sparkles" size={17} color={colors.violet} />
          <TextInput value={query} onChangeText={setQuery} style={styles.searchInput} placeholder="例如：从东门去图书馆，避开施工" placeholderTextColor={colors.muted} returnKeyType="go" onSubmitEditing={calculateByAi} />
          <TouchableOpacity disabled={planning} onPress={calculateByAi}><Ionicons name="arrow-up-circle" size={24} color={colors.primary} /></TouchableOpacity>
        </View>
        {plan ? <View style={styles.routeBadge}><Ionicons name="navigate" size={14} color="white" /><Text style={styles.routeBadgeText}>{plan.distanceMeters}m · {plan.estimatedMinutes}分钟</Text></View> : null}
      </View>

      <View style={styles.sheet}>
        <View style={styles.handle} />
        <View style={styles.sheetTitle}><Text style={styles.title}>AI实时路线规划</Text><View style={styles.verified}><Ionicons name="server-outline" size={13} color={online ? colors.teal : colors.orange} /><Text style={[styles.verifiedText, !online && { color: colors.orange }]}>{online ? '后端路网在线' : '后端离线'}</Text></View></View>
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.sheetContent}>
          <TouchableOpacity disabled={planning} onPress={() => { void refreshData() }}><Text style={styles.fieldLabel}>↻ 刷新地点、道路状态和实景资料</Text></TouchableOpacity>
          <Text style={styles.fieldLabel}>选择起点</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
            {nodes.map(node => <TouchableOpacity key={`o-${node}`} disabled={planning} style={[styles.chip, origin === node && styles.chipActive]} onPress={() => { setOrigin(node); setPlan(null) }}><Text style={[styles.chipText, origin === node && styles.chipTextActive]}>{cleanName(node)}</Text></TouchableOpacity>)}
          </ScrollView>
          <Text style={styles.fieldLabel}>选择终点</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
            {nodes.map(node => <TouchableOpacity key={`d-${node}`} disabled={planning} style={[styles.chip, destination === node && styles.chipDestination]} onPress={() => { setDestination(node); setPlan(null) }}><Text style={[styles.chipText, destination === node && styles.chipTextActive]}>{cleanName(node)}</Text></TouchableOpacity>)}
          </ScrollView>
          <View style={styles.options}>
            <TouchableOpacity style={styles.option} onPress={() => { setOptimization(value => value === 'TIME' ? 'DISTANCE' : 'TIME'); setPlan(null) }}><Ionicons name="time-outline" size={16} color={colors.primary} /><Text style={styles.optionText}>{optimization === 'TIME' ? '用时最少' : '距离最短'}</Text></TouchableOpacity>
            <TouchableOpacity disabled={planning} style={[styles.option, accessibleOnly && styles.optionActive]} onPress={() => { setAccessibleOnly(value => !value); setPlan(null) }}><Ionicons name="accessibility" size={16} color={accessibleOnly ? colors.primary : colors.text} /><Text style={[styles.optionText, accessibleOnly && styles.optionTextActive]}>无障碍路线</Text></TouchableOpacity>
            <TouchableOpacity disabled={planning} style={[styles.option, avoidRestricted && styles.optionActive]} onPress={() => { setAvoidRestricted(value => !value); setPlan(null) }}><Ionicons name="warning-outline" size={16} color={avoidRestricted ? colors.primary : colors.text} /><Text style={[styles.optionText, avoidRestricted && styles.optionTextActive]}>避开施工</Text></TouchableOpacity>
          </View>
          <PrimaryButton label={planning ? '正在计算路线…' : '开始实时规划'} icon="navigate" onPress={calculate} />

          {plan ? <View style={styles.result}>
            {destinationPlace ? <View><Text style={styles.fieldLabel}>开放时间：{destinationPlace.openingHours || '待补充，请以现场公告为准'}</Text><Text style={styles.fieldLabel}>入口：{destinationPlace.entranceHint || '待补充'}</Text></View> : null}
            <View style={styles.resultHead}><View><Text style={styles.resultTitle}>{cleanName(plan.origin)} → {cleanName(plan.destination)}</Text><Text style={styles.resultSub}>{plan.strategy === 'SEMANTIC_ROUTE' ? 'AI语义理解 + 路网算法' : '数据库路网算法'} · 置信度 {Math.round(plan.confidence * 100)}%</Text></View><View style={styles.metric}><Text style={styles.metricValue}>{plan.distanceMeters}m</Text><Text style={styles.metricLabel}>{plan.estimatedMinutes}分钟</Text></View></View>
            {destinationMedia ? <View style={styles.destinationMedia}><Image source={{ uri: resolveMediaUrl(destinationMedia.url) }} style={styles.destinationImage} /><View style={{ flex: 1 }}><Text style={styles.mediaTitle}>{destinationMedia.title}</Text><Text style={styles.mediaMeta}>终点实景 · 质检 {destinationMedia.qualityScore ?? '--'} 分</Text></View></View> : null}
            {plan.steps.map((step, index) => <View key={`${step.roadId}-${index}`} style={styles.step}><View style={styles.stepNumber}><Text style={styles.stepNumberText}>{index + 1}</Text></View><View style={{ flex: 1 }}><Text style={styles.stepTitle}>{cleanName(step.roadName)}</Text><Text style={styles.stepSub}>{cleanName(step.fromNode)} → {cleanName(step.toNode)}</Text></View><Text style={styles.stepDistance}>{step.lengthMeters}m</Text></View>)}
            <View style={styles.evidence}>{plan.evidence.map(item => <Text key={item} style={styles.evidenceText}>✓ {item}</Text>)}</View>
          </View> : <View style={styles.empty}><Ionicons name="git-network-outline" size={28} color="#A5B2C5" /><Text style={styles.emptyText}>规划结果将显示真实路段、距离、绕行原因和终点实景</Text></View>}
        </ScrollView>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas }, mapArea: { position: 'relative' },
  search: { position: 'absolute', top: 13, left: 16, right: 16, height: 48, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: 'white', borderRadius: 16, ...shadow }, searchInput: { flex: 1, color: colors.ink, fontSize: 11 },
  mapTools: { position: 'absolute', right: 14, top: 76, gap: 8 }, tool: { width: 40, height: 40, backgroundColor: 'white', borderRadius: 13, alignItems: 'center', justifyContent: 'center', ...shadow },
  routeBadge: { position: 'absolute', left: 17, top: 76, paddingHorizontal: 11, paddingVertical: 8, borderRadius: 11, backgroundColor: colors.primary, flexDirection: 'row', alignItems: 'center', gap: 5, ...shadow }, routeBadgeText: { color: 'white', fontSize: 10, fontWeight: '900' },
  layers: { position: 'absolute', left: 16, right: 16, bottom: 16, padding: 5, borderRadius: 14, backgroundColor: 'rgba(255,255,255,.93)', flexDirection: 'row' }, layer: { flex: 1, paddingVertical: 8, borderRadius: 10, alignItems: 'center' }, layerActive: { backgroundColor: colors.primary }, layerText: { color: colors.text, fontSize: 11, fontWeight: '700' }, layerTextActive: { color: 'white' },
  sheet: { flex: 1, marginTop: -4, paddingHorizontal: 18, backgroundColor: 'white', borderTopLeftRadius: 24, borderTopRightRadius: 24 }, handle: { alignSelf: 'center', width: 42, height: 4, borderRadius: 3, backgroundColor: '#D9E0EB', marginTop: 9, marginBottom: 11 }, sheetTitle: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, title: { color: colors.ink, fontSize: 17, fontWeight: '900' },
  verified: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 8, paddingVertical: 5, backgroundColor: colors.successSoft, borderRadius: 999 }, verifiedText: { color: colors.teal, fontSize: 9, fontWeight: '800' }, sheetContent: { paddingTop: 10, paddingBottom: 24, gap: 8 }, fieldLabel: { color: colors.text, fontSize: 9, fontWeight: '800', marginTop: 2 },
  chips: { gap: 7, paddingRight: 8 }, chip: { paddingHorizontal: 10, paddingVertical: 7, borderRadius: 10, backgroundColor: '#F0F3F8', borderWidth: 1, borderColor: 'transparent' }, chipActive: { backgroundColor: colors.primarySoft, borderColor: '#91B6FF' }, chipDestination: { backgroundColor: '#EEEAFE', borderColor: '#B6A6FA' }, chipText: { color: colors.text, fontSize: 9, fontWeight: '700' }, chipTextActive: { color: colors.primary, fontWeight: '900' },
  options: { flexDirection: 'row', gap: 8, marginVertical: 4 }, option: { flex: 1, height: 38, borderRadius: 11, backgroundColor: '#F3F5F9', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5 }, optionActive: { backgroundColor: colors.primarySoft }, optionText: { color: colors.text, fontSize: 9, fontWeight: '700' }, optionTextActive: { color: colors.primary, fontWeight: '900' },
  result: { marginTop: 6, padding: 12, borderRadius: 16, backgroundColor: '#F7F9FD', borderWidth: 1, borderColor: colors.line, gap: 4 }, resultHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 7 }, resultTitle: { color: colors.ink, fontSize: 13, fontWeight: '900' }, resultSub: { color: colors.muted, fontSize: 8, marginTop: 3 }, metric: { alignItems: 'flex-end' }, metricValue: { color: colors.primary, fontSize: 15, fontWeight: '900' }, metricLabel: { color: colors.muted, fontSize: 8 },
  destinationMedia: { padding: 8, marginBottom: 4, borderRadius: 12, backgroundColor: 'white', flexDirection: 'row', alignItems: 'center', gap: 9 }, destinationImage: { width: 56, height: 42, borderRadius: 9, backgroundColor: '#E8EDF4' }, mediaTitle: { color: colors.ink, fontSize: 10, fontWeight: '800' }, mediaMeta: { color: colors.muted, fontSize: 8, marginTop: 3 },
  step: { flexDirection: 'row', alignItems: 'center', gap: 9, paddingVertical: 7, borderBottomWidth: 1, borderBottomColor: colors.line }, stepNumber: { width: 26, height: 26, borderRadius: 9, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' }, stepNumberText: { color: colors.primary, fontSize: 9, fontWeight: '900' }, stepTitle: { color: colors.ink, fontSize: 10, fontWeight: '800' }, stepSub: { color: colors.muted, fontSize: 8, marginTop: 2 }, stepDistance: { color: colors.text, fontSize: 9, fontWeight: '700' },
  evidence: { marginTop: 8, padding: 9, borderRadius: 10, backgroundColor: colors.successSoft, gap: 3 }, evidenceText: { color: '#168467', fontSize: 8, lineHeight: 13 }, empty: { minHeight: 85, marginTop: 5, borderRadius: 14, backgroundColor: '#F6F8FB', alignItems: 'center', justifyContent: 'center', gap: 6, paddingHorizontal: 24 }, emptyText: { color: colors.muted, fontSize: 9, lineHeight: 14, textAlign: 'center' },
})
