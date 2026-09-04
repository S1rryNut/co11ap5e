const STORAGE_KEY = 'co11ap5e-theme'
const MEDIA = '(prefers-color-scheme: dark)'

function readStored(): string | null {
  try { return localStorage.getItem(STORAGE_KEY) } catch { return null }
}
function writeStored(value: string) {
  try { localStorage.setItem(STORAGE_KEY, value) } catch { /* 存储被禁用时静默忽略，不影响切换 */ }
}

function systemPrefersDark(): boolean {
  if (import.meta.server) return false
  return window.matchMedia(MEDIA).matches
}

function resolveInitial(): 'dark' | 'light' {
  if (import.meta.client) {
    const stored = readStored()
    if (stored === 'dark' || stored === 'light') return stored
  }
  return systemPrefersDark() ? 'dark' : 'light'
}

export function useTheme() {
  const theme = ref<'dark' | 'light'>('light')

  const apply = (value: 'dark' | 'light') => {
    if (import.meta.client) {
      document.documentElement.dataset.theme = value
      let meta = document.querySelector('meta[name="theme-color"]')
      if (!meta) {
        meta = document.createElement('meta')
        meta.setAttribute('name', 'theme-color')
        document.head.appendChild(meta)
      }
      meta.setAttribute('content', value === 'dark' ? '#0d1117' : '#f5f6f2')
    }
  }

  const toggle = () => {
    const next = theme.value === 'dark' ? 'light' : 'dark'
    theme.value = next
    if (import.meta.client) {
      // 切换瞬间给全局加过渡类，让所有组件以统一节奏同步过渡，避免快慢不一
      document.documentElement.classList.add('theme-transitioning')
      void document.documentElement.offsetHeight // 强制重排，确保从旧配色起做过渡
      window.setTimeout(() => document.documentElement.classList.remove('theme-transitioning'), 400)
    }
    apply(theme.value)
    writeStored(theme.value)
  }

  const syncWithSystem = () => {
    theme.value = resolveInitial()
    apply(theme.value)
  }

  if (import.meta.client) {
    theme.value = resolveInitial()
    apply(theme.value)

    // 管理后台保留固定的深色面板，避免变量切换破坏后台布局
    const route = useRoute()
    watch(() => route.path, (path) => {
      if (path.startsWith('/admin')) apply('light')
      else apply(theme.value)
    }, { immediate: true })

    const mql = window.matchMedia(MEDIA)
    const onChange = () => {
      if (!readStored()) {
        theme.value = mql.matches ? 'dark' : 'light'
        apply(theme.value)
      }
    }
    mql.addEventListener('change', onChange)
    onScopeDispose(() => mql.removeEventListener('change', onChange))
  }

  return { theme, toggle, syncWithSystem }
}
