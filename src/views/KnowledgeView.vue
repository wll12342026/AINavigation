<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import { analyzeOperation, knowledgeApi, type KnowledgeItem } from '@/services/api'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const rows = ref<KnowledgeItem[]>([])
const selected = ref<KnowledgeItem | null>(null)
const keyword = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const question = ref('校园卡在哪里领？需要带什么？')
const aiAnswer = ref('点击“向后端提问”，测试当前 AI 分析接口。')
const aiLoading = ref(false)
const today = new Date()
const inThirtyDays = new Date(today.getTime() + 30 * 86400000)
const emptyForm = () => ({ title: '', category: '空间地点', content: '', status: 'PUBLISHED', source: '', expiresAt: new Date(today.getTime() + 180 * 86400000).toISOString().slice(0,10) as string | null })
const form = reactive(emptyForm())
const filtered = computed(() => rows.value.filter(row => `${row.title}${row.category}${row.source}${row.content}`.includes(keyword.value)))
const publishedCount = computed(() => rows.value.filter(row => row.status === 'PUBLISHED').length)
const pendingCount = computed(() => rows.value.filter(row => row.status === 'PENDING').length)
const expiringCount = computed(() => rows.value.filter(row => row.expiresAt && new Date(row.expiresAt) < inThirtyDays).length)
const health = computed(() => Math.max(0, 99 - expiringCount.value))

async function load() {
  try { rows.value = await knowledgeApi.list(); selected.value = rows.value[0] ?? null }
  catch { store.notify('知识库加载失败，请检查后端', 'warning') }
}
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); modalOpen.value = true }
function openEdit(row: KnowledgeItem) { editingId.value = row.id; Object.assign(form, { title: row.title, category: row.category ?? '', content: row.content ?? '', status: row.status, source: row.source ?? '', expiresAt: row.expiresAt }); modalOpen.value = true }
async function save() {
  if (!form.title.trim()) return store.notify('请输入知识标题', 'warning')
  saving.value = true
  try {
    const saved = editingId.value ? await knowledgeApi.update(editingId.value, { ...form }) : await knowledgeApi.create({ ...form })
    const index = rows.value.findIndex(row => row.id === saved.id)
    if (index >= 0) rows.value[index] = saved; else rows.value.unshift(saved)
    selected.value = saved; modalOpen.value = false; store.notify('知识条目已写入 MySQL')
  } catch { store.notify('知识条目保存失败', 'warning') } finally { saving.value = false }
}
async function remove(row: KnowledgeItem) {
  if (!window.confirm(`确认删除知识“${row.title}”吗？`)) return
  try { await knowledgeApi.remove(row.id); rows.value = rows.value.filter(item => item.id !== row.id); selected.value = rows.value[0] ?? null; store.notify('知识条目已删除') } catch { store.notify('删除失败', 'warning') }
}
async function askAi() {
  if (!question.value.trim()) return
  aiLoading.value = true
  try { const result = await analyzeOperation(question.value); aiAnswer.value = `${result.title}：${result.summary}` }
  catch { aiAnswer.value = '无法连接 AI 分析接口，请确认后端正在运行。' }
  finally { aiLoading.value = false }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="grid cols-4 stat-row"><StatCard label="知识条目" :value="String(rows.length)" icon="◫" caption="MySQL 实时数据"/><StatCard label="已发布" :value="String(publishedCount)" icon="☑" tone="#18b68a" tint="#e8faf5"/><StatCard label="知识健康度" :value="`${health}%`" icon="▦" tone="#7057ee" tint="#f0edff"/><StatCard label="即将过期" :value="String(expiringCount)" icon="◫" tone="#ef7d3f" tint="#fff1e8"/></div>
    <div class="page-tools"><div class="page-tools-left"><input v-model="keyword" class="filter-input" placeholder="搜索标题、分类、来源或内容"><span class="tag green">● 后端知识库已连接</span></div><div class="page-tools-right"><button class="btn" @click="load">刷新数据</button><button class="btn primary" @click="openCreate">+新增知识</button></div></div>
    <div class="content-split left-narrow"><aside class="panel"><div class="panel-head"><div><h3>知识条目</h3><p>审核、有效期与版本来源</p></div><span class="tag orange">待审核 {{ pendingCount }}</span></div><div class="panel-pad doc-list knowledge-scroll"><button v-for="(item,index) in filtered" :key="item.id" :class="['doc-item',{active:selected?.id===item.id}]" @click="selected=item"><span class="doc-ico">{{ index+1 }}</span><span><b>{{ item.title }}</b><small>{{ item.category }} · {{ item.source || '未注明来源' }}</small></span><span :class="['tag',item.status==='PUBLISHED'?'green':'orange']">{{ item.status==='PUBLISHED'?'有效':'待审核' }}</span></button><div v-if="!filtered.length" class="empty-list">暂无匹配知识</div></div></aside>
      <section class="grid"><article class="panel"><div class="panel-head"><div><h3>{{ selected?.title || '语义关系图谱' }}</h3><p>{{ selected ? `来源：${selected.source || '未注明'} · 到期：${selected.expiresAt || '长期有效'}` : '任务—地点—部门—材料—时间关联' }}</p></div><div v-if="selected"><button class="btn" @click="openEdit(selected)">编辑</button> <button class="btn danger" @click="remove(selected)">删除</button></div></div><div class="semantic-graph"><svg viewBox="0 0 800 390"><g stroke="#bdcff0" stroke-width="1.5"><path d="M400 195 210 90M400 195 600 75M400 195 660 235M400 195 485 330M400 195 180 280M210 90 105 155M600 75 700 120M660 235 735 315M180 280 90 335"/></g></svg><span class="graph-label core" style="left:50%;top:50%">{{ selected?.title || '知识中心' }}</span><span class="graph-label" style="left:26%;top:23%">{{ selected?.category || '空间地点' }}</span><span class="graph-label" style="left:75%;top:19%">{{ selected?.source || '业务来源' }}</span><span class="graph-label" style="left:82%;top:60%">任务材料</span><span class="graph-label" style="left:61%;top:84%">有效时间</span><span class="graph-label" style="left:22%;top:72%">校园地点</span><div v-if="selected" class="knowledge-preview">{{ selected.content }}</div></div></article>
      <div class="grid cols-2"><article class="panel panel-pad"><SectionTitle title="AI接口联调测试"/><div class="qa-input"><input v-model="question" @keyup.enter="askAi"><button class="btn primary" :disabled="aiLoading" @click="askAi">{{ aiLoading?'分析中':'向后端提问' }}</button></div><div class="chat-answer"><strong>后端返回结果</strong><p>{{ aiAnswer }}</p><span class="tag green">接口 /api/ai/analyze</span></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>✦ 质量审计结果</b><p>{{ expiringCount }} 条知识将在 30 天内过期，{{ pendingCount }} 条知识等待审核。修改后首页健康度会同步变化。</p><button class="btn primary review-btn" @click="store.notify('当前待审核条目已在左侧列表标识')">查看待审核条目</button></div></article></div></section></div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false"><form class="modal" @submit.prevent="save"><div class="modal-head"><h3>{{ editingId?'编辑知识条目':'新增知识条目' }}</h3><button type="button" class="modal-close" @click="modalOpen=false">×</button></div><div class="modal-body form-grid"><div class="form-field full"><label>知识标题 *</label><input v-model="form.title" required></div><div class="form-field"><label>分类</label><input v-model="form.category"></div><div class="form-field"><label>状态</label><select v-model="form.status"><option value="PUBLISHED">已发布</option><option value="PENDING">待审核</option><option value="ARCHIVED">已归档</option></select></div><div class="form-field"><label>来源</label><input v-model="form.source"></div><div class="form-field"><label>有效期</label><input v-model="form.expiresAt" type="date"></div><div class="form-field full"><label>知识内容</label><textarea v-model="form.content" rows="5"></textarea></div></div><div class="modal-foot"><button type="button" class="btn" @click="modalOpen=false">取消</button><button class="btn primary" :disabled="saving">{{ saving?'保存中…':'保存到数据库' }}</button></div></form></div>
  </div>
</template>

<style scoped>
.knowledge-scroll{max-height:642px;overflow:auto}.doc-item{width:100%;text-align:left;cursor:pointer}.empty-list{padding:30px;text-align:center;color:#8b98aa;font-size:11px}.knowledge-preview{position:absolute;left:50%;bottom:18px;transform:translateX(-50%);width:min(560px,80%);padding:10px 13px;border:1px solid #dce7f7;border-radius:9px;color:#687890;background:rgba(255,255,255,.9);font-size:10px;line-height:1.55;text-align:center}.qa-input{display:flex;gap:8px;margin:14px 0 10px}.qa-input input{min-width:0;flex:1;border:1px solid #dde6f1;border-radius:8px;padding:0 10px;color:#35435d}.review-btn{width:100%;margin-top:10px}
</style>
