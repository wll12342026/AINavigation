<script setup lang="ts">
import { useAppStore } from '@/stores/app'
const store = useAppStore()
</script>

<template>
  <div class="toast-stack" aria-live="polite">
    <TransitionGroup name="toast-list">
      <div v-for="item in store.toasts" :key="item.id" class="toast"><i>{{ item.type === 'success' ? '✓' : '!' }}</i><span>{{ item.text }}</span></div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-list-enter-active, .toast-list-leave-active { transition: .22s ease; }
.toast-list-enter-from, .toast-list-leave-to { opacity: 0; transform: translateX(15px); }
</style>
