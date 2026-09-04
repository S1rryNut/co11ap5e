<script setup lang="ts">
import { ArrowUpRight, Eye } from '@lucide/vue'
import type { ArticleSummary } from '~/types'

const props = defineProps<{ article: ArticleSummary }>()

const path = computed(() => `/blog/${props.article.slug}`)
const counts = useViewCounts([path.value])
const views = computed(() => counts[path.value])

const date = (value: string) => new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'short', day: 'numeric'
}).format(new Date(value))
</script>

<template>
  <NuxtLink class="article-row" :to="`/blog/${article.slug}`">
    <div class="article-date">
      <span>{{ date(article.publishedAt) }}</span>
      <span v-if="views" class="view-count" :title="`${views} 次阅读`"><Eye :size="13" />{{ views }}</span>
    </div>
    <div class="article-copy">
      <div class="eyebrow">{{ article.category }} · {{ article.readingMinutes }} 分钟</div>
      <h3>{{ article.title }}</h3>
      <p>{{ article.excerpt }}</p>
      <div class="tag-list">
        <span v-for="tag in article.tags" :key="tag">#{{ tag }}</span>
      </div>
    </div>
    <ArrowUpRight class="row-arrow" :size="20" />
  </NuxtLink>
</template>
