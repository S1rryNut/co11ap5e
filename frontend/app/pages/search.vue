<script setup lang="ts">
import { ArrowUpRight, Search, X } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const api = useSiteApi()
const input = ref(String(route.query.q || ''))
const query = computed(() => String(route.query.q || '').trim())
const activeType = ref('ALL')
const { data: results, status } = await useAsyncData('search-results',
  () => query.value ? api.search(query.value) : Promise.resolve([]), { watch: [query] })

const labels = { ARTICLE: '文章', PROJECT: '项目', LEARNING: 'AI 学习', AI_NEWS: 'AI 热点', GAME: '游戏' }
const typeTabs = [{ key: 'ALL', label: '全部' }, ...Object.entries(labels).map(([key, label]) => ({ key, label }))]
const filteredResults = computed(() => activeType.value === 'ALL' ? results.value || [] : (results.value || []).filter(item => item.type === activeType.value))

const highlight = (value: string) => {
  const escaped = value.replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char] || char)
  if (!query.value) return escaped
  const term = query.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(`(${term})`, 'gi'), '<mark>$1</mark>')
}

// 搜索历史（localStorage）
const HISTORY_KEY = 'co11ap5e-search-history'
const history = ref<string[]>([])
if (import.meta.client) {
  try { history.value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]') } catch { history.value = [] }
}
const saveHistory = (term: string) => {
  const list = [term, ...history.value.filter(item => item !== term)].slice(0, 8)
  history.value = list
  if (import.meta.client) localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
}
const clearHistory = () => {
  history.value = []
  if (import.meta.client) localStorage.removeItem(HISTORY_KEY)
}
const submit = (term = input.value.trim()) => {
  if (!term) return
  saveHistory(term)
  input.value = term
  activeType.value = 'ALL'
  activeIndex.value = -1
  router.push({ path: '/search', query: { q: term } })
}

// 键盘 ↑↓ 选择结果、Enter 打开、Esc 取消
const listEl = ref<HTMLElement | null>(null)
const activeIndex = ref(-1)
const focusResult = (index: number) => {
  activeIndex.value = index
  listEl.value?.querySelectorAll<HTMLElement>('article')[index]?.scrollIntoView({ block: 'nearest' })
}
const onArrow = (delta: number) => {
  if (!filteredResults.value.length) return
  const total = filteredResults.value.length
  focusResult((activeIndex.value + delta + total) % total)
}
const onEnter = (event?: KeyboardEvent) => {
  const item = activeIndex.value >= 0 ? filteredResults.value[activeIndex.value] : null
  if (!item) return
  event?.preventDefault()
  if (item.external) window.open(item.url, '_blank', 'noreferrer')
  else router.push(item.url)
}
const onEscape = () => { activeIndex.value = -1 }

useSeoMeta({ title: '全站搜索', description: '搜索折叠思维 的文章、项目、AI 学习和游戏档案。' })
</script>

<template>
  <div class="page-wrap section-pad search-page">
    <header class="page-header content-wrap"><p class="eyebrow">Search</p><h1>全站搜索</h1><p>文章、项目、AI 学习与游戏档案。</p></header>
    <section class="content-wrap search-content">
      <form class="site-search-form" @submit.prevent="submit()">
        <Search :size="20" />
        <input v-model="input" type="search" maxlength="100" aria-label="搜索关键词" placeholder="输入关键词，↑↓ 选择结果，Enter 打开" autofocus
               @keydown.down.prevent="onArrow(1)" @keydown.up.prevent="onArrow(-1)" @keydown.enter="onEnter($event)" @keydown.esc="onEscape">
        <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
      </form>

      <div v-if="!query && history.length" class="search-history">
        <span class="search-history-label">最近搜索</span>
        <button v-for="term in history" :key="term" type="button" class="search-history-chip" @click="submit(term)">{{ term }}</button>
        <button type="button" class="search-history-clear" @click="clearHistory"><X :size="13" /> 清除</button>
      </div>

      <p v-if="query" class="search-summary">“{{ query }}” · {{ filteredResults.length }} / {{ results?.length || 0 }} 条结果 <kbd>↑</kbd><kbd>↓</kbd> 选择 · <kbd>Enter</kbd> 打开</p>
      <nav v-if="query && results?.length" class="search-type-tabs" aria-label="搜索结果类型"><button v-for="tab in typeTabs" :key="tab.key" type="button" :class="{ active: activeType === tab.key }" @click="activeType = tab.key">{{ tab.label }}</button></nav>
      <div v-if="status === 'pending'" class="empty-state">正在搜索...</div>
      <div v-else-if="filteredResults.length" ref="listEl" class="search-results" role="listbox">
        <article v-for="(result, index) in filteredResults" :key="`${result.type}-${result.url}`" :class="{ active: activeIndex === index }" role="option" :aria-selected="activeIndex === index" @mouseenter="activeIndex = index">
          <a v-if="result.external" :href="result.url" target="_blank" rel="noreferrer" @click="saveHistory(query)">
            <div><span>{{ labels[result.type] }}</span><small v-html="highlight(result.meta)"></small></div><h2 v-html="highlight(result.title)" /><p v-html="highlight(result.summary)" /><ArrowUpRight :size="19" />
          </a>
          <NuxtLink v-else :to="result.url" @click="saveHistory(query)">
            <div><span>{{ labels[result.type] }}</span><small v-html="highlight(result.meta)"></small></div><h2 v-html="highlight(result.title)" /><p v-html="highlight(result.summary)" /><ArrowUpRight :size="19" />
          </NuxtLink>
        </article>
      </div>
      <p v-else-if="query" class="empty-state"><Search :size="19" /> 没有找到——这个词还在折叠中，换一个展开试试？</p>
    </section>
  </div>
</template>
