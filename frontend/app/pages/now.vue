<script setup lang="ts">
import { BrainCircuit, Gamepad2, Hammer, Compass } from '@lucide/vue'

const api = useSiteApi()
const [{ data: now }, { data: games }] = await Promise.all([
  useAsyncData('now-profile', () => api.getNowProfile()),
  useAsyncData('now-games', () => api.getGames())
])
const groups = computed(() => now.value ? [
  { key: 'outlook', title: '接下来想做的事', icon: Compass, items: now.value.outlook || [] },
  { key: 'building', title: '正在构建', icon: Hammer, items: now.value.building },
  { key: 'learning', title: '正在学习', icon: BrainCircuit, items: now.value.learning },
  { key: 'playing', title: '正在游玩', icon: Gamepad2, items: [...new Set([...now.value.playing, ...(games.value || []).filter(game => game.status === 'PLAYING').map(game => game.title)])] }
] : [])

useSeoMeta({ title: 'Now', description: '折叠思维 最近正在构建、学习、游玩和阅读的内容。' })
</script>

<template>
  <div v-if="now" class="now-page">
    <header class="page-header section-pad content-wrap now-header">
      <div><p class="eyebrow">Now</p><h1>此时此刻</h1><p>{{ now.headline }}</p></div>
      <time :datetime="now.updatedAt">更新于 {{ new Date(`${now.updatedAt}T00:00:00`).toLocaleDateString('zh-CN') }}</time>
    </header>
    <section class="content-wrap now-grid section-pad">
      <article v-for="group in groups" :key="group.key" class="now-group">
        <header><component :is="group.icon" :size="20" /><h2>{{ group.title }}</h2></header>
        <ul v-if="group.items.length"><li v-for="item in group.items" :key="item">{{ item }}</li></ul>
        <p v-else class="now-empty">暂未公开记录。</p>
      </article>
    </section>
  </div>
</template>
