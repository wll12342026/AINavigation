<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { navigation } from '@/data/navigation'
import { getDashboardOverview, knowledgeApi, placesApi, tasksApi } from '@/services/api'
import { useAppStore } from '@/stores/app'
import AppIcon from '@/components/common/AppIcon.vue'
import { loadWorkspaceAccess, workspaceAccessReady, workspaceRequiresLogin } from '@/services/workspace'

type SearchResult = { id: string; title: string; meta: string; path: string; icon: string }
type Notice = { id: string; title: string; detail: string; path: string; tone: 'warning' | 'info' }

const route = useRoute()
const router = useRouter()
const store = useAppStore()
const keyword = ref('')
const searching = ref(false)
const searchOpen = ref(false)
const campusOpen = ref(false)
const noticeOpen = ref(false)
const profileOpen = ref(false)
const searchResults = ref<SearchResult[]>([])
const notices = ref<Notice[]>([])
const now = ref(new Date())
let clockTimer: number | undefined
const current = computed(() => navigation.find((item) => item.id === route.name) ?? navigation[0])
const dateTime = computed(() => `${now.value.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', weekday: 'short' })} ${now.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })}`)

function closeMenus() {
  campusOpen.value = false
  noticeOpen.value = false
  profileOpen.value = false
}

watch(() => route.fullPath, () => { closeMenus(); searchOpen.value = false })

async function search() {
  const term = keyword.value.trim().toLowerCase()
  if (!term) {
    searchResults.value = []
    searchOpen.value = false
    return
  }
  searching.value = true
  closeMenus()
  try {
    const [places, tasks, knowledge] = await Promise.all([placesApi.list(), tasksApi.list(), knowledgeApi.list()])
    searchResults.value = [
      ...places.filter(item => `${item.name}${item.building}${item.description}`.toLowerCase().includes(term)).map(item => ({ id: `place-${item.id}`, title: item.name, meta: `校园地点 · ${item.building || '未配置建筑'}`, path: '/places', icon: '⌖' })),
      ...tasks.filter(item => `${item.name}${item.location}${item.description}`.toLowerCase().includes(term)).map(item => ({ id: `task-${item.id}`, title: item.name, meta: `新生任务 · ${item.location || '未配置地点'}`, path: '/tasks', icon: '☑' })),
      ...knowledge.filter(item => `${item.title}${item.category}${item.content}`.toLowerCase().includes(term)).map(item => ({ id: `knowledge-${item.id}`, title: item.title, meta: `AI知识库 · ${item.category}`, path: '/knowledge', icon: '◫' })),
    ].slice(0, 10)
    searchOpen.value = true
  } catch {
    store.notify('全局搜索失败，请检查后端连接', 'warning')
  } finally {
    searching.value = false
  }
}

async function toggleNotices() {
  const next = !noticeOpen.value
  closeMenus()
  searchOpen.value = false
  noticeOpen.value = next
  if (!next) return
  try {
    const data = await getDashboardOverview()
    notices.value = [
      ...(data.pendingKnowledge ? [{ id: 'pending', title: `${data.pendingKnowledge} 条知识等待审核`, detail: '进入知识库完成内容复核', path: '/knowledge', tone: 'warning' as const }] : []),
      ...(data.expiringKnowledge ? [{ id: 'expiring', title: `${data.expiringKnowledge} 条知识即将过期`, detail: '建议核对来源并更新有效期', path: '/knowledge', tone: 'warning' as const }] : []),
      ...(data.roadCount === 0 ? [{ id: 'road', title: '尚未录入正式路网', detail: '录入道路后才能进行路线测试', path: '/network', tone: 'info' as const }] : []),
      ...(data.placeCount === 0 ? [{ id: 'place', title: '尚未录入正式地点', detail: '地点数据将用于导航与孪生标注', path: '/places', tone: 'info' as const }] : []),
    ]
  } catch {
    notices.value = [{ id: 'offline', title: '后端连接异常', detail: '请检查 Spring Boot 与 MySQL 服务', path: '/settings', tone: 'warning' }]
  }
}

function openResult(result: SearchResult) {
  searchOpen.value = false
  router.push({ path: result.path, query: { q: keyword.value.trim() } })
}

function openNotice(notice: Notice) {
  noticeOpen.value = false
  router.push(notice.path)
}

function toggleCampus() {
  const next = !campusOpen.value
  closeMenus()
  searchOpen.value = false
  campusOpen.value = next
}

function toggleProfile() {
  const next = !profileOpen.value
  closeMenus()
  searchOpen.value = false
  profileOpen.value = next
}

onMounted(() => {
  clockTimer = window.setInterval(() => { now.value = new Date() }, 30_000)
  loadWorkspaceAccess().catch(() => { /* The page retains its own connection error and retry controls. */ })
})
onUnmounted(() => { if (clockTimer) window.clearInterval(clockTimer) })
</script>

<template>
  <header class="topbar">
    <div class="top-title"><div><h1>{{ current.title }}</h1><p>{{ current.subtitle }}</p></div></div>
    <div class="top-actions">
      <div class="top-popover-wrap">
        <button class="campus-switch" :aria-expanded="campusOpen" @click="toggleCampus"><AppIcon name="campus" :size="25" class="mini-building" /><span>{{ store.campus }}</span></button>
        <div v-if="campusOpen" class="top-popover campus-menu"><button class="popover-row active" @click="store.campus='主校区';campusOpen=false"><span>▥</span><span><b>主校区</b><small>当前数据空间</small></span><em>✓</em></button><p>新增校区后会自动出现在这里。</p></div>
      </div>
      <div class="top-popover-wrap search-wrap">
        <label class="global-search"><AppIcon name="search" :size="19" /><input aria-label="搜索地点、任务或知识" v-model="keyword" placeholder="搜索地点、任务或知识…" @keydown.enter="search" @input="searchOpen=false"><span v-if="searching" class="search-loading" role="status" aria-label="正在搜索">…</span></label>
        <div v-if="searchOpen" class="top-popover search-results"><div class="popover-title">全局搜索结果 <span>{{ searchResults.length }} 条</span></div><button v-for="result in searchResults" :key="result.id" class="popover-row" @click="openResult(result)"><i>{{ result.icon }}</i><span><b>{{ result.title }}</b><small>{{ result.meta }}</small></span><em>›</em></button><div v-if="!searchResults.length" class="popover-empty">没有找到与“{{ keyword }}”匹配的真实数据</div></div>
      </div>
      <div class="top-popover-wrap">
        <button class="icon-btn notification" aria-label="通知" :aria-expanded="noticeOpen" @click="toggleNotices"><AppIcon name="bell" /><i v-if="notices.length" /></button>
        <div v-if="noticeOpen" class="top-popover notice-menu"><div class="popover-title">待处理事项 <span>{{ notices.length }}</span></div><button v-for="notice in notices" :key="notice.id" class="popover-row" @click="openNotice(notice)"><i :class="notice.tone">{{ notice.tone==='warning'?'!':'i' }}</i><span><b>{{ notice.title }}</b><small>{{ notice.detail }}</small></span><em>›</em></button><div v-if="!notices.length" class="popover-empty">当前没有待处理事项</div></div>
      </div>
      <time class="top-clock"><AppIcon name="clock" :size="20" />{{ dateTime }}</time>
      <div class="top-popover-wrap">
        <button class="profile" aria-label="林管理员，账户菜单" :aria-expanded="profileOpen" @click="toggleProfile"><span class="avatar">林</span></button>
        <div v-if="profileOpen" class="top-popover profile-menu"><span v-if="workspaceAccessReady && !workspaceRequiresLogin" class="demo-mode-badge">开发演示</span><div class="account-card"><span class="avatar">林</span><div><b>林管理员</b><small>本地管理账户 · 超级管理员</small></div></div><button class="popover-row" @click="profileOpen=false;router.push('/settings')"><i>⚙</i><span><b>系统设置</b><small>模型、导航与安全配置</small></span><em>›</em></button><button class="popover-row" @click="profileOpen=false;router.go(0)"><i>↻</i><span><b>重新加载</b><small>刷新当前管理页面</small></span><em>›</em></button></div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-popover-wrap{position:relative}.search-wrap{width:300px}.search-wrap .global-search{width:100%}.top-popover{position:absolute;z-index:80;right:0;top:calc(100% + 10px);width:330px;padding:8px;border:1px solid #dfe7f2;border-radius:13px;background:#fff;box-shadow:0 18px 45px rgba(21,48,91,.18)}.campus-menu{left:0;right:auto;width:245px}.profile-menu{width:280px}.popover-title{display:flex;justify-content:space-between;padding:8px 9px 10px;color:#25334d;font-size:12px;font-weight:800}.popover-title span{color:#8190a7;font-size:10px}.popover-row{width:100%;min-height:49px;padding:7px 9px;display:grid;grid-template-columns:28px 1fr auto;align-items:center;gap:9px;border:0;border-radius:9px;background:transparent;text-align:left;color:#40506a}.popover-row:hover,.popover-row.active{background:#f0f6ff}.popover-row>i,.popover-row>span:first-child{width:26px;height:26px;display:grid;place-items:center;border-radius:8px;color:#3275ef;background:#eaf2ff;font-style:normal}.popover-row>i.warning{color:#df6d28;background:#fff1e7}.popover-row b,.popover-row small{display:block}.popover-row b{font-size:11px}.popover-row small{margin-top:2px;color:#8a97aa;font-size:9px}.popover-row em{color:#9aa7b9;font-style:normal}.popover-empty{padding:22px 12px;text-align:center;color:#8b98aa;font-size:10px}.campus-menu p{margin:5px 9px 7px;color:#98a4b5;font-size:9px}.account-card{display:flex;align-items:center;gap:10px;padding:10px;margin-bottom:5px;border-bottom:1px solid #edf1f6}.account-card .avatar{flex:0 0 auto}.account-card b,.account-card small{display:block}.account-card b{font-size:12px}.account-card small{margin-top:3px;color:#8c99ab;font-size:9px}
.weather-chip{display:flex;align-items:center;gap:5px;padding-left:4px;color:#415673;white-space:nowrap}.weather-chip span{color:#ffad27;font-size:19px}.weather-chip b{font-size:10px}.top-clock{padding-left:10px;border-left:1px solid #e4ebf4;color:#71819a;font-size:9px;white-space:nowrap}
</style>
