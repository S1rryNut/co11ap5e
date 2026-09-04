/**
 * Co11ap5e 主站 Service Worker
 * 策略：导航网络优先 + 离线应用壳兜底；_nuxt 静态资源 stale-while-revalidate；
 * 不缓存 /api 与 /admin（后台有独立的 admin-sw）。
 */
const CACHE_NAME = 'co11ap5e-site-v2'
const SHELL = '/'

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.add(SHELL))
      .catch(() => {})
  )
  self.skipWaiting()
})

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(key => key !== CACHE_NAME && !key.startsWith('co11ap5e-admin'))
          .map(key => caches.delete(key))
    )).then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', event => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== location.origin) return
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/admin')) return

  // 页面导航：网络优先，离线时回退到缓存的应用壳
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).then(response => {
        if (response.ok) {
          const copy = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(SHELL, copy))
        }
        return response
      }).catch(() =>
        caches.match(SHELL).then(r => r || caches.match(request))
      )
    )
    return
  }

  // 构建产物：先用缓存、后台更新
  if (url.pathname.startsWith('/_nuxt/')) {
    event.respondWith(
      caches.open(CACHE_NAME).then(async cache => {
        const cached = await cache.match(request)
        const network = fetch(request).then(response => {
          if (response.ok) cache.put(request, response.clone())
          return response
        }).catch(() => cached)
        return cached || network
      })
    )
    return
  }

  // 其余静态资源（图片/字体等）：网络优先，离线兜底
  event.respondWith(
    fetch(request).catch(() => caches.match(request))
  )
})
