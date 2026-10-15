<script setup lang="ts">
import { computed, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
const keyword = ref('')
const rows = [
  { name:'图书馆', type:'公共建筑', entrances:3, time:'08:00—22:00', status:'已发布' },
  { name:'校园卡中心', type:'办事服务', entrances:2, time:'08:30—17:30', status:'已发布' },
  { name:'第一学院报到点', type:'新生服务', entrances:1, time:'08:00—18:00', status:'已发布' },
  { name:'学生公寓 A 区', type:'宿舍', entrances:4, time:'全天', status:'已发布' },
  { name:'第一食堂', type:'生活服务', entrances:5, time:'06:30—21:00', status:'已发布' },
  { name:'校医院', type:'医疗服务', entrances:2, time:'08:00—22:00', status:'待审核' },
]
const filtered = computed(() => rows.filter((row) => row.name.includes(keyword.value) || row.type.includes(keyword.value)))
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="地点总数" value="128" icon="⌖"/><StatCard label="建筑地点" value="36" icon="▦" tone="#18b68a" tint="#e8faf5"/><StatCard label="新生服务点" value="21" icon="☑" tone="#7057ee" tint="#f0edff"/><StatCard label="待完善地点" value="7" icon="◫" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="page-tools"><div class="page-tools-left"><input v-model="keyword" class="filter-input" placeholder="搜索地点名称或标签"><select class="select-input"><option>全部类型</option></select><select class="select-input"><option>全部状态</option></select></div><div class="page-tools-right"><button class="btn" @click="store.notify('AI 正在补全语义标签')">AI补全语义标签</button><button class="btn primary" @click="store.notify('新增地点表单已打开')">+新增地点</button></div></div>
    <div class="content-split"><section class="panel"><table class="data-table"><thead><tr><th>地点名称</th><th>类型</th><th>入口</th><th>开放时间</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="row in filtered" :key="row.name"><td><div class="name-cell"><span class="poi-icon">▦</span>{{ row.name }}</div></td><td>{{ row.type }}</td><td>{{ row.entrances }} 个</td><td>{{ row.time }}</td><td><span :class="['tag', row.status === '待审核' ? 'orange' : 'green']">{{ row.status }}</span></td><td><button class="table-action" @click="store.notify(`正在编辑${row.name}`)">编辑</button><button class="table-action">查看</button></td></tr></tbody></table><div class="pagination"><span class="metric-sub page-total">共 128 条地点</span><button>‹</button><button class="active">1</button><button>2</button><button>3</button><button>›</button></div></section><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>图书馆</h3><p>BLD-LIB-001 · 已发布</p></div><span class="tag green">导航可用</span></div><div class="panel-pad property-list"><div class="property"><label>建筑坐标</label><span>126.50, 82.30</span></div><div class="property"><label>推荐入口</label><strong>南门主入口</strong></div><div class="property"><label>办事事项</label><span>新生报到、借阅开通</span></div><div class="property"><label>实景素材</label><span>12 张·入口 3 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 语义完善建议</b><p>根据最近 86 次新生问询，建议补充“借阅证开通”和“东侧自助服务机”两项语义。</p></div></article></aside></div>
  </div>
</template>

<style scoped>.page-total{margin-right:auto}</style>
