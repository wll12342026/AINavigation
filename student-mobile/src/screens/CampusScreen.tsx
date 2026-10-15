import { useState } from 'react'
import { ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import CampusScene from '../components/CampusScene'
import { PrimaryButton } from '../components/Ui'
import { colors, shadow } from '../theme'
import type { ScreenProps } from '../types'

export default function CampusScreen({ notify }: ScreenProps) {
  const [layer, setLayer] = useState('导航')
  const [navigating, setNavigating] = useState(false)
  return (
    <View style={styles.page}>
      <View style={styles.mapArea}>
        <CampusScene tall />
        <View style={styles.search}>
          <Ionicons name="search" size={18} color={colors.text} />
          <TextInput style={styles.searchInput} placeholder="搜索地点或告诉AI要办的事" placeholderTextColor={colors.muted} />
          <TouchableOpacity onPress={() => notify('语音输入已开启')}><Ionicons name="mic" size={19} color={colors.primary} /></TouchableOpacity>
        </View>
        <View style={styles.mapTools}>
          {(['locate', 'cube', 'layers'] as const).map((icon, index) => <TouchableOpacity key={icon} style={styles.tool} onPress={() => notify(['已定位当前位置', '已切换鸟瞰视角', '场景图层已展开'][index] ?? '')}><Ionicons name={icon} size={18} color={colors.ink} /></TouchableOpacity>)}
        </View>
        <View style={styles.layers}>{['导航', '地点', '任务', '热力'].map(item => <TouchableOpacity key={item} onPress={() => setLayer(item)} style={[styles.layer, layer === item && styles.layerActive]}><Text style={[styles.layerText, layer === item && styles.layerTextActive]}>{item}</Text></TouchableOpacity>)}</View>
      </View>
      <View style={styles.sheet}>
        <View style={styles.handle} />
        <View style={styles.sheetTitle}><Text style={styles.title}>{navigating ? '正在导航至校园卡中心' : '最优任务路线'}</Text><View style={styles.verified}><Ionicons name="shield-checkmark" size={13} color={colors.teal} /><Text style={styles.verifiedText}>AI已验证可通行</Text></View></View>
        <View style={styles.summary}>
          {[['12分钟', '预计用时'], ['860米', '步行距离'], ['2个', '任务节点']].map(item => <View key={item[1]} style={styles.summaryItem}><Text style={styles.summaryValue}>{item[0]}</Text><Text style={styles.summaryLabel}>{item[1]}</Text></View>)}
        </View>
        <ScrollView style={styles.steps} showsVerticalScrollIndicator={false}>
          {[
            ['arrow-up', '沿中心大道向北', '经过图书馆西侧', '420m'],
            ['return-down-forward', '在钟楼路口右转', '前方有蓝色服务指示牌', '310m'],
            ['enter-outline', '从综合服务中心东门进入', 'AI已匹配入口实景图', '130m'],
          ].map((step, index) => <View key={step[1]} style={styles.step}><View style={[styles.stepIcon, index === 2 && styles.stepIconDone]}><Ionicons name={step[0] as keyof typeof Ionicons.glyphMap} size={16} color={index === 2 ? colors.teal : colors.primary} /></View><View style={{ flex: 1 }}><Text style={styles.stepTitle}>{step[1]}</Text><Text style={styles.stepSub}>{step[2]}</Text></View><Text style={styles.stepDistance}>{step[3]}</Text></View>)}
        </ScrollView>
        <PrimaryButton label={navigating ? '结束导航' : '开始3D导航'} icon={navigating ? 'stop-circle-outline' : 'navigate'} onPress={() => { setNavigating(v => !v); notify(navigating ? '导航已结束' : '已进入3D导航演示') }} />
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas },
  mapArea: { position: 'relative' },
  search: { position: 'absolute', top: 13, left: 16, right: 16, height: 48, paddingHorizontal: 13, flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: 'white', borderRadius: 16, ...shadow },
  searchInput: { flex: 1, color: colors.ink, fontSize: 12 },
  mapTools: { position: 'absolute', right: 14, top: 80, gap: 8 },
  tool: { width: 41, height: 41, backgroundColor: 'white', borderRadius: 13, alignItems: 'center', justifyContent: 'center', ...shadow },
  layers: { position: 'absolute', left: 16, right: 16, bottom: 16, padding: 5, borderRadius: 14, backgroundColor: 'rgba(255,255,255,.93)', flexDirection: 'row' },
  layer: { flex: 1, paddingVertical: 8, borderRadius: 10, alignItems: 'center' },
  layerActive: { backgroundColor: colors.primary },
  layerText: { color: colors.text, fontSize: 11, fontWeight: '700' },
  layerTextActive: { color: 'white' },
  sheet: { flex: 1, marginTop: -4, paddingHorizontal: 18, paddingBottom: 12, backgroundColor: 'white', borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  handle: { alignSelf: 'center', width: 42, height: 4, borderRadius: 3, backgroundColor: '#D9E0EB', marginTop: 9, marginBottom: 13 },
  sheetTitle: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  title: { color: colors.ink, fontSize: 18, fontWeight: '900' },
  verified: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 8, paddingVertical: 5, backgroundColor: colors.successSoft, borderRadius: 999 },
  verifiedText: { color: colors.teal, fontSize: 9, fontWeight: '800' },
  summary: { flexDirection: 'row', backgroundColor: colors.canvas, borderRadius: 16, paddingVertical: 11, marginTop: 12 },
  summaryItem: { flex: 1, alignItems: 'center', borderRightWidth: 1, borderRightColor: colors.line },
  summaryValue: { color: colors.ink, fontSize: 14, fontWeight: '900' },
  summaryLabel: { color: colors.muted, fontSize: 9, marginTop: 3 },
  steps: { maxHeight: 158, marginVertical: 9 },
  step: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 9, borderBottomWidth: 1, borderBottomColor: colors.line },
  stepIcon: { width: 33, height: 33, borderRadius: 11, backgroundColor: colors.primarySoft, alignItems: 'center', justifyContent: 'center' },
  stepIconDone: { backgroundColor: colors.successSoft },
  stepTitle: { color: colors.ink, fontSize: 12, fontWeight: '800' },
  stepSub: { color: colors.muted, fontSize: 9, marginTop: 3 },
  stepDistance: { color: colors.text, fontSize: 10, fontWeight: '700' },
})
