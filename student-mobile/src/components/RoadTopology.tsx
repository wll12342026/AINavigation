import { useState } from 'react'
import { StyleSheet, Text, View } from 'react-native'
import type { RoadSegment, RoutePlan } from '../services/api'

type Props = { roads: RoadSegment[]; plan: RoutePlan | null }

export default function RoadTopology({ roads, plan }: Props) {
  const [width, setWidth] = useState(360)
  const names = Array.from(new Set(roads.flatMap(road => [road.fromNode, road.toNode]).filter(Boolean))).sort()
  const nodes = names.map((name, index) => ({ name,
    x: width / 2 + Math.sin(index * Math.PI * 2 / names.length) * width * 0.32,
    y: 220 - Math.cos(index * Math.PI * 2 / names.length) * 88,
  }))
  const routeRoads = new Set(plan?.steps.map(step => step.roadId) ?? [])
  return <View style={styles.canvas} onLayout={event => setWidth(event.nativeEvent.layout.width)} accessibilityLabel="数据库道路连接示意图">
    <Text style={styles.caption}>路网连接示意 · 不代表地理位置</Text>
    {roads.map(road => {
      const from = nodes.find(node => node.name === road.fromNode)
      const to = nodes.find(node => node.name === road.toNode)
      if (!from || !to) return null
      const distance = Math.hypot(to.x - from.x, to.y - from.y)
      const color = routeRoads.has(road.id) ? '#2679FA' : road.status === 'OPEN' ? '#B3CCE7' : '#F0787E'
      return <View key={road.id} style={[styles.edge, { left: (from.x + to.x - distance) / 2, top: (from.y + to.y) / 2,
        width: distance, backgroundColor: color, height: routeRoads.has(road.id) ? 5 : 2,
        transform: [{ rotate: `${Math.atan2(to.y - from.y, to.x - from.x)}rad` }],
      }]} />
    })}
    {nodes.map(node => <View key={node.name} style={[styles.node, { left: node.x - 7, top: node.y - 7 }]}>
      <View style={[styles.dot, { backgroundColor: node.name === plan?.destination ? '#12B895' : '#367BE8' }]} />
      <Text numberOfLines={2} style={styles.label}>{node.name.replace(/^【[^】]+】\s*/, '')}</Text>
    </View>)}
    {!nodes.length ? <Text style={styles.empty}>暂无路网数据，连接后刷新查看</Text> : null}
    <Text style={styles.legend}>蓝色：本次路线　红色：关闭 / 受限　{names.length} 个节点</Text>
  </View>
}

const styles = StyleSheet.create({
  canvas: { height: 390, backgroundColor: '#EAF3FF', overflow: 'hidden' },
  caption: { position: 'absolute', top: 87, left: 16, color: '#7189A8', fontSize: 10 },
  edge: { position: 'absolute', borderRadius: 3 },
  node: { position: 'absolute', width: 14, height: 14 },
  dot: { width: 14, height: 14, borderRadius: 7, borderWidth: 3, borderColor: 'white' },
  label: { position: 'absolute', width: 82, top: 18, left: -34, textAlign: 'center', color: '#264F80', fontSize: 9, fontWeight: '700' },
  empty: { position: 'absolute', top: 205, alignSelf: 'center', color: '#7E95B2', fontSize: 12 },
  legend: { position: 'absolute', bottom: 22, left: 16, color: '#7189A8', fontSize: 9 },
})
