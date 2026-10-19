<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getSettings, saveSettings } from '@/services/api'

const store = useAppStore()
const loading = ref(true)
const saving = ref(false)
const activeTab = ref('ai')
const tabs = [
  { id: 'ai', label: 'AI模型与智能体' }, { id: 'knowledge', label: '知识库参数' },
  { id: 'twin', label: '数字孪生配置' }, { id: 'navigation', label: '导航引擎' },
  { id: 'student', label: '学生端配置' }, { id: 'users', label: '用户与角色' },
  { id: 'security', label: '日志与安全' },
]
const settings = reactive({
  modelName: '', multimodal: false, tools: false, citations: false, refuseUnknown: true, confidence: 80, memoryTurns: 10,
  reviewRequired: true, expiryWarningDays: 30, knowledgeTopK: 5,
  coordinateSystem: 'WGS84', twinModelName: '', twinModelUrl: '', modelScale: 1,
  accessiblePriority: true, avoidRestricted: true, walkingSpeed: 1.2,
  welcomeText: '欢迎使用新生智游', showVirtualTour: true, voiceGuidance: true,
  adminName: '林管理员', roleName: '超级管理员', allowEditors: false,
  auditLog: true, sessionMinutes: 60, allowLan: true,
})

function bool(value: string | undefined, fallback: boolean) { return value == null ? fallback : value === 'true' }
function number(value: string | undefined, fallback: number) { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : fallback }
function apply(values: Record<string, string>) {
  settings.modelName = values.modelName ?? ''
  settings.multimodal = bool(values.multimodal, false)
  settings.tools = bool(values.tools, false)
  settings.citations = bool(values.citations, false)
  settings.refuseUnknown = bool(values.refuseUnknown, true)
  settings.confidence = number(values.confidence, 80)
  settings.memoryTurns = number(values.memoryTurns, 10)
  settings.reviewRequired = bool(values.reviewRequired, true)
  settings.expiryWarningDays = number(values.expiryWarningDays, 30)
  settings.knowledgeTopK = number(values.knowledgeTopK, 5)
  settings.coordinateSystem = values.coordinateSystem ?? 'WGS84'
  settings.twinModelName = values.twinModelName ?? ''
  settings.twinModelUrl = values.twinModelUrl ?? ''
  settings.modelScale = number(values.modelScale, 1)
  settings.accessiblePriority = bool(values.accessiblePriority, true)
  settings.avoidRestricted = bool(values.avoidRestricted, true)
  settings.walkingSpeed = number(values.walkingSpeed, 1.2)
  settings.welcomeText = values.welcomeText ?? '欢迎使用新生智游'
  settings.showVirtualTour = bool(values.showVirtualTour, true)
  settings.voiceGuidance = bool(values.voiceGuidance, true)
  settings.adminName = values.adminName ?? '林管理员'
  settings.roleName = values.roleName ?? '超级管理员'
  settings.allowEditors = bool(values.allowEditors, false)
  settings.auditLog = bool(values.auditLog, true)
  settings.sessionMinutes = number(values.sessionMinutes, 60)
  settings.allowLan = bool(values.allowLan, true)
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
    const values = Object.fromEntries(Object.entries(settings)
      .filter(([key]) => !['twinModelName', 'twinModelUrl'].includes(key))
      .map(([key, value]) => [key, String(value)]))
    apply(await saveSettings(values)); store.notify(`${tabs.find(item=>item.id===activeTab.value)?.label}已保存到 MySQL`)
  } catch { store.notify('配置保存失败', 'warning') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <section class="panel settings-layout"><aside class="settings-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{active:activeTab===tab.id}" @click="activeTab=tab.id">{{ tab.label }}</button></aside>
    <div class="settings-content"><div v-if="loading" class="settings-loading">正在从数据库读取配置…</div><template v-else>
      <template v-if="activeTab==='ai'"><div class="setting-section"><h3>AI模型配置</h3><p>配置值保存于后端系统设置表</p><div class="setting-row"><label><b>主语言模型</b><small>留空表示尚未接入真实模型服务</small></label><div class="setting-control"><input v-model="settings.modelName" class="filter-input model-select" placeholder="尚未配置"><span :class="['tag',settings.modelName?'green':'orange']">{{ settings.modelName?'已配置':'未配置' }}</span></div></div><div class="setting-row"><label><b>多模态图像识别</b><small>用于建筑照片识别与实景质检</small></label><div class="setting-control"><button :class="['switch',{on:settings.multimodal}]" @click="settings.multimodal=!settings.multimodal"/><span class="metric-sub">{{ settings.multimodal?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>工具调用</b><small>允许 AI 调用地点、任务与导航服务</small></label><div class="setting-control"><button :class="['switch',{on:settings.tools}]" @click="settings.tools=!settings.tools"/><span class="metric-sub">{{ settings.tools?'已开启':'已关闭' }}</span></div></div></div><div class="setting-section"><h3>可信回答</h3><p>约束 AI 回答的来源、置信度和审核机制</p><div class="setting-row"><label><b>显示信息来源</b><small>学生端回答附带通知或手册来源</small></label><div class="setting-control"><button :class="['switch',{on:settings.citations}]" @click="settings.citations=!settings.citations"/><span class="metric-sub">{{ settings.citations?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>最低置信度</b><small>低于阈值的回答转人工确认</small></label><div class="range-wrap"><input v-model="settings.confidence" type="range" min="50" max="100"><output>{{ settings.confidence }}%</output></div></div><div class="setting-row"><label><b>未知信息拒答</b><small>知识库无证据时禁止系统自行编造</small></label><div class="setting-control"><button :class="['switch',{on:settings.refuseUnknown}]" @click="settings.refuseUnknown=!settings.refuseUnknown"/><span class="metric-sub">{{ settings.refuseUnknown?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>上下文记忆轮数</b><small>保留最近对话信息</small></label><select v-model.number="settings.memoryTurns" class="select-input"><option :value="10">10 轮</option><option :value="20">20 轮</option><option :value="30">30 轮</option></select></div></div></template>

      <template v-else-if="activeTab==='knowledge'"><div class="setting-section"><h3>知识检索与审核</h3><p>控制知识进入 AI 证据链前的质量门槛</p><div class="setting-row"><label><b>发布前必须审核</b><small>未审核内容不参与学生端回答</small></label><div class="setting-control"><button :class="['switch',{on:settings.reviewRequired}]" @click="settings.reviewRequired=!settings.reviewRequired"/><span class="metric-sub">{{ settings.reviewRequired?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>临期提醒天数</b><small>在知识到期前生成管理员通知</small></label><input v-model.number="settings.expiryWarningDays" class="filter-input small-input" type="number" min="1" max="365"></div><div class="setting-row"><label><b>检索证据数量</b><small>每次回答最多引用的知识条目</small></label><select v-model.number="settings.knowledgeTopK" class="select-input"><option :value="3">3 条</option><option :value="5">5 条</option><option :value="8">8 条</option></select></div></div></template>

      <template v-else-if="activeTab==='twin'"><div class="setting-section"><h3>数字孪生底座</h3><p>使用 SOG 高斯模型查看器；坐标参数为后续标定预留，目前不会自动转换坐标或生成三维路线。</p><div class="setting-row"><label><b>模型访问地址</b><small>请在数字孪生页面配置、校验并预览</small></label><input :value="settings.twinModelUrl" readonly class="filter-input model-select" placeholder="尚未配置 SOG 地址"></div><div class="setting-row"><label><b>坐标系统</b><small>后续需与无人机模型导出坐标核对</small></label><select v-model="settings.coordinateSystem" class="select-input"><option>WGS84</option><option>CGCS2000</option><option>LOCAL</option></select></div><div class="setting-row"><label><b>当前模型名称</b><small>在数字孪生页面维护场景名称与地址</small></label><input :value="settings.twinModelName" readonly class="filter-input model-select" placeholder="尚未配置场景"></div><div class="setting-row"><label><b>模型比例</b><small>后续标定参数，暂不作用于三维路线</small></label><input v-model.number="settings.modelScale" class="filter-input small-input" type="number" min="0.0001" step="0.0001"></div></div></template>

      <template v-else-if="activeTab==='navigation'"><div class="setting-section"><h3>路径规划策略</h3><p>用于管理端路线测试和后续学生端导航</p><div class="setting-row"><label><b>优先无障碍路线</b><small>为行动不便用户优先选择可通行路段</small></label><div class="setting-control"><button :class="['switch',{on:settings.accessiblePriority}]" @click="settings.accessiblePriority=!settings.accessiblePriority"/><span class="metric-sub">{{ settings.accessiblePriority?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>避开受限道路</b><small>关闭或施工路段不参与路径计算</small></label><div class="setting-control"><button :class="['switch',{on:settings.avoidRestricted}]" @click="settings.avoidRestricted=!settings.avoidRestricted"/><span class="metric-sub">{{ settings.avoidRestricted?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>默认步行速度</b><small>用于估算到达时间，单位米/秒</small></label><input v-model.number="settings.walkingSpeed" class="filter-input small-input" type="number" min="0.3" max="3" step="0.1"></div></div></template>

      <template v-else-if="activeTab==='student'"><div class="setting-section"><h3>学生端体验</h3><p>统一管理手机端欢迎语和辅助能力</p><div class="setting-row"><label><b>首页欢迎语</b><small>显示在学生端首页顶部</small></label><input v-model="settings.welcomeText" class="filter-input model-select"></div><div class="setting-row"><label><b>虚拟逛校园入口</b><small>数字孪生模型接入前可关闭</small></label><div class="setting-control"><button :class="['switch',{on:settings.showVirtualTour}]" @click="settings.showVirtualTour=!settings.showVirtualTour"/><span class="metric-sub">{{ settings.showVirtualTour?'已显示':'已隐藏' }}</span></div></div><div class="setting-row"><label><b>语音导航提示</b><small>允许学生端播报道路与到达信息</small></label><div class="setting-control"><button :class="['switch',{on:settings.voiceGuidance}]" @click="settings.voiceGuidance=!settings.voiceGuidance"/><span class="metric-sub">{{ settings.voiceGuidance?'已开启':'已关闭' }}</span></div></div></div></template>

      <template v-else-if="activeTab==='users'"><div class="setting-section"><h3>当前管理账户</h3><p>现阶段为单管理员模式，后续可接入统一认证</p><div class="setting-row"><label><b>管理员显示名称</b><small>用于操作记录和管理端展示</small></label><input v-model="settings.adminName" class="filter-input model-select"></div><div class="setting-row"><label><b>当前角色</b><small>角色名称仅用于本系统权限标识</small></label><input v-model="settings.roleName" class="filter-input model-select"></div><div class="setting-row"><label><b>允许内容编辑员</b><small>为后续多角色权限预留开关</small></label><div class="setting-control"><button :class="['switch',{on:settings.allowEditors}]" @click="settings.allowEditors=!settings.allowEditors"/><span class="metric-sub">{{ settings.allowEditors?'已允许':'未允许' }}</span></div></div></div></template>

      <template v-else><div class="setting-section"><h3>日志与访问安全</h3><p>配置管理端审计和局域网访问策略</p><div class="setting-row"><label><b>记录操作日志</b><small>保存关键数据变更的审计信息</small></label><div class="setting-control"><button :class="['switch',{on:settings.auditLog}]" @click="settings.auditLog=!settings.auditLog"/><span class="metric-sub">{{ settings.auditLog?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>会话有效分钟</b><small>接入登录系统后用于自动退出</small></label><input v-model.number="settings.sessionMinutes" class="filter-input small-input" type="number" min="5" max="1440"></div><div class="setting-row"><label><b>允许同网段访问</b><small>用于手机和其他电脑访问管理服务</small></label><div class="setting-control"><button :class="['switch',{on:settings.allowLan}]" @click="settings.allowLan=!settings.allowLan"/><span class="metric-sub">{{ settings.allowLan?'已允许':'仅本机' }}</span></div></div></div></template>

      <div class="settings-actions"><button class="btn" @click="load">重新读取</button><button class="btn primary" :disabled="saving" @click="save">{{ saving?'保存中…':'保存当前配置' }}</button></div>
    </template></div>
  </section>
</template>

<style scoped>.model-select{width:280px}.small-input{width:140px}.settings-actions{display:flex;justify-content:flex-end;gap:9px}.settings-loading{min-height:260px;display:grid;place-items:center;color:#8b98ab}</style>
