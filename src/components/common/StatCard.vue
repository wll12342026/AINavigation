<script setup lang="ts">
defineProps<{ label: string; value: string; icon: string; tone?: string; tint?: string; trend?: string; caption?: string }>()
</script>

<template>
  <article class="stat-card" :style="{ '--tone': tone || '#3478f5', '--tint': tint || '#ebf3ff' }">
    <div class="stat-icon stat-symbol">{{ icon }}</div>
    <div class="stat-copy"><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ caption || '较昨日数据已同步' }}</small></div>
    <div v-if="trend" class="stat-trend">{{ trend }}</div>
    <div class="stat-spark" aria-hidden="true"><i /><i /><i /><i /></div>
  </article>
</template>

<style scoped>
.stat-symbol { font-size: 27px; font-weight: 500; }
</style>
