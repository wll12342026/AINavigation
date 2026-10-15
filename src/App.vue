<script setup lang="ts">
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import ToastHost from '@/components/common/ToastHost.vue'
</script>

<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="main-area">
      <AppTopbar />
      <section class="page-root">
        <RouterView v-slot="{ Component }">
          <Transition name="route-page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </section>
    </main>
    <ToastHost />
  </div>
</template>

<style>
.route-page-enter-active, .route-page-leave-active { transition: opacity .16s ease, transform .16s ease; }
.route-page-enter-from { opacity: 0; transform: translateY(5px); }
.route-page-leave-to { opacity: 0; transform: translateY(-3px); }
</style>
