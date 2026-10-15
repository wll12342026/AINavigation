<script setup lang="ts">
import StatCard from '@/components/common/StatCard.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
const steps = [
  { title:'到达学院报到点', desc:'身份核验 · 录取通知书、身份证', time:'20 分钟' },
  { title:'完成入学资料审核', desc:'学院报到点 · 前置任务：身份核验', time:'15 分钟' },
  { title:'领取校园卡', desc:'校园卡中心 · 携带身份证原件', time:'25 分钟' },
  { title:'宿舍入住登记', desc:'学生公寓 A 区 · 领取钥匙和床品', time:'35 分钟' },
]
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="任务模板" value="12" icon="☑"/><StatCard label="任务节点" value="46" icon="▦" tone="#18b68a" tint="#e8faf5"/><StatCard label="平均步骤" value="4.6" icon="⌘" tone="#7057ee" tint="#f0edff"/><StatCard label="待审核变更" value="3" icon="◫" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>任务模板</h3><p>拖拽节点编排新生流程</p></div><button class="panel-link" @click="store.notify('已新建任务草稿')">+新建</button></div><div class="panel-pad doc-list"><div v-for="(item,index) in ['新生报到一日通','校园卡办理','宿舍入住','校医院体检']" :key="item" :class="['doc-item',{active:index===0}]"><span class="doc-ico">{{ index+1 }}</span><span><b>{{ item }}</b><small>{{ index+3 }} 个节点 · {{ index===3?'待审核':'已发布' }}</small></span><span>{{ index===0?'运行中':'⋮' }}</span></div></div><div class="panel-pad"><div class="ai-suggest"><b>✦ AI 发现新通知</b><p>最新入学通知将校园卡领取地点改为综合服务中心。</p><button class="btn primary full-btn" @click="store.notify('已生成任务变更草稿')">生成变更草稿</button></div></div></aside>
    <section class="panel"><div class="panel-head"><div><h3>新生报到一日通</h3><p>TASK-2026-001 · 预计 2 小时 35 分钟</p></div><div><button class="btn" @click="store.notify('已生成学生端预览')">预览学生端</button> <button class="btn primary" @click="store.notify('任务流程已保存发布')">保存发布</button></div></div><div class="workflow"><template v-for="(step,index) in steps" :key="step.title"><div class="flow-node"><span class="step">{{ index+1 }}</span><div><b>{{ step.title }}</b><p>{{ step.desc }}</p></div><span class="flow-meta">预计 {{ step.time }}</span></div><div class="flow-line" /></template><div class="flow-node"><span class="step">✓</span><div><b>完成报到</b><p>AI 根据开放时间和路况动态调整顺序</p></div><span class="tag green">自动触发</span></div></div></section></div>
  </div>
</template>

<style scoped>.full-btn{width:100%;margin-top:10px}</style>
