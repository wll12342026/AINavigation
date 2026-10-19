<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { normalizeModelUrl, viewerBudget } from '@/twin/viewer'

const props = defineProps<{ modelUrl: string; title?: string }>()
const emit = defineEmits<{ state: [value: 'empty' | 'loading' | 'ready' | 'error'] }>()
const host = ref<HTMLElement | null>(null)
const frame = ref<HTMLIFrameElement | null>(null)
const state = ref<'empty' | 'loading' | 'ready' | 'error'>('empty')
const error = ref('')
const loadProgress = ref(0)
const source = ref('')
let channel = ''
let timeout: number | undefined
const device = navigator as Navigator & { deviceMemory?: number }
const budget = viewerBudget(/Android|iPhone|iPad|Mobile/i.test(device.userAgent), device.deviceMemory)
const fullscreenError = ref('')
const fullScreenEnabled = computed(() => Boolean(document.fullscreenEnabled))

function setState(value: typeof state.value) { state.value = value; emit('state', value) }
function clearTimer() { if (timeout != null) window.clearTimeout(timeout); timeout = undefined }
function fail(message: string) {
  clearTimer(); error.value = message; setState('error')
  // 失败或超时后卸载 iframe，释放下载任务和 GPU 场景；重试时重新创建。
  source.value = ''
}
function load() {
  clearTimer(); error.value = ''; fullscreenError.value = ''; loadProgress.value = 0; source.value = ''
  if (!props.modelUrl.trim()) { setState('empty'); return }
  try {
    const modelUrl = normalizeModelUrl(props.modelUrl, window.location.href)
    channel = crypto.randomUUID()
    const query = new URLSearchParams({ content: modelUrl, budget: String(budget), lang: 'zh-CN', channel })
    source.value = `${import.meta.env.BASE_URL}twin-viewer/index.html?${query}`
    setState('loading')
    timeout = window.setTimeout(() => fail('模型加载超过两分钟，请检查地址、网络、文件大小或浏览器图形支持后重试'), 120000)
  } catch (reason) { fail(reason instanceof Error ? reason.message : '模型地址不合法') }
}
function onMessage(event: MessageEvent) {
  if (event.origin !== window.location.origin || event.source !== frame.value?.contentWindow) return
  const data = event.data
  if (!data || data.source !== 'campus-twin' || data.channel !== channel) return
  if (data.type === 'ready') { clearTimer(); loadProgress.value = 100; setState('ready') }
  if (data.type === 'progress' && state.value === 'loading' && typeof data.value === 'number') {
    loadProgress.value = Math.max(0, Math.min(99, data.value))
  }
  if (data.type === 'error') fail(typeof data.message === 'string' ? data.message : '模型加载失败')
}
async function fullscreen() {
  try {
    if (document.fullscreenElement === host.value) await document.exitFullscreen()
    else await host.value?.requestFullscreen()
  } catch { fullscreenError.value = '浏览器未允许全屏，请尝试桌面浏览器或直接放大页面' }
}
window.addEventListener('message', onMessage)
watch(() => props.modelUrl, load, { immediate: true })
onBeforeUnmount(() => { clearTimer(); window.removeEventListener('message', onMessage) })
</script>

<template>
  <section ref="host" class="splat-viewer" :aria-busy="state==='loading'">
    <iframe v-if="source" ref="frame" :key="source" :src="source" :title="title || '校园三维场景'" allow="fullscreen" referrerpolicy="same-origin" @error="fail('查看器页面加载失败，请重试')" />
    <div v-if="state==='ready'" class="viewer-tools"><span>真实模型 · {{ budget }} 百万点渲染预算</span><button @click="load">复位视角</button><button v-if="fullScreenEnabled" @click="fullscreen">全屏查看</button></div>
    <div v-if="state==='empty'" class="viewer-state"><span class="viewer-symbol">◇</span><h2>校园三维场景待接入</h2><p>填写校园 SOG 模型的可访问地址后，即可旋转、缩放和平移浏览。</p><small>当前没有加载模型，地点与路线规划仍可独立使用。</small></div>
    <div v-if="state==='loading'" class="viewer-state loading" role="status"><span class="viewer-symbol">◌</span><h2>正在加载{{ title || '校园模型' }}</h2><p>{{ loadProgress }}% · 等待模型首帧渲染</p><small>大文件首次加载可能较慢，请保持页面打开。</small></div>
    <div v-if="state==='error'" class="viewer-state" role="alert"><span class="viewer-symbol">!</span><h2>场景暂时无法显示</h2><p>{{ error }}</p><button class="btn primary" @click="load">重新加载</button><small>确认模型 URL 可访问；跨域地址需允许 CORS，LOD 需保留全部关联资源。</small></div>
    <p v-if="fullscreenError" class="fullscreen-error" role="alert">{{ fullscreenError }}</p>
  </section>
</template>

<style scoped>
.splat-viewer{height:100%;min-height:540px;position:relative;overflow:hidden;background:radial-gradient(ellipse at 30% 20%,#d6eaff,#eff6fd 60%);border-radius:18px}.splat-viewer:fullscreen{border-radius:0}.splat-viewer iframe{display:block;border:0;width:100%;height:100%;min-height:540px}.viewer-state{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:16px;padding:36px;text-align:center;background:linear-gradient(140deg,#e5f1ff,#f5faff)}.viewer-symbol{display:grid;place-items:center;width:76px;height:76px;border:1px solid #b8d4fa;border-radius:24px;background:#f5faff;color:#357ded;font-size:43px;box-shadow:0 16px 40px #357ded15}.viewer-state h2{margin:0;color:#234c7d;font-size:22px}.viewer-state p{max-width:550px;margin:0;color:#6c839f;line-height:1.8;overflow-wrap:anywhere}.viewer-state small{max-width:560px;line-height:1.8;color:#8c9eb3}.viewer-tools{position:absolute;top:16px;right:16px;display:flex;align-items:center;gap:8px;padding:7px;border:1px solid #ffffff70;border-radius:12px;background:#ffffffed}.viewer-tools span{padding:0 8px;color:#627996;font-size:11px}.viewer-tools button{padding:9px 13px;border:1px solid #dce7f4;border-radius:8px;background:white;color:#2868bd}.fullscreen-error{position:absolute;bottom:15px;left:20px;padding:10px;background:white;color:#bb5e3c;border-radius:8px;font-size:12px}.loading .viewer-symbol{animation:spin 2s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.loading .viewer-symbol{animation:none}}
</style>
