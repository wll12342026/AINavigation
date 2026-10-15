<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import { useAppStore } from '@/stores/app'
import { getMediaSummary, mediaApi, placesApi, type MediaAsset, type MediaSummary, type Place } from '@/services/api'

const store = useAppStore()
const rows = ref<MediaAsset[]>([])
const places = ref<Place[]>([])
const summary = ref<MediaSummary | null>(null)
const keyword = ref('')
const loading = ref(true)
const modalOpen = ref(false)
const form = reactive({ title: '', placeId: null as number | null, placeName: '', type: 'PHOTO', url: '', qualityScore: null as number | null, reviewStatus: 'PENDING', issueType: null as string | null })
const filtered = computed(() => rows.value.filter(item => !keyword.value || `${item.title}${item.placeName}`.toLowerCase().includes(keyword.value.toLowerCase())))

async function load() {
  loading.value = true
  try {
    const [items, totals, placeRows] = await Promise.all([mediaApi.list(), getMediaSummary(), placesApi.list()])
    rows.value = items; summary.value = totals; places.value = placeRows
  } catch { store.notify('实景素材加载失败，请检查后端', 'warning') }
  finally { loading.value = false }
}

async function save() {
  if (!form.title.trim()) return store.notify('请输入素材名称', 'warning')
  const place = places.value.find(item => item.id === form.placeId)
  form.placeName = place?.name ?? ''
  try {
    await mediaApi.create({ ...form })
    modalOpen.value = false
    Object.assign(form, { title: '', placeId: null, placeName: '', type: 'PHOTO', url: '', qualityScore: null, reviewStatus: 'PENDING', issueType: null })
    await load(); store.notify('素材元数据已保存到 MySQL')
  } catch { store.notify('素材保存失败', 'warning') }
}

async function remove(item: MediaAsset) {
  if (!confirm(`确定删除“${item.title}”吗？`)) return
  try { await mediaApi.remove(item.id); await load(); store.notify('素材记录已删除') }
  catch { store.notify('删除失败', 'warning') }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="实景素材" :value="String(summary?.total ?? 0)" icon="▣"/><StatCard label="已绑定地点" :value="String(summary?.boundPlaces ?? 0)" icon="⌖" tone="#18b68a" tint="#e8faf5"/><StatCard label="AI质检通过率" :value="`${summary?.passRate ?? 0}%`" icon="▦" tone="#7057ee" tint="#f0edff"/><StatCard label="待标注素材" :value="String(summary?.pending ?? 0)" icon="▣" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="page-tools"><div class="page-tools-left"><input v-model="keyword" class="filter-input" placeholder="搜索素材或地点"><span class="tag green">数据来自 MySQL</span></div><div class="page-tools-right"><button class="btn" @click="load">刷新</button><button class="btn primary" @click="modalOpen=true">+录入素材</button></div></div>
    <div class="content-split"><section class="panel"><div v-if="loading" class="empty-list">正在读取素材数据…</div><div v-else-if="!filtered.length" class="empty-list">暂无真实素材记录，请点击“录入素材”添加。</div><div v-else class="media-grid"><article v-for="item in filtered" :key="item.id" class="media-card"><div class="media-photo" :style="item.url ? {backgroundImage:`url(${item.url})`,backgroundSize:'cover'} : {}"><span class="media-check"/><span class="media-score">AI {{ item.qualityScore ?? '未质检' }}</span></div><div class="media-info"><b>{{ item.title }}</b><p>{{ item.placeName || '未绑定地点' }} · {{ item.reviewStatus }}</p><button class="panel-link" @click="remove(item)">删除</button></div></article></div><div class="pagination"><span class="metric-sub page-total">已显示 {{ filtered.length }} / {{ summary?.total ?? 0 }}</span></div></section>
    <aside class="grid"><article class="panel"><div class="panel-head"><div><h3>AI图片质检</h3><p>统计来自素材质检字段</p></div><span class="tag" :class="summary?.pending ? 'orange' : 'green'">{{ summary?.pending ?? 0 }} 待处理</span></div><div class="panel-pad property-list"><div class="property"><label>待处理</label><strong>{{ summary?.pending ?? 0 }} 张</strong></div><div class="property"><label>低清晰度</label><span>{{ summary?.lowQuality ?? 0 }} 张</span></div><div class="property"><label>方向错误</label><span>{{ summary?.wrongDirection ?? 0 }} 张</span></div><div class="property"><label>疑似重复</label><span>{{ summary?.duplicate ?? 0 }} 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>数据说明</b><p>当前页面不再生成素材名称、评分或问题数量；所有内容均来自后端素材表。</p></div></article></aside></div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false"><form class="modal-card" @submit.prevent="save"><div class="modal-title"><div><h3>录入实景素材</h3><p>保存素材地址和地点绑定信息</p></div><button type="button" @click="modalOpen=false">×</button></div><div class="form-grid"><label>素材名称<input v-model="form.title" required></label><label>绑定地点<select v-model="form.placeId"><option :value="null">未绑定</option><option v-for="place in places" :key="place.id" :value="place.id">{{ place.name }}</option></select></label><label class="form-wide">图片地址<input v-model="form.url" placeholder="https://... 或后续上传后的地址"></label><label>质检分数<input v-model.number="form.qualityScore" type="number" min="0" max="100"></label><label>审核状态<select v-model="form.reviewStatus"><option value="PENDING">待处理</option><option value="PASSED">已通过</option><option value="REJECTED">未通过</option></select></label></div><div class="modal-actions"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary">保存到数据库</button></div></form></div>
  </div>
</template>

<style scoped>.page-total{margin-right:auto}.media-info .panel-link{float:right}.empty-list{min-height:220px;display:grid;place-items:center;color:#8b98ab}</style>
