<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import type { Place, RoadSegment, RoutePlan } from '@/services/api'

const props = withDefaults(defineProps<{
  workspace?: boolean
  simulation?: boolean
  places?: Place[]
  roads?: RoadSegment[]
  routePlan?: RoutePlan | null
  simulationProgress?: number
  simulationRunning?: boolean
  blockedEvent?: boolean
  selectedLayer?: string
  modelReady?: boolean
}>(), {
  workspace: false,
  simulation: false,
  places: () => [],
  roads: () => [],
  routePlan: null,
  simulationProgress: 0,
  simulationRunning: false,
  blockedEvent: false,
  selectedLayer: '路网',
  modelReady: false,
})
const store = useAppStore()
const sceneRoot = ref<HTMLElement | null>(null)
const layer = ref('地点')
const layers = ['地点', '路网', '任务', '热力']
const zoom = ref(1)
const layerPanelOpen = ref(false)
const activeTool = ref('选择')
const viewMode = ref<'default' | 'top'>('default')
const draftPoints = ref<{ x: number; y: number; type: string }[]>([])
const actionText = ref(props.workspace ? '选择工具后可在场景中操作' : '选择图层查看数据库空间数据')
const trees = Array.from({ length: 34 }, (_, index) => index + 1)
const visiblePlaces = computed(() => props.places.slice(0, 4))
const restrictedRoad = computed(() => props.roads.find(item => item.status !== 'OPEN'))
const selectedPlace = computed(() => visiblePlaces.value[0])
const pinClasses = ['pin-a', 'pin-b', 'pin-c', 'pin-d']
const sceneTransform = computed(() => `scale(${zoom.value}) ${viewMode.value === 'top' ? 'rotateX(8deg)' : ''}`)
const simulationPosition = computed(() => {
  const points = [[10.2,73.1],[31,58.7],[51.6,51.7],[74.7,55],[90.2,46.9]]
  const progress = Math.min(100, Math.max(0, props.simulationProgress))
  const scaled = progress / 100 * (points.length - 1)
  const index = Math.min(points.length - 2, Math.floor(scaled))
  const ratio = scaled - index
  const from = points[index] ?? points[0]!
  const to = points[index + 1] ?? points.at(-1)!
  return { left: `${from[0] + (to[0] - from[0]) * ratio}%`, top: `${from[1] + (to[1] - from[1]) * ratio}%` }
})

function setZoom(value: number) {
  zoom.value = Math.min(1.45, Math.max(.8, Number(value.toFixed(2))))
  actionText.value = `当前缩放 ${Math.round(zoom.value * 100)}%`
}

function locateCenter() {
  zoom.value = 1
  viewMode.value = 'default'
  actionText.value = '已回到校园中心与默认视角'
}

function selectTool(tool: string) {
  activeTool.value = tool
  actionText.value = tool === '选择' ? '点击场景对象进行选择' : `已进入${tool}模式，点击场景放置操作点`
}

function handleSceneClick(event: MouseEvent) {
  if (!props.workspace || activeTool.value === '选择') return
  const target = sceneRoot.value
  if (!target) return
  const rect = target.getBoundingClientRect()
  const point = { x: (event.clientX - rect.left) / rect.width * 100, y: (event.clientY - rect.top) / rect.height * 100, type: activeTool.value }
  if (activeTool.value === '设置原点') {
    draftPoints.value = draftPoints.value.filter(item => item.type !== '设置原点').concat(point)
    actionText.value = `原点草稿：${point.x.toFixed(1)}%, ${point.y.toFixed(1)}%（模型导入后可标定）`
  } else if (activeTool.value === '测量') {
    const previous = draftPoints.value.filter(item => item.type === '测量').slice(-1)[0]
    draftPoints.value.push(point)
    actionText.value = previous
      ? `屏幕测量距离 ${Math.hypot(point.x - previous.x, point.y - previous.y).toFixed(1)}%，导入模型后换算实际米数`
      : '已设置测量起点，请点击终点'
  } else {
    draftPoints.value.push(point)
    actionText.value = `已添加第 ${draftPoints.value.filter(item => item.type === 'POI标注').length} 个 POI 草稿点`
  }
}

async function toggleFullscreen() {
  if (!sceneRoot.value) return
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await sceneRoot.value.requestFullscreen()
  } catch {
    store.notify('浏览器未允许全屏显示', 'warning')
  }
}
</script>

<template>
  <div ref="sceneRoot" :class="[workspace ? 'scene-workspace panel' : 'map-card', `view-${viewMode}`]" @click="handleSceneClick">
    <div class="map-image-crop">
      <div class="campus-blueprint" :style="{transform:sceneTransform}">
        <span v-for="tree in trees" :key="`tree-${tree}`" :class="['mock-tree', `tree-${tree}`]" />
        <span v-for="n in 12" :key="n" :class="['mock-building', `building-${n}`]" />
        <i class="mock-road road-a" /><i class="mock-road road-b" /><i class="mock-road road-c" />
        <b class="mock-lake" />
        <svg v-if="props.roads.length && (workspace || layer === '路网')" class="scene-route-layer" viewBox="0 0 1000 520" preserveAspectRatio="none" aria-hidden="true">
          <defs><filter id="routeGlow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>
          <path class="route-halo" d="M102 380 C190 353 220 316 310 305 S430 248 516 269 S655 330 747 286 S835 224 902 244" />
          <path class="route-main" d="M102 380 C190 353 220 316 310 305 S430 248 516 269 S655 330 747 286 S835 224 902 244" />
          <path v-if="simulation" class="route-completed" pathLength="100" :style="{strokeDasharray:`${simulationProgress} 100`}" d="M102 380 C190 353 220 316 310 305 S430 248 516 269 S655 330 747 286 S835 224 902 244" />
          <path v-if="simulation && blockedEvent" class="route-alternative" d="M516 269 C575 220 635 209 695 240 S785 272 838 232" />
          <path v-if="simulation && blockedEvent" class="route-block-zone" d="M545 310 L650 286 L684 344 L568 367 Z" />
          <path v-if="restrictedRoad" class="route-closed" d="M410 286 C446 300 475 319 514 337" />
          <circle cx="102" cy="380" r="7" class="route-node"/><circle cx="310" cy="305" r="5" class="route-node"/><circle cx="516" cy="269" r="5" class="route-node"/><circle cx="747" cy="286" r="5" class="route-node"/><circle cx="902" cy="244" r="7" class="route-node"/>
        </svg>
        <template v-if="workspace || layer === '地点'"><em v-for="(place,index) in visiblePlaces" :key="place.id" :class="['mock-pin',pinClasses[index]]"><i>⌖</i><span>{{ place.name }}<small>{{ place.status || '状态未配置' }}</small></span></em></template>
        <em v-if="restrictedRoad && (workspace || layer === '路网')" class="construction-pin"><i>!</i><span>{{ restrictedRoad.name }}<small>{{ restrictedRoad.restrictionReason || restrictedRoad.status }}</small></span></em>
        <div v-if="simulation && routePlan" class="simulation-marker" :class="{moving:simulationRunning}" :style="simulationPosition"><b>🚶</b><span>{{ Math.round(simulationProgress) }}%</span></div>
        <div v-if="simulation && blockedEvent" class="simulation-construction"><b>⚠</b><span>模拟道路封闭<small>正在动态重规划</small></span></div>
        <div v-if="simulation && selectedLayer === '人流'" class="simulation-heat-layer" aria-label="人流热力图层"><i/><i/><i/><span>湖畔步道人流较集中</span></div>
        <div v-if="simulation && selectedLayer === '任务'" class="simulation-layer-card task-layer-card"><b>新生任务锚点</b><span>学院报到 · 校园卡 · 宿舍入住</span><small>已关联 {{ props.places.length }} 个校园地点</small></div>
        <div v-if="simulation && selectedLayer === '环境'" class="simulation-layer-card environment-layer-card"><b>环境感知</b><span>晴 25°C · 能见度良好</span><small>当前路线适宜步行</small></div>
        <div v-if="simulation && selectedLayer === '建筑'" class="simulation-layer-card building-layer-card"><b>建筑语义</b><span>{{ props.places.length }} 个地点已绑定业务数据</span><small>点击地点可查看开放状态</small></div>
        <div v-if="layer === '热力' && !workspace" class="heat-layer"><i/><i/><i/></div>
        <div v-if="layer === '任务' && !workspace" class="layer-empty">任务流程共 {{ props.places.length ? '可关联地点' : '0 个空间锚点' }}</div>
        <i v-for="(point,index) in draftPoints" :key="index" class="draft-point" :class="point.type==='设置原点'?'origin':''" :style="{left:`${point.x}%`,top:`${point.y}%`}">{{ point.type==='设置原点'?'O':index+1 }}</i>
        <strong class="pending-model"><i />{{ modelReady ? '模型信息已登记 · 等待解析服务' : '演示场景 · 业务标记来自MySQL' }}</strong>
      </div>
    </div><div class="map-shade" />
    <template v-if="!workspace">
      <h2 class="scene-heading">校园空间总览</h2>
      <div class="map-toolbar"><button v-for="(item,index) in layers" :key="item" :class="{ active: layer === item }" @click.stop="layer = item"><i>{{ ['⌖','⌘','☑','◉'][index] }}</i>{{ item }}</button></div>
      <div class="map-tools"><button title="定位校园中心" @click.stop="locateCenter">◎</button><button title="放大" @click.stop="setZoom(zoom+.1)">+</button><button title="缩小" @click.stop="setZoom(zoom-.1)">−</button><button title="图层状态" @click.stop="layerPanelOpen=!layerPanelOpen">◈</button></div>
      <div v-if="layerPanelOpen" class="layer-popover"><b>当前图层：{{ layer }}</b><span>地点 {{ props.places.length }}</span><span>路段 {{ props.roads.length }}</span><span>缩放 {{ Math.round(zoom*100) }}%</span></div>
      <div class="map-legend"><span v-if="props.roads.length && layer==='路网'"><i class="legend-dot" style="background:#29e3b0" />数据库路网</span><span v-if="restrictedRoad && layer==='路网'"><i class="legend-dot" style="background:#ff5b61" />受限路段</span><span v-if="props.places.length && layer==='地点'"><i class="legend-dot" style="background:#5da0ff" />校园地点</span><span v-if="!props.roads.length&&!props.places.length"><i class="legend-dot" style="background:#8ba0b8" />暂无正式空间数据</span><span>{{ actionText }}</span></div>
      <div class="map-compass"><b>N</b><i /></div>
    </template>
    <template v-else-if="!simulation">
      <div class="scene-top"><div class="glass-control"><button v-for="tool in ['选择','POI标注','测量','设置原点']" :key="tool" :class="{active:activeTool===tool}" @click.stop="selectTool(tool)">{{ tool }}</button></div><div class="glass-control"><button :class="{active:viewMode==='top'}" @click.stop="viewMode='top';actionText='已切换俯视视角'">俯视</button><button :class="{active:viewMode==='default'}" @click.stop="locateCenter">默认视角</button><button @click.stop="toggleFullscreen">全屏</button></div></div>
      <div class="scene-action-hint">{{ actionText }}</div>
      <div class="scene-status"><span>模型：尚未接入</span><span>坐标系：{{ draftPoints.some(item=>item.type==='设置原点')?'已设草稿原点':'待标定' }}</span><span>POI：{{ props.places.length }}</span><span v-if="selectedPlace">当前：{{ selectedPlace.name }}</span></div>
    </template>
    <template v-else>
      <div class="simulation-scene-status"><span><i />{{ simulationRunning ? '导航仿真运行中' : '导航仿真已暂停' }}</span><span>路线 {{ routePlan?.distanceMeters ?? 0 }}m</span><span>进度 {{ Math.round(simulationProgress) }}%</span></div>
    </template>
  </div>
</template>

<style scoped>
.campus-blueprint{transition:transform .22s ease;transform-origin:center}.layer-popover{position:absolute;z-index:8;right:58px;top:205px;display:grid;gap:6px;padding:10px 12px;border:1px solid rgba(255,255,255,.45);border-radius:10px;color:#eaf3ff;background:rgba(7,35,65,.86);font-size:9px}.layer-popover b{font-size:10px}.heat-layer{position:absolute;inset:0;z-index:2;background:radial-gradient(circle at 38% 45%,rgba(255,70,70,.55),transparent 16%),radial-gradient(circle at 68% 38%,rgba(255,185,60,.45),transparent 17%),radial-gradient(circle at 60% 70%,rgba(61,219,176,.38),transparent 20%)}.heat-layer i{display:none}.layer-empty{position:absolute;z-index:5;left:50%;top:50%;transform:translate(-50%,-50%);padding:10px 14px;border-radius:10px;color:white;background:rgba(7,35,65,.76);font-size:10px}.draft-point{position:absolute;z-index:9;width:24px;height:24px;display:grid;place-items:center;transform:translate(-50%,-50%);border:2px solid white;border-radius:50%;color:white;background:#3479f4;box-shadow:0 5px 12px rgba(14,47,91,.28);font-size:9px;font-style:normal}.draft-point.origin{background:#f17a36}.scene-action-hint{position:absolute;z-index:6;left:50%;top:72px;transform:translateX(-50%);padding:7px 11px;border-radius:8px;color:#eaf3ff;background:rgba(7,35,65,.76);font-size:9px;pointer-events:none}.scene-workspace:fullscreen{width:100vw;height:100vh;border-radius:0}.view-top .campus-blueprint{transform-origin:center}
.route-completed,.route-alternative{fill:none;stroke-linecap:round;stroke-linejoin:round}.route-completed{stroke:#1d66ff;stroke-width:8;filter:url(#routeGlow)}.route-alternative{stroke:#f8fbff;stroke-width:6;stroke-dasharray:12 9;filter:url(#routeGlow)}.route-block-zone{fill:rgba(255,68,76,.36);stroke:#ff505b;stroke-width:4;stroke-dasharray:9 7}.simulation-marker{position:absolute;z-index:11;width:48px;height:48px;display:grid;place-items:center;transform:translate(-50%,-50%);border:4px solid white;border-radius:50%;color:white;background:#2e72f1;box-shadow:0 0 0 10px rgba(49,118,255,.24),0 0 30px rgba(34,122,255,.72)}.simulation-marker b{font-size:19px}.simulation-marker span{position:absolute;top:-28px;padding:4px 7px;border-radius:6px;color:white;background:#173d78;font-size:9px}.simulation-marker.moving{animation:markerPulse 1.2s ease-in-out infinite}.simulation-construction{position:absolute;z-index:10;left:58%;top:62%;display:flex;align-items:center;gap:8px;padding:8px 10px;border:1px solid #ff8c90;border-radius:10px;color:white;background:rgba(171,37,45,.9);box-shadow:0 7px 20px rgba(91,17,22,.3)}.simulation-construction b{font-size:17px}.simulation-construction span,.simulation-construction small{display:block}.simulation-construction span{font-size:9px;font-weight:800}.simulation-construction small{margin-top:2px;color:#ffd3d5;font-size:7px}.simulation-scene-status{position:absolute;z-index:9;left:22px;bottom:92px;display:flex;gap:7px}.simulation-scene-status span{padding:7px 9px;border:1px solid rgba(255,255,255,.4);border-radius:8px;color:white;background:rgba(9,47,87,.74);font-size:8px;backdrop-filter:blur(9px)}.simulation-scene-status i{display:inline-block;width:6px;height:6px;margin-right:5px;border-radius:50%;background:#18d5a3}@keyframes markerPulse{50%{box-shadow:0 0 0 16px rgba(49,118,255,.1),0 0 35px rgba(34,122,255,.85)}}
.simulation-heat-layer{position:absolute;inset:0;z-index:3;pointer-events:none;background:radial-gradient(circle at 44% 52%,rgba(255,61,72,.54),transparent 13%),radial-gradient(circle at 72% 48%,rgba(255,177,56,.48),transparent 16%),radial-gradient(circle at 26% 68%,rgba(31,222,173,.3),transparent 18%)}.simulation-heat-layer i{display:none}.simulation-heat-layer span{position:absolute;right:24%;top:36%;padding:6px 9px;border-radius:8px;color:#fff;background:rgba(130,55,20,.76);font-size:9px;font-weight:700}.simulation-layer-card{position:absolute;z-index:12;left:22px;top:104px;display:grid;gap:3px;min-width:168px;padding:10px 12px;border:1px solid rgba(255,255,255,.56);border-radius:12px;color:#ecf7ff;background:rgba(10,53,94,.78);box-shadow:0 10px 24px rgba(18,61,99,.2);backdrop-filter:blur(10px);pointer-events:none}.simulation-layer-card b{font-size:11px}.simulation-layer-card span{font-size:9px}.simulation-layer-card small{color:#a8cff0;font-size:8px}.environment-layer-card{background:rgba(13,93,86,.8)}.building-layer-card{background:rgba(24,72,150,.8)}.task-layer-card{background:rgba(88,54,154,.8)}
</style>
