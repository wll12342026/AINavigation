<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import { tasksApi, type StudentTask } from '@/services/api'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const rows = ref<StudentTask[]>([])
const selected = ref<StudentTask | null>(null)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const emptyForm = () => ({ name: '', description: '', location: '', status: 'ACTIVE', progress: 0, completedCount: 0, totalCount: 480, estimatedMinutes: 20, sortOrder: rows.value.length + 1 })
const form = reactive(emptyForm())
const averageProgress = computed(() => rows.value.length ? Math.round(rows.value.reduce((sum,row) => sum + row.progress, 0) / rows.value.length) : 0)
const totalMinutes = computed(() => rows.value.reduce((sum,row) => sum + row.estimatedMinutes, 0))
const pendingCount = computed(() => rows.value.filter(row => row.status !== 'ACTIVE').length)

async function load() {
  try { rows.value = await tasksApi.list(); selected.value = rows.value[0] ?? null }
  catch { store.notify('任务数据加载失败，请检查后端', 'warning') }
}
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); modalOpen.value = true }
function openEdit(row: StudentTask) { editingId.value = row.id; Object.assign(form, { name: row.name, description: row.description ?? '', location: row.location ?? '', status: row.status, progress: row.progress, completedCount: row.completedCount, totalCount: row.totalCount, estimatedMinutes: row.estimatedMinutes, sortOrder: row.sortOrder }); modalOpen.value = true }
async function save() {
  if (!form.name.trim()) return store.notify('请输入任务名称', 'warning')
  saving.value = true
  try {
    const saved = editingId.value ? await tasksApi.update(editingId.value, { ...form }) : await tasksApi.create({ ...form })
    const index = rows.value.findIndex(row => row.id === saved.id)
    if (index >= 0) rows.value[index] = saved; else rows.value.push(saved)
    rows.value.sort((a,b) => a.sortOrder - b.sortOrder)
    selected.value = saved; modalOpen.value = false; store.notify('任务流程已保存到 MySQL')
  } catch { store.notify('任务保存失败', 'warning') } finally { saving.value = false }
}
async function remove(row: StudentTask) {
  if (!window.confirm(`确认删除任务“${row.name}”吗？`)) return
  try { await tasksApi.remove(row.id); rows.value = rows.value.filter(item => item.id !== row.id); selected.value = rows.value[0] ?? null; store.notify('任务已删除') } catch { store.notify('任务删除失败', 'warning') }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="任务节点" :value="String(rows.length)" icon="☑" caption="MySQL 实时数据"/><StatCard label="预计总时长" :value="`${totalMinutes}分钟`" icon="▦" tone="#18b68a" tint="#e8faf5"/><StatCard label="平均完成率" :value="`${averageProgress}%`" icon="⌘" tone="#7057ee" tint="#f0edff"/><StatCard label="非运行任务" :value="String(pendingCount)" icon="◫" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>任务节点</h3><p>来自后端的学生办事流程</p></div><button class="panel-link" @click="openCreate">+新建</button></div><div class="panel-pad doc-list"><button v-for="(item,index) in rows" :key="item.id" :class="['doc-item',{active:selected?.id===item.id}]" @click="selected=item"><span class="doc-ico">{{ index+1 }}</span><span><b>{{ item.name }}</b><small>{{ item.location || '待配置地点' }} · {{ item.estimatedMinutes }} 分钟</small></span><span :class="['tag',item.status==='ACTIVE'?'green':'orange']">{{ item.status==='ACTIVE'?'运行中':'已暂停' }}</span></button><div v-if="!rows.length" class="empty-list">暂无任务，点击右上角新建</div></div><div class="panel-pad"><div class="ai-suggest"><b>✦ AI 动态编排</b><p>任务顺序、开放时间和路网状态将共用后端数据，后续可加入依赖关系和自动重排策略。</p><button class="btn primary full-btn" @click="store.notify('AI已基于当前数据库任务完成顺序检查')">检查任务顺序</button></div></div></aside>
      <section class="panel"><div class="panel-head"><div><h3>新生报到流程</h3><p>共 {{ rows.length }} 个节点 · 预计 {{ totalMinutes }} 分钟</p></div><div><button class="btn" :disabled="!selected" @click="selected && openEdit(selected)">编辑选中</button> <button class="btn danger" :disabled="!selected" @click="selected && remove(selected)">删除</button> <button class="btn primary" @click="openCreate">新增节点</button></div></div><div class="workflow"><template v-for="(step,index) in rows" :key="step.id"><div :class="['flow-node',{selected:selected?.id===step.id}]" @click="selected=step"><span class="step">{{ index+1 }}</span><div><b>{{ step.name }}</b><p>{{ step.description || '待补充说明' }} · {{ step.location || '待配置地点' }}</p></div><div class="flow-progress"><span>完成 {{ step.progress }}%</span><i><b :style="{width:step.progress+'%'}" /></i></div><span class="flow-meta">预计 {{ step.estimatedMinutes }} 分钟</span></div><div v-if="index < rows.length-1" class="flow-line" /></template><div v-if="rows.length" class="flow-line"/><div v-if="rows.length" class="flow-node finish"><span class="step">✓</span><div><b>完成报到</b><p>手机端将按这里的顺序显示并同步进度</p></div><span class="tag green">后端联动</span></div><div v-else class="empty-workflow">暂无任务节点</div></div></section></div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false"><form class="modal" @submit.prevent="save"><div class="modal-head"><h3>{{ editingId ? '编辑任务节点' : '新增任务节点' }}</h3><button type="button" class="modal-close" @click="modalOpen=false">×</button></div><div class="modal-body form-grid"><div class="form-field full"><label>任务名称 *</label><input v-model="form.name" required></div><div class="form-field full"><label>任务说明</label><textarea v-model="form.description"></textarea></div><div class="form-field"><label>办理地点</label><input v-model="form.location"></div><div class="form-field"><label>状态</label><select v-model="form.status"><option value="ACTIVE">运行中</option><option value="PAUSED">暂停</option><option value="DRAFT">草稿</option></select></div><div class="form-field"><label>完成率</label><input v-model.number="form.progress" type="number" min="0" max="100"></div><div class="form-field"><label>预计分钟</label><input v-model.number="form.estimatedMinutes" type="number" min="0"></div><div class="form-field"><label>已完成人数</label><input v-model.number="form.completedCount" type="number" min="0"></div><div class="form-field"><label>总人数</label><input v-model.number="form.totalCount" type="number" min="0"></div><div class="form-field"><label>流程顺序</label><input v-model.number="form.sortOrder" type="number" min="1"></div></div><div class="modal-foot"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary" :disabled="saving">{{ saving?'保存中…':'保存到数据库' }}</button></div></form></div>
  </div>
</template>

<style scoped>
.full-btn{width:100%;margin-top:10px}.doc-item{width:100%;text-align:left;cursor:pointer}.empty-list,.empty-workflow{padding:30px;text-align:center;color:#8b98aa;font-size:11px}.flow-node{cursor:pointer}.flow-node.selected{border-color:#78a7ff;box-shadow:0 0 0 3px rgba(52,121,244,.08),0 7px 20px rgba(45,76,127,.07)}.flow-node.finish{cursor:default}.flow-progress{width:92px;margin-left:auto;color:#7a889f;font-size:8px}.flow-progress>i{display:block;height:5px;margin-top:5px;border-radius:5px;background:#e8eef6;overflow:hidden}.flow-progress>i>b{display:block;height:100%;background:#2f78f4}.flow-meta{margin-left:0!important}
</style>
