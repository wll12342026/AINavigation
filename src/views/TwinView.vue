<script setup lang="ts">
import CampusScene from '@/components/common/CampusScene.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">场景编辑</button><button>模型版本</button><button>语义标注</button></div><span class="tag orange">● 真实模型未接入</span></div><div class="page-tools-right"><button class="btn" @click="store.notify('AI 场景检查已启动')">AI 场景检查</button><button class="btn primary" @click="store.notify('已打开模型上传面板')">上传孪生模型</button></div></div>
    <div class="content-split"><CampusScene workspace /><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>场景对象</h3><p>当前选中的空间语义对象</p></div><span class="tag green">已启用</span></div><div class="panel-pad property-list"><div class="property"><label>对象名称</label><strong>图书馆</strong></div><div class="property"><label>对象 ID</label><span>BLD-LIB-001</span></div><div class="property"><label>场景坐标</label><span>126.50, 0, 82.30</span></div><div class="property"><label>类型</label><span class="tag">公共建筑</span></div><div class="property"><label>数据状态</label><span class="tag orange">等待真实坐标</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ AI 场景建议</b><p>当前建筑已绑定 3 个入口，但东侧入口缺少朝向和实景照片。</p></div><button class="btn primary twin-btn" @click="store.notify('已进入语义标注模式')">开始语义标注</button></article><article class="panel panel-pad"><SectionTitle title="接入进度" /><div class="progress-list twin-progress"><div v-for="item in [{n:'POI数据',v:78,c:'#3479f4'},{n:'路网数据',v:66,c:'#18c79a'},{n:'3D模型',v:8,c:'#7657ed'}]" :key="item.n" class="progress-item compact"><b>{{ item.n }}</b><div class="progress-track"><i :style="{width:item.v+'%',background:item.c}" /></div><span>{{ item.v }}%</span></div></div></article></aside></div>
  </div>
</template>

<style scoped>.twin-btn{width:100%;margin-top:12px}.twin-progress{margin-top:15px}.progress-item.compact{grid-template-columns:90px 1fr 38px}</style>
