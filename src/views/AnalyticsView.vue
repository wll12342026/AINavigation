<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { useAppStore } from '@/stores/app'
import { getAnalyticsOverview, type AnalyticsOverview } from '@/services/api'

const store = useAppStore()
const days = ref(7)
const data = ref<AnalyticsOverview | null>(null)
const loading = ref(true)
const maxTrend = computed(() => Math.max(1, ...(data.value?.trend.map(item => item.value) ?? [1])))
const categoryTotal = computed(() => data.value?.categories.reduce((sum, item) => sum + item.count, 0) ?? 0)

async function load(value = days.value) {
  days.value = value; loading.value = true
  try { data.value = await getAnalyticsOverview(value) }
  catch { data.value = null; store.notify('运营分析加载失败，请检查后端', 'warning') }
  finally { loading.value = false }
}

onMounted(() => load())
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button :class="{active:days===1}" @click="load(1)">今日</button><button :class="{active:days===7}" @click="load(7)">近7天</button><button :class="{active:days===30}" @click="load(30)">近30天</button></div><span class="tag green">真实操作记录</span></div><div class="page-tools-right"><button class="btn" @click="load()">刷新数据</button></div></div>
    <article v-if="loading" class="panel panel-pad analytics-state">正在汇总数据库记录…</article>
    <article v-else-if="!data" class="panel panel-pad analytics-state">无法连接后端，未显示演示统计。</article>
    <template v-else>
      <div class="grid cols-4 stat-row"><StatCard label="AI问询量" :value="String(data.aiQueries)" icon="◫" :trend="`${data.aiQueryChangeRate}%`"/><StatCard label="导航规划次数" :value="String(data.navigationPlans)" icon="⌘" tone="#18b68a" tint="#e8faf5" trend="待记录"/><StatCard label="任务完成率" :value="`${data.taskCompletionRate}%`" icon="☑" tone="#7057ee" tint="#f0edff" :trend="`${data.completedTasks}/${data.totalTasks}`"/><StatCard label="AI有效回答率" :value="`${data.aiAnswerRate}%`" icon="▦" tone="#ef7d3f" tint="#fff1e8" trend="基于可核验证据"/></div>
      <div class="grid cols-2"><article class="panel panel-pad"><SectionTitle title="AI咨询趋势" /><div v-if="!data.trend.some(item=>item.value)" class="analytics-state">当前周期暂无咨询记录</div><div v-else class="bar-chart"><div v-for="item in data.trend" :key="item.label" class="bar-col" :style="{height:`${Math.max(6,item.value/maxTrend*100)}%`}"><span>{{ item.label }}</span></div></div></article><article class="panel panel-pad"><SectionTitle title="咨询类型分布" /><div v-if="!categoryTotal" class="analytics-state">暂无咨询类型数据</div><div v-else class="legend-list category-list"><span v-for="item in data.categories" :key="item.category"><i style="background:#367af4" />{{ item.category }} {{ Math.round(item.count/categoryTotal*100) }}%</span></div></article></div>
      <div class="grid cols-3 analytics-bottom"><article class="panel panel-pad"><SectionTitle title="咨询热门地点" /><div v-if="!data.popularPlaces.length" class="analytics-state">咨询中尚未提及已录入地点</div><div v-else class="progress-list analytics-progress"><div v-for="(item,index) in data.popularPlaces" :key="item.name" class="progress-item"><b>{{ index+1 }} {{ item.name }}</b><div class="progress-track"><i :style="{width:`${Math.max(8,item.count/data.popularPlaces[0].count*100)}%`}" /></div><span class="progress-num">{{ item.count }}</span></div></div></article><article class="panel panel-pad"><SectionTitle title="任务完成率" /><div class="health analytics-health"><div class="health-ring blue-ring" :style="{background:`conic-gradient(#3479f4 0 ${data.taskCompletionRate}%,#e7edf4 ${data.taskCompletionRate}%)`}"><strong>{{ data.taskCompletionRate }}%</strong></div><div class="health-info"><span><b>{{ data.completedTasks }}</b> 条完成记录</span><span class="metric-sub">{{ data.totalTasks }} 条任务总记录</span></div></div></article><article class="panel panel-pad"><SectionTitle title="数据洞察" /><div class="insight-list analytics-insights"><div v-for="(text,index) in data.insights" :key="text" class="insight"><span class="insight-num">{{ index+1 }}</span><div><b>数据库分析</b><p>{{ text }}</p></div></div></div></article></div>
    </template>
  </div>
</template>

<style scoped>
.analytics-bottom{margin-top:12px}.analytics-progress{margin-top:18px}.analytics-health{margin-top:22px}.analytics-insights{margin-top:14px}.progress-track i{background:#3479f4}.analytics-state{min-height:150px;display:grid;place-items:center;color:#8b98ab}.category-list{margin-top:24px;display:grid;gap:12px}.category-list span{display:flex;align-items:center;gap:8px}.category-list i{width:9px;height:9px;border-radius:50%}
</style>
