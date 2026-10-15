<script setup lang="ts">
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="知识条目" value="2,486" icon="◫"/><StatCard label="可追溯率" value="96.2%" icon="☑" tone="#18b68a" tint="#e8faf5"/><StatCard label="知识健康度" value="92%" icon="▦" tone="#7057ee" tint="#f0edff"/><StatCard label="即将过期" value="7" icon="◫" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>知识来源</h3><p>文档解析、审核与版本追踪</p></div><button class="panel-link" @click="store.notify('知识文档上传面板已打开')">+上传</button></div><div class="panel-pad doc-list"><div v-for="(item,index) in ['2026级新生入学指南','校园卡办理通知','校园地点与开放时间','新生常见问题']" :key="item" :class="['doc-item',{active:index===0}]"><span class="doc-ico">{{ ['文','通','图','问'][index] }}</span><span><b>{{ item }}</b><small>{{ [286,42,196,363][index] }} 条知识 · {{ index+1 }}天内更新</small></span><span :class="['tag',index===1?'orange':'green']">{{ index===1?'待审核':'有效' }}</span></div></div></aside>
    <section class="grid"><article class="panel"><div class="panel-head"><div><h3>语义关系图谱</h3><p>任务—地点—部门—材料—时间关联</p></div><div class="segmented"><button class="active">图谱</button><button>条目</button></div></div><div class="semantic-graph"><svg viewBox="0 0 800 390"><g stroke="#bdcff0" stroke-width="1.5"><path d="M400 195 210 90M400 195 600 75M400 195 660 235M400 195 485 330M400 195 180 280M210 90 105 155M600 75 700 120M660 235 735 315M180 280 90 335"/></g></svg><span class="graph-label core" style="left:50%;top:50%">新生报到</span><span class="graph-label" style="left:26%;top:23%">学院报到点</span><span class="graph-label" style="left:75%;top:19%">学生工作处</span><span class="graph-label" style="left:82%;top:60%">录取通知书</span><span class="graph-label" style="left:61%;top:84%">新生报到日</span><span class="graph-label" style="left:22%;top:72%">身份证</span></div></article>
    <div class="grid cols-2"><article class="panel panel-pad"><SectionTitle title="AI问答测试" /><div class="chat-user test-question">校园卡在哪里领？需要带什么？</div><div class="chat-answer"><strong>请前往综合服务中心 1 楼</strong><p>携带身份证原件与录取通知书。服务时间 08:30—17:30。</p><span class="tag green">置信度 96%</span> <span class="tag">来源 2 条</span></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ 质量审计结果</b><p>7 条知识将在 3 天后过期；2 个办事时间与最新通知存在冲突。</p><button class="btn primary review-btn" @click="store.notify('已进入知识审核队列')">进入审核队列</button></div></article></div></section></div>
  </div>
</template>

<style scoped>.test-question{margin:15px 0 10px}.review-btn{width:100%;margin-top:10px}</style>
