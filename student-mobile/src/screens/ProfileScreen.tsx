import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { Card, SectionTitle } from '../components/Ui'
import { colors } from '../theme'
import type { ScreenProps } from '../types'

const menu = [
  { icon: 'star-outline', title: '我的收藏', detail: '地点、路线和校园攻略', color: colors.primary, bg: colors.primarySoft },
  { icon: 'footsteps-outline', title: '历史路线', detail: '查看最近导航和行程', color: colors.teal, bg: colors.successSoft },
  { icon: 'sparkles-outline', title: 'AI对话记录', detail: '管理智能体对话与任务', color: colors.violet, bg: '#F0EDFF' },
  { icon: 'create-outline', title: '我的上报', detail: '查看地点纠错与素材贡献', color: colors.orange, bg: colors.warningSoft },
] as const

export default function ProfileScreen({ notify }: ScreenProps) {
  return (
    <ScrollView style={styles.page} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 28 }}>
      <View style={styles.hero}>
        <View style={styles.top}><Text style={styles.brand}>新生智游</Text><TouchableOpacity style={styles.settings} onPress={() => notify('设置页面已打开')}><Ionicons name="settings-outline" size={19} color="white" /></TouchableOpacity></View>
        <View style={styles.identity}><View style={styles.avatar}><Text style={styles.avatarText}>林</Text></View><View><Text style={styles.name}>林知夏</Text><Text style={styles.school}>2026级 · 软件工程学院</Text><View style={styles.level}><Ionicons name="sparkles" size={12} color="#FFE07A" /><Text style={styles.levelText}>校园探索者 Lv.3</Text></View></View></View>
      </View>
      <Card style={styles.stats}>{[['12', '已到访地点'], ['8.6km', '校园足迹'], ['5', '已完成任务']].map(item => <View key={item[1]} style={styles.stat}><Text style={styles.statValue}>{item[0]}</Text><Text style={styles.statLabel}>{item[1]}</Text></View>)}</Card>

      <View style={styles.section}><SectionTitle title="我的校园" /><Card style={styles.menu}>{menu.map(item => <TouchableOpacity key={item.title} style={styles.menuItem} onPress={() => notify(`${item.title}已打开`)}><View style={[styles.menuIcon, { backgroundColor: item.bg }]}><Ionicons name={item.icon} size={19} color={item.color} /></View><View style={{ flex: 1 }}><Text style={styles.menuTitle}>{item.title}</Text><Text style={styles.menuDetail}>{item.detail}</Text></View><Ionicons name="chevron-forward" size={17} color="#B2BDCC" /></TouchableOpacity>)}</Card></View>

      <View style={styles.section}><SectionTitle title="服务与安全" /><Card style={styles.menu}>
        <MenuItem icon="chatbubble-ellipses-outline" title="意见反馈" detail="帮我们改进导航和AI回答" onPress={() => notify('意见反馈已打开')} />
        <MenuItem icon="shield-checkmark-outline" title="隐私与数据" detail="管理位置、照片与对话数据" color={colors.teal} bg={colors.successSoft} onPress={() => notify('隐私与数据管理已打开')} />
        <MenuItem icon="information-circle-outline" title="关于新生智游" detail="语义数字孪生与校园任务智能体" color={colors.violet} bg="#F0EDFF" onPress={() => notify('新生智游 Expo 演示版 v0.2.0')} />
      </Card></View>
      <Text style={styles.version}>新生智游 v0.2.0 · Expo 演示版</Text>
    </ScrollView>
  )
}

function MenuItem({ icon, title, detail, color = colors.primary, bg = colors.primarySoft, onPress }: { icon: keyof typeof Ionicons.glyphMap; title: string; detail: string; color?: string; bg?: string; onPress?: () => void }) {
  return <TouchableOpacity style={styles.menuItem} onPress={onPress}><View style={[styles.menuIcon, { backgroundColor: bg }]}><Ionicons name={icon} size={19} color={color} /></View><View style={{ flex: 1 }}><Text style={styles.menuTitle}>{title}</Text><Text style={styles.menuDetail}>{detail}</Text></View><Ionicons name="chevron-forward" size={17} color="#B2BDCC" /></TouchableOpacity>
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas },
  hero: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 64, backgroundColor: colors.primaryDark },
  top: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, brand: { color: 'white', fontSize: 17, fontWeight: '900' }, settings: { width: 39, height: 39, borderRadius: 13, backgroundColor: 'rgba(255,255,255,.12)', alignItems: 'center', justifyContent: 'center' },
  identity: { marginTop: 25, flexDirection: 'row', gap: 13, alignItems: 'center' }, avatar: { width: 64, height: 64, borderRadius: 22, backgroundColor: '#DDE7FF', borderWidth: 3, borderColor: 'rgba(255,255,255,.4)', alignItems: 'center', justifyContent: 'center' }, avatarText: { color: colors.primaryDark, fontSize: 22, fontWeight: '900' },
  name: { color: 'white', fontSize: 21, fontWeight: '900' }, school: { color: '#C6D4FA', fontSize: 10, marginTop: 4 }, level: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 8, paddingHorizontal: 8, paddingVertical: 5, borderRadius: 999, backgroundColor: 'rgba(255,255,255,.12)', alignSelf: 'flex-start' }, levelText: { color: '#FFE9A7', fontSize: 9, fontWeight: '700' },
  stats: { marginHorizontal: 16, marginTop: -39, paddingVertical: 17, flexDirection: 'row' }, stat: { flex: 1, alignItems: 'center', borderRightWidth: 1, borderRightColor: colors.line }, statValue: { color: colors.ink, fontSize: 18, fontWeight: '900' }, statLabel: { color: colors.muted, fontSize: 9, marginTop: 4 },
  section: { paddingHorizontal: 16, marginTop: 21 }, menu: { paddingHorizontal: 13 }, menuItem: { height: 65, flexDirection: 'row', alignItems: 'center', gap: 11, borderBottomWidth: 1, borderBottomColor: colors.line }, menuIcon: { width: 37, height: 37, borderRadius: 12, alignItems: 'center', justifyContent: 'center' }, menuTitle: { color: colors.ink, fontSize: 12, fontWeight: '800' }, menuDetail: { color: colors.muted, fontSize: 9, marginTop: 4 }, version: { textAlign: 'center', color: colors.muted, fontSize: 9, marginTop: 22 },
})
