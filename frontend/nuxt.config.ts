export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    apiBase: process.env.NUXT_API_BASE || 'http://127.0.0.1:8080/api',
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || '/api',
      // 站点公开地址：部署时用环境变量 NUXT_PUBLIC_SITE_URL 覆盖
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || 'https://example.com',
      // giscus 评论（在 giscus.app 配置后填入，留空则评论区不渲染）
      giscus: {
        repo: process.env.NUXT_PUBLIC_GISCUS_REPO || '',
        repoId: process.env.NUXT_PUBLIC_GISCUS_REPO_ID || '',
        category: process.env.NUXT_PUBLIC_GISCUS_CATEGORY || '',
        categoryId: process.env.NUXT_PUBLIC_GISCUS_CATEGORY_ID || ''
      }
    }
  },
  app: {
    pageTransition: { name: 'page', mode: 'out-in' },
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      titleTemplate: '%s | 折叠思维',
      meta: [
        { name: 'description', content: '个人博客、项目作品、简历与 AI 学习记录。' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'theme-color', content: '#f5f6f2' }
      ],
      link: [
        { rel: 'manifest', href: '/site.webmanifest' },
        { rel: 'icon', type: 'image/png', href: '/site-icon-192.png' },
        { rel: 'apple-touch-icon', href: '/site-icon-192.png' },
        { rel: 'alternate', type: 'application/rss+xml', title: '折叠思维 的博客 RSS', href: '/rss.xml' },
        { rel: 'sitemap', type: 'application/xml', title: 'Sitemap', href: '/sitemap.xml' }
      ],
      script: [
        {
          // 首屏前应用主题，避免暗色用户刷新出现浅色闪烁（FOUC）
          innerHTML: "(function(){try{var p=location.pathname;if(p.indexOf('/admin')===0){document.documentElement.setAttribute('data-theme','light');return;}var t=localStorage.getItem('co11ap5e-theme');if(t==='dark'||t==='light'){document.documentElement.setAttribute('data-theme',t);}else if(window.matchMedia('(prefers-color-scheme: dark)').matches){document.documentElement.setAttribute('data-theme','dark');}}catch(e){}})();",
          tagPriority: 'high'
        }
      ]
    }
  },
  nitro: {
    compressPublicAssets: true
  },
  routeRules: {
    '/': { swr: 60 },
    '/now': { swr: 60 },
    '/games/**': { swr: 120 },
    '/projects/**': { swr: 120 },
    '/ai/**': { swr: 120 },
    '/article/**': { swr: 120 },
    '/blog/**': { swr: 60 },
    // 旧路径重定向：/messages → /message，/c → 首页
    '/messages': { redirect: { to: '/message', statusCode: 301 } },
    '/c': { redirect: { to: '/', statusCode: 301 } }
  }
})
