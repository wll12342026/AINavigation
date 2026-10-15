<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { roadsApi, type RoadSegment } from '@/services/api'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const rows = ref<RoadSegment[]>([])
const selected = ref<RoadSegment | null>(null)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const keyword = ref('')
const emptyForm = () => ({ name: '', fromNode: '', toNode: '', lengthMeters: 100, status: 'OPEN', restrictionReason: '', accessible: true })
const form = reactive(emptyForm())
const filtered = computed(() => rows.value.filter(row => `${row.name}${row.fromNode}${row.toNode}`.includes(keyword.value)))
const openCount = computed(() => rows.value.filter(row => row.status === 'OPEN').length)
const restrictedCount = computed(() => rows.value.length - openCount.value)
const openRate = computed(() => rows.value.length ? Math.round(openCount.value * 100 / rows.value.length) : 100)
const totalKm = computed(() => (rows.value.reduce((sum, row) => sum + row.lengthMeters, 0) / 1000).toFixed(1))

async function load() {
  try {
    rows.value = await roadsApi.list()
    selected.value = rows.value.find(row => row.status !== 'OPEN') ?? rows.value[0] ?? null
  } catch { store.notify('路网数据加载失败，请检查后端', 'warning') }
}
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); modalOpen.value = true }
function openEdit(row: RoadSegment) { editingId.value = row.id; Object.assign(form, { name: row.name, fromNode: row.fromNode, toNode: row.toNode, lengthMeters: row.lengthMeters, status: row.status, restrictionReason: row.restrictionReason ?? '', accessible: row.accessible }); modalOpen.value = true }
async function save() {
  if (!form.name.trim() || !form.fromNode.trim() || !form.toNode.trim()) return store.notify('请完整填写路段名称和起止节点', 'warning')
  saving.value = true
  try {
    const saved = editingId.value ? await roadsApi.update(editingId.value, { ...form }) : await roadsApi.create({ ...form })
    const index = rows.value.findIndex(row => row.id === saved.id)
    if (index >= 0) rows.value[index] = saved; else rows.value.unshift(saved)
    selected.value = saved; modalOpen.value = false
    store.notify('路网变更已写入 MySQL，首页指标将同步更新')
  } catch { store.notify('路段保存失败', 'warning') } finally { saving.value = false }
}
async function remove(row: RoadSegment) {
  if (!window.confirm(`确认删除路段“${row.name}”吗？`)) return
  try { await roadsApi.remove(row.id); rows.value = rows.value.filter(item => item.id !== row.id); selected.value = rows.value[0] ?? null; store.notify('路段已删除') } catch { store.notify('路段删除失败', 'warning') }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="page-tools"><div class="page-tools-left"><div class="segmented"><button class="active">路网编辑</button><button @click="store.notify('路线测试将使用当前数据库路网')">路线测试</button><button @click="store.notify('变更记录功能待接入审计表')">变更记录</button></div><span class="tag green">{{ openRate }}% 道路可通行</span><span class="tag">{{ totalKm }} km</span></div><div class="page-tools-right"><input v-model="keyword" class="filter-input road-search" placeholder="搜索路段或节点"><button class="btn" @click="load">刷新</button><button class="btn primary" @click="openCreate">+新增路段</button></div></div>
    <div class="content-split"><section class="panel network-canvas"><svg viewBox="0 0 900 640" preserveAspectRatio="none"><g fill="none" stroke="#79a7ef" stroke-width="4"><path d="M120 510 C210 460 190 360 310 330 S460 250 560 300 S690 245 790 160"/><path d="M310 330 C390 400 500 470 690 470"/><path d="M560 300 C590 200 520 150 420 110"/><path d="M560 300 C660 330 735 370 790 410"/></g><path v-if="restrictedCount" d="M310 330 C390 305 470 275 560 300" fill="none" stroke="#ff626c" stroke-width="7" stroke-dasharray="12 8"/><g fill="#fff" stroke="#4d8bf4" stroke-width="3"><circle cx="120" cy="510" r="7"/><circle cx="310" cy="330" r="7"/><circle cx="560" cy="300" r="7"/><circle cx="790" cy="160" r="7"/><circle cx="690" cy="470" r="7"/><circle cx="420" cy="110" r="7"/><circle cx="790" cy="410" r="7"/></g></svg><div class="node major" style="left:8%;top:78%">东门入口<br>N-001</div><div class="node" style="left:28%;top:47%">学院路口<br>N-016</div><div class="node closed" style="left:50%;top:43%">{{ selected?.name || '选择路段' }}<br>{{ selected?.status === 'OPEN' ? '开放' : '受限' }}</div><div class="node major" style="left:80%;top:20%">校园卡中心<br>N-042</div><div class="node" style="left:68%;top:72%">学生公寓<br>N-063</div><div class="node" style="left:38%;top:12%">图书馆西口<br>N-028</div><div class="scene-status"><span>路段 {{ rows.length }}</span><span>开放 {{ openCount }}</span><span>受限 {{ restrictedCount }}</span><span>MySQL 实时</span></div></section>
      <aside class="grid road-side"><article v-if="selected" class="panel"><div class="panel-head"><div><h3>{{ selected.name }}</h3><p>ROAD-{{ String(selected.id).padStart(3,'0') }} · {{ selected.fromNode }} → {{ selected.toNode }}</p></div><span :class="['tag',selected.status === 'OPEN' ? 'green' : 'red']">{{ selected.status === 'OPEN' ? '开放' : '受限' }}</span></div><div class="panel-pad property-list"><div class="property"><label>路段长度</label><strong>{{ selected.lengthMeters }} 米</strong></div><div class="property"><label>无障碍通行</label><span>{{ selected.accessible ? '支持' : '不支持' }}</span></div><div class="property"><label>限制原因</label><span>{{ selected.restrictionReason || '无' }}</span></div><div class="detail-actions"><button class="btn" @click="openEdit(selected)">编辑路段</button><button class="btn danger" @click="remove(selected)">删除</button></div></div></article><article class="panel road-list-panel"><div class="panel-head"><div><h3>数据库路段</h3><p>点击切换当前路段</p></div><span class="tag">{{ filtered.length }} 条</span></div><div class="road-list"><button v-for="road in filtered.slice(0,8)" :key="road.id" :class="{active:selected?.id===road.id}" @click="selected=road"><span><b>{{ road.name }}</b><small>{{ road.fromNode }} → {{ road.toNode }}</small></span><em :class="road.status === 'OPEN' ? 'open' : 'closed'">{{ road.status === 'OPEN' ? '开放' : '受限' }}</em></button></div></article></aside></div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false"><form class="modal" @submit.prevent="save"><div class="modal-head"><h3>{{ editingId ? '编辑路段' : '新增路段' }}</h3><button type="button" class="modal-close" @click="modalOpen=false">×</button></div><div class="modal-body form-grid"><div class="form-field full"><label>路段名称 *</label><input v-model="form.name" required></div><div class="form-field"><label>起点节点 *</label><input v-model="form.fromNode" required></div><div class="form-field"><label>终点节点 *</label><input v-model="form.toNode" required></div><div class="form-field"><label>长度（米）</label><input v-model.number="form.lengthMeters" type="number" min="0"></div><div class="form-field"><label>状态</label><select v-model="form.status"><option value="OPEN">开放</option><option value="RESTRICTED">受限</option><option value="CLOSED">关闭</option></select></div><div class="form-field"><label>无障碍通行</label><select v-model="form.accessible"><option :value="true">支持</option><option :value="false">不支持</option></select></div><div class="form-field"><label>限制原因</label><input v-model="form.restrictionReason" placeholder="开放路段可留空"></div></div><div class="modal-foot"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存到数据库' }}</button></div></form></div>
  </div>
</template>

<style scoped>
.road-search{width:180px}.road-side{grid-template-rows:auto 1fr}.road-list-panel{min-height:0}.road-list{padding:10px;display:grid;gap:6px;max-height:328px;overflow:auto}.road-list button{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:9px 10px;border:1px solid #e5ebf4;border-radius:9px;background:white;text-align:left}.road-list button.active{border-color:#82abfa;background:#f0f6ff}.road-list b,.road-list small{display:block}.road-list b{font-size:10px;color:#30405d}.road-list small{margin-top:3px;color:#8b98ac;font-size:8px}.road-list em{font-style:normal;font-size:8px}.road-list em.open{color:#12a77d}.road-list em.closed{color:#ea5762}.detail-actions{display:flex;gap:8px;margin-top:5px}.full-btn{width:100%;margin-top:12px}
</style>
