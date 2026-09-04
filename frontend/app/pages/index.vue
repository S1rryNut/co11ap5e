<script setup lang="ts">
import { ArrowRight, BrainCircuit, Code2, FileText, MapPin, Terminal, Compass } from '@lucide/vue'

const api = useSiteApi()
const heroPhrases = [
  '前端开发 · Web 安全 · 密码学',
  '记录构建、学习和持续思考',
  '欢迎交流，一起做点有意思的'
]
const [{ data: projects }, { data: news }, { data: now }, { data: games }, { data: allArticles }] = await Promise.all([
  useAsyncData('home-projects', () => api.getProjects()),
  useAsyncData('home-news', () => api.getAiNews()),
  useAsyncData('home-now', () => api.getNowProfile()),
  useAsyncData('home-games', () => api.getGames()),
  useAsyncData('home-all-articles', () => api.getArticles())
])

// 最近文章：三大类（AI 学习 / 技术文章 / 游戏杂谈）各取最近一篇，按时间倒序
const majorGroups = [
  { name: 'AI 学习', match: (a: { category: string }) => a.category === 'AI 学习' },
  { name: '技术文章', match: (a: { category: string }) => a.category === '工程实践' || a.category === '项目复盘' },
  { name: '游戏杂谈', match: (a: { category: string }) => a.category === '杂谈' }
]
const recentArticles = computed(() => {
  const list = allArticles.value || []
  const byDate = (xs: typeof list) => [...xs].sort((a, b) => b.publishedAt.localeCompare(a.publishedAt))
  const picked = majorGroups
    .map(group => byDate(list.filter(a => group.match(a)))[0])
    .filter(Boolean)
  const pickedIds = new Set(picked.map(a => a.id))
  const rest = byDate(list.filter(a => !pickedIds.has(a.id)))
  return [...picked, ...rest].slice(0, 6).sort((a, b) => b.publishedAt.localeCompare(a.publishedAt))
})

useSeoMeta({
  title: '首页',
  ogTitle: '折叠思维 | 开发者与 AI 学习者',
  ogDescription: '折叠千行思考，展开一瞬顿悟。个人博客、项目作品与 AI 学习记录。'
})

const siteUrl = useRuntimeConfig().public.siteUrl
useHead(() => ({
  link: [{ rel: 'canonical', href: `${siteUrl}/` }],
  script: [{
    type: 'application/ld+json',
    innerHTML: JSON.stringify({
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'WebSite',
          name: '折叠思维',
          url: `${siteUrl}/`,
          description: '个人博客、项目作品、简历与 AI 学习记录。',
          inLanguage: 'zh-CN'
        },
        {
          '@type': 'Person',
          name: '折叠思维',
          url: `${siteUrl}/`,
          jobTitle: '前端开发者 / AI 学习者',
          knowsAbout: ['前端开发', 'Web 安全', '密码学', 'AI 应用开发']
        }
      ]
    })
  }]
}))
</script>

<template>
  <div>
    <section class="hero section-pad">
      <div class="hero-bg" aria-hidden="true"><span class="hero-grain" /></div>
      <div class="content-wrap hero-grid">
        <div class="hero-copy" v-reveal>
          <p class="status-line"><Terminal :size="16" /> 折叠思维 / home</p>
          <h1>把想法做成<br><span class="hero-accent">可用的成果</span></h1>
          <p class="hero-lead">我是折叠思维，主要做前端开发，也在补 Web 安全、密码学和 AI 应用开发。</p>
          <p class="hero-fold">Folded, to unfold better.</p>
          <Typewriter :phrases="heroPhrases" />
          <div class="hero-actions">
            <NuxtLink class="primary-button" to="/projects">查看我的成果 <ArrowRight :size="18" /></NuxtLink>
            <NuxtLink class="text-link" to="/article/tech">阅读文章</NuxtLink>
          </div>
        </div>
        <div class="profile-panel" aria-label="个人概览" v-reveal>
          <div class="profile-filebar"><FileText :size="16" /><span>profile / README.md</span></div>
          <div class="profile-summary">
            <div class="portrait-block" aria-hidden="true"><img src="/logo.png?v=3" alt="" /></div>
            <div><strong>Co11ap5e</strong><span>@Cls</span></div>
          </div>
          <p class="profile-bio">熟悉 Vue、React、Java 和前端安全，做过业务平台、个人工具和内容站点。</p>
          <p class="profile-fold">我折叠思考，你展开收获。</p>
          <dl class="quick-facts">
            <div><dt><MapPin :size="16" /> 所在地</dt><dd>江苏苏州</dd></div>
            <div><dt><Code2 :size="16" /> 方向</dt><dd>前端 / Web 安全</dd></div>
            <div><dt><BrainCircuit :size="16" /> 正在学习</dt><dd>RAG / Agent / Web 安全</dd></div>
          </dl>
          <p class="availability"><span /> 可接受新的合作与交流</p>
        </div>
      </div>
    </section>

    <section class="metric-band" v-reveal>
      <div class="content-wrap metrics">
        <div><strong>{{ allArticles?.length || 0 }}</strong><span>篇文章</span></div>
        <div><strong>{{ projects?.length || 0 }}</strong><span>项项目</span></div>
        <div><strong>{{ news?.length || 0 }}</strong><span>条 AI 动态</span></div>
        <div><strong>{{ games?.length || 0 }}</strong><span>款游戏档案</span></div>
      </div>
    </section>

    <section class="section-pad">
      <div class="content-wrap">
        <div class="section-heading">
          <div><p class="eyebrow">Selected work</p><h2>成果展示</h2></div>
          <NuxtLink class="text-link" to="/projects">全部项目 <ArrowRight :size="16" /></NuxtLink>
        </div>
        <div class="project-list home-project-list">
          <ProjectCard v-for="(project, index) in projects?.slice(0, 3)" :key="project.id" :project="project" :index="index" v-reveal />
          <p v-if="!projects?.length" class="empty-state">成果还在路上，先去文章或 Now 页逛逛。</p>
        </div>
      </div>
    </section>

    <section class="section-pad alt-band">
      <div class="content-wrap split-section" v-reveal>
        <div class="section-intro"><p class="eyebrow">Next / Now</p><h2>近期计划</h2><p>{{ now?.headline || '持续构建、学习和记录。' }}</p><NuxtLink class="text-link" to="/now">查看完整 Now <ArrowRight :size="16" /></NuxtLink></div>
        <div class="news-stack home-plan-list">
          <div v-for="item in now?.outlook?.slice(0, 4)" :key="`outlook-${item}`" class="news-line"><span class="source-dot" /><div><small>接下来</small><strong>{{ item }}</strong></div><Compass :size="17" /></div>
          <div v-if="!now?.outlook?.length" class="empty-state">接下来的计划还没记下来，之后补上。</div>
        </div>
      </div>
    </section>

    <section class="section-pad">
      <div class="content-wrap">
        <div class="section-heading">
          <div><p class="eyebrow">Writing</p><h2>最近文章</h2></div>
          <NuxtLink class="text-link" to="/article/tech">查看全部 <ArrowRight :size="16" /></NuxtLink>
        </div>
        <div class="article-list" v-reveal>
          <ArticleRow v-for="article in recentArticles" :key="article.id" :article="article" />
          <p v-if="!recentArticles.length" class="empty-state">最近没更新文章，先去项目页看看。</p>
        </div>
      </div>
    </section>

    <section class="section-pad alt-band">
      <div class="content-wrap split-section" v-reveal>
        <div class="section-intro">
          <p class="eyebrow">AI Notebook</p>
          <h2>追踪变化，沉淀理解</h2>
          <p>把值得关注的行业动态收在这里，顺带整理成能反复翻看的学习路径。</p>
          <NuxtLink class="primary-button" to="/article/ai">查看 AI 文章 <ArrowRight :size="18" /></NuxtLink>
        </div>
        <div class="news-stack">
          <a v-for="item in news?.slice(0, 3)" :key="item.id" :href="item.sourceUrl" target="_blank" rel="noreferrer" class="news-line">
            <span class="source-dot" />
            <div><small>{{ item.sourceName }}</small><strong>{{ item.title }}</strong></div>
            <ArrowRight :size="17" />
          </a>
          <div v-if="!news?.length" class="empty-state"><FileText :size="20" /> 动态还在整理，晚点再来看看。</div>
        </div>
      </div>
    </section>
  </div>
</template>
