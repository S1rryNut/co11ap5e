<script setup lang="ts">
import { ArrowRight } from '@lucide/vue'
import type { LearningResource } from '~/types'

const api = useSiteApi()
const { data: resources } = await useAsyncData('learning-resources', () => api.getLearningResources(), { default: () => [] as LearningResource[] })

// 按 sortOrder 排序
const ordered = computed(() => [...resources.value].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id))

const LEVEL_LABEL: Record<string, string> = { BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '深入' }

useSeoMeta({ title: '学习路线', description: 'AI 转行学习的过程与记录：18 站路线的完整教程。' })
</script>

<template>
  <div class="page-wrap section-pad">
    <header class="page-header content-wrap">
      <p class="eyebrow">Learning / Route</p>
      <h1>学习路线</h1>
      <p>从零转向 AI 应用开发的完整过程：18 站路线的完整教程，每站打开都是可以直接照做的。</p>
    </header>

    <!-- 学习路线 -->
    <section class="section-pad content-wrap learning-layout">
      <div class="sticky-intro">
        <p class="eyebrow">Route</p>
        <h2>学习路线</h2>
        <p>18 站，从大模型认知到项目实战。按顺序学，每站都是一篇完整教程，点开就能照着做。</p>
      </div>
      <div>
        <div v-if="!ordered.length" class="empty-state">学习路线正在整理中。</div>
        <ol v-else class="learning-list">
          <li v-for="(item, index) in ordered" :key="item.id">
            <span class="step-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <NuxtLink class="learning-main" :to="`/learn/${item.slug}`" :title="`查看「${item.title}」详细教程`">
              <small>{{ LEVEL_LABEL[item.level] || item.level }} · {{ item.topic }}</small>
              <h3>{{ item.title }}</h3>
              <span class="learning-more">打开详细教程 <ArrowRight :size="15" /></span>
            </NuxtLink>
          </li>
        </ol>
      </div>
    </section>
  </div>
</template>
