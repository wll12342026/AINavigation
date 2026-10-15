<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'
const store = useAppStore()
const confidence = ref(82)
const multimodal = ref(true)
const tools = ref(true)
const citations = ref(true)
const refuseUnknown = ref(true)
</script>

<template>
  <section class="panel settings-layout"><aside class="settings-tabs"><button class="active">AI模型与智能体</button><button>知识库参数</button><button>数字孪生配置</button><button>导航引擎</button><button>学生端配置</button><button>用户与角色</button><button>日志与安全</button></aside>
    <div class="settings-content"><div class="setting-section"><h3>AI模型配置</h3><p>配置校园智能体使用的语言与视觉模型</p><div class="setting-row"><label><b>主语言模型</b><small>用于任务理解、工具调用和回答生成</small></label><div class="setting-control"><select class="select-input model-select"><option>校园智能体模型 v1</option></select><span class="tag green">连接正常</span></div></div><div class="setting-row"><label><b>多模态图像识别</b><small>用于建筑照片识别与实景质检</small></label><div class="setting-control"><button :class="['switch',{on:multimodal}]" @click="multimodal=!multimodal"/><span class="metric-sub">{{ multimodal?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>工具调用</b><small>允许 AI 调用地点、任务与导航服务</small></label><div class="setting-control"><button :class="['switch',{on:tools}]" @click="tools=!tools"/><span class="metric-sub">{{ tools?'已开启':'已关闭' }}</span></div></div></div>
    <div class="setting-section"><h3>可信回答</h3><p>约束 AI 回答的来源、置信度和审核机制</p><div class="setting-row"><label><b>显示信息来源</b><small>学生端回答附带通知或手册来源</small></label><div class="setting-control"><button :class="['switch',{on:citations}]" @click="citations=!citations"/><span class="metric-sub">{{ citations?'已开启':'已关闭' }}</span></div></div><div class="setting-row"><label><b>最低置信度</b><small>低于阈值的回答转人工确认</small></label><div class="range-wrap"><input v-model="confidence" type="range" min="50" max="100"><output>{{ confidence }}%</output></div></div><div class="setting-row"><label><b>未知信息拒答</b><small>知识库无证据时禁止模型自行编造</small></label><div class="setting-control"><button :class="['switch',{on:refuseUnknown}]" @click="refuseUnknown=!refuseUnknown"/><span class="metric-sub">{{ refuseUnknown?'已开启':'已关闭' }}</span></div></div></div>
    <div class="setting-section"><h3>对话与任务策略</h3><p>配置智能体的记忆和主动提醒能力</p><div class="setting-row"><label><b>上下文记忆轮数</b><small>保留当前会话中最近对话信息</small></label><div class="setting-control"><select class="select-input"><option>10 轮</option><option>20 轮</option></select></div></div></div><div class="settings-actions"><button class="btn">恢复默认</button><button class="btn primary" @click="store.notify('系统配置已保存')">保存配置</button></div></div>
  </section>
</template>

<style scoped>.model-select{width:280px}.settings-actions{display:flex;justify-content:flex-end;gap:9px}</style>
