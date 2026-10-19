<script setup lang="ts">
// 保留原有路线仿真原型；真实模型展示入口使用 TwinView，不自动叠加未经校准的路线。
import { computed, onMounted, onUnmounted, ref } from 'vue'
import CampusScene from '@/components/common/CampusScene.vue'
import { useAppStore } from '@/stores/app'
import {
  getSettings,
  planRoute,
  placesApi,
  roadsApi,
  saveSettings,
  type Place,
  type RoadSegment,
  type RoutePlan,
} from '@/services/api'

const store = useAppStore()
const places = ref<Place[]>([])
const roads = ref<RoadSegment[]>([])
const route = ref<RoutePlan | null>(null)
const loading = ref(false)
const running = ref(false)
const progress = ref(42)
const speed = ref(1)
const closureTriggered = ref(false)
const selectedLayer = ref('路网')
const modelName = ref('')
const modelSize = ref('')
const modelInput = ref<HTMLInputElement | null>(null)
let timer: number | undefined

const nodes = computed(() => Array.from(new Set(roads.value.flatMap(item => [item.fromNode, item.toNode]).filter(Boolean))).sort())
const origin = ref('')
const destination = ref('')
const cleanName = (value: string) => value.replace(/^【测试】\s*/, '')
const totalDistance = computed(() => route.value?.distanceMeters ?? 0)
const travelled = computed(() => Math.round(totalDistance.value * progress.value / 100))
const remaining = computed(() => Math.max(0, totalDistance.value - travelled.value))
const elapsedMinutes = computed(() => Math.max(0, Math.round((route.value?.estimatedMinutes ?? 0) * progress.value / 100)))
const remainingMinutes = computed(() => Math.max(0, (route.value?.estimatedMinutes ?? 0) - elapsedMinutes.value))
const currentStep = computed(() => {
  if (!route.value?.steps.length) return null
  const target = travelled.value
  let sum = 0
  return route.value.steps.find(step => (sum += step.lengthMeters) >= target) ?? route.value.steps.at(-1) ?? null
})
const routeTitle = computed(() => route.value
  ? `${cleanName(route.value.origin)} → ${cleanName(route.value.destination)}`
  : `${cleanName(origin.value || '起点')} → ${cleanName(destination.value || '终点')}`)

function pickDefaultNode(keyword: string, fallbackIndex: number) {
  return nodes.value.find(item => cleanName(item).includes(keyword)) ?? nodes.value[fallbackIndex] ?? ''
}

async function load() {
  try {
    const [placeRows, roadRows, settings] = await Promise.all([placesApi.list(), roadsApi.list(), getSettings()])
    places.value = placeRows
    roads.value = roadRows
    modelName.value = settings.twinModelName ?? ''
    modelSize.value = settings.twinModelSize ?? ''
    origin.value = pickDefaultNode('东门', 0)
    destination.value = pickDefaultNode('图书馆', Math.max(0, nodes.value.length - 1))
    if (origin.value && destination.value && origin.value !== destination.value) await calculateRoute(false)
  } catch {
    store.notify('数字孪生导航数据加载失败，请检查后端', 'warning')
  }
}

async function calculateRoute(notify = true) {
  if (!origin.value || !destination.value || origin.value === destination.value) {
    store.notify('请选择不同的起点和终点', 'warning')
    return
  }
  loading.value = true
  try {
    route.value = await planRoute(origin.value, destination.value, false, true)
    if (notify) store.notify('路线已从 MySQL 路网重新计算')
  } catch (error) {
    route.value = null
    store.notify(error instanceof Error ? error.message : '路线计算失败', 'warning')
  } finally {
    loading.value = false
  }
}

function ensureTimer() {
  if (timer) window.clearInterval(timer)
  timer = window.setInterval(() => {
    if (!running.value) return
    progress.value = Math.min(100, progress.value + speed.value * .7)
    if (progress.value >= 100) {
      running.value = false
      store.notify('导航仿真已到达目的地')
    }
  }, 700)
}

async function startSimulation() {
  if (!route.value) await calculateRoute(false)
  if (!route.value) return
  if (progress.value >= 100) progress.value = 0
  running.value = true
  ensureTimer()
}

function pauseSimulation() {
  running.value = false
}

function restartSimulation() {
  progress.value = 0
  closureTriggered.value = false
  running.value = true
  ensureTimer()
}

async function simulateClosure() {
  closureTriggered.value = true
  running.value = false
  await calculateRoute(false)
  running.value = true
  ensureTimer()
  store.notify('已模拟道路封闭，并使用实时路网重新规划路线', 'warning')
}

async function onModelSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const extension = file.name.split('.').pop()?.toLowerCase()
  if (!extension || !['glb', 'gltf', 'obj', 'fbx', 'zip'].includes(extension)) {
    store.notify('仅支持 GLB、GLTF、OBJ、FBX 或 ZIP 模型文件', 'warning')
    return
  }
  const size = file.size >= 1024 * 1024 ? `${(file.size / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(file.size / 1024)} KB`
  try {
    const saved = await saveSettings({ twinModelName: file.name, twinModelSize: size, twinModelRegisteredAt: new Date().toISOString() })
    modelName.value = saved.twinModelName ?? file.name
    modelSize.value = saved.twinModelSize ?? size
    store.notify('模型信息已登记，等待三维解析服务接入')
  } catch {
    store.notify('模型信息登记失败', 'warning')
  } finally {
    (event.target as HTMLInputElement).value = ''
  }
}

onMounted(load)
onUnmounted(() => { if (timer) window.clearInterval(timer) })
</script>

<template>
  <div class="twin-live-page">
    <input ref="modelInput" class="hidden-file" type="file" accept=".glb,.gltf,.obj,.fbx,.zip" @change="onModelSelected">
    <div class="twin-stage">
      <CampusScene
        workspace
        simulation
        :places="places"
        :roads="roads"
        :route-plan="route"
        :simulation-progress="progress"
        :simulation-running="running"
        :blocked-event="closureTriggered"
        :selected-layer="selectedLayer"
        :model-ready="Boolean(modelName)"
      />

      <div class="twin-layer-bar panel">
        <button v-for="item in ['建筑','路网','人流','任务','环境']" :key="item" :class="{active:selectedLayer===item}" @click="selectedLayer=item">
          <i>{{ ({建筑:'▦',路网:'⌘',人流:'♟',任务:'☑',环境:'◉'} as Record<string,string>)[item] }}</i><span>{{ item }}</span>
        </button>
      </div>

      <aside class="panel simulation-inspector">
        <div class="simulation-head"><div><span class="cube-icon">◇</span><h2>导航仿真实况</h2></div><em :class="{active:running}"><i />{{ running ? '仿真运行中' : '仿真已暂停' }}</em></div>
        <div class="simulation-body">
          <h3>{{ routeTitle }}</h3>
          <div class="instruction"><span>↰</span><div><b>{{ currentStep ? `沿${cleanName(currentStep.roadName)}继续前行` : '等待路线计算' }}</b><small>{{ closureTriggered ? '已避开模拟封闭路段' : '数字孪生场景实时同步' }}</small></div></div>
          <div class="route-metrics"><div><span>🚶</span><b>{{ travelled }}<small>m</small></b><em>已行进</em></div><div><span>▦</span><b>{{ remaining }}<small>m</small></b><em>剩余</em></div><div><span>◷</span><b>{{ remainingMinutes }}<small>分钟</small></b><em>预计剩余</em></div></div>
          <div v-if="closureTriggered" class="reroute-alert"><b>⚠ 已检测到模拟封闭</b><span>系统已按当前 MySQL 路网重新计算可行路线</span></div>
          <div class="mini-route">
            <div class="mini-route-head"><b>路线概览</b><span>{{ Math.round(progress) }}%</span></div>
            <svg viewBox="0 0 300 155" preserveAspectRatio="none"><path d="M25 128 C65 112 79 80 115 91 S164 116 184 75 S235 53 273 25"/><path class="done" d="M25 128 C65 112 79 80 115 91 S164 116 184 75 S235 53 273 25" :style="{strokeDasharray:`${progress} 100`}" pathLength="100"/><circle cx="25" cy="128" r="6"/><circle cx="273" cy="25" r="6" class="end"/></svg>
            <div class="mini-label start">{{ cleanName(route?.origin || origin) }}</div><div class="mini-label finish">{{ cleanName(route?.destination || destination) }}</div>
          </div>
          <div class="evidence"><span v-for="item in (route?.evidence ?? ['等待路线证据']).slice(0,3)" :key="item">✓ {{ item }}</span></div>
        </div>
      </aside>

      <div class="scene-quick-actions"><button title="定位">◎</button><button title="放大">＋</button><button title="缩小">−</button><button title="旋转">↻</button><button title="登记模型" @click="modelInput?.click()">▱</button></div>

      <section class="panel simulation-dock">
        <div class="dock-title"><span>◇</span><b>导航路线仿真</b></div>
        <select v-model="origin" aria-label="仿真起点" @change="calculateRoute(false)"><option v-for="node in nodes" :key="node" :value="node">{{ cleanName(node) }}</option></select>
        <span class="route-arrow">→</span>
        <select v-model="destination" aria-label="仿真终点" @change="calculateRoute(false)"><option v-for="node in nodes" :key="node" :value="node">{{ cleanName(node) }}</option></select>
        <button v-if="!running" class="dock-btn run" :disabled="loading" @click="startSimulation">▶ {{ loading ? '计算中' : '开始' }}</button>
        <button v-else class="dock-btn pause" @click="pauseSimulation">Ⅱ 暂停</button>
        <button class="dock-btn reset" @click="restartSimulation">↻ 重新开始</button>
        <select v-model.number="speed" class="speed-select" aria-label="仿真速度"><option :value="0.5">0.5×</option><option :value="1">1×</option><option :value="2">2×</option></select>
        <div class="dock-progress"><span>路线进度 <b>{{ Math.round(progress) }}%</b></span><div><i :style="{width:`${progress}%`} "/></div></div>
        <div class="dock-time"><small>已用时</small><b>{{ elapsedMinutes }} 分钟</b></div>
        <button class="dock-btn danger" :class="{active:closureTriggered}" @click="simulateClosure">⚠ {{ closureTriggered ? '已模拟封路' : '模拟道路封闭' }}</button>
        <div class="dock-status"><span><i />{{ modelName ? `模型 ${modelSize}` : '演示场景' }}</span><span>⌁ 数据延迟 1.2s</span><span>ϟ 路线实时计算</span></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hidden-file{display:none}.twin-live-page{height:calc(100vh - 94px);min-height:700px}.twin-stage{height:100%;position:relative;overflow:hidden;border:1px solid rgba(205,221,240,.9);border-radius:17px;box-shadow:0 12px 35px rgba(42,78,128,.12)}.twin-stage>:deep(.scene-workspace){height:100%;min-height:0;border:0;border-radius:0}.twin-layer-bar{position:absolute;z-index:12;left:22px;top:22px;display:flex;padding:6px;border-radius:14px;background:rgba(255,255,255,.88);backdrop-filter:blur(15px)}.twin-layer-bar button{width:72px;height:58px;display:grid;place-items:center;align-content:center;gap:4px;border:0;border-right:1px solid #edf2f7;color:#506580;background:transparent}.twin-layer-bar button:last-child{border:0}.twin-layer-bar button.active{border-radius:10px;color:#1e72ef;background:#e9f3ff}.twin-layer-bar i{font-size:17px;font-style:normal}.twin-layer-bar span{font-size:10px}.simulation-inspector{position:absolute;z-index:13;right:14px;top:14px;bottom:94px;width:352px;border-color:rgba(255,255,255,.68);background:rgba(248,252,255,.9);backdrop-filter:blur(18px)}.simulation-head{height:68px;padding:0 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e3edf7}.simulation-head>div{display:flex;align-items:center;gap:9px}.cube-icon{width:31px;height:31px;display:grid;place-items:center;border-radius:9px;color:white;background:#2e7cf4}.simulation-head h2{margin:0;font-size:17px}.simulation-head em{padding:7px 9px;border:1px solid #f1c7c3;border-radius:8px;color:#d66b61;background:#fff5f3;font-size:9px;font-style:normal}.simulation-head em.active{color:#0b9c72;border-color:#9be3c9;background:#edfff8}.simulation-head em i{display:inline-block;width:6px;height:6px;margin-right:5px;border-radius:50%;background:currentColor}.simulation-body{padding:15px}.simulation-body>h3{margin:0 0 13px;font-size:20px}.instruction{display:flex;align-items:center;gap:11px;padding:13px;border-radius:11px;background:white;box-shadow:0 7px 20px rgba(37,76,127,.06)}.instruction>span{width:40px;height:40px;display:grid;place-items:center;border-radius:10px;color:white;background:#2f7df5;font-size:22px}.instruction b,.instruction small{display:block}.instruction b{font-size:12px}.instruction small{margin-top:4px;color:#8997aa;font-size:9px}.route-metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin:13px 0}.route-metrics>div{padding:10px 4px;border-radius:10px;background:rgba(255,255,255,.88);text-align:center}.route-metrics span,.route-metrics b,.route-metrics em{display:block}.route-metrics span{color:#2974e9}.route-metrics b{margin:4px 0 2px;font-size:18px}.route-metrics b small{font-size:9px}.route-metrics em{color:#8896aa;font-size:8px;font-style:normal}.reroute-alert{padding:10px 11px;border:1px solid #ffd6d2;border-radius:9px;color:#d84d43;background:#fff3f1}.reroute-alert b,.reroute-alert span{display:block}.reroute-alert b{font-size:10px}.reroute-alert span{margin-top:3px;color:#ae756f;font-size:8px}.mini-route{height:180px;position:relative;margin-top:13px;padding:11px;border:1px solid #e2ebf5;border-radius:11px;background:linear-gradient(145deg,#edf8f4,#f4f8fc);overflow:hidden}.mini-route-head{display:flex;justify-content:space-between;font-size:9px}.mini-route svg{position:absolute;inset:31px 12px 12px;width:calc(100% - 24px);height:130px}.mini-route path{fill:none;stroke:#8ac8ef;stroke-width:5;stroke-linecap:round}.mini-route path.done{stroke:#2274ed;stroke-width:6}.mini-route circle{fill:#2274ed;stroke:white;stroke-width:3}.mini-route circle.end{fill:#ff5b58}.mini-label{position:absolute;padding:3px 6px;border-radius:5px;color:white;background:#1d4e8e;font-size:7px}.mini-label.start{left:17px;bottom:7px}.mini-label.finish{right:17px;top:41px}.evidence{display:flex;flex-wrap:wrap;gap:5px;margin-top:10px}.evidence span{padding:4px 6px;border-radius:6px;color:#477190;background:#edf5fb;font-size:7px}.scene-quick-actions{position:absolute;z-index:12;right:378px;top:22px;display:grid;gap:7px}.scene-quick-actions button{width:39px;height:39px;border:1px solid rgba(255,255,255,.68);border-radius:9px;color:#184c83;background:rgba(255,255,255,.88);box-shadow:0 5px 14px rgba(24,62,105,.15)}.simulation-dock{position:absolute;z-index:14;left:14px;right:14px;bottom:14px;height:68px;padding:8px 11px;display:flex;align-items:center;gap:8px;border-color:rgba(255,255,255,.72);background:rgba(255,255,255,.91);backdrop-filter:blur(16px)}.dock-title{display:flex;align-items:center;gap:7px;padding-right:8px;white-space:nowrap}.dock-title span{width:30px;height:30px;display:grid;place-items:center;border-radius:8px;color:white;background:#2d79ef}.dock-title b{font-size:12px}.simulation-dock select{height:35px;min-width:104px;border:1px solid #dbe6f2;border-radius:8px;color:#344b69;background:#f8fbff;padding:0 8px}.route-arrow{color:#7f8ea3}.dock-btn{height:35px;padding:0 11px;border:1px solid transparent;border-radius:8px;font-size:9px;font-weight:700;white-space:nowrap}.dock-btn.run,.dock-btn.pause{color:white;background:#2f78f2}.dock-btn.reset{color:#426384;border-color:#dbe6f2;background:white}.dock-btn.danger{color:#df564c;background:#fff1ee}.dock-btn.danger.active{color:white;background:#eb5a50}.speed-select{min-width:62px!important}.dock-progress{width:160px}.dock-progress>span{display:flex;justify-content:space-between;color:#75849a;font-size:8px}.dock-progress>div{height:6px;margin-top:5px;overflow:hidden;border-radius:8px;background:#e7edf5}.dock-progress i{display:block;height:100%;border-radius:inherit;background:#2e78f1}.dock-time{padding:0 5px;white-space:nowrap}.dock-time small,.dock-time b{display:block}.dock-time small{color:#8997aa;font-size:7px}.dock-time b{margin-top:2px;font-size:9px}.dock-status{margin-left:auto;display:flex;gap:8px}.dock-status span{padding:7px 8px;border-radius:8px;color:#4a667f;background:#f2f7fb;font-size:7px;white-space:nowrap}.dock-status i{display:inline-block;width:6px;height:6px;margin-right:4px;border-radius:50%;background:#19c793}@media(max-width:1450px){.simulation-inspector{width:315px}.scene-quick-actions{right:341px}.dock-status span:nth-child(-n+2){display:none}.dock-progress{width:130px}}
</style>
