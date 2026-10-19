<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
const props = defineProps<{ label: string; value: string; icon: string; tone?: string; tint?: string; trend?: string; caption?: string }>()
const iconName = computed(() => ({ '⌖': 'places', '⌘': 'network', '▦': 'road', '◫': 'knowledge', '☑': 'tasks', '▣': 'media', '◇': 'twin' } as Record<string, string>)[props.icon] || 'analytics')
const percent = computed(() => props.value.endsWith('%') && Number.isFinite(Number.parseFloat(props.value)) ? Math.min(100, Math.max(0, Number.parseFloat(props.value))) : null)
</script>

<template>
  <article class="stat-card" :style="{ '--tone': tone || '#00865e', '--tint': tint || '#e8f7f0' }">
    <div class="stat-icon stat-symbol"><AppIcon :name="iconName" :size="30" /></div>
    <div class="stat-copy"><span>{{ label }}</span><strong>{{ value }}</strong><small v-if="caption">{{ caption }}</small></div>
    <div v-if="trend" class="stat-trend">{{ trend }}</div>
    <div v-if="percent !== null" class="stat-mini-ring" :style="{ '--percent': `${percent}%` }" aria-hidden="true" />
  </article>
</template>

<style scoped>
.stat-symbol { font-size: 27px; font-weight: 500; }
</style>
