<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'

defineProps<{ workspace?: boolean }>()
const store = useAppStore()
const layer = ref('地点')
const layers = ['地点', '路网', '任务', '热力']
const trees = Array.from({ length: 34 }, (_, index) => index + 1)
</script>

<template>
  <div :class="workspace ? 'scene-workspace panel' : 'map-card'">
    <div class="map-image-crop">
      <div class="campus-blueprint" aria-label="等待导入的数字孪生演示场景">
        <span v-for="tree in trees" :key="`tree-${tree}`" :class="['mock-tree', `tree-${tree}`]" />
        <span v-for="n in 12" :key="n" :class="['mock-building', `building-${n}`]" />
        <i class="mock-road road-a" /><i class="mock-road road-b" /><i class="mock-road road-c" />
        <b class="mock-lake" />
        <svg class="scene-route-layer" viewBox="0 0 1000 520" preserveAspectRatio="none" aria-hidden="true">
          <defs><filter id="routeGlow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>
          <path class="route-halo" d="M102 380 C190 353 220 316 310 305 S430 248 516 269 S655 330 747 286 S835 224 902 244" />
          <path class="route-main" d="M102 380 C190 353 220 316 310 305 S430 248 516 269 S655 330 747 286 S835 224 902 244" />
          <path class="route-closed" d="M410 286 C446 300 475 319 514 337" />
          <circle cx="102" cy="380" r="7" class="route-node"/><circle cx="310" cy="305" r="5" class="route-node"/><circle cx="516" cy="269" r="5" class="route-node"/><circle cx="747" cy="286" r="5" class="route-node"/><circle cx="902" cy="244" r="7" class="route-node"/>
        </svg>
        <em class="mock-pin pin-a"><i>⌖</i><span>东门<small>导航起点</small></span></em>
        <em class="mock-pin pin-b"><i>●</i><span>学院报到点<small>开放中</small></span></em>
        <em class="mock-pin pin-c"><i>▣</i><span>校园卡中心<small>等待 12 分钟</small></span></em>
        <em class="mock-pin pin-d"><i>⌂</i><span>学生公寓<small>A 区</small></span></em>
        <em class="construction-pin"><i>!</i><span>图书馆东侧道路施工<small>AI已生成绕行方案</small></span></em>
        <strong class="pending-model"><i />数字孪生演示场景 · 等待真实模型接入</strong>
      </div>
    </div><div class="map-shade" />
    <template v-if="!workspace">
      <div class="map-toolbar"><button v-for="(item,index) in layers" :key="item" :class="{ active: layer === item }" @click="layer = item"><i>{{ ['⌖','⌘','☑','◉'][index] }}</i>{{ item }}</button></div>
      <div class="map-tools"><button @click="store.notify('已定位校园中心')">◎</button><button @click="store.notify('场景已放大')">+</button><button @click="store.notify('场景已缩小')">−</button><button @click="store.notify('图层面板已展开')">◈</button></div>
      <div class="map-legend"><span><i class="legend-dot" style="background:#29e3b0" />推荐路线</span><span><i class="legend-dot" style="background:#ff5b61" />施工路段</span><span><i class="legend-dot" style="background:#5da0ff" />校园地点</span></div>
      <div class="map-compass"><b>N</b><i /></div>
    </template>
    <template v-else>
      <div class="scene-top"><div class="glass-control"><button class="active">选择</button><button>POI标注</button><button>测量</button><button>设置原点</button></div><div class="glass-control"><button>俯视</button><button>默认视角</button><button>全屏</button></div></div>
      <div class="scene-status"><span>模型：演示场景 v0.1</span><span>坐标系：待标定</span><span>POI：128</span></div>
    </template>
  </div>
</template>
