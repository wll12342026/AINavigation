<script setup lang="ts">
import StatCard from '@/components/common/StatCard.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
const names = ['东门入口全景','图书馆南门','校园卡中心入口','学生公寓 A 区','学院报到点','第一食堂东门','校医院正门','体育场西入口','综合服务中心']
const scores = [98,96,93,97,91,95,89,94,92]
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="实景图片" value="1,286" icon="▣"/><StatCard label="已绑定地点" value="128" icon="⌖" tone="#18b68a" tint="#e8faf5"/><StatCard label="AI质检通过率" value="94%" icon="▦" tone="#7057ee" tint="#f0edff"/><StatCard label="待标注素材" value="23" icon="▣" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="page-tools"><div class="page-tools-left"><input class="filter-input" placeholder="搜索素材或地点"><select class="select-input"><option>全部地点</option></select><select class="select-input"><option>全部类型</option></select></div><div class="page-tools-right"><button class="btn" @click="store.notify('AI 已开始批量质检')">AI批量质检</button><button class="btn primary" @click="store.notify('素材上传面板已打开')">+上传素材</button></div></div>
    <div class="content-split"><section class="panel"><div class="media-grid"><article v-for="(name,index) in names" :key="name" class="media-card"><div class="media-photo"><span class="media-check"/><span class="media-score">AI {{ scores[index] }}分</span></div><div class="media-info"><b>{{ name }}</b><p>{{ index%3===0?'入口指引':'建筑外观' }} · {{ index+2 }} 天前更新</p></div></article></div><div class="pagination"><span class="metric-sub page-total">已显示 9 / 1,286</span><button>‹</button><button class="active">1</button><button>2</button><button>3</button><button>›</button></div></section>
    <aside class="grid"><article class="panel"><div class="panel-head"><div><h3>AI图片质检</h3><p>检查清晰度、方向与地点一致性</p></div><span class="tag green">模型已就绪</span></div><div class="panel-pad property-list"><div class="property"><label>待处理</label><strong>23 张</strong></div><div class="property"><label>低清晰度</label><span class="tag orange">6 张</span></div><div class="property"><label>方向错误</label><span>3 张</span></div><div class="property"><label>疑似重复</label><span>8 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ 采集建议</b><p>图书馆东侧入口在夜间和雨天缺少对照照片，可能影响拍照识别效果。</p></div></article></aside></div>
  </div>
</template>

<style scoped>.page-total{margin-right:auto}</style>
