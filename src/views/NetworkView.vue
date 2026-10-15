<script setup lang="ts">
import { useAppStore } from '@/stores/app'
const store = useAppStore()
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">路网编辑</button><button>路线测试</button><button>变更记录</button></div><span class="tag green">96% 道路可通行</span></div><div class="page-tools-right"><button class="btn" @click="store.notify('路网质检完成，发现 3 项建议')">AI路网质检</button><button class="btn primary" @click="store.notify('已进入新增路段模式')">+新增路段</button></div></div>
    <div class="content-split"><section class="panel network-canvas"><svg viewBox="0 0 900 640" preserveAspectRatio="none"><g fill="none" stroke="#79a7ef" stroke-width="4"><path d="M120 510 C210 460 190 360 310 330 S460 250 560 300 S690 245 790 160"/><path d="M310 330 C390 400 500 470 690 470"/><path d="M560 300 C590 200 520 150 420 110"/><path d="M560 300 C660 330 735 370 790 410"/></g><path d="M310 330 C390 305 470 275 560 300" fill="none" stroke="#ff626c" stroke-width="7" stroke-dasharray="12 8"/><g fill="#fff" stroke="#4d8bf4" stroke-width="3"><circle cx="120" cy="510" r="7"/><circle cx="310" cy="330" r="7"/><circle cx="560" cy="300" r="7"/><circle cx="790" cy="160" r="7"/><circle cx="690" cy="470" r="7"/><circle cx="420" cy="110" r="7"/><circle cx="790" cy="410" r="7"/></g></svg><div class="node major" style="left:8%;top:78%">东门入口<br>N-001</div><div class="node" style="left:28%;top:47%">学院路口<br>N-016</div><div class="node closed" style="left:50%;top:43%">图书馆东路<br>施工中</div><div class="node major" style="left:80%;top:20%">校园卡中心<br>N-042</div><div class="node" style="left:68%;top:72%">学生公寓<br>N-063</div><div class="node" style="left:38%;top:12%">图书馆西口<br>N-028</div><div class="scene-status"><span>节点 186</span><span>路段 244</span><span>闭合路段 2</span></div></section>
    <aside class="grid"><article class="panel"><div class="panel-head"><div><h3>图书馆东路</h3><p>ROAD-056 · 已关闭</p></div><span class="tag red">施工</span></div><div class="panel-pad property-list"><div class="property"><label>路段长度</label><strong>126 米</strong></div><div class="property"><label>通行方向</label><span>双向步行</span></div><div class="property"><label>影响路线</label><span class="tag red">3 条</span></div><div class="property"><label>关闭时间</label><span>10月15日—10月22日</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 路网质检</b><p>发现 2 个路网断点与 1 条疑似重复路段。建议将西侧林荫路设为首选绕行道路。</p></div><button class="btn primary full-btn" @click="store.notify('路径仿真完成，绕行方案可行')">运行路径仿真</button></article></aside></div>
  </div>
</template>

<style scoped>.full-btn{width:100%;margin-top:12px}</style>
