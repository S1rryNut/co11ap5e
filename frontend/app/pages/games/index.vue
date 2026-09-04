<script setup lang="ts">
import { ArrowUpRight, Gamepad2, Star } from '@lucide/vue'
import type { GameEntry } from '~/types'

const api = useSiteApi()
const [{ data: games }, { data: accounts }] = await Promise.all([
  useAsyncData('games', () => api.getGames()),
  useAsyncData('game-accounts', () => api.getGameAccounts())
])
const activeStatus = ref<'ALL' | GameEntry['status']>('ALL')
const statuses = [
  { value: 'ALL' as const, label: '全部' },
  { value: 'PLAYING' as const, label: '游玩中' },
  { value: 'LIBRARY' as const, label: '游戏库' },
  { value: 'COMPLETED' as const, label: '已通关' },
  { value: 'WISHLIST' as const, label: '愿望单' },
  { value: 'DROPPED' as const, label: '已弃坑' }
]
const statusLabel = (status: GameEntry['status']) => statuses.find(item => item.value === status)?.label || status
const sortGamesByHours = (items: GameEntry[]) => [...items].sort((left, right) =>
  Number(right.status === 'PLAYING') - Number(left.status === 'PLAYING')
  || right.hoursPlayed - left.hoursPlayed
  || left.sortOrder - right.sortOrder
)
const filteredGames = computed(() => {
  const source = activeStatus.value === 'ALL' ? games.value || [] : games.value?.filter(game => game.status === activeStatus.value) || []
  return activeStatus.value === 'ALL' ? sortGamesByHours(source) : sortGamesByHours(source)
})
const completedCount = computed(() => games.value?.filter(game => game.status === 'COMPLETED').length || 0)
const totalHours = computed(() => games.value?.reduce((sum, game) => sum + (game.hoursPlayed || 0), 0) || 0)

useSeoMeta({ title: '游戏档案', description: '折叠思维 的游戏记录、锐评与推荐。' })
</script>

<template>
  <div class="page-wrap section-pad game-archive-page">
    <header class="page-header content-wrap"><p class="eyebrow">Games / Library</p><h1>游戏档案</h1><p>按状态整理游玩记录，用评分和短评留下真实体验。</p><div class="game-summary"><span><strong>{{ games?.length || 0 }}</strong>款记录</span><span><strong>{{ completedCount }}</strong>款通关</span><span><strong>{{ totalHours }}</strong>小时游玩</span></div></header>
    <section class="content-wrap game-archive-content">
      <section v-if="accounts?.length" class="game-accounts-section">
        <div class="section-heading compact-heading"><div><p class="eyebrow">Personal</p><h2>个人账号</h2></div></div>
        <div class="account-grid"><a v-for="account in accounts" :key="account.platform" :href="account.url" target="_blank" rel="noreferrer" class="account-item"><div><small>{{ account.platform }}</small><strong>{{ account.handle }}</strong><p>{{ account.description }}</p></div><ArrowUpRight :size="18" /></a></div>
      </section>
      <nav class="filter-tabs game-filter" aria-label="游戏状态">
        <button v-for="status in statuses" :key="status.value" type="button" :class="{ active: activeStatus === status.value }" @click="activeStatus = status.value">{{ status.label }}</button>
      </nav>
      <div v-if="filteredGames.length" class="game-library-list">
        <NuxtLink v-for="game in filteredGames" :key="game.id" class="game-library-row" :to="`/games/${game.slug}`">
          <div class="game-cover">
            <img v-if="game.coverUrl" :src="game.coverUrl" :alt="`${game.title} 封面`" loading="lazy">
            <Gamepad2 v-else :size="34" />
          </div>
          <div class="game-entry-copy"><div class="game-entry-meta"><span class="game-status">{{ statusLabel(game.status) }}</span><span>{{ game.platform || '未标注平台' }}</span></div><h2>{{ game.title }}</h2><p>{{ game.verdict || '暂未添加短评。' }}</p><div class="game-progress"><span :style="{ width: `${Math.min(100, Math.max(4, (game.hoursPlayed || 0) / 100 * 100))}%` }" /><small>{{ game.hoursPlayed || 0 }}h</small></div></div>
          <div class="game-library-score"><strong v-if="game.score != null"><Star :size="15" /> {{ game.score.toFixed(1) }}</strong><span>{{ game.hoursPlayed || 0 }} 小时</span></div>
        </NuxtLink>
      </div>
      <p v-else class="empty-state"><Gamepad2 :size="19" /> 暂无对应的游戏记录。</p>
    </section>
  </div>
</template>
