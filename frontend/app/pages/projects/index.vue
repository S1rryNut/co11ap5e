<script setup lang="ts">
const api = useSiteApi()
const { data: projects } = await useAsyncData('projects', () => api.getProjects())
useSeoMeta({ title: '项目', description: '个人项目与工程实践。' })
</script>

<template>
  <div class="page-wrap section-pad">
    <header class="page-header content-wrap"><p class="eyebrow">Projects</p><h1>项目与实践</h1><p>从问题出发，记录设计、实现与迭代过程。</p></header>
    <section class="content-wrap project-list">
      <ProjectCard v-for="(project, index) in projects" :key="project.id" :project="project" :index="index" />
      <p v-if="!projects?.length" class="empty-state">项目档案还没开放，先去文章或 Now 页逛逛。</p>
    </section>
  </div>
</template>
