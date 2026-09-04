<script setup lang="ts">
import { ArrowLeft, ArrowRight, ArrowUpRight, Clock, ListTree, X } from '@lucide/vue'

const route = useRoute()
const api = useSiteApi()

// 旧 slug → 新含义 slug 的 301 重定向，避免历史链接失效
// 站2 已拆分为 Part 2/3/4，原站3~18 顺延 +2 → Part 5~20
const LEGACY_SLUGS: Record<string, string> = {
  'station-01': 'part-1-llm-basics',
  'station-02': 'part-2-llm-principles',
  'station-03': 'part-5-prompt-engineering',
  'station-04': 'part-6-llm-api',
  'station-05': 'part-7-rag',
  'station-06': 'part-8-rag-optimization',
  'station-07': 'part-9-langchain',
  'station-08': 'part-10-agent',
  'station-09': 'part-11-multi-agent',
  'station-10': 'part-12-llamaindex',
  'station-11': 'part-13-transformer',
  'station-12': 'part-14-open-models',
  'station-13': 'part-15-finetuning',
  'station-14': 'part-16-peft-lora',
  'station-15': 'part-17-quantization',
  'station-16': 'part-18-data-evaluation',
  'station-17': 'part-19-multimodal',
  'station-18': 'part-20-projects-career',
  // v24 曾发布的旧含义 slug（Part 编号已 +2，这里做二次跳转）
  'part-3-prompt-engineering': 'part-5-prompt-engineering',
  'part-4-llm-api': 'part-6-llm-api',
  'part-5-rag': 'part-7-rag',
  'part-6-rag-optimization': 'part-8-rag-optimization',
  'part-7-langchain': 'part-9-langchain',
  'part-8-agent': 'part-10-agent',
  'part-9-multi-agent': 'part-11-multi-agent',
  'part-10-llamaindex': 'part-12-llamaindex',
  'part-11-transformer': 'part-13-transformer',
  'part-12-open-models': 'part-14-open-models',
  'part-13-finetuning': 'part-15-finetuning',
  'part-14-peft-lora': 'part-16-peft-lora',
  'part-15-quantization': 'part-17-quantization',
  'part-16-data-evaluation': 'part-18-data-evaluation',
  'part-17-multimodal': 'part-19-multimodal',
  'part-18-projects-career': 'part-20-projects-career'
}
const rawSlug = String(route.params.slug)
if (LEGACY_SLUGS[rawSlug]) {
  await navigateTo(`/learn/${LEGACY_SLUGS[rawSlug]}`, { redirectCode: 301 })
}

const { data: item, error } = await useAsyncData(`learning-${route.params.slug}`, () => api.getLearningResource(String(route.params.slug)))
const { data: allResources } = await useAsyncData('learning-all', () => api.getLearningResources(), { default: () => [] })

if (error.value) throw createError({ statusCode: 404, statusMessage: '学习路线不存在' })

const levelLabel: Record<string, string> = { BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '深入' }

// 上一节 / 下一节（按 sortOrder 排序）
const ordered = computed(() => [...allResources.value].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id))
const currentIndex = computed(() => ordered.value.findIndex(r => r.slug === route.params.slug))
const prevItem = computed(() => currentIndex.value > 0 ? ordered.value[currentIndex.value - 1] : null)
const nextItem = computed(() => currentIndex.value >= 0 && currentIndex.value < ordered.value.length - 1 ? ordered.value[currentIndex.value + 1] : null)

// 正文去掉首个顶层标题（与页面 h1 重复）
const stripFirstH1 = (md: string) => md.replace(/^\s*#\s+[^\n]+\n+/m, '')
const rendered = computed(() => useMarkdown(stripFirstH1(item.value?.content || '')))
const toc = computed(() => item.value ? useMarkdownToc(stripFirstH1(item.value.content || '')) : [])

useSeoMeta({
  title: () => item.value?.title || '学习路线',
  description: () => item.value?.description || '',
  ogTitle: () => item.value?.title || '学习路线',
  ogDescription: () => item.value?.description || ''
})

// 目录滚动高亮
const activeId = ref('')
const updateActiveHeading = () => {
  const threshold = 96
  const headings = Array.from(document.querySelectorAll<HTMLElement>('.learning-detail .markdown-body h2[id], .learning-detail .markdown-body h3[id]'))
  let current = ''
  for (const heading of headings) {
    if (heading.getBoundingClientRect().top <= threshold) current = heading.id
    else break
  }
  activeId.value = current
}
onMounted(() => window.addEventListener('scroll', updateActiveHeading, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', updateActiveHeading))

// 代码块复制（共享事件委托）+ 图片灯箱
const { onContentClick } = useCodeCopy()
const lightbox = ref<{ src: string; alt: string } | null>(null)
const tocOpen = ref(false)
const onContentClickHandler = (e: MouseEvent) => {
  const img = (e.target as HTMLElement).closest<HTMLImageElement>('.markdown-body img')
  if (img && img.src) {
    lightbox.value = { src: img.currentSrc || img.src, alt: img.alt || '' }
    return
  }
  // 自检清单勾选（本地记忆）
  const check = (e.target as HTMLElement).closest<HTMLElement>('.task-check')
  if (check && check.dataset.key) {
    const now = check.dataset.check === '1' ? '0' : '1'
    check.dataset.check = now
    check.setAttribute('aria-checked', now === '1' ? 'true' : 'false')
    check.classList.toggle('is-checked', now === '1')
    try {
      const saved = JSON.parse(localStorage.getItem('co11ap5e-checks') || '{}')
      saved[check.dataset.key] = now === '1'
      localStorage.setItem('co11ap5e-checks', JSON.stringify(saved))
    } catch { /* 静默 */ }
    return
  }
  onContentClick(e)
}
const closeLightbox = () => { lightbox.value = null }
watch(lightbox, (v) => { document.body.style.overflow = v ? 'hidden' : '' })
onMounted(() => {
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') { closeLightbox(); tocOpen.value = false } })
  // 恢复自检清单勾选状态（按 slug:序号 记忆）
  let saved: Record<string, boolean> = {}
  try { saved = JSON.parse(localStorage.getItem('co11ap5e-checks') || '{}') } catch { /* 静默 */ }
  const slug = String(route.params.slug)
  document.querySelectorAll<HTMLElement>('.learning-detail .task-check').forEach((el, i) => {
    const key = `${slug}:${i}`
    el.dataset.key = key
    const checked = saved[key]
    if (checked) {
      el.dataset.check = '1'
      el.setAttribute('aria-checked', 'true')
      el.classList.add('is-checked')
    }
  })
})
</script>

<template>
  <article v-if="item" class="article-page learning-detail section-pad">
    <div class="article-container">
      <NuxtLink class="back-link" to="/learn"><ArrowLeft :size="17" /> 返回学习路线</NuxtLink>
      <header class="article-header">
        <p class="eyebrow">AI 学习路线 · {{ levelLabel[item.level] || item.level }}</p>
        <h1>{{ item.title }}</h1>
        <p>{{ item.description }}</p>
        <div class="article-meta">
          <span class="learning-topic">{{ item.topic }}</span>
          <span v-if="item.url" class="learning-external"><a :href="item.url" target="_blank" rel="noreferrer">外部学习资源 <ArrowUpRight :size="14" /></a></span>
        </div>
      </header>
      <nav v-if="toc.length" class="article-toc" aria-label="目录">
        <p class="article-toc-title">目录</p>
        <ol>
          <li v-for="t in toc" :key="t.id" :class="{ 'toc-h3': t.level === 3 }"><a :href="`#${t.id}`" :class="{ active: activeId === t.id }">{{ t.text }}</a></li>
        </ol>
      </nav>
      <div v-if="rendered" class="markdown-body" v-html="rendered" @click="onContentClickHandler" />
      <div v-else class="empty-state">这份教程还在整理中，先去<a href="/learn">学习路线</a>看看其他内容。</div>

      <nav v-if="prevItem || nextItem" class="article-pagination" aria-label="章节导航">
        <NuxtLink v-if="prevItem" class="paginate-link paginate-prev" :to="`/learn/${prevItem.slug}`">
          <span><ArrowLeft :size="16" /> 上一节</span>
          <strong>{{ prevItem.title }}</strong>
        </NuxtLink>
        <span v-else class="paginate-link paginate-prev paginate-edge"><span><ArrowLeft :size="16" /> 这是第一站</span></span>
        <NuxtLink v-if="nextItem" class="paginate-link paginate-next" :to="`/learn/${nextItem.slug}`">
          <span>下一节 <ArrowRight :size="16" /></span>
          <strong>{{ nextItem.title }}</strong>
        </NuxtLink>
        <span v-else class="paginate-link paginate-next paginate-edge"><span>这是最后一站 <ArrowRight :size="16" /></span></span>
      </nav>
    </div>

    <button v-if="toc.length" type="button" class="toc-fab" aria-label="打开目录" title="展开被折叠的目录" @click="tocOpen = true"><ListTree :size="19" /><span>展开目录</span></button>
    <Transition name="drawer-fade"><div v-if="tocOpen" class="toc-backdrop" @click="tocOpen = false" /></Transition>
    <Transition name="drawer-slide">
      <aside v-if="tocOpen" class="toc-drawer" role="dialog" aria-modal="true" aria-label="目录">
        <header>
          <p class="article-toc-title">目录</p>
          <button type="button" class="toc-drawer-close" aria-label="关闭目录" @click="tocOpen = false"><X :size="20" /></button>
        </header>
        <ol>
          <li v-for="t in toc" :key="t.id" :class="{ 'toc-h3': t.level === 3 }"><a :href="`#${t.id}`" :class="{ active: activeId === t.id }" @click="tocOpen = false">{{ t.text }}</a></li>
        </ol>
      </aside>
    </Transition>

    <Transition name="lightbox-fade">
      <div v-if="lightbox" class="lightbox" role="dialog" aria-modal="true" :aria-label="lightbox.alt || '图片预览'" @click.self="closeLightbox">
        <button type="button" class="lightbox-close" aria-label="关闭预览" @click="closeLightbox"><X :size="22" /></button>
        <figure><img :src="lightbox.src" :alt="lightbox.alt" /><figcaption v-if="lightbox.alt">{{ lightbox.alt }}</figcaption></figure>
      </div>
    </Transition>
  </article>
</template>
