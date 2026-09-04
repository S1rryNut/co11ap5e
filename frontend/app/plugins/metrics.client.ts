export default defineNuxtPlugin((nuxtApp) => {
  const router = useRouter()
  const config = useRuntimeConfig()
  let lastRecorded = ''

  const nextPaint = () => new Promise<void>(resolve => {
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
  })

  const record = async (path: string) => {
    if (path.startsWith('/admin') || path === lastRecorded) return
    lastRecorded = path
    const startedAt = performance.now()
    await nextPaint()
    try {
      await $fetch('/public/csrf', {
        baseURL: config.public.apiBase,
        credentials: 'include'
      })
      const token = document.cookie.split('; ')
        .find(value => value.startsWith('XSRF-TOKEN='))
        ?.slice('XSRF-TOKEN='.length)
      if (!token) return
      await $fetch('/public/metrics', {
        baseURL: config.public.apiBase,
        method: 'POST',
        credentials: 'include',
        headers: { 'X-XSRF-TOKEN': decodeURIComponent(token) },
        body: { path, renderMs: Math.round((performance.now() - startedAt) * 10) / 10 }
      })
    } catch {
      // Metrics must never affect navigation or page rendering.
    }
  }

  nuxtApp.hook('app:mounted', () => { void record(router.currentRoute.value.path) })
  router.afterEach(to => { void record(to.path) })
})
