<script setup lang="ts">
import {
  BarChart3, Check, Clock3, Columns3, Copy, Download, Eye, FilePlus2, FolderGit2, Gamepad2, History, KeyRound, Library, LogOut,
  ImagePlus, MessageSquareText, Newspaper, Pencil, Radio, RefreshCw, RotateCcw, Route, Save,
  ShieldCheck, Trash2, Upload, UserRound, X, BrainCircuit
} from '@lucide/vue'
import type { AiNewsItem, Article, ArticleVersion, GameAccount, GameEntry, LearningResource, Message, MetricsSummary, NowProfile, Project, Resume } from '~/types'

definePageMeta({ layout: false })
useSeoMeta({ title: '内容管理', robots: 'noindex, nofollow' })
useHead({ link: [{ rel: 'manifest', href: '/manifest.webmanifest' }] })

type Section = 'blog' | 'talk' | 'ai-articles' | 'games' | 'projects' | 'learning' | 'news' | 'accounts' | 'resume' | 'now' | 'metrics' | 'messages'

const api = useSiteApi()
const nuxtApp = useNuxtApp()
const canInstallAdmin = computed(() => Boolean((nuxtApp.$canInstallAdmin as Ref<boolean> | undefined)?.value))
const username = ref('admin')
const password = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmNewPassword = ref('')
const passwordPanelOpen = ref(false)
const authenticated = ref(false)
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const activeSection = ref<Section>('blog')
const editing = ref<Record<string, any>>({})
const articles = ref<(Article & { published: boolean })[]>([])
const projects = ref<Project[]>([])
const games = ref<GameEntry[]>([])
const resources = ref<LearningResource[]>([])
const news = ref<AiNewsItem[]>([])
const accounts = ref<GameAccount[]>([])
const resume = ref<Resume | null>(null)
const nowProfile = ref<NowProfile | null>(null)
const metrics = ref<MetricsSummary>({ viewsToday: 0, viewsThirtyDays: 0, visitorsToday: 0, visitorsThirtyDays: 0, averageRenderMs: 0, topPages: [], dailyViews: [] })
const messages = ref<Message[]>([])
const markdownInput = ref<HTMLInputElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const steamAppId = ref('')
const steamProfileUrl = ref('')
const steamApiKey = ref('')
const steamSettingsSaved = ref(false)
const editorMode = ref<'edit' | 'split' | 'preview'>('split')
const articleVersions = ref<ArticleVersion[]>([])
const currentDraftKey = ref('')
const draftAvailable = ref(false)
const draftSavedAt = ref('')
const draftReady = ref(false)
let draftTimer: ReturnType<typeof setTimeout> | undefined
const markdownPreview = computed(() => useMarkdown(String(editing.value.content || '')))
const { onContentClick } = useCodeCopy()

const sections = [
  { key: 'blog' as const, label: '博客', icon: Newspaper },
  { key: 'talk' as const, label: '游戏杂谈', icon: MessageSquareText },
  { key: 'ai-articles' as const, label: '人工智能', icon: BrainCircuit },
  { key: 'games' as const, label: '游戏档案', icon: Library },
  { key: 'projects' as const, label: '项目', icon: FolderGit2 },
  { key: 'learning' as const, label: 'AI 学习', icon: Route },
  { key: 'news' as const, label: 'AI 热点', icon: Radio },
  { key: 'accounts' as const, label: '个人账号', icon: Gamepad2 },
  { key: 'now' as const, label: 'Now', icon: Clock3 },
  { key: 'metrics' as const, label: '访问统计', icon: BarChart3 },
  { key: 'messages' as const, label: '留言', icon: MessageSquareText }
]

const sectionTitle = computed(() => sections.find(item => item.key === activeSection.value)?.label || '')
const blogArticles = computed(() => articles.value.filter(item => item.category !== '杂谈' && item.category !== 'AI 学习'))
const talkArticles = computed(() => articles.value.filter(item => item.category === '杂谈'))
const aiArticles = computed(() => articles.value.filter(item => item.category === 'AI 学习'))
const currentItems = computed<any[]>(() => {
  if (activeSection.value === 'blog') return blogArticles.value
  if (activeSection.value === 'talk') return talkArticles.value
  if (activeSection.value === 'ai-articles') return aiArticles.value
  if (activeSection.value === 'games') return games.value
  if (activeSection.value === 'projects') return projects.value
  if (activeSection.value === 'learning') return resources.value
  if (activeSection.value === 'news') return news.value
  if (activeSection.value === 'accounts') return accounts.value
  if (activeSection.value === 'messages') return messages.value
  return []
})
const hasEditor = computed(() => activeSection.value === 'resume' || activeSection.value === 'now' || activeSection.value === 'metrics' || activeSection.value === 'messages' || Object.keys(editing.value).length > 0)
const maxDailyViews = computed(() => Math.max(1, ...metrics.value?.dailyViews.map(item => item.views) || [1]))
const maxTopViews = computed(() => Math.max(1, ...metrics.value?.topPages.map(item => item.views) || [1]))
const talkCategories = ['游戏锐评', '游戏推荐', '随笔']

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const failureMessage = (error: any, fallback: string) => {
  const status = error?.statusCode || error?.response?.status
  if (status === 401) return '登录状态已失效，请重新登录后再保存。'
  if (status === 403) return '安全校验失败，请刷新页面后重试。'
  if (status === 409) return '固定链接已经存在，请修改固定链接后重试。'
  if (status === 400) {
    const details = error?.data?.errors || error?.data?.detail
    if (Array.isArray(details)) return `字段校验失败：${details.slice(0, 4).join('；')}。`
    if (details) return `保存失败：${details}。`
  }
  return `${fallback}${status ? `（HTTP ${status}）` : ''}`
}
const toLocalDateTime = (value: string) => {
  const date = new Date(value)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

const csrfHeader = async () => {
  await api.request('/public/csrf')
  const cookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))
  if (!cookie) throw new Error('CSRF cookie was not returned')
  return { 'X-XSRF-TOKEN': decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) }
}

const loadAll = async () => {
  const result = await Promise.all([
    api.request<(Article & { published: boolean })[]>('/admin/articles').catch(() => null),
    api.request<Project[]>('/admin/projects').catch(() => null),
    api.request<GameEntry[]>('/admin/games').catch(() => null),
    api.request<LearningResource[]>('/admin/learning-resources').catch(() => null),
    api.request<AiNewsItem[]>('/admin/ai-news').catch(() => null),
    api.request<GameAccount[]>('/admin/game-accounts').catch(() => null),
    api.request<Resume>('/admin/resume').catch(() => null),
    api.request<NowProfile>('/admin/now').catch(() => null),
    api.request<MetricsSummary>('/admin/metrics').catch(() => null),
    api.request<Message[]>('/admin/messages').catch(() => null),
    api.request<{ profileUrl: string, hasApiKey: boolean }>('/admin/games/steam/settings').catch(() => null)
  ])
  if (result[0]) articles.value = result[0]
  if (result[1]) projects.value = result[1]
  if (result[2]) games.value = result[2]
  if (result[3]) resources.value = result[3]
  if (result[4]) news.value = result[4]
  if (result[5]) accounts.value = result[5]
  if (result[6]) resume.value = result[6]
  if (result[7]) nowProfile.value = result[7]
  if (result[8]) metrics.value = result[8]
  if (result[9]) messages.value = result[9]
  if (result[10]) {
    steamProfileUrl.value = result[10].profileUrl
    steamSettingsSaved.value = result[10].hasApiKey
  }
}

const checkSession = async () => {
  try {
    await api.request('/admin/auth/me')
    authenticated.value = true
    await loadAll()
  } catch {
    authenticated.value = false
  } finally {
    loading.value = false
  }
}

const login = async () => {
  message.value = ''
  try {
    const session = await api.request<{ username: string, token: string }>('/admin/auth/login', {
      method: 'POST', headers: await csrfHeader(), body: { username: username.value, password: password.value }
    })
    if (session.token) sessionStorage.setItem('personal-site-admin-token', session.token)
    password.value = ''
    authenticated.value = true
    await loadAll()
  } catch {
    message.value = '登录失败，请检查用户名和密码。'
  }
}

const logout = async () => {
  await api.request('/admin/auth/logout', { method: 'POST', headers: await csrfHeader() })
  sessionStorage.removeItem('personal-site-admin-token')
  authenticated.value = false
  editing.value = {}
}

const installAdmin = async () => {
  const install = nuxtApp.$installAdmin as (() => Promise<boolean>) | undefined
  message.value = install && await install()
    ? '安装入口已打开，请按浏览器提示完成安装。'
    : '当前浏览器没有提供安装入口，请使用支持 PWA 的浏览器。'
}

const changePassword = async () => {
  message.value = ''
  if (newPassword.value !== confirmNewPassword.value) { message.value = '两次输入的新密码不一致。'; return }
  try {
    await api.request('/admin/auth/password', { method: 'PUT', headers: await csrfHeader(), body: { currentPassword: currentPassword.value, newPassword: newPassword.value } })
    passwordPanelOpen.value = false
    currentPassword.value = ''; newPassword.value = ''; confirmNewPassword.value = ''
    authenticated.value = false
    message.value = '密码已修改，请使用新密码重新登录。'
  } catch { message.value = '密码修改失败，请确认当前密码正确且新密码不少于 14 位。' }
}

const selectSection = (section: Section) => {
  activeSection.value = section
  message.value = ''
  if (section === 'resume' && resume.value) editing.value = clone(resume.value)
  else if (section === 'now' && nowProfile.value) editing.value = clone(nowProfile.value)
  else editing.value = {}
  articleVersions.value = []
  currentDraftKey.value = ''
  draftReady.value = false
}

const prepareDraft = async (key: string) => {
  draftReady.value = false
  currentDraftKey.value = `personal-site:${key}`
  const stored = localStorage.getItem(currentDraftKey.value)
  draftAvailable.value = Boolean(stored)
  draftSavedAt.value = stored ? JSON.parse(stored).savedAt || '' : ''
  await nextTick()
  draftReady.value = true
}

const loadVersions = async (articleId: number) => {
  articleVersions.value = await api.request<ArticleVersion[]>(`/admin/articles/${articleId}/versions`)
}

const editItem = (item: any) => {
  editing.value = clone(item)
  if (activeSection.value === 'news') editing.value.publishedAt = toLocalDateTime(item.publishedAt)
  if (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles') {
    void prepareDraft(`article-${item.id}`)
    void loadVersions(item.id)
  }
  message.value = ''
}

const newItem = () => {
  message.value = ''
  if (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles') {
    editing.value = { title: '', slug: '', excerpt: '', category: activeSection.value === 'talk' ? '杂谈' : activeSection.value === 'ai-articles' ? 'AI 学习' : '工程实践', tags: [], content: '', published: false, readingMinutes: 5 }
  } else if (activeSection.value === 'projects') {
    editing.value = { slug: '', name: '', summary: '', stack: [], background: '', responsibility: '', workflow: [], architecture: '', highlights: [], retrospective: '', repositoryUrl: '', liveUrl: '', featured: false, sortOrder: projects.value.length + 1 }
  } else if (activeSection.value === 'games') {
    editing.value = { slug: '', title: '', coverUrl: '', platform: '', status: 'LIBRARY', score: null, hoursPlayed: 0, completedOn: null, verdict: '', reviewArticleSlug: '', sortOrder: games.value.length + 1 }
  } else if (activeSection.value === 'learning') {
    editing.value = { slug: '', title: '', description: '', level: 'BEGINNER', topic: '', url: '', content: '', sortOrder: resources.value.length + 1 }
  } else if (activeSection.value === 'news') {
    editing.value = { title: '', summary: '', sourceName: '', sourceUrl: '', publishedAt: toLocalDateTime(new Date().toISOString()), topics: [] }
  } else if (activeSection.value === 'accounts') {
    editing.value = { platform: '', handle: '', url: '', description: '', sortOrder: accounts.value.length + 1 }
  }
  if (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles') {
    articleVersions.value = []
    void prepareDraft(`${activeSection.value}-new`)
  }
}

const setTalkCategory = (category: string) => {
  if (!editing.value.tags) editing.value.tags = []
  editing.value.tags = [...editing.value.tags.filter((tag: string) => !talkCategories.includes(tag)), category]
}

watch(editing, () => {
  if (!draftReady.value || !currentDraftKey.value || (activeSection.value !== 'blog' && activeSection.value !== 'talk' && activeSection.value !== 'ai-articles')) return
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    const savedAt = new Date().toISOString()
    localStorage.setItem(currentDraftKey.value, JSON.stringify({ savedAt, data: clone(editing.value) }))
    draftAvailable.value = true
    draftSavedAt.value = savedAt
  }, 800)
}, { deep: true })

const restoreDraft = () => {
  const stored = currentDraftKey.value && localStorage.getItem(currentDraftKey.value)
  if (!stored) return
  draftReady.value = false
  editing.value = JSON.parse(stored).data
  nextTick(() => { draftReady.value = true })
  message.value = '本地草稿已恢复。'
}

const clearDraft = () => {
  if (currentDraftKey.value) localStorage.removeItem(currentDraftKey.value)
  draftAvailable.value = false
  draftSavedAt.value = ''
}

const restoreVersion = async (version: ArticleVersion) => {
  if (!editing.value.id || !confirm(`恢复到 ${new Date(version.createdAt).toLocaleString('zh-CN')} 的版本吗？`)) return
  const saved = await api.request<Article>(`/admin/articles/${editing.value.id}/versions/${version.id}/restore`, {
    method: 'POST', headers: await csrfHeader()
  })
  draftReady.value = false
  editing.value = clone(saved)
  clearDraft()
  await loadAll()
  await loadVersions(saved.id)
  await nextTick()
  draftReady.value = true
  message.value = '历史版本已恢复。'
}

const endpoint = () => {
  const id = editing.value.id ? `/${editing.value.id}` : ''
  if (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles') return `/admin/articles${id}`
  if (activeSection.value === 'projects') return `/admin/projects${id}`
  if (activeSection.value === 'games') return `/admin/games${id}`
  if (activeSection.value === 'learning') return `/admin/learning-resources${id}`
  if (activeSection.value === 'news') return `/admin/ai-news${id}`
  if (activeSection.value === 'accounts') return `/admin/game-accounts${id}`
  if (activeSection.value === 'now') return '/admin/now'
  if (activeSection.value === 'messages') return `/admin/messages${id}`
  return '/admin/resume'
}

const saveItem = async () => {
  saving.value = true
  message.value = ''
  try {
    await api.request('/admin/auth/me')
  } catch {
    authenticated.value = false
    editing.value = {}
    message.value = '登录状态已失效，请重新登录后再保存。'
    saving.value = false
    return
  }

  try {
    const payload = clone(editing.value)
    delete payload.id
    if (activeSection.value === 'talk') payload.category = '杂谈'
    if (activeSection.value === 'ai-articles') payload.category = 'AI 学习'
    if (activeSection.value === 'news') payload.publishedAt = new Date(payload.publishedAt).toISOString()
    if (activeSection.value === 'now') { payload.building = payload.building || []; payload.learning = payload.learning || []; payload.playing = payload.playing || []; payload.reading = payload.reading || []; payload.outlook = payload.outlook || [] }
    const isProfile = activeSection.value === 'resume' || activeSection.value === 'now'
    const method = isProfile || editing.value.id ? 'PUT' : 'POST'
    const saved = await api.request<any>(endpoint(), { method, headers: await csrfHeader(), body: payload })
    await loadAll()
    editing.value = clone(saved)
    if (activeSection.value === 'news') editing.value.publishedAt = toLocalDateTime(saved.publishedAt)
    if (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles') {
      clearDraft()
      await loadVersions(saved.id)
    }
    message.value = editing.value.published === false && (activeSection.value === 'blog' || activeSection.value === 'talk' || activeSection.value === 'ai-articles')
      ? '已保存为草稿，尚未同步到主站。'
      : '内容已保存并同步到主站。'
  } catch (error) {
    const status = (error as any)?.statusCode || (error as any)?.response?.status
    if (status === 403) {
      try {
        const payload = clone(editing.value)
        delete payload.id
        if (activeSection.value === 'talk') payload.category = '杂谈'
        if (activeSection.value === 'ai-articles') payload.category = 'AI 学习'
        if (activeSection.value === 'news') payload.publishedAt = new Date(payload.publishedAt).toISOString()
        const isProfile = activeSection.value === 'resume' || activeSection.value === 'now'
        const method = isProfile || editing.value.id ? 'PUT' : 'POST'
        const saved = await api.request<any>(endpoint(), { method, headers: await csrfHeader(), body: payload })
        await loadAll()
        editing.value = clone(saved)
        if (activeSection.value === 'news') editing.value.publishedAt = toLocalDateTime(saved.publishedAt)
        message.value = '安全校验已自动重试，内容已保存并同步到主站。'
      } catch (retryError) {
        message.value = failureMessage(retryError, '保存失败，请检查必填字段、链接或固定链接是否重复')
      }
    } else if (status === 401) {
      authenticated.value = false
      message.value = '登录状态已失效，请重新登录后再保存。'
    } else {
      message.value = failureMessage(error, '保存失败，请检查必填字段、链接或固定链接是否重复')
    }
  } finally {
    saving.value = false
  }
}

const removeItem = async () => {
  if (!editing.value.id || !confirm('确定删除这条内容吗？此操作不可撤销。')) return
  try {
    await api.request(endpoint(), { method: 'DELETE', headers: await csrfHeader() })
    editing.value = {}
    await loadAll()
    message.value = '内容已删除。'
  } catch {
    message.value = '删除失败，请稍后重试。'
  }
}

const removeMessage = async (id: number) => {
  if (!confirm('确定删除这条留言吗？')) return
  try {
    await api.request(`/admin/messages/${id}`, { method: 'DELETE', headers: await csrfHeader() })
    messages.value = messages.value.filter(item => item.id !== id)
    message.value = '留言已删除。'
  } catch {
    message.value = '留言删除失败。'
  }
}

const copyArticle = async () => {
  if (!editing.value.id || (activeSection.value !== 'blog' && activeSection.value !== 'talk' && activeSection.value !== 'ai-articles')) return
  try {
    const copied = await api.request<any>(`/admin/articles/${editing.value.id}/copy`, { method: 'POST', headers: await csrfHeader() })
    await loadAll()
    editing.value = clone(copied)
    await loadVersions(copied.id)
    message.value = '文章副本已创建。'
  } catch { message.value = '文章复制失败。' }
}

const itemTitle = (item: any) => item.title || item.name || item.platform || '未命名'
const itemMeta = (item: any) => {
  if ('published' in item) return `${item.category || ''}${item.published ? '' : ' · 草稿'}`
  return item.category || item.topic || item.sourceName || item.handle || item.platform || ''
}
const addExperience = () => editing.value.experiences.push({ organization: '', role: '', period: '', description: '' })
const addEducation = () => editing.value.education.push({ institution: '', major: '', period: '' })
const uploadMarkdown = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 200_000) {
    message.value = 'Markdown 文件超过 200 KB，请压缩内容后重试。'
    input.value = ''
    return
  }
  editing.value.content = await file.text()
  message.value = `已读取 ${file.name}，点击保存后才会写入站点。`
  input.value = ''
}

const uploadImage = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const body = new FormData(); body.append('file', file)
    const result = await api.request<{ url: string }>('/admin/uploads/images', { method: 'POST', headers: await csrfHeader(), body })
    editing.value.content = `${editing.value.content || ''}\n\n![${file.name.replace(/\.[^.]+$/, '')}](${result.url})\n`
    message.value = '图片已压缩上传并插入正文。'
  } catch { message.value = '图片上传失败，仅支持 5MB 以内的有效图片。' }
  input.value = ''
}

const importSteamGame = async () => {
  const appId = steamAppId.value.trim()
  if (!/^\d+$/.test(appId)) { message.value = '请输入有效的 Steam App ID。'; return }
  try {
    const draft = await api.request<any>(`/admin/games/steam/${appId}`)
    editing.value = { ...draft, sortOrder: games.value.length + 1 }
    message.value = 'Steam 信息已读取，请补充个人状态、评分和游玩记录后保存。'
  } catch { message.value = 'Steam 信息读取失败，请检查 App ID 或稍后重试。' }
}

const syncSteamLibrary = async () => {
  if (!/^https:\/\/steamcommunity\.com\/(?:id|profiles)\//.test(steamProfileUrl.value.trim())) {
    message.value = '请输入完整的 Steam Community 个人主页地址。'; return
  }
  try {
    const result = await api.request<{ count: number }>('/admin/games/steam/sync', {
      method: 'POST',
      headers: await csrfHeader(),
      body: { profileUrl: steamProfileUrl.value.trim(), apiKey: steamApiKey.value.trim() }
    })
    await loadAll()
    message.value = `Steam 游戏库同步完成，共读取 ${result.count} 款游戏。`
  } catch (error) {
    const status = (error as any)?.statusCode || (error as any)?.response?.status
    if (status === 502) {
      message.value = '服务器无法访问 Steam Community，请填写 Steam Web API Key 后重试。'
    } else {
      message.value = failureMessage(error, 'Steam 游戏库同步失败')
    }
  }
}

const saveSteamSettings = async () => {
  if (!/^https:\/\/steamcommunity\.com\/(?:id|profiles)\//.test(steamProfileUrl.value.trim())) {
    message.value = '请输入完整的 Steam Community 个人主页地址。'; return
  }
  try {
    await api.request('/admin/games/steam/settings', {
      method: 'PUT',
      headers: await csrfHeader(),
      body: { profileUrl: steamProfileUrl.value.trim(), apiKey: steamApiKey.value.trim() }
    })
    steamApiKey.value = ''
    steamSettingsSaved.value = true
    message.value = 'Steam 同步配置已保存。'
  } catch {
    message.value = 'Steam 同步配置保存失败。'
  }
}

onMounted(checkSession)
</script>

<template>
  <div class="admin-shell">
    <div v-if="loading" class="admin-loading"><RefreshCw class="spin" :size="24" /> 正在验证会话</div>
    <main v-else-if="!authenticated" class="login-panel">
        <NuxtLink class="brand" to="/"><span class="brand-mark"><img src="/logo.png?v=3" alt="折叠思维" /></span><span>折叠思维</span></NuxtLink>
      <div class="login-copy"><ShieldCheck :size="30" /><h1>内容管理</h1><p>该入口仅供站点管理员使用。</p></div>
      <form @submit.prevent="login">
        <label>用户名<input v-model="username" autocomplete="username" required></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" required></label>
        <button class="primary-button full-button" type="submit">安全登录</button>
        <p v-if="message" class="form-message error-message">{{ message }}</p>
      </form>
      <NuxtLink class="login-back" to="/">返回主站</NuxtLink>
    </main>

    <template v-else>
      <aside class="admin-sidebar">
        <NuxtLink class="brand" to="/"><span class="brand-mark"><img src="/logo.png?v=3" alt="折叠思维" /></span><span>内容管理</span></NuxtLink>
        <p class="sidebar-meta">折叠思维 · 管理台</p>
        <nav class="admin-section-nav" aria-label="内容分区">
          <button v-for="section in sections" :key="section.key" :class="{ active: activeSection === section.key }" type="button" @click="selectSection(section.key)">
            <component :is="section.icon" :size="17" /><span>{{ section.label }}</span>
          </button>
        </nav>
        <button v-if="!['resume', 'now', 'metrics', 'messages'].includes(activeSection)" class="primary-button full-button" type="button" @click="newItem"><FilePlus2 :size="17" /> 新建{{ sectionTitle }}</button>
        <button class="logout-button" type="button" @click="logout"><LogOut :size="17" /> 退出登录</button>
        <button class="logout-button" type="button" @click="passwordPanelOpen = !passwordPanelOpen"><KeyRound :size="17" /> 修改密码</button>
        <button v-if="canInstallAdmin" class="logout-button" type="button" @click="installAdmin"><Download :size="17" /> 安装管理台</button>
      </aside>

      <main class="editor-main">
        <div class="admin-workspace-layout" :class="{ single: ['resume', 'now', 'metrics', 'messages'].includes(activeSection) }">
          <div class="admin-primary">
        <p v-if="message && !hasEditor" class="form-message admin-global-message" :class="{ 'error-message': message.includes('失败') }">{{ message }}</p>
        <section v-if="activeSection === 'metrics'" class="metrics-dashboard">
          <header class="metrics-header"><div><p class="eyebrow">访问统计</p><h1>站点运行概览</h1></div><button class="secondary-button" type="button" @click="loadAll"><RefreshCw :size="16" /> 刷新</button></header>
          <div class="metrics-summary">
            <article><span>今日访问</span><strong>{{ metrics.viewsToday }}</strong></article>
            <article><span>今日独立访客</span><strong>{{ metrics.visitorsToday }}</strong></article>
            <article><span>30 日访问</span><strong>{{ metrics.viewsThirtyDays }}</strong></article>
            <article><span>30 日独立访客</span><strong>{{ metrics.visitorsThirtyDays }}</strong></article>
            <article><span>平均渲染耗时</span><strong>{{ Math.round(metrics.averageRenderMs) }} ms</strong></article>
          </div>
          <section class="metrics-panel">
            <header><h2>30 日趋势</h2><span>按自然日统计</span><div class="metrics-legend"><span><i class="legend-view"></i>访问 PV</span><span><i class="legend-uv"></i>访客 UV</span></div></header>
            <div v-if="metrics.dailyViews.length" class="metrics-chart">
              <div v-for="item in metrics.dailyViews" :key="item.date" class="metrics-bar-column" :title="`${item.date}：访问 ${item.views} · 访客 ${item.visitors}`">
                <strong>{{ item.views }}</strong><div class="metrics-bar-track"><span class="metrics-bar-view" :style="{ height: `${Math.max(3, item.views / maxDailyViews * 100)}%` }" /><span class="metrics-bar-uv" :style="{ height: `${Math.max(3, item.visitors / maxDailyViews * 100)}%` }" /></div><time>{{ item.date.slice(5) }}</time>
              </div>
            </div>
            <p v-else class="metrics-empty">尚无访问数据</p>
          </section>
          <section class="metrics-panel">
            <header><h2>热门页面</h2><span>最近 30 日</span></header>
            <div class="metrics-table-wrap"><table class="metrics-table"><thead><tr><th>路径</th><th>访问</th><th>平均渲染</th><th>最慢渲染</th></tr></thead><tbody><tr v-for="page in metrics.topPages" :key="page.path"><td>{{ page.path }}</td><td><div class="top-page-cell"><span class="top-page-bar" :style="{ width: `${page.views / maxTopViews * 100}%` }" /><b>{{ page.views }}</b></div></td><td>{{ Math.round(page.averageRenderMs) }} ms</td><td>{{ Math.round(page.maxRenderMs) }} ms</td></tr></tbody></table></div>
            <p v-if="!metrics.topPages.length" class="metrics-empty">尚无页面数据</p>
          </section>
          <p class="metrics-privacy"><ShieldCheck :size="16" /> 仅统计页面路径与匿名性能数据；独立访客使用 IP+UA 的不可逆哈希，不保存任何可识别身份的信息。</p>
        </section>
        <section v-else-if="activeSection === 'messages'" class="metrics-dashboard">
          <header class="metrics-header"><div><p class="eyebrow">留言</p><h1>访客留言</h1></div><button class="secondary-button" type="button" @click="loadAll"><RefreshCw :size="16" /> 刷新</button></header>
          <div v-if="messages.length" class="admin-message-list">
            <article v-for="item in messages" :key="item.id">
              <div class="message-card-head"><strong>{{ item.name }}</strong><small v-if="item.email">{{ item.email }}</small><time>{{ new Date(item.createdAt).toLocaleString('zh-CN') }}</time></div>
              <p>{{ item.content }}</p>
              <button class="secondary-button" type="button" @click="removeMessage(item.id)"><Trash2 :size="16" /> 删除</button>
            </article>
          </div>
          <p v-else class="metrics-empty">暂无留言</p>
        </section>
        <form v-else-if="hasEditor" class="editor-form" @submit.prevent="saveItem">
          <header>
            <div><p class="eyebrow">{{ sectionTitle }}</p><div class="editor-title-row"><h1>{{ editing.id ? '编辑内容' : activeSection === 'resume' ? '编辑简历' : activeSection === 'now' ? '编辑 Now' : '新建内容' }}</h1><span v-if="activeSection === 'blog' || activeSection === 'talk' || activeSection === 'ai-articles'" class="publish-state" :class="{ draft: !editing.published }">{{ editing.published ? '已公开' : '草稿' }}</span></div></div>
            <div class="editor-actions"><button v-if="(activeSection === 'blog' || activeSection === 'talk' || activeSection === 'ai-articles') && editing.id" class="icon-button" type="button" title="复制文章" @click="copyArticle"><Copy :size="18" /></button><button v-if="editing.id" class="icon-button danger" type="button" title="删除" @click="removeItem"><Trash2 :size="18" /></button><button class="primary-button" type="submit" :disabled="saving"><Save :size="17" /> {{ saving ? '保存中' : '保存' }}</button></div>
          </header>
          <p v-if="message" class="form-message" :class="{ 'error-message': message.includes('失败') }">{{ message }}</p>

          <div v-if="activeSection === 'blog' || activeSection === 'talk' || activeSection === 'ai-articles'" class="editor-fields">
            <label class="wide-field">标题<input v-model="editing.title" required maxlength="160"></label>
            <label>固定链接<input v-model="editing.slug" required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" placeholder="例如 my-first-article" title="只能使用小写字母、数字和连字符"></label>
            <label v-if="activeSection === 'blog'">分类<input v-model="editing.category" list="category-options" required><datalist id="category-options"><option value="工程实践" /><option value="项目复盘" /></datalist></label>
            <label v-else-if="activeSection === 'ai-articles'">分类<input value="AI 学习" disabled></label>
            <label v-else>分类<input value="杂谈" disabled></label>
            <label class="wide-field">摘要<textarea v-model="editing.excerpt" rows="3" required maxlength="320" /></label>
            <fieldset v-if="activeSection === 'talk'" class="wide-field status-choice talk-category-choice"><legend>游戏杂谈分类</legend><label v-for="option in talkCategories" :key="option"><input type="radio" :checked="editing.tags?.includes(option)" @change="setTalkCategory(option)">{{ option }}</label></fieldset>
            <label>标签（逗号分隔）<input :value="editing.tags?.join(', ')" @input="editing.tags = ($event.target as HTMLInputElement).value.split(',').map(v => v.trim()).filter(Boolean)"></label>
            <label>预计阅读时间<input v-model.number="editing.readingMinutes" type="number" min="1" max="120"></label>
            <label class="publish-field"><input v-model="editing.published" type="checkbox"><span><strong>同步到主站</strong><small>关闭后只保存为后台草稿，不会出现在公开页面。</small></span></label>
            <section class="wide-field markdown-upload-field">
              <div class="field-action-row">
                <label for="markdown-content">Markdown 正文</label>
                <div class="markdown-actions">
                  <div class="editor-mode-tabs" aria-label="Markdown 视图">
                    <button type="button" :class="{ active: editorMode === 'edit' }" title="编辑" @click="editorMode = 'edit'"><Pencil :size="16" /></button>
                    <button type="button" :class="{ active: editorMode === 'split' }" title="双栏" @click="editorMode = 'split'"><Columns3 :size="16" /></button>
                    <button type="button" :class="{ active: editorMode === 'preview' }" title="预览" @click="editorMode = 'preview'"><Eye :size="16" /></button>
                  </div>
                  <button class="secondary-button" type="button" @click="imageInput?.click()"><ImagePlus :size="16" /> 插入图片</button>
                  <button class="secondary-button" type="button" @click="markdownInput?.click()"><Upload :size="16" /> 上传 Markdown</button>
                </div>
              </div>
              <input ref="markdownInput" class="visually-hidden" type="file" accept=".md,.markdown,text/markdown,text/plain" @change="uploadMarkdown">
              <input ref="imageInput" class="visually-hidden" type="file" accept="image/jpeg,image/png" @change="uploadImage">
              <div class="markdown-workbench" :class="`mode-${editorMode}`">
                <textarea v-show="editorMode !== 'preview'" id="markdown-content" v-model="editing.content" class="content-editor" required />
                <div v-show="editorMode !== 'edit'" class="markdown-body markdown-preview" v-html="markdownPreview" @click="onContentClick" />
              </div>
              <div v-if="draftAvailable" class="draft-status">
                <span><Check :size="15" /> 本地草稿 {{ draftSavedAt ? new Date(draftSavedAt).toLocaleTimeString('zh-CN') : '' }}</span>
                <div><button type="button" @click="restoreDraft"><RotateCcw :size="15" /> 恢复</button><button type="button" title="清除本地草稿" @click="clearDraft"><X :size="15" /></button></div>
              </div>
              <section v-if="editing.id" class="version-history">
                <header><div><History :size="18" /><strong>历史版本</strong></div><span>{{ articleVersions.length }} 个版本</span></header>
                <div class="version-list">
                  <article v-for="version in articleVersions" :key="version.id">
                    <div><strong>{{ version.title }}</strong><small>{{ new Date(version.createdAt).toLocaleString('zh-CN') }} · {{ version.published ? '已发布' : '草稿' }}</small></div>
                    <button class="icon-button" type="button" title="恢复此版本" @click="restoreVersion(version)"><RotateCcw :size="16" /></button>
                  </article>
                </div>
              </section>
            </section>
          </div>

          <div v-else-if="activeSection === 'projects'" class="editor-fields">
            <label class="wide-field">项目名称<input v-model="editing.name" required maxlength="160"></label>
            <label class="wide-field">固定链接<input v-model="editing.slug" required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" maxlength="180"></label>
            <label class="wide-field">项目摘要<textarea v-model="editing.summary" rows="5" required /></label>
            <label class="wide-field">技术栈（逗号分隔）<input :value="editing.stack?.join(', ')" @input="editing.stack = ($event.target as HTMLInputElement).value.split(',').map(v => v.trim()).filter(Boolean)"></label>
            <label class="wide-field">项目背景<textarea v-model="editing.background" rows="5" required /></label>
            <label class="wide-field">我的职责<textarea v-model="editing.responsibility" rows="5" required /></label>
            <label class="wide-field">业务流程（每行一步）<textarea :value="editing.workflow?.join('\n')" rows="7" required @input="editing.workflow = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label class="wide-field">技术架构<textarea v-model="editing.architecture" rows="5" required /></label>
            <label class="wide-field">工程亮点（每行一项）<textarea :value="editing.highlights?.join('\n')" rows="7" required @input="editing.highlights = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label class="wide-field">项目复盘<textarea v-model="editing.retrospective" rows="5" required /></label>
            <label>代码仓库 URL<input v-model="editing.repositoryUrl" type="url"></label>
            <label>在线演示 URL<input v-model="editing.liveUrl" type="url"></label>
            <label>排序<input v-model.number="editing.sortOrder" type="number" min="0" max="10000"></label>
            <label class="check-field"><input v-model="editing.featured" type="checkbox"> 重点项目</label>
          </div>

          <div v-else-if="activeSection === 'games'" class="editor-fields">
            <div class="wide-field steam-import"><label>Steam 个人主页<input v-model="steamProfileUrl" type="url" placeholder="https://steamcommunity.com/id/你的账号"></label><button class="secondary-button" type="button" @click="saveSteamSettings">保存配置</button><button class="secondary-button" type="button" @click="syncSteamLibrary">同步游戏库</button></div>
            <label class="wide-field">Steam Web API Key<input v-model="steamApiKey" type="password" autocomplete="off" :placeholder="steamSettingsSaved ? '已保存，留空则继续使用原 Key' : '输入一次即可保存并定时同步'"></label>
            <div class="wide-field steam-import"><label>Steam App ID<input v-model="steamAppId" inputmode="numeric" placeholder="例如 2358720"></label><button class="secondary-button" type="button" @click="importSteamGame">读取游戏信息</button></div>
            <label class="wide-field">游戏名称<input v-model="editing.title" required maxlength="200"></label>
            <label>固定链接<input v-model="editing.slug" required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" maxlength="180"></label>
            <label>平台<input v-model="editing.platform" required maxlength="100"></label>
            <fieldset class="wide-field status-choice"><legend>游戏状态</legend><label><input v-model="editing.status" type="radio" value="PLAYING">游玩中</label><label><input v-model="editing.status" type="radio" value="LIBRARY">游戏库</label><label><input v-model="editing.status" type="radio" value="COMPLETED">已通关</label><label><input v-model="editing.status" type="radio" value="WISHLIST">愿望单</label><label><input v-model="editing.status" type="radio" value="DROPPED">已弃坑</label></fieldset>
            <label>评分（0-10）<input v-model.number="editing.score" type="number" min="0" max="10" step="0.1"></label>
            <label>游玩时长<input v-model.number="editing.hoursPlayed" type="number" min="0" max="100000"></label>
            <label>完成日期<input v-model="editing.completedOn" type="date"></label>
            <label class="wide-field">封面 URL<input v-model="editing.coverUrl" type="url" maxlength="1000"></label>
            <label class="wide-field">一句话锐评<textarea v-model="editing.verdict" rows="4" required maxlength="1000" /></label>
            <label>关联文章固定链接<input v-model="editing.reviewArticleSlug" pattern="(?:[a-z0-9]+(?:-[a-z0-9]+)*)?" maxlength="180"></label>
            <label>排序<input v-model.number="editing.sortOrder" type="number" min="0" max="10000"></label>
          </div>

          <div v-else-if="activeSection === 'learning'" class="editor-fields">
            <label class="wide-field">标题<input v-model="editing.title" required maxlength="200"></label>
            <label>固定链接<input v-model="editing.slug" required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" maxlength="180"></label>
            <label>难度<select v-model="editing.level" required><option value="BEGINNER">入门</option><option value="INTERMEDIATE">进阶</option><option value="ADVANCED">深入</option></select></label>
            <label>主题<input v-model="editing.topic" required maxlength="100"></label>
            <label class="wide-field">说明<textarea v-model="editing.description" rows="5" required /></label>
            <label>资料 URL<input v-model="editing.url" type="url"></label>
            <label>排序<input v-model.number="editing.sortOrder" type="number" min="0" max="10000"></label>
            <label class="wide-field">教程正文（Markdown，展示在独立详情页）<textarea v-model="editing.content" rows="16" /></label>
          </div>


          <div v-else-if="activeSection === 'news'" class="editor-fields">
            <label class="wide-field">标题<input v-model="editing.title" required maxlength="300"></label>
            <label>来源名称<input v-model="editing.sourceName" required maxlength="100"></label>
            <label>发布时间<input v-model="editing.publishedAt" type="datetime-local" required></label>
            <label class="wide-field">原文 URL<input v-model="editing.sourceUrl" type="url" required></label>
            <label class="wide-field">摘要<textarea v-model="editing.summary" rows="5" required /></label>
            <label class="wide-field">主题（逗号分隔）<input :value="editing.topics?.join(', ')" @input="editing.topics = ($event.target as HTMLInputElement).value.split(',').map(v => v.trim()).filter(Boolean)"></label>
          </div>

          <div v-else-if="activeSection === 'accounts'" class="editor-fields">
            <label>平台<input v-model="editing.platform" required maxlength="80"></label>
            <label>账号昵称<input v-model="editing.handle" required maxlength="120"></label>
            <label class="wide-field">个人主页 URL<input v-model="editing.url" type="url" required></label>
            <label class="wide-field">介绍<textarea v-model="editing.description" rows="4" required maxlength="500" /></label>
            <label>排序<input v-model.number="editing.sortOrder" type="number" min="0" max="10000"></label>
          </div>

          <div v-else-if="activeSection === 'resume'" class="editor-fields resume-editor-fields">
            <label>姓名<input v-model="editing.displayName" required maxlength="120"></label>
            <label>职业标题<input v-model="editing.headline" required maxlength="160"></label>
            <label>所在地<input v-model="editing.location" required maxlength="120"></label>
            <label>邮箱<input v-model="editing.email" type="email" required maxlength="200"></label>
            <label class="wide-field">个人简介<textarea v-model="editing.summary" rows="5" required /></label>
            <label class="wide-field">技能（逗号分隔）<input :value="editing.skills?.join(', ')" @input="editing.skills = ($event.target as HTMLInputElement).value.split(',').map(v => v.trim()).filter(Boolean)"></label>

            <section class="wide-field repeat-editor"><div class="repeat-heading"><h2>经历</h2><button class="secondary-button" type="button" @click="addExperience"><FilePlus2 :size="16" /> 添加</button></div><div v-for="(item, index) in editing.experiences" :key="index" class="repeat-row"><label>组织<input v-model="item.organization" required></label><label>角色<input v-model="item.role" required></label><label>时间<input v-model="item.period" required></label><button class="icon-button danger" type="button" title="移除" @click="editing.experiences.splice(index, 1)"><Trash2 :size="17" /></button><label class="repeat-description">说明<textarea v-model="item.description" rows="3" required /></label></div></section>
            <section class="wide-field repeat-editor"><div class="repeat-heading"><h2>教育</h2><button class="secondary-button" type="button" @click="addEducation"><FilePlus2 :size="16" /> 添加</button></div><div v-for="(item, index) in editing.education" :key="index" class="repeat-row education-row"><label>学校<input v-model="item.institution" required></label><label>专业<input v-model="item.major" required></label><label>时间<input v-model="item.period" required></label><button class="icon-button danger" type="button" title="移除" @click="editing.education.splice(index, 1)"><Trash2 :size="17" /></button></div></section>
          </div>

          <div v-else class="editor-fields now-editor-fields">
            <label class="wide-field">当前状态<textarea v-model="editing.headline" rows="3" required maxlength="160" /></label>
            <label>更新日期<input v-model="editing.updatedAt" type="date" required></label>
            <span aria-hidden="true" />
            <label>接下来（展望，每行一项）<textarea :value="editing.outlook?.join('\n')" rows="6" @input="editing.outlook = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label>正在构建（每行一项）<textarea :value="editing.building?.join('\n')" rows="8" @input="editing.building = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label>正在学习（每行一项）<textarea :value="editing.learning?.join('\n')" rows="8" @input="editing.learning = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label>正在游玩（每行一项）<textarea :value="editing.playing?.join('\n')" rows="8" @input="editing.playing = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
            <label>正在阅读（每行一项）<textarea :value="editing.reading?.join('\n')" rows="8" @input="editing.reading = ($event.target as HTMLTextAreaElement).value.split('\n').map(v => v.trim()).filter(Boolean)" /></label>
          </div>
        </form>
          </div>
          <aside v-if="!['resume', 'now', 'metrics', 'messages'].includes(activeSection)" class="admin-content-rail">
            <header><div><p class="eyebrow">{{ sectionTitle }}</p><h2>内容列表</h2></div><span>{{ currentItems.length }}</span></header>
            <nav class="admin-item-list" :aria-label="`${sectionTitle}内容列表`">
              <button v-for="item in currentItems" :key="item.id" :class="{ active: editing.id === item.id }" type="button" @click="editItem(item)">
                <span>{{ itemTitle(item) }}</span><small>{{ itemMeta(item) }}</small>
              </button>
              <div v-if="!currentItems.length" class="admin-list-empty">
                <component :is="sections.find(item => item.key === activeSection)?.icon" :size="22" />
                <span>暂无内容</span>
                <button class="secondary-button" type="button" @click="newItem">新建{{ sectionTitle }}</button>
              </div>
            </nav>
          </aside>
        </div>
      </main>
    </template>
    <div v-if="passwordPanelOpen && authenticated" class="password-modal-backdrop" @click.self="passwordPanelOpen = false">
      <form class="password-modal" @submit.prevent="changePassword">
        <header><div><p class="eyebrow">账户安全</p><h2>修改密码</h2></div><button class="icon-button" type="button" title="关闭" @click="passwordPanelOpen = false"><X :size="18" /></button></header>
        <label>当前密码<input v-model="currentPassword" type="password" autocomplete="current-password" required></label>
        <label>新密码<input v-model="newPassword" type="password" minlength="14" autocomplete="new-password" required></label>
        <label>确认新密码<input v-model="confirmNewPassword" type="password" minlength="14" autocomplete="new-password" required></label>
        <p class="password-note">修改后所有管理员会话都会注销。</p>
        <button class="primary-button full-button" type="submit"><KeyRound :size="17" /> 确认修改</button>
        <p v-if="message" class="form-message error-message">{{ message }}</p>
      </form>
    </div>
  </div>
</template>
