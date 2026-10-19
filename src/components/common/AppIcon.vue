<script setup lang="ts">
import { computed } from 'vue'
const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 22 })
const shapes: Record<string, string[]> = {
  overview: ['m3 10 9-7 9 7', 'M5 9v12h5v-7h4v7h5V9'],
  twin: ['m12 3 9 5-9 5-9-5 9-5Z', 'm3 12 9 5 9-5', 'm3 16 9 5 9-5'],
  places: ['M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z', 'M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z'],
  network: ['M6 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M6 8v3a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3V8', 'm15 10 3-3 3 3', 'M6 14v7'],
  tasks: ['M8 5H5v16h14V5h-3', 'M8 3h8v4H8Z', 'm8 14 3 3 5-6'],
  knowledge: ['M12 5C9 3 5 3 2 4v16c3-1 7-1 10 1 3-2 7-2 10-1V4c-3-1-7-1-10 1Z', 'M12 5v16'],
  agents: ['M5 7h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Z', 'M12 3v4', 'M1 12v5m22-5v5M8 12v3m8-3v3m-7 3h6'],
  media: ['M3 3h18v18H3Z', 'm3 17 6-6 4 4 3-3 5 5', 'M16 7h.01'],
  analytics: ['M3 21h19', 'M6 17V9h3v8', 'M12 17V3h3v14', 'M18 17v-5h3v5'],
  settings: ['m10 2-.7 3-2.2 1.3-3-.8-2 3.5 2.3 2.2v2.6L2.1 16l2 3.5 3-.8L9.3 20l.7 2h4l.7-2 2.2-1.3 3 .8 2-3.5-2.3-2.2v-2.6L21.9 9l-2-3.5-3 .8L14.7 5 14 2Z', 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z'],
  road: ['M7 3 3 21h18L17 3', 'M12 3v3m0 4v4m0 4v3'],
  clock: ['M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z', 'M12 6v6l4 2'],
  bell: ['M18 8a6 6 0 0 0-12 0c0 8-3 8-3 10h18c0-2-3-2-3-10', 'M10 21h4'],
  search: ['M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z', 'm16 16 6 6'],
  shield: ['M12 2 3 6v6c0 6 9 10 9 10s9-4 9-10V6l-9-4Z', 'm8 12 3 3 5-6'],
  refresh: ['M20 7a9 9 0 1 0 1 10', 'M20 2v6h-6'],
  arrow: ['M4 12h16', 'm14 6 6 6-6 6'],
  campus: ['M7 21V7l5-5 5 5v14M7 10H2v11h20V10h-5', 'M10 21v-6h4v6M11 7h2v3h-2ZM4 14v3m16-3v3'],
  building: ['M3 22V8h8v14M11 22V2h10v20M6 12h2m-2 4h2m6-10h4m-4 4h4m-4 4h4m-4 4h4'],
}
const paths = computed(() => shapes[props.name] ?? shapes.overview)
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="app-icon"><path v-for="(path, index) in paths" :key="index" :d="path" /></svg>
</template>
