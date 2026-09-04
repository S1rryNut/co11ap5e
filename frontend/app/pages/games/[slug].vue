<script setup lang="ts">
import { ArrowLeft, CalendarDays, Clock3, Gamepad2, MessageSquareText, Star } from '@lucide/vue'
import type { GameEntry } from '~/types'

const route = useRoute()
const api = useSiteApi()
const { data: game, error } = await useAsyncData(`game-${route.params.slug}`, () => api.getGame(String(route.params.slug)))
if (error.value || !game.value) throw createError({ statusCode: 404, statusMessage: '游戏档案不存在' })

const labels: Record<GameEntry['status'], string> = { PLAYING: '游玩中', COMPLETED: '已通关', WISHLIST: '愿望单', DROPPED: '已弃坑', LIBRARY: '游戏库' }
useSeoMeta({ title: () => game.value?.title || '游戏档案', description: () => game.value?.verdict || '' })
</script>

<template>
  <article v-if="game" class="game-detail section-pad">
    <div class="content-wrap">
      <NuxtLink class="back-link" to="/games"><ArrowLeft :size="16" /> 返回游戏档案</NuxtLink>
      <div class="game-detail-layout">
        <div class="game-detail-cover"><img v-if="game.coverUrl" :src="game.coverUrl" :alt="`${game.title} 封面`"><Gamepad2 v-else :size="52" /></div>
        <div class="game-detail-main">
          <p class="eyebrow">{{ labels[game.status] }} · {{ game.platform }}</p>
          <h1>{{ game.title }}</h1>
          <blockquote>{{ game.verdict }}</blockquote>
          <dl><div v-if="game.score != null"><dt><Star :size="17" /> 评分</dt><dd>{{ game.score.toFixed(1) }} / 10</dd></div><div><dt><Clock3 :size="17" /> 游玩时长</dt><dd>{{ game.hoursPlayed }} 小时</dd></div><div v-if="game.completedOn"><dt><CalendarDays :size="17" /> 完成日期</dt><dd>{{ game.completedOn }}</dd></div></dl>
          <NuxtLink v-if="game.reviewArticleSlug" class="primary-button game-review-link" :to="`/blog/${game.reviewArticleSlug}`"><MessageSquareText :size="17" /> 阅读完整锐评</NuxtLink>
        </div>
      </div>
    </div>
  </article>
</template>
