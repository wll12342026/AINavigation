<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import CampusScene from '@/components/common/CampusScene.vue'
import { useAppStore } from '@/stores/app'
import { analyzeOperation, getDashboardOverview, type AiAnalysis, type DashboardOverview } from '@/services/api'
const store = useAppStore()

const fallback: DashboardOverview = {
  placeCount: 128,
  roadKilometers: 6.8,
  openRoadRate: 96,
  pendingKnowledge: 12,
  knowledgeHealth: 92,
  expiringKnowledge: 7,
  tasks: [
    { name: '学院报到', progress: 89, completed: 428, total: 480 },
    { name: '领取校园卡', progress: 82, completed: 392, total: 480 },
    { name: '宿舍入住', progress: 74, completed: 356, total: 480 },
    { name: '校园导览', progress: 65, completed: 310, total: 480 },
  ],
  events: [
    { title: '路网检查完成', detail: '已识别封闭路段及周边可通行道路', time: '14:23' },
    { title: '任务影响已分析', detail: '共影响 3 条新生任务路线', time: '14:23' },
    { title: '新路线已验证', detail: '绕行方案已通过路径仿真', time: '14:24' },
  ],
}

const overview = ref<DashboardOverview>(fallback)
const dataSource = ref<'loading' | 'mysql' | 'demo'>('loading')
const aiPrompt = ref('关闭图书馆东侧道路，并检查受影响的新生路线')
const aiResult = ref<AiAnalysis>({
  intent: 'ROAD_IMPACT_ANALYSIS',
  title: '影响 3 条任务路线，已生成绕行方案',
  summary: '施工将影响新生报到、校园卡领取和宿舍入住路线。已基于当前路网生成最优绕行，预计增加步行约 280 米。',
  confidence: 0.96,
  reasoningSteps: ['识别受影响路段', '匹配关联任务', '生成候选路线', '完成可通行性验证'],
  affectedObjects: ['报到路线', '领卡路线', '入住路线'],
})
const taskColors = ['#1cc89b', '#3c83f7', '#7655ec', '#ff8b45']
const taskIcons = ['♟', '▣', '⌂', '⌖']
const taskRows = computed(() => overview.value.tasks.map((item, index) => ({
  ...item,
  color: taskColors[index % taskColors.length],
  icon: taskIcons[index % taskIcons.length],
})))
const totalStudents = computed(() => overview.value.tasks[0]?.total ?? 480)
const averageProgress = computed(() => overview.value.tasks.length
  ? Math.round(overview.value.tasks.reduce((sum, item) => sum + item.progress, 0) / overview.value.tasks.length)
  : 0)

async function loadOverview() {
  try {
    overview.value = await getDashboardOverview()
    dataSource.value = 'mysql'
  } catch {
    dataSource.value = 'demo'
  }
}

async function runAnalysis() {
  try {
    aiResult.value = await analyzeOperation(aiPrompt.value)
    store.notify('AI 已根据当前路网完成影响分析')
  } catch {
    store.notify('后端暂未连接，已保留当前演示方案', 'warning')
  }
}

onMounted(loadOverview)
</script>

<template>
  <div class="overview-page">
    <div class="grid cols-4 stat-row">
      <StatCard label="校园地点" :value="String(overview.placeCount)" icon="⌖" trend="+8" caption="建筑 36 · 服务点 21" />
      <StatCard label="路网里程" :value="`${overview.roadKilometers}km`" icon="⌘" tone="#2f7af2" tint="#e8f5ff" trend="+0.4km" caption="节点 286 · 道路 94" />
      <StatCard label="开放道路" :value="`${overview.openRoadRate}%`" icon="▦" tone="#13ba8e" tint="#e8faf5" trend="良好" caption="路网状态由后端实时汇总" />
      <StatCard label="待审核知识" :value="String(overview.pendingKnowledge)" icon="◫" tone="#7057ee" tint="#f0edff" trend="-3" caption="知识内容等待管理员复核" />
    </div>
    <div class="dashboard-grid">
      <CampusScene />
      <article class="panel ai-panel">
        <div class="ai-head"><div class="ai-brand"><div class="ai-orb"><span>AI</span></div><div><h2>AI空间运营助手</h2><p>实时解析校园空间 · 辅助决策与任务调度</p></div></div><span class="agent-online" :class="{ demo: dataSource === 'demo' }"><i />{{ dataSource === 'mysql' ? 'MYSQL' : dataSource === 'demo' ? 'DEMO' : 'CONNECTING' }}</span></div>
        <div class="ai-body">
          <div class="chat-user"><span class="user-dot">管</span><p>{{ aiPrompt }}</p></div>
          <div class="chat-answer"><div class="answer-title"><span class="mini-ai">AI</span><strong>{{ aiResult.title }}</strong></div><p>{{ aiResult.summary }}</p><div class="impact-chips"><span v-for="item in aiResult.affectedObjects" :key="item">{{ item }} ×1</span></div><div class="chat-actions"><button class="btn" @click="runAnalysis">重新分析</button><button class="btn primary" @click="store.notify('绕行方案已同步至学生端')">应用调整</button></div></div>
          <div class="event-list"><div v-for="event in overview.events" :key="event.title" class="event"><i>✓</i><span><b>{{ event.title }}</b>{{ event.detail }}</span><time>{{ event.time }}</time></div></div>
          <div class="agent-input"><input v-model="aiPrompt" aria-label="AI分析指令" placeholder="向AI追问空间运行情况…" @keyup.enter="runAnalysis" /><button @click="runAnalysis">↑</button></div>
        </div>
      </article>
    </div>
    <div class="bottom-grid">
      <article class="panel panel-pad overview-chart"><div class="panel-heading"><SectionTitle title="AI咨询趋势" /><div class="chart-tabs"><button class="active">今日</button><button>近7天</button><button>近30天</button></div></div><div class="chart-kpis"><span><b>1,284</b> 今日问询</span><span class="up">↗ 18.6%</span></div><div class="line-chart"><span class="chart-tooltip"><b>168</b><small>14:00 咨询量</small></span><svg viewBox="0 0 400 130" preserveAspectRatio="none"><defs><linearGradient id="area" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3778f5" stop-opacity=".32"/><stop offset="1" stop-color="#3778f5" stop-opacity="0"/></linearGradient></defs><path d="M0 115 C35 108 45 92 72 95 S112 70 138 73 S170 30 195 54 S220 18 245 44 S272 52 291 43 S321 77 343 57 S370 72 400 62 L400 130 L0 130Z" fill="url(#area)"/><path d="M0 115 C35 108 45 92 72 95 S112 70 138 73 S170 30 195 54 S220 18 245 44 S272 52 291 43 S321 77 343 57 S370 72 400 62" fill="none" stroke="#3275f3" stroke-width="3"/><circle cx="220" cy="18" r="5" fill="#3275f3" stroke="white" stroke-width="3"/></svg></div><div class="chart-axis"><span>00:00</span><span>04:00</span><span>08:00</span><span>12:00</span><span>16:00</span><span>20:00</span></div></article>
      <article class="panel panel-pad"><div class="panel-heading"><SectionTitle title="新生任务完成情况" /><button class="panel-link" @click="store.notify('已进入新生任务详情')">查看全部 ›</button></div><div class="task-summary"><span><b>{{ totalStudents }}</b> 今日新生</span><span><i />平均完成率 {{ averageProgress }}%</span></div><div class="progress-list overview-progress"><div v-for="item in taskRows" :key="item.name" class="progress-item"><i class="task-mini-icon" :style="{background:item.color}">{{ item.icon }}</i><b>{{ item.name }}</b><div class="progress-track"><i :style="{width:item.progress+'%',background:item.color}" /></div><span class="progress-num">{{ item.completed }}/{{ item.total }}　<b :style="{color:item.color}">{{ item.progress }}%</b></span></div></div></article>
      <article class="panel panel-pad"><div class="panel-heading"><SectionTitle title="知识库健康度" /><button class="panel-link" @click="store.notify('已进入知识库健康检查')">进入知识库 ›</button></div><div class="health"><div class="health-ring" :style="{ '--health': `${overview.knowledgeHealth}%` }"><strong>{{ overview.knowledgeHealth }}%</strong><small>健康</small></div><div class="health-info"><div class="health-alert"><b>!</b><span>{{ overview.expiringKnowledge }} 条内容即将过期<small>建议本周完成复核</small></span><em>›</em></div><span class="metric-sub">最近更新</span><div class="knowledge-updates"><div v-for="item in [{i:'⌂',n:'东门入口',t:'10:24'},{i:'⌘',n:'图书馆东路',t:'09:18'},{i:'▣',n:'校园卡中心',t:'昨天'}]" :key="item.n"><i>{{ item.i }}</i><span>{{ item.n }}<small>{{ item.t }}</small></span></div></div></div></div></article>
    </div>
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
.agent-input input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#526079;font:inherit}
.agent-input input::placeholder{color:#9aa6b7}
</style>
