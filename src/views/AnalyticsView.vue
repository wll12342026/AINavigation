<script setup lang="ts">
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
const bars = [42,55,48,68,61,78,73,88,82,95,74,67]
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button>今日</button><button class="active">近7天</button><button>近30天</button></div><select class="select-input"><option>全部新生</option></select></div><div class="page-tools-right"><button class="btn" @click="store.notify('分析报告已生成')">导出分析报告</button><button class="btn primary" @click="store.notify('AI 运营周报已生成')">AI生成周报</button></div></div>
    <div class="grid cols-4 stat-row"><StatCard label="AI问询量" value="8,642" icon="◫" trend="+18.6%"/><StatCard label="导航规划次数" value="3,286" icon="⌘" tone="#18b68a" tint="#e8faf5" trend="+12.4%"/><StatCard label="任务完成率" value="81.6%" icon="☑" tone="#7057ee" tint="#f0edff" trend="+6.2%"/><StatCard label="AI有效回答率" value="93.8%" icon="▦" tone="#ef7d3f" tint="#fff1e8" trend="+2.1%"/></div>
    <div class="grid cols-2"><article class="panel panel-pad"><SectionTitle title="AI咨询与导航趋势" /><div class="bar-chart"><div v-for="(value,index) in bars" :key="index" class="bar-col" :style="{height:value+'%'}"><span>{{ index+10 }}:00</span></div></div></article><article class="panel panel-pad"><SectionTitle title="新生问题分类" /><div class="donut-row"><div class="donut"><strong>8,642</strong></div><div class="legend-list"><span><i style="background:#367af4" />办事任务 38%</span><span><i style="background:#6b58ec" />地点导航 24%</span><span><i style="background:#18c79a" />校园生活 20%</span><span><i style="background:#ff9b55" />其他问题 18%</span></div></div></article></div>
    <div class="grid cols-3 analytics-bottom"><article class="panel panel-pad"><SectionTitle title="热门地点" /><div class="progress-list analytics-progress"><div v-for="(item,index) in [{n:'图书馆',v:92,t:'1,682'},{n:'校园卡中心',v:78,t:'1,426'},{n:'学生公寓',v:64,t:'1,108'},{n:'学院报到点',v:53,t:'926'}]" :key="item.n" class="progress-item"><b>{{ index+1 }} {{ item.n }}</b><div class="progress-track"><i :style="{width:item.v+'%'}" /></div><span class="progress-num">{{ item.t }}</span></div></div></article><article class="panel panel-pad"><SectionTitle title="任务完成率" /><div class="health analytics-health"><div class="health-ring blue-ring"><strong>82%</strong></div><div class="health-info"><span><b>392</b> 人已完成</span><span class="metric-sub">480 人已开始报到</span><span class="tag green">+较上周 6.2%</span></div></div></article><article class="panel panel-pad"><SectionTitle title="AI运营洞察" /><div class="insight-list analytics-insights"><div class="insight"><span class="insight-num">1</span><div><b>校园卡问询显著上升</b><p>近 24 小时上升 36%，建议将领取入口提至首页。</p></div></div><div class="insight"><span class="insight-num">2</span><div><b>东门导航误差较高</b><p>17 位用户在同一路口重新规划，建议补充实景指引。</p></div></div></div></article></div>
  </div>
</template>

<style scoped>
.analytics-bottom{margin-top:12px}.analytics-progress{margin-top:18px}.analytics-health{margin-top:22px}.analytics-insights{margin-top:14px}.progress-track i{background:#3479f4}.blue-ring{background:conic-gradient(#3479f4 0 82%,#e7edf4 82%)}
</style>
