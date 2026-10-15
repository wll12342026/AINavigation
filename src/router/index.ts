import { createRouter, createWebHashHistory } from 'vue-router'

export default createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'overview', component: () => import('@/views/OverviewView.vue') },
    { path: '/twin', name: 'twin', component: () => import('@/views/TwinView.vue') },
    { path: '/places', name: 'places', component: () => import('@/views/PlacesView.vue') },
    { path: '/network', name: 'network', component: () => import('@/views/NetworkView.vue') },
    { path: '/tasks', name: 'tasks', component: () => import('@/views/TasksView.vue') },
    { path: '/knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue') },
    { path: '/media', name: 'media', component: () => import('@/views/MediaView.vue') },
    { path: '/analytics', name: 'analytics', component: () => import('@/views/AnalyticsView.vue') },
    { path: '/settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
  ],
})
