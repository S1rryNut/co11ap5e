<script setup lang="ts">
import { ArrowRight, ArrowUpRight, GitFork as Github } from '@lucide/vue'

interface ProjectItem {
  id: string
  slug: string
  name: string
  summary: string
  responsibility: string
  stack: string[]
  featured?: boolean
  repositoryUrl?: string
  liveUrl?: string
}

const props = defineProps<{ project: ProjectItem; index: number }>()
const { coverFor } = useProjectCovers()
const projectName = (name: string) => name
const role = computed(() => props.project.responsibility?.split('\n').find(Boolean) || '')

// GitHub 风格语言色点（按主流技术栈配色，未命中用中性灰）
const langColors: Record<string, string> = {
  vue: '#41b883', react: '#61dafb', javascript: '#f1e05a', typescript: '#3178c6',
  java: '#b07219', python: '#3572a5', go: '#00add8', rust: '#dea584',
  css: '#663399', html: '#e34c26', nuxt: '#00dc82', spring: '#6db33f',
  node: '#339933', sql: '#e38c00', postgresql: '#336791', nginx: '#009639',
  kotlin: '#a97bff', shell: '#89e051', docker: '#2496ed', 'c++': '#f34b7d', c: '#555555'
}
const langColor = (s: string) => {
  const key = s.trim().toLowerCase()
  for (const [k, c] of Object.entries(langColors)) if (key.includes(k)) return c
  return '#8b949e'
}

// 极轻量 3D 微倾斜（仅客户端，触摸设备与减弱动效用户自动关闭）
const cardEl = ref<HTMLElement | null>(null)
let tiltRaf = 0
const tiltMove = (e: PointerEvent) => {
  const el = cardEl.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  const px = (e.clientX - rect.left) / rect.width - 0.5
  const py = (e.clientY - rect.top) / rect.height - 0.5
  cancelAnimationFrame(tiltRaf)
  tiltRaf = requestAnimationFrame(() => {
    el.style.transform = `perspective(900px) rotateY(${(px * 4).toFixed(2)}deg) rotateX(${(-py * 4).toFixed(2)}deg) translateY(-3px)`
  })
}
const tiltReset = () => { cancelAnimationFrame(tiltRaf); if (cardEl.value) cardEl.value.style.transform = '' }
onMounted(() => {
  const el = cardEl.value
  if (!el || window.matchMedia('(prefers-reduced-motion: reduce)').matches || window.matchMedia('(hover: none)').matches) return
  el.addEventListener('pointermove', tiltMove, { passive: true })
  el.addEventListener('pointerleave', tiltReset, { passive: true })
})
onUnmounted(() => {
  const el = cardEl.value
  if (!el) return
  el.removeEventListener('pointermove', tiltMove)
  el.removeEventListener('pointerleave', tiltReset)
})
</script>

<template>
  <article ref="cardEl" class="project-card">
    <NuxtLink v-if="coverFor(project.slug)" class="project-card-cover" :to="`/projects/${project.slug}`" :aria-label="`查看 ${projectName(project.name)} 案例`">
      <img :src="coverFor(project.slug)" :alt="`${projectName(project.name)} 封面`" loading="lazy" decoding="async">
    </NuxtLink>
    <div class="project-card-body">
      <div class="project-card-meta">
        <span class="project-index">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="project-badge">{{ project.featured ? 'Featured' : 'Project' }}</span>
      </div>
      <NuxtLink class="project-card-title" :to="`/projects/${project.slug}`"><h2>{{ projectName(project.name) }}</h2></NuxtLink>
      <p class="project-card-summary">{{ project.summary }}</p>
      <p v-if="role" class="project-role">{{ role }}</p>
      <div class="tag-list"><span v-for="(item, i) in project.stack" :key="item" :class="{ 'tag-primary': i === 0 }"><i v-if="i === 0" class="lang-dot" :style="{ background: langColor(item) }" />{{ item }}</span></div>
    </div>
    <div class="project-card-foot">
      <NuxtLink class="project-detail-link" :to="`/projects/${project.slug}`">查看完整案例 <ArrowRight :size="15" /></NuxtLink>
      <div class="project-links">
        <a v-if="project.repositoryUrl" :href="project.repositoryUrl" target="_blank" rel="noreferrer" title="查看代码"><Github :size="17" /></a>
        <a v-if="project.liveUrl" :href="project.liveUrl" target="_blank" rel="noreferrer" title="访问项目"><ArrowUpRight :size="17" /></a>
      </div>
    </div>
  </article>
</template>
