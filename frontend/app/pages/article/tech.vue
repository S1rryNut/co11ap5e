<script setup lang="ts">
import { Search } from '@lucide/vue'

const query = ref('')
const api = useSiteApi()
const { data: articles, status, refresh } = await useAsyncData('article-tech', () => api.getArticles(query.value, '', '杂谈'))
const filteredArticles = computed(() => (articles.value || []).filter(article => article.category !== 'AI 学习'))
const pageSize = 6
const visibleCount = ref(pageSize)
const pagedArticles = computed(() => filteredArticles.value.slice(0, visibleCount.value))
const canLoadMore = computed(() => filteredArticles.value.length > visibleCount.value)
const loadMore = () => { visibleCount.value += pageSize }
let timer: ReturnType<typeof setTimeout>
watch(query, () => {
  clearTimeout(timer)
  timer = setTimeout(() => { visibleCount.value = pageSize; refresh() }, 300)
})

useSeoMeta({ title: '文章', description: '关于软件开发、系统设计与 AI 学习的文章。' })
</script>

<template>
  <div class="page-wrap section-pad">
    <header class="page-header content-wrap">
      <p class="eyebrow">Article / Tech</p>
      <h1>写作与思考</h1>
      <p>记录工程实践、技术判断和学习过程中值得留下来的部分。</p>
      <nav class="content-switch" aria-label="文章类型"><NuxtLink class="active" to="/article/tech">技术文章</NuxtLink><NuxtLink to="/article/game">游戏杂谈</NuxtLink><NuxtLink to="/article/ai">人工智能</NuxtLink></nav>
    </header>
    <section class="content-wrap content-section">
      <label class="search-field">
        <Search :size="18" />
        <input v-model="query" type="search" placeholder="搜索文章、分类或标签" aria-label="搜索文章">
      </label>
      <div class="article-list">
        <ArticleRow v-for="article in pagedArticles" :key="article.id" :article="article" />
        <p v-if="status === 'pending'" class="empty-state">正在加载文章…</p>
        <p v-else-if="!filteredArticles.length" class="empty-state">没有找到匹配的文章。</p>
      </div>
      <div v-if="canLoadMore" class="load-more-wrap"><button type="button" class="load-more-button" @click="loadMore">加载更多</button></div>
    </section>
  </div>
</template>
