/**
 * 键盘快捷键：
 * - `/` 聚焦全站搜索（已处于搜索页则直接聚焦输入框）
 */
export default defineNuxtPlugin(() => {
  if (!import.meta.client) return

  const isEditable = (el: Element | null): boolean => {
    if (!el) return false
    const tag = el.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
    if ((el as HTMLElement).isContentEditable) return true
    return false
  }

  const onKeydown = (e: KeyboardEvent) => {
    if (e.key !== '/' || e.metaKey || e.ctrlKey || e.altKey) return
    if (isEditable(e.target as Element)) return
    e.preventDefault()
    const route = useRoute()
    if (route.path === '/search') {
      document.querySelector<HTMLInputElement>('.site-search-form input')?.focus()
    } else {
      navigateTo('/search')
    }
  }

  window.addEventListener('keydown', onKeydown)
})
