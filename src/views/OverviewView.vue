<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import CampusScene from '@/components/common/CampusScene.vue'
import { useAppStore } from '@/stores/app'
import { analyzeOperation, getDashboardOverview, placesApi, roadsApi, type AiAnalysis, type DashboardOverview, type Place, type RoadSegment } from '@/services/api'
const store = useAppStore()

const overview = ref<DashboardOverview | null>(null)
const places = ref<Place[]>([])
const roads = ref<RoadSegment[]>([])
const dataSource = ref<'loading' | 'mysql' | 'error'>('loading')
const aiPrompt = ref('')
const aiResult = ref<AiAnalysis | null>(null)
const taskColors = ['#1cc89b', '#3c83f7', '#7655ec', '#ff8b45']
const taskIcons = ['♟', '▣', '⌂', '⌖']
const taskRows = computed(() => (overview.value?.tasks ?? []).map((item, index) => ({
  ...item,
  color: taskColors[index % taskColors.length],
  icon: taskIcons[index % taskIcons.length],
})))
const totalStudents = computed(() => overview.value?.tasks[0]?.total ?? 0)
const averageProgress = computed(() => overview.value?.tasks.length
  ? Math.round(overview.value.tasks.reduce((sum, item) => sum + item.progress, 0) / overview.value.tasks.length)
  : 0)
const trendMax = computed(() => Math.max(1, ...(overview.value?.consultationTrend.map(item => item.value) ?? [1])))
const trendPoints = computed(() => (overview.value?.consultationTrend ?? []).map((item, index, values) => {
  const x = values.length <= 1 ? 0 : index * 400 / (values.length - 1)
  const y = 120 - item.value / trendMax.value * 100
  return `${x},${y}`
}).join(' '))

async function loadOverview() {
  try {
    const [dashboard, placeRows, roadRows] = await Promise.all([getDashboardOverview(), placesApi.list(), roadsApi.list()])
    overview.value = dashboard
    places.value = placeRows
    roads.value = roadRows
    dataSource.value = 'mysql'
  } catch {
    overview.value = null
    dataSource.value = 'error'
    store.notify('首页数据加载失败，请确认后端和 MySQL 已启动', 'warning')
  }
}

async function runAnalysis() {
  if (!aiPrompt.value.trim()) return store.notify('请先输入需要查询或分析的问题', 'warning')
  try {
    aiResult.value = await analyzeOperation(aiPrompt.value)
    store.notify('AI 已根据当前路网完成影响分析')
  } catch {
    aiResult.value = null
    store.notify('AI分析失败，未使用前端演示答案', 'warning')
  }
}

onMounted(loadOverview)
</script>

<template>
  <div class="overview-page">
    <article v-if="dataSource === 'error'" class="panel panel-pad data-error">
      <b>无法读取后端真实数据</b><span>请确认 Spring Boot 与 MySQL 已启动，然后重试。</span><button class="btn primary" @click="loadOverview">重新连接</button>
    </article>
    <article v-else-if="!overview" class="panel panel-pad data-loading">正在从 MySQL 汇总首页数据…</article>
    <template v-else>
    <div v-if="overview.excludedDemoRecords" class="demo-excluded"><b>已排除 {{ overview.excludedDemoRecords }} 条历史演示记录</b><span>首页只统计手工录入或正式导入的数据，旧记录仍安全保留在数据库中。</span></div>
    <div class="grid cols-4 stat-row">
      <StatCard label="校园地点" :value="String(overview.placeCount)" icon="⌖" :trend="`${overview.placeCount}条`" :caption="`建筑 ${overview.buildingCount} · 服务点 ${overview.servicePointCount}`" />
      <StatCard label="路网里程" :value="`${overview.roadKilometers}km`" icon="⌘" tone="#2f7af2" tint="#e8f5ff" :trend="`${overview.roadCount}条`" :caption="`节点 ${overview.roadNodeCount} · 道路 ${overview.roadCount}`" />
      <StatCard label="开放道路" :value="`${overview.openRoadRate}%`" icon="▦" tone="#13ba8e" tint="#e8faf5" :trend="overview.roadCount ? '实时' : '暂无'" caption="路网状态由后端实时汇总" />
      <StatCard label="待审核知识" :value="String(overview.pendingKnowledge)" icon="◫" tone="#7057ee" tint="#f0edff" :trend="`${overview.expiringKnowledge}条临期`" caption="知识内容等待管理员复核" />
    </div>
    <div class="dashboard-grid">
      <CampusScene :places="places" :roads="roads" />
      <article class="panel ai-panel">
        <div class="ai-head"><div class="ai-brand"><div class="ai-orb"><span>AI</span></div><div><h2>AI空间运营助手</h2><p>基于数据库证据解析校园空间</p></div></div><span class="agent-online"><i />MYSQL</span></div>
        <div class="ai-body">
          <div v-if="aiPrompt" class="chat-user"><span class="user-dot">管</span><p>{{ aiPrompt }}</p></div>
          <div class="chat-answer"><template v-if="aiResult"><div class="answer-title"><span class="mini-ai">AI</span><strong>{{ aiResult.title }}</strong></div><p>{{ aiResult.summary }}</p><div class="impact-chips"><span v-for="item in aiResult.affectedObjects" :key="item">{{ item }}</span></div></template><template v-else><div class="answer-title"><span class="mini-ai">AI</span><strong>等待真实数据分析</strong></div><p>输入问题后，系统将查询地点、路网、任务和知识库，不再显示预设结论。</p></template><div class="chat-actions"><button class="btn" @click="runAnalysis">开始分析</button></div></div>
          <div class="event-list"><div v-for="event in overview.events" :key="event.title" class="event"><i>✓</i><span><b>{{ event.title }}</b>{{ event.detail }}</span><time>{{ event.time }}</time></div></div>
          <div class="agent-input"><input v-model="aiPrompt" aria-label="AI分析指令" placeholder="向AI追问空间运行情况…" @keyup.enter="runAnalysis" /><button @click="runAnalysis">↑</button></div>
        </div>
      </article>
    </div>
    <div class="bottom-grid">
      <article class="panel panel-pad overview-chart"><div class="panel-heading"><SectionTitle title="AI咨询趋势" /><span class="tag green">今日真实记录</span></div><div class="chart-kpis"><span><b>{{ overview.consultationsToday }}</b> 今日问询</span><span class="up">MySQL 实时汇总</span></div><div class="line-chart"><div v-if="!overview.consultationTrend.some(item=>item.value)" class="chart-empty">产生咨询后显示趋势</div><svg v-else viewBox="0 0 400 130" preserveAspectRatio="none"><polyline :points="trendPoints" fill="none" stroke="#3275f3" stroke-width="3"/></svg></div><div class="chart-axis"><span v-for="item in overview.consultationTrend.filter((_,index)=>index%2===0)" :key="item.label">{{ item.label }}</span></div></article>
      <article class="panel panel-pad"><div class="panel-heading"><SectionTitle title="新生任务完成情况" /><button class="panel-link" @click="store.notify('已进入新生任务详情')">查看全部 ›</button></div><div class="task-summary"><span><b>{{ totalStudents }}</b> 今日新生</span><span><i />平均完成率 {{ averageProgress }}%</span></div><div class="progress-list overview-progress"><div v-for="item in taskRows" :key="item.name" class="progress-item"><i class="task-mini-icon" :style="{background:item.color}">{{ item.icon }}</i><b>{{ item.name }}</b><div class="progress-track"><i :style="{width:item.progress+'%',background:item.color}" /></div><span class="progress-num">{{ item.completed }}/{{ item.total }}　<b :style="{color:item.color}">{{ item.progress }}%</b></span></div></div></article>
      <article class="panel panel-pad"><div class="panel-heading"><SectionTitle title="知识库健康度" /><button class="panel-link" @click="store.notify('请从左侧进入AI知识库')">进入知识库 ›</button></div><div class="health"><div class="health-ring" :style="{ '--health': `${overview.knowledgeHealth}%` }"><strong>{{ overview.knowledgeHealth }}%</strong><small>{{ overview.knowledgeHealth >= 80 ? '健康' : overview.knowledgeHealth >= 50 ? '需复核' : '待完善' }}</small></div><div class="health-info"><div class="health-alert"><b>!</b><span>{{ overview.expiringKnowledge }} 条内容即将过期<small>数据来自知识库有效期</small></span><em>›</em></div><span class="metric-sub">最近更新</span><div v-if="overview.recentKnowledge.length" class="knowledge-updates"><div v-for="item in overview.recentKnowledge" :key="item.title"><i>▣</i><span>{{ item.title }}<small>{{ new Date(item.updatedAt).toLocaleString() }}</small></span></div></div><div v-else class="metric-sub">暂无知识更新记录</div></div></div></article>
    </div>
    </template>
  </div>
</template>

<style scoped>
.overview-page{max-width:1760px;margin:0 auto}.overview-progress{margin-top:13px}.panel-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.ai-head{display:flex;align-items:center;justify-content:space-between}.ai-orb span{font-size:13px;letter-spacing:.08em}.agent-online{display:flex;align-items:center;gap:5px;color:#97f7dd;font-size:8px;letter-spacing:.08em}.agent-online i{width:6px;height:6px;border-radius:50%;background:#38e5b5;box-shadow:0 0 8px #38e5b5}.chat-user{display:flex;align-items:flex-start;gap:8px;margin-left:24px}.chat-user p{margin:0;flex:1}.user-dot{width:25px;height:25px;flex:0 0 auto;display:grid;place-items:center;border-radius:8px;color:#3570d8;background:white;font-size:9px;font-weight:800}.answer-title{display:flex;align-items:center;gap:8px}.mini-ai{width:25px;height:25px;display:grid;place-items:center;border-radius:8px;color:white;background:linear-gradient(135deg,#51bffb,#6757f4);font-size:8px;font-weight:800}.impact-chips{display:flex;flex-wrap:wrap;gap:5px;margin:0 0 10px}.impact-chips span{padding:4px 7px;border-radius:6px;color:#5b6d88;background:#f1f5fb;font-size:8px}.agent-input{height:38px;margin-top:10px;padding:0 5px 0 11px;display:flex;align-items:center;border:1px solid #dfe7f2;border-radius:10px;background:white;color:#9aa6b7;font-size:9px}.agent-input span{flex:1}.agent-input button{width:28px;height:28px;border:0;border-radius:8px;color:white;background:#3478f5}.chart-tabs{display:flex;gap:2px;padding:2px;border-radius:7px;background:#f1f4f9}.chart-tabs button{height:25px;padding:0 9px;border:0;border-radius:6px;color:#8a97aa;background:transparent;font-size:8px}.chart-tabs button.active{color:#2f72ff;background:white;box-shadow:0 2px 8px rgba(38,69,118,.08)}.chart-kpis{display:flex;align-items:center;justify-content:space-between;margin-top:10px;color:#8a96a8;font-size:9px}.chart-kpis b{color:#25334d;font-size:18px;margin-right:4px}.chart-kpis .up{color:#14ad84;font-weight:700}.chart-tooltip{position:absolute;z-index:2;left:49%;top:7px;padding:6px 8px;border-radius:7px;color:white;background:#213553;box-shadow:0 5px 15px rgba(18,39,72,.22);font-size:9px}.chart-tooltip b,.chart-tooltip small{display:block}.chart-tooltip small{margin-top:2px;color:#c8d4e8;font-size:7px}.task-summary{display:flex;justify-content:space-between;margin-top:12px;padding:8px 10px;border-radius:9px;background:#f7f9fc;color:#7f8ca1;font-size:9px}.task-summary b{color:#23314a;font-size:13px;margin-right:3px}.task-summary i{display:inline-block;width:6px;height:6px;margin-right:4px;border-radius:50%;background:#1cc89b}.progress-item{grid-template-columns:28px 74px minmax(60px,1fr) 82px}.task-mini-icon{width:25px;height:25px;display:grid;place-items:center;border-radius:8px;color:white;font-size:10px;font-style:normal}.health-ring{grid-template-rows:1fr auto;align-content:center}.health-ring strong{align-self:end}.health-ring small{position:relative;align-self:start;margin-top:1px;color:#10a97f;font-size:8px}.health-alert{display:flex;align-items:center;gap:7px}.health-alert>b{width:22px;height:22px;display:grid;place-items:center;border-radius:7px;color:white;background:#ff7044}.health-alert span{flex:1;font-weight:700}.health-alert small{display:block;margin-top:2px;color:#e18a6d;font-size:7px;font-weight:400}.health-alert em{font-style:normal}.knowledge-updates{display:grid;grid-template-columns:repeat(3,1fr);gap:5px}.knowledge-updates>div{min-width:0}.knowledge-updates i{height:35px;display:grid;place-items:center;border-radius:7px;color:#4a78d4;background:linear-gradient(145deg,#dceeff,#e8e1c6);font-style:normal}.knowledge-updates span{display:block;margin-top:4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#59667b;font-size:7px}.knowledge-updates small{display:block;color:#99a5b6;font-size:6px;margin-top:1px}
.health-ring{
  justify-self:center;
  display:flex;
  flex-direction:column;
  align-items:center;
  justify-content:center;
  gap:2px;
  padding:0;
}
.health-ring::before{
  left:50%;
  top:50%;
  transform:translate(-50%,-50%);
}
.health-ring strong{
  position:relative;
  z-index:1;
  align-self:auto;
  line-height:1;
  font-variant-numeric:tabular-nums;
}
.health-ring small{
  position:relative;
  z-index:1;
  align-self:auto;
  margin:0;
  color:#10a97f;
  font-size:8px;
  line-height:1;
}
.agent-online.demo{color:#ffd48b}
.agent-online.demo i{background:#ffb44a;box-shadow:0 0 8px #ffb44a}
.data-error,.data-loading{min-height:180px;display:flex;align-items:center;justify-content:center;gap:14px}.data-error span{color:#7d899b}.chart-empty{height:130px;display:grid;place-items:center;color:#97a4b6;font-size:10px}
.demo-excluded{display:flex;align-items:center;gap:10px;margin-bottom:10px;padding:9px 13px;border:1px solid #ffe0ae;border-radius:10px;background:#fff8ea;color:#9a6517;font-size:9px}.demo-excluded span{color:#a67b3e}
.agent-input input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#526079;font:inherit}
.agent-input input::placeholder{color:#9aa6b7}
</style>
