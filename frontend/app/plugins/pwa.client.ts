export default defineNuxtPlugin(() => {
  if (!import.meta.client || !('serviceWorker' in navigator)) return
  window.addEventListener('load', () => {
    // 主站 PWA（作用域 /）
    navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(() => {})
    // 管理后台 PWA（作用域 /admin/）
    navigator.serviceWorker.register('/admin-sw.js', { scope: '/admin/' }).catch(() => {})
  })
})
