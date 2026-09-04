<script setup lang="ts">
import { ArrowLeft, Check, ChevronLeft, ChevronRight, Clock, Copy, Eye, ListTree, QrCode, Share2, X } from '@lucide/vue'

const route = useRoute()
const api = useSiteApi()
const { data: article, error } = await useAsyncData(`article-${route.params.slug}`, () => api.getArticle(String(route.params.slug)))

if (error.value) throw createError({ statusCode: 404, statusMessage: '文章不存在' })

const rendered = computed(() => useMarkdown(article.value?.content || ''))
const toc = computed(() => article.value ? useMarkdownToc(article.value.content) : [])
const { data: neighbors } = await useAsyncData(`article-neighbors-${route.params.slug}`, () => api.getArticleNeighbors(String(route.params.slug)))
const { data: related } = await useAsyncData(`article-related-${route.params.slug}`, () => api.getRelatedArticles(String(route.params.slug)))

const parentPage = computed(() => {
  if (article.value?.category === '杂谈') return { to: '/article/game', label: '返回游戏杂谈' }
  if (article.value?.category === 'AI 学习') return { to: '/article/ai', label: '返回人工智能' }
  return { to: '/article/tech', label: '返回技术文章' }
})

const sectionLabel = computed(() => {
  const category = article.value?.category
  if (category === '杂谈') return '游戏板块'
  if (category === 'AI 学习') return 'AI 板块'
  return '技术板块'
})

const siteUrl = useRuntimeConfig().public.siteUrl
const canonical = computed(() => `${siteUrl}/blog/${article.value?.slug}`)
const publishedAt = computed(() => article.value?.publishedAt || '')
const viewCounts = useViewCounts([`/blog/${String(route.params.slug)}`])
const articleViews = computed(() => viewCounts[`/blog/${String(route.params.slug)}`])

// 文章分享：复制链接 + 微信扫码
const copyState = ref<'idle' | 'copied'>('idle')
const showQr = ref(false)
const qrDataUrl = ref('')

const copyLink = async () => {
  const url = canonical.value
  try {
    await navigator.clipboard.writeText(url)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = url
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  copyState.value = 'copied'
  setTimeout(() => { copyState.value = 'idle' }, 2000)
}

const openQr = async () => {
  if (showQr.value) { showQr.value = false; return }
  if (!qrDataUrl.value) {
    const { default: QRCode } = await import('qrcode')
    qrDataUrl.value = await QRCode.toDataURL(canonical.value, { width: 220, margin: 1 })
  }
  showQr.value = true
}

useSeoMeta({
  title: () => article.value?.title || '文章',
  description: () => article.value?.excerpt || '',
  ogTitle: () => article.value?.title || '文章',
  ogDescription: () => article.value?.excerpt || '',
  ogType: 'article',
  ogUrl: canonical,
  ogSiteName: '折叠思维',
  ogImage: `${siteUrl}/og-cover.png`,
  twitterCard: 'summary_large_image',
  twitterTitle: () => article.value?.title || '文章',
  twitterDescription: () => article.value?.excerpt || '',
  twitterImage: `${siteUrl}/og-cover.png`
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonical.value }],
  script: [{
    type: 'application/ld+json',
    innerHTML: JSON.stringify({
      '@context': 'https://schema.org',
      '@type': 'Article',
      headline: article.value?.title,
      description: article.value?.excerpt,
      datePublished: publishedAt.value,
      dateModified: publishedAt.value,
      mainEntityOfPage: canonical.value,
      author: { '@type': 'Person', name: '折叠思维' },
      publisher: { '@type': 'Person', name: '折叠思维' }
    })
  }]
}))

// 阅读进度条 + 目录滚动高亮
const progress = ref(0)
const activeId = ref('')
const updateProgress = () => {
  const doc = document.documentElement
  const total = doc.scrollHeight - doc.clientHeight
  progress.value = total > 0 ? Math.min(1, window.scrollY / total) : 0
}
const updateActiveHeading = () => {
  const threshold = 96
  const headings = Array.from(document.querySelectorAll<HTMLElement>('.article-container .markdown-body h2[id], .article-container .markdown-body h3[id]'))
  let current = ''
  for (const heading of headings) {
    if (heading.getBoundingClientRect().top <= threshold) current = heading.id
    else break
  }
  activeId.value = current
}
const onArticleScroll = () => { updateProgress(); updateActiveHeading() }
onMounted(() => {
  window.addEventListener('scroll', onArticleScroll, { passive: true })
  onArticleScroll()
})

// 代码块复制（共享事件委托）
const { onContentClick } = useCodeCopy()

// 图片灯箱
const lightbox = ref<{ src: string; alt: string } | null>(null)

// 移动端目录抽屉
const tocOpen = ref(false)
const onTocKey = (e: KeyboardEvent) => { if (e.key === 'Escape' && tocOpen.value) tocOpen.value = false }
onMounted(() => document.addEventListener('keydown', onTocKey))
const onArticleContentClick = (e: MouseEvent) => {
  const img = (e.target as HTMLElement).closest<HTMLImageElement>('.markdown-body img')
  if (img && img.src) {
    lightbox.value = { src: img.currentSrc || img.src, alt: img.alt || '' }
    return
  }
  onContentClick(e)
}
const closeLightbox = () => { lightbox.value = null }
const onLightboxKey = (e: KeyboardEvent) => { if (e.key === 'Escape') closeLightbox() }
watch(lightbox, (v) => { document.body.style.overflow = v ? 'hidden' : '' })
onMounted(() => document.addEventListener('keydown', onLightboxKey))
onUnmounted(() => { document.removeEventListener('keydown', onLightboxKey); document.removeEventListener('keydown', onTocKey); window.removeEventListener('scroll', onArticleScroll) })
</script>

<template>
  <article v-if="article" class="article-page section-pad">
    <div class="reading-progress" aria-hidden="true"><i :style="{ '--progress': progress }" /></div>
    <div class="article-container">
      <NuxtLink class="back-link" :to="parentPage.to"><ArrowLeft :size="17" /> {{ parentPage.label }}</NuxtLink>
      <header class="article-header">
        <p class="eyebrow">{{ article.category }}</p>
        <h1>{{ article.title }}</h1>
        <p>{{ article.excerpt }}</p>
        <div class="article-meta"><Clock :size="16" /> {{ article.readingMinutes }} 分钟阅读 · {{ new Date(article.publishedAt).toLocaleDateString('zh-CN') }}<span v-if="articleViews"> · </span><span v-if="articleViews" class="view-count"><Eye :size="14" /> {{ articleViews }} 阅读</span></div>
      </header>
      <nav v-if="toc.length" class="article-toc" aria-label="文章目录">
        <p class="article-toc-title">目录</p>
        <ol>
          <li v-for="item in toc" :key="item.id" :class="{ 'toc-h3': item.level === 3 }"><a :href="`#${item.id}`" :class="{ active: activeId === item.id }">{{ item.text }}</a></li>
        </ol>
      </nav>      <div class="markdown-body" v-html="rendered" @click="onArticleContentClick" />

      <div class="article-share">
        <span class="article-share-label"><Share2 :size="15" /> 分享这篇文章</span>
        <p class="article-fold-note">如果你在这里有了 10 分钟的顿悟，那它折叠得刚刚好。</p>
        <div class="article-share-actions">
          <button type="button" class="share-btn" @click="copyLink">
            <Check v-if="copyState === 'copied'" :size="16" /><Copy v-else :size="16" />
            {{ copyState === 'copied' ? '已复制' : '复制链接' }}
          </button>
          <button type="button" class="share-btn" @click="openQr">
            <QrCode :size="16" /> 微信扫码
          </button>
        </div>
        <Transition name="qr-fade">
          <div v-if="showQr" class="share-qr" @click.self="showQr = false">
            <div class="share-qr-card">
              <button type="button" class="share-qr-close" aria-label="关闭" @click="showQr = false"><X :size="18" /></button>
              <img v-if="qrDataUrl" :src="qrDataUrl" alt="微信扫码分享" />
              <p>微信扫一扫 · 把文章分享给朋友</p>
            </div>
          </div>
        </Transition>
      </div>

      <nav v-if="neighbors" class="article-pagination" aria-label="文章导航">
        <NuxtLink v-if="neighbors?.prev" class="paginate-link paginate-prev" :to="`/blog/${neighbors.prev.slug}`">
          <span><ChevronLeft :size="16" /> 上一篇</span>
          <strong>{{ neighbors.prev.title }}</strong>
        </NuxtLink>
        <NuxtLink v-else class="paginate-link paginate-prev paginate-edge" to="/article/tech">
          <span><ChevronLeft :size="16" /> 这是本{{ sectionLabel }}的第一篇</span>
          <strong>看看其他板块 →</strong>
        </NuxtLink>
        <NuxtLink v-if="neighbors?.next" class="paginate-link paginate-next" :to="`/blog/${neighbors.next.slug}`">
          <span>下一篇 <ChevronRight :size="16" /></span>
          <strong>{{ neighbors.next.title }}</strong>
        </NuxtLink>
        <NuxtLink v-else class="paginate-link paginate-next paginate-edge" to="/article/tech">
          <span>本{{ sectionLabel }}的文章已经读完 <ChevronRight :size="16" /></span>
          <strong>看看其他板块 →</strong>
        </NuxtLink>
      </nav>

      <section v-if="related?.length" class="related-articles" aria-label="相关文章">
        <h2>相关文章</h2>
        <div class="related-list">
          <NuxtLink v-for="item in related" :key="item.id" class="related-item" :to="`/blog/${item.slug}`">
            <div><span>{{ item.category }}</span><small>{{ item.readingMinutes }} 分钟</small></div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.excerpt }}</p>
          </NuxtLink>
        </div>
      </section>

      <ArticleComments :term="`/blog/${article.slug}`" />
    </div>

    <!-- 移动端目录抽屉 -->
    <button v-if="toc.length" type="button" class="toc-fab" aria-label="打开目录" title="展开被折叠的目录" @click="tocOpen = true">
      <ListTree :size="19" /><span>展开目录</span>
    </button>
    <Transition name="drawer-fade">
      <div v-if="tocOpen" class="toc-backdrop" @click="tocOpen = false" />
    </Transition>
    <Transition name="drawer-slide">
      <aside v-if="tocOpen" class="toc-drawer" role="dialog" aria-modal="true" aria-label="文章目录">
        <header>
          <p class="article-toc-title">目录</p>
          <button type="button" class="toc-drawer-close" aria-label="关闭目录" @click="tocOpen = false"><X :size="20" /></button>
        </header>
        <ol>
          <li v-for="item in toc" :key="item.id" :class="{ 'toc-h3': item.level === 3 }">
            <a :href="`#${item.id}`" :class="{ active: activeId === item.id }" @click="tocOpen = false">{{ item.text }}</a>
          </li>
        </ol>
      </aside>
    </Transition>

    <Transition name="lightbox-fade">
      <div v-if="lightbox" class="lightbox" role="dialog" aria-modal="true" :aria-label="lightbox.alt || '图片预览'" @click.self="closeLightbox">
        <button type="button" class="lightbox-close" aria-label="关闭预览" @click="closeLightbox"><X :size="22" /></button>
        <figure>
          <img :src="lightbox.src" :alt="lightbox.alt" />
          <figcaption v-if="lightbox.alt">{{ lightbox.alt }}</figcaption>
        </figure>
      </div>
    </Transition>
  </article>
</template>
