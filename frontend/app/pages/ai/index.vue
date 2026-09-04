<script setup lang="ts">
import { ArrowRight, ArrowUpRight, BrainCircuit, Radio, Route } from '@lucide/vue'

const api = useSiteApi()
const { data: news } = await useAsyncData('ai-news', () => api.getAiNews())
useSeoMeta({ title: 'AI 总览', description: '近期 AI 动态与结构化学习路径。' })
</script>

<template>
  <div class="page-wrap">
    <header class="page-header section-pad content-wrap ai-header">
      <div><p class="eyebrow">AI / Overview</p><h1>人工智能</h1><p>把行业里值得关注的变化收进来，整理成学习路径，再沉淀成能复用的文章。</p><NuxtLink class="primary-button" to="/article/ai">阅读人工智能文章 <ArrowRight :size="17" /></NuxtLink></div>
      <div class="ai-index"><div><BrainCircuit :size="17" /> 学习与实践</div><div><Radio :size="17" /> 近期动态</div><div><NuxtLink to="/learn"><Route :size="17" /> 学习路线</NuxtLink></div></div>
    </header>
    <section class="section-pad content-wrap">
      <div class="section-heading"><div><p class="eyebrow">Signals</p><h2>近期 AI 动态</h2></div><span class="update-note">整理自公开报道 · 附原文链接</span></div>
      <div class="news-grid"><a v-for="item in news" :key="item.id" class="news-item" :href="item.sourceUrl" target="_blank" rel="noreferrer"><div class="news-item-meta"><span>{{ item.sourceName }}</span><ArrowUpRight :size="16" /></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><div class="news-item-footer"><span>{{ new Date(item.publishedAt).toLocaleDateString('zh-CN') }}</span></div></a><p v-if="!news?.length" class="empty-state">暂无 AI 动态。</p></div>
    </section>
  </div>
</template>
