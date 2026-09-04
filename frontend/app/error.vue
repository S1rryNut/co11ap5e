<script setup lang="ts">
import { ArrowLeft, Terminal } from '@lucide/vue'

const props = defineProps<{ error?: any }>()
const statusCode = computed(() => props.error?.statusCode || 500)
const is404 = computed(() => statusCode.value === 404)

useSeoMeta({
  title: () => (is404.value ? '页面不存在' : '出错了'),
  robots: 'noindex'
})
</script>

<template>
  <div class="error-page">
    <SiteHeader />
    <main class="error-card content-wrap" id="main-content">
      <p class="status-line"><Terminal :size="16" /> 折叠思维 / {{ statusCode }}</p>
      <div class="error-code">{{ statusCode }}</div>
      <h1 v-if="is404">这个页面像是走丢了</h1>
      <h1 v-else>页面出了点问题</h1>
      <p v-if="is404">你访问的地址不存在，或者已经被我挪走了。可以从下面几个地方继续逛。</p>
      <p v-else>服务开小差了，稍后再试试，或者先从别的地方逛逛。</p>
      <p v-if="is404" class="error-fold">这一页被折叠弄丢了。—— Folded, to unfold better.</p>
      <div class="error-actions">
        <NuxtLink class="primary-button" to="/"><ArrowLeft :size="17" /> 回到首页</NuxtLink>
        <NuxtLink class="secondary-button" to="/article/tech">浏览文章</NuxtLink>
        <NuxtLink class="text-link" to="/projects">看看项目</NuxtLink>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
