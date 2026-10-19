<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CampusScene from '@/components/common/CampusScene.vue'
import StatCard from '@/components/common/StatCard.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import { useAppStore } from '@/stores/app'
import {
  analyzeOperation,
  getDashboardOverview,
  mediaApi,
  placesApi,
  roadsApi,
  type AiAnalysis,
  type DashboardOverview,
  type MediaAsset,
  type Place,
  type RoadSegment,
} from '@/services/api'

const store = useAppStore()
const router = useRouter()
const overview = ref<DashboardOverview | null>(null)
const places = ref<Place[]>([])
const roads = ref<RoadSegment[]>([])
const media = ref<MediaAsset[]>([])
const dataSource = ref<'loading' | 'mysql' | 'error'>('loading')
const aiPrompt = ref('从东门去图书馆，避开施工道路')
const aiResult = ref<AiAnalysis | null>(null)
const analyzing = ref(false)

const taskColors = ['#18c99a', '#3c83f7', '#7655ec', '#ff8b45']
const taskIcons = ['✓', '▣', '⌂', '⌖']
const taskRows = computed(() => (overview.value?.tasks ?? []).map((item, index) => ({
  ...item,
  color: taskColors[index % taskColors.length],
  icon: taskIcons[index % taskIcons.length],
})))
const totalStudents = computed(() => overview.value?.tasks[0]?.total ?? 0)
const averageProgress = computed(() => overview.value?.tasks.length
  ? Math.round(overview.value.tasks.reduce((sum, item) => sum + item.progress, 0) / overview.value.tasks.length)
  : 0)
const openRoads = computed(() => roads.value.filter(item => item.status === 'OPEN').length)
const restrictedRoads = computed(() => roads.value.filter(item => item.status !== 'OPEN').length)
const destinationMedia = computed(() => media.value.filter(item => item.reviewStatus === 'APPROVED').slice(0, 3))
const consultationTrend = computed(() => overview.value?.consultationTrend ?? [])
const trendMax = computed(() => Math.max(2, ...consultationTrend.value.map(item => item.value)))
const trendPoints = computed(() => consultationTrend.value.map((item, index) => ({
  ...item,
  x: 36 + index / Math.max(1, consultationTrend.value.length - 1) * 414,
  y: 110 - item.value / trendMax.value * 86,
})))
const trendLine = computed(() => trendPoints.value.map(item => `${item.x},${item.y}`).join(' '))
const trendArea = computed(() => trendPoints.value.length ? `${trendPoints.value[0]!.x},110 ${trendLine.value} ${trendPoints.value.at(-1)!.x},110` : '')

async function loadOverview() {
  dataSource.value = 'loading'
  try {
    const [dashboard, placeRows, roadRows, mediaRows] = await Promise.all([
      getDashboardOverview(), placesApi.list(), roadsApi.list(), mediaApi.list(),
    ])
    overview.value = dashboard
    places.value = placeRows
    roads.value = roadRows
    media.value = mediaRows
    dataSource.value = 'mysql'
  } catch {
    overview.value = null
    dataSource.value = 'error'
    store.notify('首页数据加载失败，请确认后端和 MySQL 已启动', 'warning')
  }
}

async function runAnalysis() {
  if (!aiPrompt.value.trim()) return store.notify('请先输入需要查询或导航的问题', 'warning')
  analyzing.value = true
  aiResult.value = null
  try {
    aiResult.value = await analyzeOperation(aiPrompt.value)
    store.notify('已读取当前校园数据并返回查询结果')
  } catch {
    aiResult.value = null
    store.notify('AI分析失败，未使用前端演示答案', 'warning')
  } finally {
    analyzing.value = false
  }
}

onMounted(loadOverview)
</script>

<template>
  <div class="overview-page overview-reference">
    <article v-if="dataSource === 'error'" class="panel panel-pad data-state">
      <b>无法读取后端真实数据</b><span>请确认 Spring Boot 与 MySQL 已启动。</span><button class="btn primary" @click="loadOverview">重新连接</button>
    </article>
    <article v-else-if="!overview" class="panel panel-pad data-state">正在从 MySQL 汇总首页数据…</article>
    <template v-else>
      <div class="grid cols-4 stat-row">
        <StatCard label="校园地点" :value="String(overview.placeCount)" icon="⌖" :caption="`建筑 ${overview.buildingCount} · 服务点 ${overview.servicePointCount}`" />
        <StatCard label="路网里程" :value="`${overview.roadKilometers}km`" icon="⌘" tone="#079c76" tint="#e8f8f0" :caption="`节点 ${overview.roadNodeCount} · 道路 ${overview.roadCount}`" />
        <StatCard label="开放道路" :value="`${overview.openRoadRate}%`" icon="▦" tone="#00865e" tint="#e8faf5" caption="MySQL 路网实时汇总" />
        <StatCard label="待处理知识" :value="String(overview.pendingKnowledge)" icon="◫" tone="#f28b31" tint="#fff3e8" :caption="`${overview.expiringKnowledge} 条内容临期`" />
      </div>

      <div class="dashboard-grid reference-main">
        <CampusScene :places="places" :roads="roads" />
        <article class="panel ai-panel reference-ai">
          <div class="ai-head">
            <div class="ai-brand"><div class="ai-orb"><AppIcon name="agents" :size="26" /></div><div><h2>AI 校园路线助手</h2><p>规则检索版 · 数据库证据与路径计算</p></div></div>
            <span class="agent-online"><i />MYSQL</span>
          </div>
          <div class="ai-body">
            <div class="ai-query"><input v-model="aiPrompt" aria-label="AI分析指令" @keyup.enter="runAnalysis"><button :disabled="analyzing" @click="runAnalysis">{{ analyzing ? '…' : '➤' }}</button></div>
            <div class="chat-answer route-answer">
              <template v-if="aiResult">
                <div class="answer-title"><span class="answer-ok">✓</span><strong>{{ aiResult.title }}</strong></div>
                <p>{{ aiResult.summary }}</p>
                <div class="impact-chips"><span v-for="item in aiResult.affectedObjects.slice(0,4)" :key="item">{{ item }}</span></div>
                <details v-if="aiResult.sources?.length" class="knowledge-sources"><summary>查看知识来源（{{ aiResult.sources.length }}）</summary><div v-for="source in aiResult.sources" :key="source.id"><b>{{ source.title }}</b><p>{{ source.content }}</p><small>来源：{{ source.source || '待补充' }} · 更新：{{ source.updatedAt?.replace('T',' ').slice(0,16) || '未记录' }} · 有效期：{{ source.expiresAt || '未设置' }}</small></div></details>
                <details v-if="aiResult.route" class="knowledge-sources"><summary>查看路线步骤 · {{ aiResult.route.distanceMeters }}米</summary><p v-for="(step,index) in aiResult.route.steps" :key="`${step.roadId}-${index}`">{{ index+1 }}. {{ step.roadName }}：{{ step.fromNode }} → {{ step.toNode }}（{{ step.lengthMeters }}米）</p></details>
                <button class="route-open" @click="router.push('/network')">进入路网规划与验证</button>
              </template>
              <template v-else>
                <div class="answer-title"><span class="answer-ok">AI</span><strong>等待生成可信路线</strong></div>
                <p>输入起点、终点与限制条件，系统将读取数据库路网并给出可追溯结果。</p>
                <div class="assistant-steps"><span><i>1</i>识别出行需求</span><span><i>2</i>查询校园路网</span><span><i>3</i>返回路线依据</span></div>
                <button class="route-open" :disabled="analyzing" @click="runAnalysis">{{ analyzing ? '正在查询路网…' : '生成路线' }}</button>
              </template>
            </div>
            <div class="event-list compact-events">
              <div v-for="event in overview.events.slice(0,3)" :key="event.title" class="event"><i>✓</i><span><b>{{ event.title }}</b>{{ event.detail }}</span><time>{{ event.time }}</time></div>
            </div>
            <div class="assistant-road-status"><span><i />开放道路 <b>{{ openRoads }}</b></span><span><i class="restricted" />受限路段 <b>{{ restrictedRoads }}</b></span><button @click="router.push('/network')">查看路网 ›</button></div>
          </div>
        </article>
      </div>

      <div class="bottom-grid reference-bottom">
        <article class="panel compact-card">
          <div class="card-title-row"><h3><i />AI 咨询趋势</h3><button @click="router.push('/analytics')">运营分析 ›</button></div>
          <div class="consultation-summary"><strong>{{ overview.consultationsToday }}<small> 今日咨询</small></strong><span><i />数据库记录</span></div>
          <svg v-if="trendPoints.length" class="consultation-chart" viewBox="0 0 470 140" role="img" aria-label="今日 AI 咨询趋势，数据来自后台记录">
            <defs><linearGradient id="consultationFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#12a773" stop-opacity=".2"/><stop offset="100%" stop-color="#12a773" stop-opacity=".02"/></linearGradient></defs>
            <g v-for="tick in [0,1,2]" :key="tick"><line x1="36" :y1="24+43*tick" x2="450" :y2="24+43*tick" stroke="#e8f0ec"/><text x="27" :y="28+43*tick" text-anchor="end">{{ Math.round(trendMax*(1-tick/2)) }}</text></g>
            <polygon :points="trendArea" fill="url(#consultationFill)"/><polyline :points="trendLine" fill="none" stroke="#009c68" stroke-width="2.5" stroke-linejoin="round"/>
            <g v-for="(item,index) in trendPoints" :key="index"><circle :cx="item.x" :cy="item.y" r="3" fill="white" stroke="#009c68" stroke-width="1.5"><title>{{ item.label }}：{{ item.value }} 次咨询</title></circle><text v-if="index % Math.max(1,Math.ceil(trendPoints.length/6))===0 || index===trendPoints.length-1" :x="item.x" y="133" text-anchor="middle">{{ item.label }}</text></g>
          </svg>
          <div v-else class="trend-empty">暂无咨询记录，学生产生问询后展示趋势。</div>
        </article>

        <article class="panel compact-card">
          <div class="card-title-row"><h3><i />目的地实景</h3><button @click="router.push('/media')">管理素材 ›</button></div>
          <div v-if="destinationMedia.length" class="destination-grid">
            <button v-for="item in destinationMedia" :key="item.id" @click="router.push('/media')">
              <img v-if="item.url" :src="item.url" :alt="item.title"><span v-else class="photo-placeholder">▣</span>
              <b>{{ item.placeName || item.title }}</b><small>AI质检 {{ item.qualityScore ?? '—' }}</small>
            </button>
          </div>
          <div v-else class="empty-dest"><span>▣</span><p>上传校园实景后，目的地照片会显示在这里。</p><button @click="router.push('/media')">上传素材</button></div>
        </article>

        <article class="panel compact-card task-card">
          <div class="card-title-row"><h3><i />新生任务完成率</h3><button @click="router.push('/tasks')">查看全部 ›</button></div>
          <div class="task-compact-body">
            <div class="task-ring" :style="{'--task-progress':`${averageProgress}%`}"><strong>{{ averageProgress }}%</strong><span>{{ totalStudents }} 名新生</span></div>
            <div class="task-mini-list"><div v-for="item in taskRows.slice(0,4)" :key="item.name"><i :style="{background:item.color}">{{ item.icon }}</i><span>{{ item.name }}</span><b>{{ item.progress }}%</b></div></div>
          </div>
        </article>
      </div>
      <div v-if="overview.excludedDemoRecords" class="dashboard-data-note">统计口径：已自动排除 {{ overview.excludedDemoRecords }} 条历史演示记录；带【测试】标记的数据仅用于功能验证。</div>
    </template>
  </div>
</template>

<style scoped>
.knowledge-sources{font-size:11px;color:#60718b;padding:10px 0}.knowledge-sources summary{cursor:pointer;color:#326ed9}.knowledge-sources>div{padding:10px 0;border-bottom:1px solid #e7edf6}.knowledge-sources small{line-height:1.6;color:#8997ac}
.overview-page{max-width:1900px;margin:0 auto}.demo-note{position:absolute;right:24px;top:76px;z-index:2;padding:5px 9px;border-radius:999px;color:#a26b18;background:#fff6df;font-size:9px}.data-state{min-height:180px;display:flex;align-items:center;justify-content:center;gap:14px}.reference-main{grid-template-columns:minmax(0,3.3fr) minmax(330px,1fr)}.reference-main :deep(.map-card){min-height:545px}.reference-ai{min-height:545px}.ai-head{display:flex;align-items:center;justify-content:space-between}.ai-orb span{font-size:12px}.agent-online{display:flex;align-items:center;gap:5px;color:#a8f7df;font-size:8px}.agent-online i{width:6px;height:6px;border-radius:50%;background:#35e2b2;box-shadow:0 0 8px #35e2b2}.ai-query{height:48px;padding:5px 6px 5px 14px;display:flex;align-items:center;border:1px solid #dce8f7;border-radius:12px;background:white;box-shadow:0 7px 20px rgba(39,75,130,.06)}.ai-query input{min-width:0;flex:1;border:0;outline:0;color:#264163;background:transparent}.ai-query button{width:36px;height:36px;border:0;border-radius:9px;color:white;background:linear-gradient(145deg,#3c86ff,#1f67e9);font-size:17px}.route-answer{margin-top:12px}.answer-title{display:flex;align-items:center;gap:9px}.answer-title strong{font-size:14px}.answer-ok{width:27px;height:27px;display:grid;place-items:center;border-radius:50%;color:white;background:#17c89a;font-size:10px;font-weight:800}.impact-chips{display:flex;flex-wrap:wrap;gap:5px;margin:0 0 10px}.impact-chips span{padding:4px 7px;border-radius:6px;color:#5b6d88;background:#f1f5fb;font-size:8px}.route-open{width:100%;height:36px;border:0;border-radius:8px;color:white;background:#2f75f3;font-weight:700}.compact-events{margin-top:13px}.compact-events .event{padding:7px 4px;border-bottom:1px solid #edf2f7}.compact-events .event:last-child{border-bottom:0}.reference-bottom{grid-template-columns:1.08fr 1.08fr 1fr}.compact-card{min-height:150px;padding:15px 17px}.compact-card h3{display:flex;align-items:center;gap:8px;margin:0;color:#1d3152;font-size:14px}.compact-card h3>i{width:3px;height:17px;border-radius:5px;background:#2f75f3}.compact-card>small{display:block;margin-top:9px;color:#92a0b4;font-size:8px}.status-metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:14px}.status-metrics>div{padding:10px 6px;border-right:1px solid #edf1f7;text-align:center}.status-metrics>div:last-child{border:0}.status-metrics b{display:inline-grid;width:22px;height:22px;place-items:center;border-radius:7px;color:#2e74ee;background:#eaf3ff;font-style:normal}.status-metrics strong,.status-metrics span{display:block}.status-metrics strong{margin-top:4px;font-size:18px}.status-metrics span{margin-top:2px;color:#8795a9;font-size:8px}.status-metrics .warn b{color:#f05b49;background:#fff0ec}.card-title-row{display:flex;align-items:center;justify-content:space-between}.card-title-row button{border:0;color:#3976dd;background:transparent;font-size:9px}.destination-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:12px}.destination-grid button{min-width:0;padding:0;overflow:hidden;border:1px solid #e0e8f2;border-radius:9px;background:white;text-align:left}.destination-grid img,.photo-placeholder{width:100%;height:68px;display:grid;place-items:center;object-fit:cover;color:#4c82e7;background:linear-gradient(145deg,#b9d8ef,#e8e0be)}.destination-grid b,.destination-grid small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:0 7px}.destination-grid b{margin-top:6px;font-size:9px}.destination-grid small{margin:3px 0 6px;color:#97a3b4;font-size:7px}.empty-dest{height:105px;display:flex;align-items:center;justify-content:center;gap:9px;color:#8a98ab}.empty-dest>span{font-size:26px;color:#77a4ed}.empty-dest p{max-width:180px;font-size:9px}.empty-dest button{border:0;color:#2f72ef;background:transparent}.task-compact-body{display:grid;grid-template-columns:112px 1fr;align-items:center;gap:15px;margin-top:8px}.task-ring{width:96px;height:96px;display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:50%;background:radial-gradient(circle,#fff 0 54%,transparent 55%),conic-gradient(#2f75f3 0 var(--task-progress),#e9eef6 var(--task-progress) 100%)}.task-ring strong{font-size:20px}.task-ring span{margin-top:2px;color:#83a0c1;font-size:8px}.task-mini-list{display:grid;gap:7px}.task-mini-list>div{display:grid;grid-template-columns:20px 1fr auto;align-items:center;gap:7px;font-size:9px}.task-mini-list i{width:20px;height:20px;display:grid;place-items:center;border-radius:6px;color:white;font-style:normal}.task-mini-list span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#607089}.task-mini-list b{font-size:9px}.empty-dest{font-size:9px}
</style>
