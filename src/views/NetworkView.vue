<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { planRoute, planRouteFromPrompt, roadsApi, type RoadSegment, type RoutePlan } from '@/services/api'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const rows = ref<RoadSegment[]>([])
const selected = ref<RoadSegment | null>(null)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const keyword = ref('')
const mode = ref<'edit' | 'test' | 'changes'>('edit')
const routeStart = ref('')
const routeEnd = ref('')
const accessibleOnly = ref(false)
const avoidRestricted = ref(true)
const optimization = ref('DISTANCE')
const loadError = ref('')
const routePrompt = ref('从东门去图书馆，避开施工道路')
const planning = ref(false)
const routeResult = ref<RoutePlan | null>(null)
const routeError = ref('')
const emptyForm = () => ({ name: '', fromNode: '', toNode: '', lengthMeters: 100, status: 'OPEN', restrictionReason: '', accessible: true, bidirectional: true, travelSeconds: null as number | null })
const form = reactive(emptyForm())
const filtered = computed(() => rows.value.filter(row => `${row.name}${row.fromNode}${row.toNode}`.toLowerCase().includes(keyword.value.toLowerCase())))
const openCount = computed(() => rows.value.filter(row => row.status === 'OPEN').length)
const restrictedCount = computed(() => rows.value.length - openCount.value)
const openRate = computed(() => rows.value.length ? Math.round(openCount.value * 100 / rows.value.length) : 0)
const totalKm = computed(() => (rows.value.reduce((sum, row) => sum + row.lengthMeters, 0) / 1000).toFixed(1))
const nodes = computed(() => Array.from(new Set(rows.value.flatMap(row => [row.fromNode, row.toNode]).filter(Boolean))).sort())
const graphNodes = computed(() => nodes.value.map((name, index, all) => ({
  name, x: 450 + Math.sin(index * 2 * Math.PI / all.length) * 310,
  y: 310 - Math.cos(index * 2 * Math.PI / all.length) * 235,
})))
const graphEdges = computed(() => (mode.value === 'test' ? rows.value : filtered.value).flatMap(road => {
  const from = graphNodes.value.find(node => node.name === road.fromNode)
  const to = graphNodes.value.find(node => node.name === road.toNode)
  return from && to ? [{ road, from, to }] : []
}))
const changes = computed(() => [...rows.value].sort((a, b) => new Date(b.updatedAt ?? b.createdAt ?? 0).getTime() - new Date(a.updatedAt ?? a.createdAt ?? 0).getTime()))

async function load() {
  loadError.value = ''
  try {
    rows.value = await roadsApi.list()
    selected.value = rows.value.find(row => row.status !== 'OPEN') ?? rows.value[0] ?? null
    routeResult.value = null
  } catch (error) { rows.value = []; selected.value = null; routeResult.value = null; loadError.value = error instanceof Error ? error.message : '路网加载失败'; store.notify(loadError.value, 'warning') }
}
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); modalOpen.value = true }
function openEdit(row: RoadSegment) { editingId.value = row.id; Object.assign(form, { name: row.name, fromNode: row.fromNode, toNode: row.toNode, lengthMeters: row.lengthMeters, status: row.status, restrictionReason: row.restrictionReason ?? '', accessible: row.accessible, bidirectional: row.bidirectional !== false, travelSeconds: row.travelSeconds ?? null }); modalOpen.value = true }
async function save() {
  if (!form.name.trim() || !form.fromNode.trim() || !form.toNode.trim()) return store.notify('请完整填写路段名称和起止节点', 'warning')
  if (form.fromNode.trim() === form.toNode.trim()) return store.notify('起点与终点不能相同', 'warning')
  saving.value = true
  try {
    const saved = editingId.value ? await roadsApi.update(editingId.value, { ...form }) : await roadsApi.create({ ...form })
    const index = rows.value.findIndex(row => row.id === saved.id)
    if (index >= 0) rows.value[index] = saved; else rows.value.unshift(saved)
    selected.value = saved; modalOpen.value = false; routeResult.value = null
    store.notify('路网变更已写入 MySQL，首页指标将同步更新')
  } catch (error) { store.notify(error instanceof Error ? error.message : '路段保存失败', 'warning') } finally { saving.value = false }
}
async function remove(row: RoadSegment) {
  if (!window.confirm(`确认删除路段“${row.name}”吗？`)) return
  try { await roadsApi.remove(row.id); rows.value = rows.value.filter(item => item.id !== row.id); selected.value = rows.value[0] ?? null; store.notify('路段已删除') } catch { store.notify('路段删除失败', 'warning') }
}

async function runRouteTest() {
  routeResult.value = null
  routeError.value = ''
  if (!routeStart.value || !routeEnd.value) { routeError.value = '请选择起点和终点'; return }
  planning.value = true
  try {
    routeResult.value = await planRoute(routeStart.value, routeEnd.value, accessibleOnly.value, avoidRestricted.value, optimization.value)
    store.notify('路线已由后端实时路网引擎计算并记录')
  } catch (error) {
    routeError.value = error instanceof Error ? error.message : '路线计算失败'
  } finally { planning.value = false }
}

async function runAiRoute() {
  routeResult.value = null
  routeError.value = ''
  if (!routePrompt.value.trim()) { routeError.value = '请输入自然语言导航需求'; return }
  planning.value = true
  try {
    const result = await planRouteFromPrompt(routePrompt.value)
    routeStart.value = result.origin
    routeEnd.value = result.destination
    accessibleOnly.value = result.accessibleOnly
    avoidRestricted.value = result.avoidRestricted
    optimization.value = result.optimization ?? 'DISTANCE'
    routeResult.value = result
    store.notify('AI已提取起终点与约束，并调用真实路网引擎')
  } catch (error) {
    routeError.value = error instanceof Error ? error.message : 'AI路线解析失败'
  } finally { planning.value = false }
}

watch(mode, value => {
  routeResult.value = null
  routeError.value = ''
  if (value === 'test') {
    routeStart.value = nodes.value[0] ?? ''
    routeEnd.value = nodes.value[nodes.value.length - 1] ?? ''
  }
})
watch([routeStart, routeEnd, accessibleOnly, avoidRestricted, optimization], () => { routeResult.value = null; routeError.value = '' }, { flush: 'sync' })
onMounted(load)
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button :class="{active:mode==='edit'}" @click="mode='edit'">路网编辑</button><button :class="{active:mode==='test'}" @click="mode='test'">路线测试</button><button :class="{active:mode==='changes'}" @click="mode='changes'">变更记录</button></div><span class="tag green">{{ openRate }}% 道路可通行</span><span class="tag">{{ totalKm }} km</span></div><div class="page-tools-right"><input v-if="mode==='edit'" v-model="keyword" class="filter-input road-search" placeholder="搜索路段或节点"><button class="btn" @click="load">刷新</button><button v-if="mode==='edit'" class="btn primary" @click="openCreate">+新增路段</button></div></div>

    <p v-if="loadError" role="alert">{{ loadError }}，请点击刷新重试。</p>
    <div v-if="mode==='edit'" class="content-split"><section class="panel network-canvas">
      <div class="topology-caption">数据库道路拓扑 · 连线表示连接关系，不表示真实地理位置</div>
      <svg viewBox="0 0 900 640" role="img" aria-label="道路节点连接图">
        <defs><marker id="road-direction" viewBox="0 0 10 10" refX="17" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#678abd"/></marker></defs>
        <g v-for="edge in graphEdges" :key="edge.road.id" tabindex="0" role="button" :aria-label="edge.road.name" @click="selected=edge.road" @keydown.enter="selected=edge.road">
          <line :x1="edge.from.x" :y1="edge.from.y" :x2="edge.to.x" :y2="edge.to.y" stroke="transparent" stroke-width="24"/>
          <line :x1="edge.from.x" :y1="edge.from.y" :x2="edge.to.x" :y2="edge.to.y" :stroke="edge.road.status==='OPEN' ? '#73a8ee' : '#ef6874'" :stroke-width="selected?.id===edge.road.id?7:3" :stroke-dasharray="edge.road.status==='OPEN'?undefined:'10 7'" :marker-end="edge.road.bidirectional===false?'url(#road-direction)':undefined"/>
          <text :x="(edge.from.x+edge.to.x)/2" :y="(edge.from.y+edge.to.y)/2-8" text-anchor="middle" fill="#506781" font-size="11">{{ edge.road.name }} · {{ edge.road.lengthMeters }}m</text>
        </g>
        <g v-for="node in graphNodes" :key="node.name"><circle :cx="node.x" :cy="node.y" r="9" fill="white" stroke="#367ef3" stroke-width="3"/><text :x="node.x" :y="node.y+28" text-anchor="middle" fill="#23456d" font-size="13">{{ node.name }}</text></g>
      </svg>
      <div v-if="!nodes.length" class="topology-empty">暂无路网，请先新增道路节点连接。</div>
      <div class="scene-status"><span>路段 {{ rows.length }}</span><span>开放 {{ openCount }}</span><span>受限 {{ restrictedCount }}</span><span>节点 {{ nodes.length }}</span></div></section>
      <aside class="grid road-side"><article v-if="selected" class="panel"><div class="panel-head"><div><h3>{{ selected.name }}</h3><p>ROAD-{{ String(selected.id).padStart(3,'0') }} · {{ selected.fromNode }} → {{ selected.toNode }}</p></div><span :class="['tag',selected.status === 'OPEN' ? 'green' : 'red']">{{ selected.status === 'OPEN' ? '开放' : '受限' }}</span></div><div class="panel-pad property-list"><div class="property"><label>路段长度</label><strong>{{ selected.lengthMeters }} 米</strong></div><div class="property"><label>无障碍通行</label><span>{{ selected.accessible ? '支持' : '不支持' }}</span></div><div class="property"><label>限制原因</label><span>{{ selected.restrictionReason || '无' }}</span></div><div class="detail-actions"><button class="btn" @click="openEdit(selected)">编辑路段</button><button class="btn danger" @click="remove(selected)">删除</button></div></div></article><article class="panel road-list-panel"><div class="panel-head"><div><h3>数据库路段</h3><p>点击切换当前路段</p></div><span class="tag">{{ filtered.length }} 条</span></div><div class="road-list"><button v-for="road in filtered.slice(0,8)" :key="road.id" :class="{active:selected?.id===road.id}" @click="selected=road"><span><b>{{ road.name }}</b><small>{{ road.fromNode }} → {{ road.toNode }}</small></span><em :class="road.status === 'OPEN' ? 'open' : 'closed'">{{ road.status === 'OPEN' ? '开放' : '受限' }}</em></button><div v-if="!filtered.length" class="network-empty">暂无路段数据</div></div></article></aside></div>

    <div v-else-if="mode==='test'" class="route-planner">
      <section class="panel planner-conditions">
        <div class="panel-head"><h3>规划条件设置</h3><span class="tag green">实时路网</span></div>
        <div class="planner-form">
          <label>起点<select v-model="routeStart" class="select-input"><option value="">请选择起点</option><option v-for="node in nodes" :key="node">{{ node }}</option></select></label>
          <button class="swap-endpoints" aria-label="交换起点和终点" @click="[routeStart,routeEnd]=[routeEnd,routeStart]">↑↓ 交换起终点</button>
          <label>终点<select v-model="routeEnd" class="select-input"><option value="">请选择终点</option><option v-for="node in nodes" :key="node">{{ node }}</option></select></label>
          <label>规划策略<select v-model="optimization" class="select-input"><option value="DISTANCE">距离最短</option><option value="TIME">预计用时最少</option></select></label>
          <label class="planner-check"><span>仅选择无障碍道路</span><input v-model="accessibleOnly" type="checkbox"></label>
          <label class="planner-check"><span>避开施工 / 受限道路</span><input v-model="avoidRestricted" type="checkbox"></label>
          <button class="btn primary" :disabled="planning||!rows.length" @click="runRouteTest">{{ planning ? '计算中…' : '生成路线' }}</button>
        </div>
        <div class="planner-language"><h4>AI 自然语言规划</h4><p>描述地点和条件，由后端路网引擎计算路线。</p><textarea v-model="routePrompt" rows="3" aria-label="自然语言路线需求" placeholder="从东门去图书馆，避开施工道路"/><button class="btn soft" :disabled="planning||!rows.length" @click="runAiRoute">{{ planning ? '分析中…' : '解析并规划' }}</button></div>
      </section>
      <section class="panel planner-map">
        <div class="panel-head"><h3>校园道路连接图</h3><span class="tag">{{ nodes.length }} 个节点 · {{ rows.length }} 条道路</span></div>
        <div class="planner-graph">
          <svg v-if="nodes.length" viewBox="0 0 900 640" role="img" aria-label="规划路线拓扑图，非地理地图">
            <defs><marker id="planner-direction" viewBox="0 0 10 10" refX="17" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M0 0 10 5 0 10Z" fill="#73a78c"/></marker></defs>
            <g v-for="edge in graphEdges" :key="edge.road.id">
              <line :x1="edge.from.x" :y1="edge.from.y" :x2="edge.to.x" :y2="edge.to.y" :stroke="routeResult?.steps.some(step=>step.roadId===edge.road.id)?'#009a65':edge.road.status==='OPEN'?'#bdcec2':'#ee795c'" :stroke-width="routeResult?.steps.some(step=>step.roadId===edge.road.id)?8:3" :stroke-dasharray="edge.road.status==='OPEN'?undefined:'8 7'" :marker-end="edge.road.bidirectional===false?'url(#planner-direction)':undefined"/>
              <text :x="(edge.from.x+edge.to.x)/2" :y="(edge.from.y+edge.to.y)/2-10" text-anchor="middle" fill="#6a8270" font-size="14">{{ edge.road.lengthMeters }}m</text>
            </g>
            <g v-for="node in graphNodes" :key="node.name"><circle :cx="node.x" :cy="node.y" r="13" :fill="node.name===routeStart?'#009c68':node.name===routeEnd?'#ef7756':'white'" :stroke="node.name===routeEnd?'#ef7756':'#009c68'" stroke-width="3"/><text :x="node.x" :y="node.y+34" text-anchor="middle" fill="#284f39" font-size="15">{{ node.name }}</text><text v-if="node.name===routeStart||node.name===routeEnd" :x="node.x" :y="node.y+5" text-anchor="middle" fill="white" font-size="12">{{ node.name===routeStart?'起':'终' }}</text></g>
          </svg>
          <div v-else class="planner-empty">暂无道路数据，请先在路网编辑中录入路段。</div>
        </div>
        <div class="planner-legend"><span><i/>规划路线</span><span><i class="restricted"/>受限路段</span><span><i class="other"/>其他道路</span></div>
        <p class="planner-map-note">连线表示数据库连接关系，不代表真实坐标；接入模型并标定后可显示地理路线。</p>
      </section>
      <aside class="panel planner-result">
        <div class="panel-head"><h3>路线结果与依据</h3><span v-if="routeResult" class="tag green">已计算</span></div>
        <div v-if="routeError" class="planner-empty error" role="alert">{{ routeError }}</div>
        <template v-else-if="routeResult">
          <div class="planner-result-title"><span>推荐路线</span><h4>{{ routeResult.origin }} → {{ routeResult.destination }}</h4></div>
          <div class="planner-result-metrics"><div><b>{{ routeResult.distanceMeters }}<small> 米</small></b><span>路线长度</span></div><div><b>{{ routeResult.estimatedMinutes }}<small> 分钟</small></b><span>预计用时</span></div></div>
          <div class="planner-route-detail"><h4>路线关键点</h4><ol><li v-for="(step,index) in routeResult.steps" :key="`${step.roadId}-${index}`"><i>{{ index+1 }}</i><div><b>{{ step.roadName }}</b><span>{{ step.fromNode }} → {{ step.toNode }}</span><small>{{ step.lengthMeters }} 米</small></div></li><li v-if="!routeResult.steps.length">起点与终点相同，无需经过路段</li></ol></div>
          <div class="planner-evidence"><h4>路线依据与说明</h4><p v-for="item in routeResult.evidence" :key="item">✓ {{ item }}</p><details v-if="routeResult.blockedRoads.length"><summary>已排除 {{ routeResult.blockedRoads.length }} 条道路</summary><p>{{ routeResult.blockedRoads.join('；') }}</p></details></div>
        </template>
        <div v-else class="planner-empty"><span class="planner-empty-icon">⌘</span><h4>等待规划出行路线</h4><p>选择起终点与通行偏好，查看实际距离、路段及推荐依据。</p><small>路线来自后端计算，不预置演示方案。</small></div>
      </aside>
    </div>

    <section v-else class="panel"><div class="panel-head"><div><h3>路网变更记录</h3><p>按数据库更新时间排序</p></div><span class="tag">{{ changes.length }} 条</span></div><table class="data-table"><thead><tr><th>路段</th><th>起止节点</th><th>当前状态</th><th>无障碍</th><th>最后更新</th></tr></thead><tbody><tr v-for="road in changes" :key="road.id"><td>{{ road.name }}</td><td>{{ road.fromNode }} → {{ road.toNode }}</td><td><span :class="['tag',road.status==='OPEN'?'green':'red']">{{ road.status }}</span></td><td>{{ road.accessible?'支持':'不支持' }}</td><td>{{ road.updatedAt ? new Date(road.updatedAt).toLocaleString() : '暂无时间' }}</td></tr><tr v-if="!changes.length"><td colspan="5" class="network-empty">暂无路网变更记录</td></tr></tbody></table></section>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false"><form class="modal" @submit.prevent="save">
      <div class="modal-head"><h3>{{ editingId ? '编辑路段' : '新增路段' }}</h3><button type="button" class="modal-close" @click="modalOpen=false">×</button></div>
      <div class="modal-body form-grid">
        <div class="form-field full"><label>路段名称 *</label><input v-model="form.name" required></div>
        <div class="form-field"><label>起点节点 *</label><input v-model="form.fromNode" required list="road-node-options"></div>
        <div class="form-field"><label>终点节点 *</label><input v-model="form.toNode" required list="road-node-options"></div>
        <datalist id="road-node-options"><option v-for="node in nodes" :key="node" :value="node"/></datalist>
        <div class="form-field"><label>长度（米）*</label><input v-model.number="form.lengthMeters" type="number" min="1" required></div>
        <div class="form-field"><label>预计通行秒数（选填）</label><input v-model.number="form.travelSeconds" type="number" min="1" placeholder="未填按75米/分钟估算"></div>
        <div class="form-field"><label>通行方向</label><select v-model="form.bidirectional"><option :value="true">双向通行</option><option :value="false">单向：起点 → 终点</option></select></div>
        <div class="form-field"><label>状态</label><select v-model="form.status"><option value="OPEN">开放</option><option value="RESTRICTED">受限</option><option value="CLOSED">关闭</option></select></div>
        <div class="form-field"><label>无障碍通行</label><select v-model="form.accessible"><option :value="true">支持</option><option :value="false">不支持</option></select></div>
        <div class="form-field"><label>限制原因</label><input v-model="form.restrictionReason" placeholder="开放路段可留空"></div>
      </div>
      <div class="modal-foot"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存到数据库' }}</button></div>
    </form></div>
  </div>
</template>

<style scoped>
.topology-caption{position:absolute;top:20px;left:20px;z-index:1;font-size:12px;color:#667d99}.topology-empty{position:absolute;inset:0;display:grid;place-items:center;color:#7b8da3}.network-canvas svg g[role=button]{cursor:pointer}.modal{max-height:90vh;overflow:auto}
.road-search{width:180px}.road-side{grid-template-rows:auto 1fr}.road-list-panel{min-height:0}.road-list{padding:10px;display:grid;gap:6px;max-height:328px;overflow:auto}.road-list button{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:9px 10px;border:1px solid #e5ebf4;border-radius:9px;background:white;text-align:left}.road-list button.active{border-color:#82abfa;background:#f0f6ff}.road-list b,.road-list small{display:block}.road-list b{font-size:10px;color:#30405d}.road-list small{margin-top:3px;color:#8b98ac;font-size:8px}.road-list em{font-style:normal;font-size:8px}.road-list em.open{color:#12a77d}.road-list em.closed{color:#ea5762}.detail-actions{display:flex;gap:8px;margin-top:5px}.network-empty{padding:32px!important;text-align:center;color:#8b98ab}.route-test{min-height:560px}.ai-route-box{margin:18px 24px 0;padding:16px;display:grid;grid-template-columns:200px 1fr auto;align-items:center;gap:14px;border:1px solid #dce8ff;border-radius:13px;background:linear-gradient(135deg,#f5f9ff,#eef4ff)}.ai-route-box b,.ai-route-box small{display:block}.ai-route-box b{font-size:12px;color:#263b63}.ai-route-box small{margin-top:4px;color:#8190a8;font-size:9px;line-height:1.45}.ai-route-box input{height:40px;border:1px solid #d7e1ef;border-radius:9px;padding:0 12px;outline:none;background:white}.route-form{padding:18px 24px;display:flex;align-items:end;gap:14px;border-bottom:1px solid #edf1f6}.route-form label{display:grid;gap:7px;color:#66758c;font-size:10px}.route-form .check-row{display:flex;align-items:center;height:40px;white-space:nowrap}.route-state{min-height:300px;display:grid;place-items:center;color:#8996a9}.route-state.error{color:#db5962}.route-result{margin:24px;display:grid;grid-template-columns:190px 1fr;gap:24px;padding:22px;border:1px solid #dfe8f6;border-radius:14px;background:#f7faff}.route-result .route-metric{display:grid;place-items:center;align-content:center;border-right:1px solid #dce5f2}.route-result .route-metric b{font-size:30px;color:#2f72ef}.route-result .route-metric span{color:#657692;font-size:11px}.route-result .route-metric em{margin-top:8px;color:#7d8ba0;font-size:9px;font-style:normal}.route-detail ol{margin:0;padding-left:20px;display:grid;gap:10px;color:#526079;font-size:11px}.route-detail li b,.route-detail li span{display:block}.route-detail li span{margin-top:3px;color:#8a97aa;font-size:9px}.evidence-list{display:grid;gap:5px;margin-top:14px;padding:11px;border-radius:9px;color:#167f64;background:#eaf9f4;font-size:9px}.route-detail details{margin-top:10px;color:#78869b;font-size:9px}.route-detail details p{line-height:1.6}.advanced{align-items:stretch}@media(max-width:1200px){.ai-route-box{grid-template-columns:1fr}.route-form{flex-wrap:wrap}}
</style>
