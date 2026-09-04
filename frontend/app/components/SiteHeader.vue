<script setup lang="ts">
import { Menu, Search, X } from '@lucide/vue'

const open = ref(false)
const route = useRoute()
const { theme, toggle } = useTheme()
watch(() => route.fullPath, () => { open.value = false })

const links = [
  { to: '/now', label: 'Now' },
  { to: '/article/tech', label: '文章' },
  { to: '/projects', label: '项目' },
  { to: '/games', label: '游戏' },
  { to: '/ai', label: 'Ai' },
  { to: '/learn', label: '学习' },
  { to: '/message', label: '留言' }
]
const isLinkActive = (to: string) => {
  const path = route.path
  if (to === '/article/tech') return path === '/article/tech' || path.startsWith('/article/') || path === '/blog' || path.startsWith('/blog/') || path === '/talk' || path.startsWith('/talk/')
  if (to === '/ai') return path === '/ai' || path.startsWith('/ai/')
  if (to === '/learn') return path === '/learn' || path.startsWith('/learn/')
  return path === to || path.startsWith(`${to}/`)
}
</script>

<template>
  <header class="site-header">
    <div class="nav-wrap">
      <NuxtLink class="brand" to="/" aria-label="返回首页">
        <span class="brand-mark"><img src="/logo.png?v=3" alt="折叠思维" /></span>
        <span>折叠思维</span>
      </NuxtLink>
      <div class="nav-actions">
        <nav class="desktop-nav" aria-label="主导航">
          <NuxtLink v-for="link in links" :key="link.to" :to="link.to" :class="{ 'nav-active': isLinkActive(link.to) }">{{ link.label }}</NuxtLink>
        </nav>
        <button class="theme-toggle" type="button" :aria-label="theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'" :title="theme === 'dark' ? '明暗之间，折叠一次世界 · 切到浅色' : '明暗之间，折叠一次世界 · 切到深色'" :data-theme-state="theme" @click="toggle">
          <svg class="icon-sun" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2"></path><path d="M12 20v2"></path><path d="m4.93 4.93 1.41 1.41"></path><path d="m17.66 17.66 1.41 1.41"></path><path d="M2 12h2"></path><path d="M20 12h2"></path><path d="m6.34 17.66-1.41 1.41"></path><path d="m19.07 4.93-1.41 1.41"></path></svg>
          <svg class="icon-moon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"></path></svg>
        </button>
        <NuxtLink class="header-search" to="/search" aria-label="全站搜索" title="全站搜索"><Search :size="19" /></NuxtLink>
        <button class="icon-button menu-button" type="button" :aria-expanded="open" aria-label="切换导航" @click="open = !open">
          <X v-if="open" :size="20" />
          <Menu v-else :size="20" />
        </button>
      </div>
    </div>
    <nav v-if="open" class="mobile-nav" aria-label="移动端导航">
      <NuxtLink v-for="link in links" :key="link.to" :to="link.to" :class="{ 'nav-active': isLinkActive(link.to) }">{{ link.label }}</NuxtLink>
    </nav>
  </header>
</template>
