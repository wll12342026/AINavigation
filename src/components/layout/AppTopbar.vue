<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { navigation } from '@/data/navigation'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const store = useAppStore()
const keyword = ref('')
const current = computed(() => navigation.find((item) => item.id === route.name) ?? navigation[0])

function search() {
  if (keyword.value.trim()) store.notify(`正在全局搜索“${keyword.value.trim()}”`)
}
</script>

<template>
  <header class="topbar">
    <div class="top-title"><div><h1>{{ current.title }}</h1><p>{{ current.subtitle }}</p></div></div>
    <div class="top-actions">
      <button class="campus-switch" @click="store.notify('已切换校区选择器')"><span class="mini-building">▥</span><span>{{ store.campus }}</span><b>⌄</b></button>
      <label class="global-search"><span>⌕</span><input v-model="keyword" placeholder="搜索地点、建筑或任务…" @keydown.enter="search"><kbd>Enter</kbd></label>
      <button class="icon-btn notification" aria-label="通知" @click="store.notify('你有 3 条待处理通知')"><span>♢</span><i /></button>
      <button class="profile" @click="store.notify('管理员账户信息')"><span class="avatar">林</span><span><b>林管理员</b><small>超级管理员</small></span><em>⌄</em></button>
    </div>
  </header>
</template>
