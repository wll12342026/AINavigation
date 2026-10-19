<script setup lang="ts">
import { navigation } from '@/data/navigation'
import AppIcon from '@/components/common/AppIcon.vue'
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true"><i /><i /><i /><span /></div>
      <div><strong>新生智游</strong><small>校园空间智能管理中心</small></div>
    </div>
    <nav class="main-nav" aria-label="主导航">
      <RouterLink v-for="item in navigation" :key="item.id" :to="item.path" class="nav-item" active-class="active" :exact-active-class="item.path === '/' ? 'active' : ''">
        <AppIcon :name="item.id" :size="24" /><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-foot"><div class="pulse-dot" /><div><strong>校园管理工作空间</strong><span>空间 · 导航 · 知识 · 服务</span></div></div>
    <svg class="sidebar-skyline" viewBox="0 0 240 250" fill="none" aria-hidden="true">
      <g stroke="currentColor" stroke-width=".8">
        <path d="M-10 221 96 187l154 24M-10 235l106-35 154 25M-10 249l106-35 154 26M10 213V89L82 57l71 39v114M10 89l72 30 71-23M82 57v145M17 98v111m9-107v103m10-99v95m10-91v87m10-83v79m10-75v71M89 127v68m10-72v76m11-80v84m11-88v92m11-95v99m10-103v107M10 124l72 29 71-23M10 153l72 28 71-23M18 76l64-29 78 42M170 207v-67l52-20 30 15M170 140l52 17 30-22M222 120v92M179 146v59m11-55v58m11-55v57M170 165l52 17 30-21M170 190l52 17 30-21"/>
        <path d="M-3 218v-39m0-6c-18-5-18-21-7-25-6-14 11-24 17-11 17-3 21 17 10 21 10 13-4 24-20 15M157 216v-36m0-5c-15-3-16-19-6-24-4-13 9-20 15-10 14-4 18 15 9 20 5 13-6 20-18 14"/>
      </g>
    </svg>
  </aside>
</template>

<style scoped>
.nav-item { text-decoration: none; }
.nav-symbol { width: 23px; height: 23px; display: grid; place-items: center; font-size: 21px; color: #345f9b; }
.nav-item.active .nav-symbol { color: #2176ef; }
</style>
