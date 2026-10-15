<script setup lang="ts">
import { navigation } from '@/data/navigation'
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true"><i /><i /><i /><span /></div>
      <div><strong>新生智游</strong><small>校园空间智能管理中心</small></div>
    </div>
    <nav class="main-nav" aria-label="主导航">
      <RouterLink v-for="item in navigation" :key="item.id" :to="item.path" class="nav-item" active-class="active" :exact-active-class="item.path === '/' ? 'active' : ''">
        <span class="nav-symbol">{{ item.icon }}</span><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-foot"><div class="pulse-dot" /><div><strong>管理端已启动</strong><span>服务状态以页面实时检测为准</span></div></div>
    <div class="sidebar-grid" aria-hidden="true" />
  </aside>
</template>

<style scoped>
.nav-item { text-decoration: none; }
.nav-symbol { width: 23px; height: 23px; display: grid; place-items: center; font-size: 21px; color: #c9ddff; }
.nav-item.active .nav-symbol { color: #70eaff; }
</style>
