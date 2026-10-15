import { StyleSheet, Text, View } from 'react-native'
import { Ionicons } from '@expo/vector-icons'

type Props = { tall?: boolean; showRoute?: boolean }

const buildings = [
  { left: '8%', top: '18%', w: 54, h: 46 },
  { left: '27%', top: '10%', w: 76, h: 54 },
  { left: '58%', top: '16%', w: 62, h: 44 },
  { left: '76%', top: '29%', w: 48, h: 68 },
  { left: '13%', top: '55%', w: 76, h: 50 },
  { left: '43%', top: '48%', w: 68, h: 64 },
  { left: '68%', top: '63%', w: 82, h: 48 },
] as const

export default function CampusScene({ tall = false, showRoute = true }: Props) {
  return (
    <View style={[styles.scene, tall && styles.tall]}>
      <View style={styles.grid} />
      <View style={[styles.road, styles.roadA]} />
      <View style={[styles.road, styles.roadB]} />
      <View style={[styles.road, styles.roadC]} />
      {buildings.map((building, index) => (
        <View key={index} style={[styles.building, { left: building.left, top: building.top, width: building.w, height: building.h }]}>
          <View style={styles.roofLine} />
          <View style={styles.windows} />
        </View>
      ))}
      <View style={styles.lake} />
      {showRoute ? (
        <>
          <View style={styles.route} />
          <View style={[styles.routeDot, { left: '16%', top: '66%' }]} />
          <View style={[styles.routeDot, styles.routeEnd, { left: '74%', top: '32%' }]} />
        </>
      ) : null}
      <View style={[styles.pin, { left: '12%', bottom: 18 }]}>
        <Ionicons name="location" color="white" size={13} />
        <Text style={styles.pinText}>东门</Text>
      </View>
      <View style={[styles.pin, styles.pinBlue, { right: 12, top: 19 }]}>
        <Ionicons name="business" color="white" size={12} />
        <Text style={styles.pinText}>校园卡中心</Text>
      </View>
      <View style={styles.pending}><Text style={styles.pendingText}>等待导入校园数字孪生</Text></View>
    </View>
  )
}

const styles = StyleSheet.create({
  scene: { height: 208, borderRadius: 18, overflow: 'hidden', backgroundColor: '#BFDDEE', position: 'relative' },
  tall: { height: 390, borderRadius: 0 },
  grid: { position: 'absolute', left: 0, right: 0, top: 0, bottom: 0, backgroundColor: 'rgba(255,255,255,0.08)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.35)' },
  road: { position: 'absolute', backgroundColor: '#E7EEF2', borderWidth: 1, borderColor: '#CEDDE4', borderRadius: 14 },
  roadA: { width: '120%', height: 24, left: '-10%', top: '44%', transform: [{ rotate: '-12deg' }] },
  roadB: { width: 22, height: '120%', left: '37%', top: '-10%', transform: [{ rotate: '17deg' }] },
  roadC: { width: 18, height: '95%', right: '22%', top: '10%', transform: [{ rotate: '-5deg' }] },
  building: { position: 'absolute', borderRadius: 7, backgroundColor: '#F7FBFC', borderWidth: 1, borderColor: '#95B8C8', padding: 5, shadowColor: '#335A6D', shadowOpacity: 0.2, shadowRadius: 4, elevation: 3 },
  roofLine: { height: 5, borderRadius: 3, backgroundColor: '#6A93A6', opacity: 0.65 },
  windows: { flex: 1, marginTop: 5, borderRadius: 3, backgroundColor: '#D9E9EF', borderWidth: 2, borderColor: '#EDF6F8' },
  lake: { position: 'absolute', width: 74, height: 40, right: '5%', bottom: '8%', borderRadius: 32, backgroundColor: '#73C8DC', borderWidth: 3, borderColor: 'rgba(255,255,255,.7)' },
  route: { position: 'absolute', width: '64%', height: 6, left: '18%', top: '49%', borderRadius: 5, backgroundColor: '#315FEF', transform: [{ rotate: '-18deg' }] },
  routeDot: { position: 'absolute', width: 14, height: 14, borderRadius: 8, backgroundColor: '#11A982', borderWidth: 3, borderColor: 'white' },
  routeEnd: { backgroundColor: '#315FEF' },
  pin: { position: 'absolute', flexDirection: 'row', alignItems: 'center', gap: 3, paddingHorizontal: 8, paddingVertical: 5, backgroundColor: '#11A982', borderRadius: 10 },
  pinBlue: { backgroundColor: '#315FEF' },
  pinText: { color: 'white', fontSize: 9, fontWeight: '800' },
  pending: { position: 'absolute', left: 10, top: 10, borderRadius: 8, paddingHorizontal: 8, paddingVertical: 5, backgroundColor: 'rgba(18,37,63,.72)' },
  pendingText: { color: 'white', fontSize: 9, fontWeight: '700' },
})
