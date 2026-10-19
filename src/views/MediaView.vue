<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import StatCard from '@/components/common/StatCard.vue'
import { useAppStore } from '@/stores/app'
import { getMediaSummary, mediaApi, placesApi, recognizeLandmark, uploadMedia, type MediaAsset, type MediaSummary, type Place, type RecognitionResult } from '@/services/api'

const store = useAppStore()
const rows = ref<MediaAsset[]>([])
const places = ref<Place[]>([])
const summary = ref<MediaSummary | null>(null)
const keyword = ref('')
const loading = ref(true)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const recognizeInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const recognizing = ref(false)
const recognition = ref<RecognitionResult | null>(null)
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

function resetForm() {
  Object.assign(form, { title: '', placeId: null, placeName: '', type: 'PHOTO', url: '', qualityScore: null, reviewStatus: 'PENDING', issueType: null })
  selectedFile.value = null
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(item: MediaAsset) {
  editingId.value = item.id
  Object.assign(form, { title: item.title, placeId: item.placeId, placeName: item.placeName ?? '', type: item.type, url: item.url ?? '', qualityScore: item.qualityScore, reviewStatus: item.reviewStatus, issueType: item.issueType })
  modalOpen.value = true
}

async function save() {
  if (!form.title.trim()) return store.notify('请输入素材名称', 'warning')
  const place = places.value.find(item => item.id === form.placeId)
  form.placeName = place?.name ?? ''
  saving.value = true
  try {
    if (editingId.value) await mediaApi.update(editingId.value, { ...form })
    else if (selectedFile.value) await uploadMedia(selectedFile.value, form.title, form.placeId, form.type)
    else await mediaApi.create({ ...form })
    modalOpen.value = false
    resetForm()
    await load(); store.notify(editingId.value ? '素材信息已更新到 MySQL' : '素材元数据已保存到 MySQL')
  } catch { store.notify('素材保存失败', 'warning') }
  finally { saving.value = false }
}

function chooseFile() { fileInput.value?.click() }
function onFileSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  selectedFile.value = file
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = file ? URL.createObjectURL(file) : ''
  if (file && !form.title.trim()) form.title = file.name.replace(/\.[^.]+$/, '')
}

async function recognizeFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  recognizing.value = true
  recognition.value = null
  try {
    recognition.value = await recognizeLandmark(file)
    store.notify(recognition.value.sufficientEvidence ? '已找到可信的地标候选' : '图库证据不足，已返回候选结果', recognition.value.sufficientEvidence ? 'success' : 'warning')
  } catch (error) {
    store.notify(error instanceof Error ? error.message : '地标识别失败', 'warning')
  } finally {
    recognizing.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
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
    <div class="page-tools"><div class="page-tools-left"><input v-model="keyword" class="filter-input" placeholder="搜索素材或地点"><span class="tag green">数据来自 MySQL</span></div><div class="page-tools-right"><input ref="recognizeInput" type="file" accept="image/*" hidden @change="recognizeFile"><button class="btn" :disabled="recognizing" @click="recognizeInput?.click()">{{ recognizing ? '识别中…' : '识图找地点' }}</button><button class="btn" @click="load">刷新</button><button class="btn primary" @click="openCreate">+上传素材</button></div></div>
    <div v-if="recognition" :class="['recognition-banner',recognition.sufficientEvidence?'success':'warning']"><div><b>{{ recognition.message }}</b><span>识别方法：{{ recognition.method }}</span></div><div class="candidate-list"><span v-for="candidate in recognition.candidates" :key="candidate.placeId">{{ candidate.placeName }} <strong>{{ Math.round(candidate.confidence*100) }}%</strong></span><span v-if="!recognition.candidates.length">暂无已绑定且完成特征提取的参考图片</span></div><button @click="recognition=null">×</button></div>
    <div class="content-split"><section class="panel"><div v-if="loading" class="empty-list">正在读取素材数据…</div><div v-else-if="!filtered.length" class="empty-list">暂无真实素材记录，请点击“录入素材”添加。</div><div v-else class="media-grid"><article v-for="item in filtered" :key="item.id" class="media-card"><div class="media-photo" :style="item.url ? {backgroundImage:`url(${item.url})`,backgroundSize:'cover'} : {}"><span class="media-check"/><span class="media-score">AI {{ item.qualityScore ?? '未质检' }}</span></div><div class="media-info"><b>{{ item.title }}</b><p>{{ item.placeName || '未绑定地点' }} · {{ item.reviewStatus }}</p><div class="media-actions"><button class="panel-link" @click="openEdit(item)">编辑</button><button class="panel-link danger-text" @click="remove(item)">删除</button></div></div></article></div><div class="pagination"><span class="metric-sub page-total">已显示 {{ filtered.length }} / {{ summary?.total ?? 0 }}</span></div></section>
    <aside class="grid"><article class="panel"><div class="panel-head"><div><h3>AI图片质检</h3><p>统计来自素材质检字段</p></div><span class="tag" :class="summary?.pending ? 'orange' : 'green'">{{ summary?.pending ?? 0 }} 待处理</span></div><div class="panel-pad property-list"><div class="property"><label>待处理</label><strong>{{ summary?.pending ?? 0 }} 张</strong></div><div class="property"><label>低清晰度</label><span>{{ summary?.lowQuality ?? 0 }} 张</span></div><div class="property"><label>方向错误</label><span>{{ summary?.wrongDirection ?? 0 }} 张</span></div><div class="property"><label>疑似重复</label><span>{{ summary?.duplicate ?? 0 }} 张</span></div></div></article><article class="panel panel-pad"><div class="ai-suggest"><b>数据说明</b><p>当前页面不再生成素材名称、评分或问题数量；所有内容均来自后端素材表。</p></div></article></aside></div>

    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen=false">
      <form class="modal media-modal" @submit.prevent="save">
        <div class="modal-head media-modal-head">
          <div>
            <span class="modal-kicker">REAL-WORLD MEDIA</span>
            <h3>{{ editingId ? '编辑实景素材' : '录入实景素材' }}</h3>
            <p>维护素材信息、地点绑定与 AI 质检结果</p>
          </div>
          <button type="button" class="modal-close" aria-label="关闭" @click="modalOpen=false">×</button>
        </div>

        <div class="modal-body form-grid">
          <div class="form-field full">
            <label>素材名称 *</label>
            <input v-model="form.title" required placeholder="例如：东门主入口正面实景">
          </div>
          <div class="form-field">
            <label>绑定地点</label>
            <select v-model="form.placeId">
              <option :value="null">暂不绑定地点</option>
              <option v-for="place in places" :key="place.id" :value="place.id">{{ place.name }}</option>
            </select>
          </div>
          <div class="form-field">
            <label>素材类型</label>
            <select v-model="form.type">
              <option value="PHOTO">普通照片</option>
              <option value="PANORAMA">全景照片</option>
              <option value="VIDEO">实景视频</option>
            </select>
          </div>
          <div v-if="!editingId" class="form-field full">
            <label>上传图片</label>
            <input ref="fileInput" class="native-file" type="file" accept="image/jpeg,image/png,image/webp" @change="onFileSelected">
            <button type="button" class="upload-zone" @click="chooseFile">
              <img v-if="previewUrl" :src="previewUrl" alt="待上传素材预览">
              <span v-else class="upload-icon">＋</span>
              <b>{{ selectedFile ? selectedFile.name : '选择校园实景图片' }}</b>
              <small>{{ selectedFile ? `${(selectedFile.size/1024/1024).toFixed(2)} MB · 保存后自动质检` : '支持 JPG、PNG、WebP，最大 15MB' }}</small>
            </button>
          </div>
          <div v-else class="form-field full">
            <label>已保存素材地址</label>
            <input v-model="form.url" readonly>
            <small>替换图片请上传为新素材，避免覆盖历史质检证据。</small>
          </div>
          <div v-if="editingId" class="form-field">
            <label>AI 质检分数</label>
            <div class="score-input">
              <input v-model.number="form.qualityScore" type="number" min="0" max="100" placeholder="0–100">
              <span>/ 100</span>
            </div>
          </div>
          <div v-if="editingId" class="form-field">
            <label>审核状态</label>
            <select v-model="form.reviewStatus">
              <option value="PENDING">待处理</option>
              <option value="PASSED">已通过</option>
              <option value="REJECTED">未通过</option>
            </select>
          </div>
          <div v-if="editingId" class="form-field full">
            <label>质检问题</label>
            <select v-model="form.issueType">
              <option :value="null">无明显问题</option>
              <option value="LOW_QUALITY">清晰度不足</option>
              <option value="WRONG_DIRECTION">拍摄方向错误</option>
              <option value="DUPLICATE">疑似重复素材</option>
            </select>
          </div>
        </div>

        <div class="modal-foot">
          <span class="save-hint"><i></i> 保存后立即同步到 MySQL</span>
          <button type="button" class="btn" @click="modalOpen=false">取消</button>
          <button class="btn primary" :disabled="saving">{{ saving ? '保存中…' : '保存到数据库' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.page-total{margin-right:auto}
.media-actions{display:flex;justify-content:flex-end;gap:10px}
.media-actions .panel-link{border:0;background:transparent}
.danger-text{color:#e2525c}
.empty-list{min-height:220px;display:grid;place-items:center;color:#8b98ab}
.media-modal{width:min(620px,calc(100vw - 32px))}
.media-modal-head{height:auto;min-height:82px;padding:15px 20px;background:linear-gradient(135deg,#f8fbff 0%,#f1f6ff 100%)}
.media-modal-head h3{margin:2px 0 3px;font-size:18px;color:#172849}
.media-modal-head p{margin:0;color:#8794a7;font-size:11px}
.modal-kicker{color:#3b7bf5;font-size:9px;font-weight:800;letter-spacing:1.4px}
.media-modal .modal-body{gap:15px;padding:20px}
.media-modal .form-field label{font-weight:700;color:#53647c}
.media-modal .form-field input,.media-modal .form-field select{height:42px;border-color:#dce5f1;background:#fbfdff;transition:border-color .2s,box-shadow .2s}
.media-modal .form-field input:focus,.media-modal .form-field select:focus{border-color:#4c83f4;box-shadow:0 0 0 3px rgba(76,131,244,.12);background:#fff}
.media-modal .form-field small{color:#96a2b3;font-size:9px;line-height:1.5}
.score-input{position:relative}
.score-input input{padding-right:55px!important}
.score-input span{position:absolute;right:12px;top:50%;transform:translateY(-50%);color:#9aa6b8;font-size:10px;pointer-events:none}
.media-modal .modal-foot{align-items:center;padding:14px 20px}
.save-hint{margin-right:auto;display:flex;align-items:center;gap:7px;color:#8592a5;font-size:10px}
.save-hint i{width:7px;height:7px;border-radius:50%;background:#18be91;box-shadow:0 0 0 4px rgba(24,190,145,.1)}
.native-file{display:none}
.upload-zone{min-height:142px;display:grid;place-items:center;align-content:center;gap:7px;border:1.5px dashed #b8cdf4;border-radius:13px;color:#5872a0;background:#f6f9ff;overflow:hidden;cursor:pointer}
.upload-zone:hover{border-color:#4c83f4;background:#f0f6ff}.upload-zone img{width:100%;height:120px;object-fit:cover}.upload-zone b{font-size:11px}.upload-zone small{color:#8c9ab0;font-size:9px}.upload-icon{width:38px;height:38px;display:grid;place-items:center;border-radius:12px;color:white;background:#4c83f4;font-size:22px;box-shadow:0 8px 18px rgba(76,131,244,.25)}
.recognition-banner{margin:-2px 0 14px;padding:13px 16px;display:grid;grid-template-columns:240px 1fr auto;align-items:center;gap:16px;border:1px solid;border-radius:12px}.recognition-banner.success{border-color:#bfeadd;background:#f0fbf7}.recognition-banner.warning{border-color:#f2d5af;background:#fff8ee}.recognition-banner b,.recognition-banner span{display:block}.recognition-banner b{font-size:11px;color:#30405d}.recognition-banner>div>span{margin-top:3px;color:#8592a5;font-size:9px}.candidate-list{display:flex;gap:8px;flex-wrap:wrap}.candidate-list span{padding:6px 9px;border-radius:8px;background:rgba(255,255,255,.8);color:#607089!important;font-size:9px!important}.candidate-list strong{color:#257260}.recognition-banner>button{border:0;background:transparent;color:#8290a4;font-size:18px}
@media (max-width:640px){
  .media-modal{max-height:calc(100vh - 24px);overflow:auto}
  .media-modal .form-grid{grid-template-columns:1fr}
  .media-modal .form-field.full{grid-column:auto}
  .save-hint{display:none}
}
</style>
