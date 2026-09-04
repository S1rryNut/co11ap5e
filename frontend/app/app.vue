<script setup lang="ts">
const siteUrl = useRuntimeConfig().public.siteUrl

// 全站默认社交分享元数据（页面级 useSeoMeta 会覆盖同名项）
useSeoMeta({
  ogSiteName: '折叠思维',
  ogType: 'website',
  ogLocale: 'zh_CN',
  ogImage: `${siteUrl}/og-cover.png`,
  ogImageAlt: '折叠思维 — 把想法做成可用的成果',
  twitterCard: 'summary_large_image',
  twitterImage: `${siteUrl}/og-cover.png`,
  twitterCreator: '@Co11ap5e'
})

useHead({
  link: [{ rel: 'manifest', href: '/site.webmanifest' }]
})

// 手机端 APP（PWA）：注册 Service Worker，支持离线壳与安装
if (import.meta.client) {
  onMounted(() => {
    if (window.location.pathname.startsWith('/admin')) return
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(() => {})
    }
  })
}
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
