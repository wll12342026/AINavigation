<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getSettings, saveSettings } from '@/services/api'

const store = useAppStore()
const loading = ref(true)
const saving = ref(false)
const settings = reactive({
  modelName: '', multimodal: false, tools: false, citations: false,
  refuseUnknown: true, confidence: 80, memoryTurns: 10,
})

function apply(values: Record<string, string>) {
  settings.modelName = values.modelName ?? ''
  settings.multimodal = values.multimodal === 'true'
  settings.tools = values.tools === 'true'
  settings.citations = values.citations === 'true'
  settings.refuseUnknown = values.refuseUnknown !== 'false'
  settings.confidence = Number(values.confidence ?? 80)
  settings.memoryTurns = Number(values.memoryTurns ?? 10)
}

async function load() {
  loading.value = true
  try { apply(await getSettings()) }
  catch { store.notify('系统配置加载失败，请检查后端', 'warning') }
  finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    const values = Object.fromEntries(Object.entries(settings).map(([key, value]) => [key, String(value)]))
    apply(await saveSettings(values)); store.notify('系统配置已保存到 MySQL')
  } catch { store.notify('配置保存失败', 'warning') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <section class="panel settings-layout"><aside class="settings-tabs"><button class="active">AI模型与智能体</button><button>知识库参数</button><button>数字孪生配置</button><button>导航引擎</button><button>学生端配置</button><button>用户与角色</button><button>日志与安全</button></aside>
    <div class="settings-content"><div v-if="loading" class="settings-loading">正在从数据库读取配置…</div><template v-else><div class="setting-section"><h3>AI模型配置</h3><p>配置值保存于后端系统设置表</p><div class="setting-row"><label><b>主语言模型</b><small>留空表示尚未接入真实模型服务</small></label><div class="setting-control"><input v-model="settings.modelName" class="filter-input model-select" placeholder="尚未配置"><span :class="['tag',settings.modelName?'green':'orange']">{{ settings.modelName?'已配置':'未配置' }}</span></div></div><div class="setting-row"><label><b>多模态图像识别</b><small>用于建筑照片识别与实景质检</small></label><div class="setting-control"><button :class="['switch',{on:settings.multimodal}]" @click="settings.multimodal=!settings.multimodal"/><span class="metric-sub">{{ settings.multimodal?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>工具调用</b><small>允许 AI 调用地点、任务与导航服务</small></label><div class="setting-control"><button :class="['switch',{on:settings.tools}]" @click="settings.tools=!settings.tools"/><span class="metric-sub">{{ settings.tools?'已开启':'已关闭' }}</span></div></div></div>
    <div class="setting-section"><h3>可信回答</h3><p>约束 AI 回答的来源、置信度和审核机制</p><div class="setting-row"><label><b>显示信息来源</b><small>学生端回答附带通知或手册来源</small></label><div class="setting-control"><button :class="['switch',{on:settings.citations}]" @click="settings.citations=!settings.citations"/><span class="metric-sub">{{ settings.citations?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>最低置信度</b><small>低于阈值的回答转人工确认</small></label><div class="range-wrap"><input v-model="settings.confidence" type="range" min="50" max="100"><output>{{ settings.confidence }}%</output></div></div><div class="setting-row"><label><b>未知信息拒答</b><small>知识库无证据时禁止系统自行编造</small></label><div class="setting-control"><button :class="['switch',{on:settings.refuseUnknown}]" @click="settings.refuseUnknown=!settings.refuseUnknown"/><span class="metric-sub">{{ settings.refuseUnknown?'已开启':'已关闭' }}</span></div></div></div>
    <div class="setting-section"><h3>对话与任务策略</h3><p>配置智能体的记忆和主动提醒能力</p><div class="setting-row"><label><b>上下文记忆轮数</b><small>保留当前会话中最近对话信息</small></label><div class="setting-control"><select v-model.number="settings.memoryTurns" class="select-input"><option :value="10">10 轮</option><option :value="20">20 轮</option></select></div></div></div><div class="settings-actions"><button class="btn" @click="load">重新读取</button><button class="btn primary" :disabled="saving" @click="save">{{ saving?'保存中…':'保存配置' }}</button></div></template></div>
  </section>
</template>

<style scoped>.model-select{width:280px}.settings-actions{display:flex;justify-content:flex-end;gap:9px}.settings-loading{min-height:260px;display:grid;place-items:center;color:#8b98ab}</style>
