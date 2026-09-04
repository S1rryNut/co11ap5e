const CACHE_NAME = 'co11ap5e-admin-v1'
const ADMIN_SHELL = '/admin'

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.add(ADMIN_SHELL)))
  self.skipWaiting()
})

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(
    keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
  )))
  self.clients.claim()
})

self.addEventListener('fetch', event => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.pathname.startsWith('/api/')) return

  if (url.pathname.startsWith('/_nuxt/')) {
    event.respondWith(caches.open(CACHE_NAME).then(async cache => {
      const cached = await cache.match(request)
      const network = fetch(request).then(response => {
        if (response.ok) cache.put(request, response.clone())
        return response
      }).catch(() => cached)
      return cached || network
    }))
    return
  }

  event.respondWith(fetch(request).then(response => {
    if (response.ok && url.pathname === '/admin') {
      const copy = response.clone()
      caches.open(CACHE_NAME).then(cache => cache.put(ADMIN_SHELL, copy))
    }
    return response
  }).catch(() => caches.match(ADMIN_SHELL)))
})
