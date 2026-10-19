<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import StatCard from '@/components/common/StatCard.vue'
import { placesApi, type Place } from '@/services/api'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const route = useRoute()
const rows = ref<Place[]>([])
const keyword = ref('')
const typeFilter = ref('ALL')
const statusFilter = ref('ALL')
const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const selected = ref<Place | null>(null)
const currentPage = ref(1)
const pageSize = 12

const categoryNames: Record<string, string> = {
  GATE: '校门入口', SERVICE: '办事服务', DORMITORY: '学生宿舍', LIBRARY: '图书馆',
  MEDICAL: '医疗服务', DINING: '生活服务', SPORT: '体育场馆',
}
const statusNames: Record<string, string> = { OPEN: '开放', MAINTENANCE: '维护中', CLOSED: '关闭' }
const emptyForm = () => ({
  name: '', category: 'SERVICE', building: '', floor: '1F', status: 'OPEN',
  longitude: null as number | null, latitude: null as number | null, entranceHint: '', description: '', aliases: '', openingHours: '',
})
const form = reactive(emptyForm())

const filtered = computed(() => rows.value.filter((row) => {
  const textMatch = !keyword.value || `${row.name}${row.aliases ?? ''}${row.building}${categoryNames[row.category] ?? row.category}`.includes(keyword.value)
  return textMatch && (typeFilter.value === 'ALL' || row.category === typeFilter.value)
    && (statusFilter.value === 'ALL' || row.status === statusFilter.value)
}))
const serviceCount = computed(() => rows.value.filter(row => row.category === 'SERVICE').length)
const buildingCount = computed(() => new Set(rows.value.map(row => row.building).filter(Boolean)).size)
const pendingCount = computed(() => rows.value.filter(row => row.status !== 'OPEN').length)
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const pagedRows = computed(() => filtered.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    rows.value = await placesApi.list()
    selected.value = rows.value[0] ?? null
  } catch (error) {
    rows.value = []; selected.value = null
    loadError.value = error instanceof Error ? error.message : '地点数据加载失败'
    store.notify(loadError.value, 'warning')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(row: Place) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, category: row.category, building: row.building ?? '', floor: row.floor ?? '', status: row.status,
    longitude: row.longitude, latitude: row.latitude, entranceHint: row.entranceHint ?? '', description: row.description ?? '',
    aliases: row.aliases ?? '', openingHours: row.openingHours ?? '',
  })
  modalOpen.value = true
}

async function save() {
  if (!form.name.trim()) return store.notify('请填写地点名称', 'warning')
  saving.value = true
  try {
    const payload = { ...form }
    const saved = editingId.value ? await placesApi.update(editingId.value, payload) : await placesApi.create(payload)
    const index = rows.value.findIndex(row => row.id === saved.id)
    if (index >= 0) rows.value[index] = saved
    else rows.value.unshift(saved)
    selected.value = saved
    modalOpen.value = false
    store.notify(editingId.value ? '地点信息已同步到 MySQL' : '新地点已保存到 MySQL')
  } catch (error) {
    store.notify(error instanceof Error ? error.message : '地点保存失败', 'warning')
  } finally {
    saving.value = false
  }
}

async function remove(row: Place) {
  if (!window.confirm(`确认删除“${row.name}”吗？`)) return
  try {
    await placesApi.remove(row.id)
    rows.value = rows.value.filter(item => item.id !== row.id)
    if (selected.value?.id === row.id) selected.value = rows.value[0] ?? null
    store.notify('地点已从 MySQL 删除')
  } catch {
    store.notify('删除失败，请稍后重试', 'warning')
  }
}

watch([keyword, typeFilter, statusFilter], () => { currentPage.value = 1 })
watch(() => route.query.q, value => { if (typeof value === 'string') keyword.value = value }, { immediate: true })
onMounted(load)
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row">
      <StatCard label="地点总数" :value="String(rows.length)" icon="⌖" caption="MySQL 实时数据" />
      <StatCard label="建筑地点" :value="String(buildingCount)" icon="▦" tone="#18b68a" tint="#e8faf5" />
      <StatCard label="新生服务点" :value="String(serviceCount)" icon="☑" tone="#7057ee" tint="#f0edff" />
      <StatCard label="待完善地点" :value="String(pendingCount)" icon="◫" tone="#ef7d3f" tint="#fff1e8" />
    </div>
    <div class="page-tools">
      <div class="page-tools-left">
        <input v-model="keyword" class="filter-input" placeholder="搜索地点名称、建筑或类型">
        <select v-model="typeFilter" class="select-input"><option value="ALL">全部类型</option><option v-for="(label,key) in categoryNames" :key="key" :value="key">{{ label }}</option></select>
        <select v-model="statusFilter" class="select-input"><option value="ALL">全部状态</option><option value="OPEN">开放</option><option value="MAINTENANCE">维护中</option><option value="CLOSED">关闭</option></select>
      </div>
      <div class="page-tools-right"><span :class="['tag',loadError?'red':'green']">{{ loading ? '正在连接…' : loadError ? '连接失败' : '● 数据已加载' }}</span><button class="btn" @click="load">刷新数据</button><button class="btn primary" @click="openCreate">+新增地点</button></div>
    </div>
    <div class="content-split">
      <section class="panel">
        <table class="data-table"><thead><tr><th>地点名称</th><th>类型</th><th>所在建筑</th><th>楼层</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-if="loading"><td colspan="6" class="table-state">正在从后端加载地点数据…</td></tr>
          <tr v-else-if="loadError"><td colspan="6" class="table-state">{{ loadError }}，请刷新重试</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="6" class="table-state">没有符合条件的地点</td></tr>
          <tr v-for="row in pagedRows" :key="row.id" :class="{ selected: selected?.id === row.id }" @click="selected = row"><td><div class="name-cell"><span class="poi-icon">▦</span>{{ row.name }}</div></td><td>{{ categoryNames[row.category] ?? row.category }}</td><td>{{ row.building || '待补充' }}</td><td>{{ row.floor || '—' }}</td><td><span :class="['tag', row.status === 'OPEN' ? 'green' : row.status === 'CLOSED' ? 'red' : 'orange']">{{ statusNames[row.status] ?? row.status }}</span></td><td><button class="table-action" @click.stop="openEdit(row)">编辑</button><button class="table-action danger-text" @click.stop="remove(row)">删除</button></td></tr>
        </tbody></table>
        <div class="pagination"><span class="metric-sub page-total">共 {{ filtered.length }} 条地点 · 第 {{ currentPage }} / {{ pageCount }} 页</span><button :disabled="currentPage===1" @click="currentPage--">‹</button><button v-for="page in pageCount" :key="page" :class="{active:currentPage===page}" @click="currentPage=page">{{ page }}</button><button :disabled="currentPage===pageCount" @click="currentPage++">›</button></div>
      </section>
      <aside class="grid">
        <article class="panel" v-if="selected"><div class="panel-head"><div><h3>{{ selected.name }}</h3><p>PLACE-{{ String(selected.id).padStart(3,'0') }} · {{ statusNames[selected.status] }}</p></div><span :class="['tag',selected.status === 'OPEN' ? 'green' : 'orange']">{{ selected.status === 'OPEN' ? '导航可用' : '暂不可用' }}</span></div><div class="panel-pad property-list"><div class="property"><label>建筑与楼层</label><strong>{{ selected.building }} · {{ selected.floor }}</strong></div><div class="property"><label>空间坐标</label><span>{{ selected.longitude }}, {{ selected.latitude }}</span></div><div class="property"><label>推荐入口</label><span>{{ selected.entranceHint || '待补充' }}</span></div><div class="property"><label>语义描述</label><span>{{ selected.description || '待补充' }}</span></div></div></article>
        <article class="panel panel-pad"><div class="ai-suggest"><b>地点语义信息</b><p>别名：{{ selected?.aliases || '待补充，可填写简称、俗称' }}</p><p>开放时间：{{ selected?.openingHours || '待补充，以现场公告为准' }}</p><p>别名可用于搜索与自然语言问路。开放时间目前作为文字说明，不自动判断是否营业。</p></div></article>
      </aside>
    </div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false">
      <form class="modal" @submit.prevent="save">
        <div class="modal-head"><h3>{{ editingId ? '编辑地点' : '新增地点' }}</h3><button type="button" class="modal-close" @click="modalOpen=false">×</button></div>
        <div class="modal-body form-grid">
          <div class="form-field"><label>地点名称 *</label><input v-model="form.name" required maxlength="255"></div>
          <div class="form-field"><label>地点类型 *</label><select v-model="form.category"><option v-for="(label,key) in categoryNames" :key="key" :value="key">{{ label }}</option><option v-if="!categoryNames[form.category]" :value="form.category">{{ form.category }}</option></select></div>
          <div class="form-field full"><label>别名 / 俗称</label><input v-model="form.aliases" maxlength="255" placeholder="如：一卡通中心，领卡处（用逗号分隔）"></div>
          <div class="form-field full"><label>开放时间</label><input v-model="form.openingHours" maxlength="255" placeholder="如：周一至周五 08:30—17:00，节假日另行通知"></div>
          <div class="form-field"><label>所在建筑</label><input v-model="form.building"></div>
          <div class="form-field"><label>楼层</label><input v-model="form.floor"></div>
          <div class="form-field"><label>经度（可暂不填）</label><input v-model.number="form.longitude" type="number" min="-180" max="180" step="0.000001"></div>
          <div class="form-field"><label>纬度（可暂不填）</label><input v-model.number="form.latitude" type="number" min="-90" max="90" step="0.000001"></div>
          <div class="form-field"><label>状态</label><select v-model="form.status"><option value="OPEN">开放</option><option value="MAINTENANCE">维护中</option><option value="CLOSED">关闭</option></select></div>
          <div class="form-field"><label>推荐入口</label><input v-model="form.entranceHint"></div>
          <div class="form-field full"><label>地点描述</label><textarea v-model="form.description"></textarea></div>
        </div>
        <div class="modal-foot"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存到数据库' }}</button></div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.page-total{margin-right:auto}.table-state{text-align:center!important;height:180px!important;color:#8a97aa!important}.data-table tbody tr{cursor:pointer}.data-table tr.selected td{background:#f0f6ff}.danger-text{color:#e2525c}.modal{max-height:90vh;overflow:auto}
</style>
