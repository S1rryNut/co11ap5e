<script setup lang="ts">
import { Gamepad2, MessageSquareText } from '@lucide/vue'
const filters = ['全部', '游戏锐评', '游戏推荐', '随笔'] as const
const activeFilter = ref<(typeof filters)[number]>('全部')
const api = useSiteApi()
const { data: articles, status } = await useAsyncData('article-game', () => api.getArticles('', '杂谈'))
const filteredArticles = computed(() => {
  if (activeFilter.value === '全部') return articles.value || []
  return (articles.value || []).filter(article => article.tags.includes(activeFilter.value))
})
const pageSize = 6
const visibleCount = ref(pageSize)
const pagedArticles = computed(() => filteredArticles.value.slice(0, visibleCount.value))
const canLoadMore = computed(() => filteredArticles.value.length > visibleCount.value)
const loadMore = () => { visibleCount.value += pageSize }
watch(activeFilter, () => { visibleCount.value = pageSize })

useSeoMeta({
  title: '游戏杂谈',
  description: '游戏通关后的锐评、推荐，以及技术之外值得记录的内容。'
})
</script>

<template>
  <div class="talk-page">
    <header class="talk-header section-pad">
      <div class="content-wrap talk-heading-grid">
        <div>
          <p class="eyebrow">Article / Game</p>
          <h1>游戏杂谈</h1>
          <p>游戏通关后的锐评与推荐，也记录技术之外值得认真聊聊的内容。</p>
          <nav class="content-switch" aria-label="文章类型"><NuxtLink to="/article/tech">技术文章</NuxtLink><NuxtLink class="active" to="/article/game">游戏杂谈</NuxtLink><NuxtLink to="/article/ai">人工智能</NuxtLink></nav>
        </div>
        <Gamepad2 :size="72" :stroke-width="1.2" aria-hidden="true" />
      </div>
    </header>

    <section class="content-wrap talk-content section-pad">
      <div class="talk-toolbar">
        <div><p class="eyebrow">Article / Game</p><h2>最近游戏杂谈</h2></div>
        <div class="filter-tabs" role="tablist" aria-label="游戏杂谈分类">
          <button v-for="filter in filters" :key="filter" type="button" role="tab" :aria-selected="activeFilter === filter" :class="{ active: activeFilter === filter }" @click="activeFilter = filter">{{ filter }}</button>
        </div>
      </div>
      <div class="article-list">
        <ArticleRow v-for="article in pagedArticles" :key="article.id" :article="article" />
        <p v-if="status === 'pending'" class="empty-state">正在加载内容...</p>
        <p v-else-if="!filteredArticles.length" class="empty-state"><MessageSquareText :size="20" /> 这一栏还没有公开内容。</p>
      </div>
      <div v-if="canLoadMore" class="load-more-wrap"><button type="button" class="load-more-button" @click="loadMore">加载更多</button></div>
    </section>
  </div>
</template>
