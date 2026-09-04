<script setup lang="ts">
import { ArrowRight, BrainCircuit, Search } from '@lucide/vue'

const api = useSiteApi()
const query = ref('')
const { data: articles, status, refresh } = await useAsyncData('article-ai', () => api.getArticles(query.value, 'AI 学习'))
const pageSize = 6
const visibleCount = ref(pageSize)
const pagedArticles = computed(() => (articles.value || []).slice(0, visibleCount.value))
const canLoadMore = computed(() => (articles.value?.length || 0) > visibleCount.value)
const loadMore = () => { visibleCount.value += pageSize }
let timer: ReturnType<typeof setTimeout>
watch(query, () => { clearTimeout(timer); timer = setTimeout(() => { visibleCount.value = pageSize; refresh() }, 300) })
useSeoMeta({ title: '人工智能', description: '人工智能学习、实践与思考文章。' })
</script>

<template>
  <div class="page-wrap section-pad">
    <header class="page-header content-wrap"><p class="eyebrow">Article / AI</p><h1>人工智能</h1><p>记录人工智能学习、工程实践和技术判断。</p><nav class="content-switch" aria-label="文章类型"><NuxtLink to="/article/tech">技术文章</NuxtLink><NuxtLink to="/article/game">游戏杂谈</NuxtLink><NuxtLink class="active" to="/article/ai">人工智能</NuxtLink></nav></header>
    <section class="content-wrap content-section">
      <label class="search-field"><Search :size="18" /><input v-model="query" type="search" placeholder="搜索人工智能文章" aria-label="搜索人工智能文章"></label>
      <div class="article-list"><ArticleRow v-for="article in pagedArticles" :key="article.id" :article="article" /><p v-if="status === 'pending'" class="empty-state">正在加载文章...</p><p v-else-if="!articles?.length" class="empty-state"><BrainCircuit :size="20" /> 暂无人工智能文章。</p></div>
      <div v-if="canLoadMore" class="load-more-wrap"><button type="button" class="load-more-button" @click="loadMore">加载更多</button></div>
      <NuxtLink class="text-link" to="/ai">返回 AI 总览 <ArrowRight :size="16" /></NuxtLink>
    </section>
  </div>
</template>
