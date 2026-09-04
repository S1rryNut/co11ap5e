<script setup lang="ts">
import { ArrowLeft, ArrowUpRight, GitFork as Github, Lightbulb, Network, ShieldCheck, Workflow } from '@lucide/vue'

const route = useRoute()
const api = useSiteApi()
const { coverFor } = useProjectCovers()
const projectName = (name: string) => name
const slug = computed(() => String(route.params.slug))
const { data: project, error } = await useAsyncData(`project-${slug.value}`, () => api.getProject(slug.value))

if (error.value || !project.value) {
  throw createError({ statusCode: 404, statusMessage: '项目案例不存在' })
}

useSeoMeta({
  title: () => project.value?.name || '项目案例',
  description: () => project.value?.summary || '',
  ogTitle: () => project.value?.name || '项目案例',
  ogDescription: () => project.value?.summary || ''
})
</script>

<template>
  <article v-if="project" class="project-case">
    <header class="project-case-hero section-pad">
      <div class="content-wrap">
        <NuxtLink class="back-link" to="/projects"><ArrowLeft :size="16" /> 返回项目列表</NuxtLink>
        <div v-if="coverFor(project.slug)" class="project-hero-cover"><img :src="coverFor(project.slug)" :alt="`${projectName(project.name)} 封面`" loading="eager" decoding="async"></div>
        <div class="project-hero-grid">
          <div class="project-hero-copy">
            <p class="eyebrow">Case Study / {{ String(project.id).padStart(2, '0') }}</p>
            <h1>{{ projectName(project.name) }}</h1>
            <p class="project-case-lead">{{ project.summary }}</p>
          </div>
          <aside class="project-hero-card">
            <p class="eyebrow">Stack</p>
            <div class="tag-list"><span v-for="item in project.stack" :key="item">{{ item }}</span></div>
            <div class="project-case-links">
              <a v-if="project.repositoryUrl" :href="project.repositoryUrl" target="_blank" rel="noreferrer"><Github :size="17" /> 代码仓库</a>
              <a v-if="project.liveUrl" :href="project.liveUrl" target="_blank" rel="noreferrer">在线演示 <ArrowUpRight :size="17" /></a>
            </div>
          </aside>
        </div>
      </div>
    </header>

    <section v-if="project.background || project.responsibility" class="content-wrap project-overview-cards section-pad">
      <div v-if="project.background"><p class="eyebrow">Background</p><h2>项目背景</h2><p>{{ project.background }}</p></div>
      <div v-if="project.responsibility"><p class="eyebrow">Responsibility</p><h2>我的职责</h2><p>{{ project.responsibility }}</p></div>
    </section>

    <section class="project-case-band section-pad">
      <div class="content-wrap">
        <header class="section-heading"><div><p class="eyebrow">Workflow</p><h2>业务流程</h2></div><Workflow :size="28" /></header>
        <ol class="case-workflow">
          <li v-for="(step, index) in project.workflow" :key="step"><span>{{ String(index + 1).padStart(2, '0') }}</span><p>{{ step }}</p></li>
        </ol>
      </div>
    </section>

    <section v-if="project.architecture" class="content-wrap case-architecture section-pad">
      <div class="case-section-title"><Network :size="24" /><div><p class="eyebrow">Architecture</p><h2>技术架构</h2></div></div>
      <div class="case-architecture-panel"><p>{{ project.architecture }}</p></div>
    </section>

    <section v-if="project.highlights?.length" class="content-wrap case-highlights section-pad">
      <header class="section-heading"><div><p class="eyebrow">Engineering</p><h2>工程亮点</h2></div><ShieldCheck :size="28" /></header>
      <ul><li v-for="item in project.highlights" :key="item">{{ item }}</li></ul>
    </section>

    <section v-if="project.retrospective" class="project-retrospective section-pad">
      <div class="content-wrap"><Lightbulb :size="26" /><div><p class="eyebrow">Retrospective</p><h2>项目复盘</h2><p>{{ project.retrospective }}</p></div></div>
    </section>
  </article>
</template>
