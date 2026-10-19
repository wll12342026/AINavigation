<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SplatViewer from '@/components/common/SplatViewer.vue'
import { getSettings, saveSettings } from '@/services/api'
import { normalizeModelUrl } from '@/twin/viewer'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const router = useRouter()
const form = reactive({ name: '', url: '' })
const activeUrl = ref('')
const activeName = ref('')
const loading = ref(true)
const saving = ref(false)
const configLoaded = ref(false)
const error = ref('')
const modelState = ref<'empty' | 'loading' | 'ready' | 'error'>('empty')
const savedUrl = ref('')
const viewerRevision = ref(0)
const statusText = computed(() => ({ empty:'等待模型', loading:'模型加载中', ready:'模型首帧已显示', error:'模型加载失败' })[modelState.value])

async function load() {
  loading.value = true; error.value = ''
  try {
    const values = await getSettings()
    form.name = values.twinModelName ?? ''
    form.url = values.twinModelUrl ?? ''
    savedUrl.value = form.url
    activeUrl.value = form.url
    activeName.value = form.name
    configLoaded.value = true
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '模型配置读取失败'
    configLoaded.value = false
  } finally { loading.value = false }
}
function validate() {
  try { normalizeModelUrl(form.url, window.location.href); error.value = ''; return true }
  catch (reason) { error.value = reason instanceof Error ? reason.message : '模型地址不合法'; return false }
}
function preview() {
  if (!validate()) return
  activeUrl.value = form.url.trim(); activeName.value = form.name.trim()
  viewerRevision.value++
}
async function save() {
  if (!configLoaded.value || !validate()) return
  saving.value = true
  try {
    const values = await saveSettings({ twinModelName: form.name.trim(), twinModelUrl: form.url.trim() })
    savedUrl.value = values.twinModelUrl ?? ''
    form.url = savedUrl.value; form.name = values.twinModelName ?? ''
    preview()
    store.notify(form.url ? '模型地址已保存；加载是否成功以查看器状态为准' : '已保存为空模型配置')
  } catch (reason) { error.value = reason instanceof Error ? reason.message : '模型配置保存失败' }
  finally { saving.value = false }
}
onMounted(load)
</script>

<template>
  <div class="twin-page">
    <header class="twin-heading"><div><span class="eyebrow">CAMPUS DIGITAL TWIN</span><h2>校园数字孪生</h2><p>三维场景浏览 · 校园空间数据接入</p></div><div class="heading-actions"><span :class="['tag',modelState==='ready'?'green':modelState==='error'?'red':'orange']">{{ statusText }}</span><button class="btn" @click="router.push('/network')">路线规划与验证 ↗</button></div></header>
    <div class="twin-layout">
      <article class="model-stage panel">
        <div v-if="loading" class="config-loading" role="status">正在读取模型配置…</div>
        <SplatViewer v-else :key="viewerRevision" :model-url="activeUrl" :title="activeName || '校园三维场景'" @state="modelState=$event" />
        <footer class="scene-footer"><span><i :class="{online:modelState==='ready'}"/>{{ activeName || '未命名校园场景' }}</span><span>Gaussian Splatting · SOG</span></footer>
      </article>
      <aside class="twin-sidebar">
        <section class="panel config-panel"><div class="panel-head"><div><h3>场景接入</h3><p>连接当前项目自己的模型资源</p></div><span class="tag">第一阶段</span></div>
          <form class="config-form" @submit.prevent="save">
            <label>场景名称<input v-model="form.name" maxlength="100" placeholder="例如：主校区航拍场景" :disabled="loading || saving"></label>
            <label>模型访问地址<input v-model="form.url" type="text" placeholder="/models/campus.sog 或 https://…/campus.sog" :disabled="loading || saving" spellcheck="false"></label>
            <p class="field-hint">填写浏览器可访问的地址，不填写电脑文件路径。跨域资源需开放 CORS；完整 LOD 目录可填写 lod-meta.json 地址。</p>
            <p v-if="error" class="config-error" role="alert">{{ error }}</p>
            <div class="config-actions"><button type="button" class="btn" :disabled="loading || saving" @click="preview">临时预览</button><button class="btn primary" :disabled="loading || saving || !configLoaded">{{ saving ? '保存中…' : '保存并加载' }}</button></div>
            <button type="button" class="reload-config" :disabled="loading || saving" @click="load">重新读取已保存配置</button>
            <small v-if="activeUrl && activeUrl!==savedUrl" class="preview-note">正在临时预览，地址尚未保存。</small>
          </form>
        </section>
        <section class="panel information-panel"><h3>模型准备指引</h3><ol><li>准备本校已训练或导出的真实 SOG 文件。</li><li>将文件放入本项目 public/models 目录，或交由文件服务提供 HTTP 地址。</li><li>填写地址并预览，首帧显示后可旋转、缩放、平移和全屏浏览。</li></ol><p>目前未提供上传或 PLY 转换；普通点云、GLB、OBJ、FBX 不能作为此查看器的模型输入。模型不会随代码提交。</p></section>
        <section class="panel next-panel"><span class="tag">下一步 · 坐标校准</span><h3>让校园地点与三维场景对应</h3><p>模型到位后标定地点、建筑入口和路网坐标，再验证三维路线叠加与镜头跟随。</p><button class="btn" @click="router.push('/places')">维护校园地点 →</button></section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.twin-page{max-width:1920px;margin:auto}.twin-heading{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:22px}.eyebrow{font-size:10px;letter-spacing:2px;color:#598bc9}.twin-heading h2{margin:7px 0;color:#1b395f;font-size:25px}.twin-heading p{margin:0;font-size:12px;color:#8295ae}.heading-actions{display:flex;align-items:center;gap:12px}.twin-layout{display:grid;grid-template-columns:minmax(0,1fr) 350px;gap:20px}.model-stage{height:calc(100vh - 218px);min-height:600px;padding:8px;display:flex;flex-direction:column;overflow:hidden;background:#f9fcff}.model-stage :deep(.splat-viewer){flex:1;min-height:0}.model-stage :deep(iframe){height:100%;min-height:0}.scene-footer{padding:14px 10px 6px;display:flex;justify-content:space-between;gap:12px;color:#8b9db3;font-size:11px}.scene-footer i{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#c6d1df}.scene-footer i.online{background:#13c99d}.twin-sidebar{display:flex;flex-direction:column;gap:16px}.config-form{padding:20px;display:grid;gap:16px}.config-form label{display:grid;gap:9px;font-size:12px;color:#476484}.config-form input{width:100%;box-sizing:border-box;border:1px solid #dce7f4;background:#f8fbff;border-radius:9px;padding:12px;color:#234b7a;font-size:12px;outline:none}.config-form input:focus{border-color:#619df5;box-shadow:0 0 0 3px #3787f410}.field-hint{margin:0;color:#8b9db4;font-size:11px;line-height:1.9}.config-actions{display:flex;gap:10px}.config-actions>*{flex:1}.config-error{margin:0;padding:10px;background:#fff1ef;border-radius:8px;color:#c56055;font-size:12px;line-height:1.7}.reload-config{padding:0;border:0;background:none;text-align:left;color:#5485c5;font-size:11px;cursor:pointer}.information-panel,.next-panel{padding:21px}.information-panel h3,.next-panel h3{margin:0 0 12px;color:#2b4b73;font-size:14px}.information-panel ol{padding-left:18px;margin:0;display:grid;gap:12px;color:#6d829c;font-size:12px;line-height:1.8}.information-panel p,.next-panel p{color:#8a9cb2;font-size:11px;line-height:1.9}.next-panel{background:linear-gradient(135deg,#eff6ff,#fbfdff)}.next-panel h3{margin-top:13px}.next-panel .btn{margin-top:8px}.config-loading{flex:1;display:grid;place-items:center;color:#8097b3}.preview-note{color:#bd8b42;line-height:1.7}@media(max-width:1150px){.twin-layout{grid-template-columns:1fr}.model-stage{height:65vh;min-height:450px}.twin-sidebar{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.config-panel{grid-row:span 2}.twin-heading{flex-wrap:wrap}}@media(max-width:700px){.twin-sidebar{display:flex}.heading-actions{flex-wrap:wrap}.scene-footer{flex-wrap:wrap}}
</style>
