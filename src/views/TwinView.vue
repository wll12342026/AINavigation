<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CampusScene from '@/components/common/CampusScene.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { useAppStore } from '@/stores/app'
import { placesApi, roadsApi, type Place, type RoadSegment } from '@/services/api'
const store = useAppStore()
const places = ref<Place[]>([])
const roads = ref<RoadSegment[]>([])
const selected = computed(() => places.value[0] ?? null)
const poiProgress = computed(() => places.value.length ? Math.round(places.value.filter(item => item.longitude != null && item.latitude != null).length * 100 / places.value.length) : 0)
const roadProgress = computed(() => roads.value.length ? Math.round(roads.value.filter(item => item.fromNode && item.toNode).length * 100 / roads.value.length) : 0)
async function load() {
  try { [places.value, roads.value] = await Promise.all([placesApi.list(), roadsApi.list()]) }
  catch { store.notify('孪生语义数据加载失败，请检查后端', 'warning') }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">场景编辑</button><button>模型版本</button><button>语义标注</button></div><span class="tag orange">● 真实模型未接入</span></div><div class="page-tools-right"><button class="btn" @click="store.notify('AI 场景检查已启动')">AI 场景检查</button><button class="btn primary" @click="store.notify('已打开模型上传面板')">上传孪生模型</button></div></div>
    <div class="content-split"><CampusScene workspace :places="places" :roads="roads"/><aside class="grid"><article class="panel"><div class="panel-head"><div><h3>场景对象</h3><p>首条数据库语义对象</p></div><span :class="['tag',selected?'green':'orange']">{{ selected?'已读取':'暂无数据' }}</span></div><div v-if="selected" class="panel-pad property-list"><div class="property"><label>对象名称</label><strong>{{ selected.name }}</strong></div><div class="property"><label>对象 ID</label><span>{{ selected.id }}</span></div><div class="property"><label>经纬度</label><span>{{ selected.longitude ?? '未配置' }}, {{ selected.latitude ?? '未配置' }}</span></div><div class="property"><label>类型</label><span class="tag">{{ selected.category }}</span></div><div class="property"><label>数据状态</label><span :class="['tag',selected.longitude!=null&&selected.latitude!=null?'green':'orange']">{{ selected.longitude!=null&&selected.latitude!=null?'已配置':'等待真实坐标' }}</span></div></div><div v-else class="panel-pad metric-sub">请先在校园地点页面录入真实地点。</div></article><article class="panel panel-pad"><div class="ai-suggest"><b>数据接入说明</b><p>当前仅展示数据库语义对象；无人机模型导入前，系统不会生成虚假的模型完成度。</p></div><button class="btn primary twin-btn" @click="store.notify('请先在校园地点中补充真实坐标')">检查语义数据</button></article><article class="panel panel-pad"><SectionTitle title="接入进度" /><div class="progress-list twin-progress"><div v-for="item in [{n:'POI坐标',v:poiProgress,c:'#3479f4'},{n:'路网节点',v:roadProgress,c:'#18c79a'},{n:'3D模型',v:0,c:'#7657ed'}]" :key="item.n" class="progress-item compact"><b>{{ item.n }}</b><div class="progress-track"><i :style="{width:item.v+'%',background:item.c}" /></div><span>{{ item.v }}%</span></div></div></article></aside></div>
  </div>
</template>

<style scoped>.twin-btn{width:100%;margin-top:12px}.twin-progress{margin-top:15px}.progress-item.compact{grid-template-columns:90px 1fr 38px}</style>
