import type { ArticleSummary } from '~/types'

const xml = (value: string) => value.replace(/[<>&'\"]/g, char => ({
  '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;'
}[char] || char))

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const siteUrl = String(config.public.siteUrl).replace(/\/+$/, '')
  const articles = (await $fetch<ArticleSummary[]>('/public/articles', { baseURL: config.apiBase }))
    .filter(item => item.category !== '杂谈')
  setHeader(event, 'content-type', 'application/rss+xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom"><channel><title>折叠思维 的博客</title><link>${xml(siteUrl)}</link><description>软件开发、项目实践与 AI 学习记录</description><atom:link href="${xml(`${siteUrl}/rss.xml`)}" rel="self" type="application/rss+xml"/>${articles.map(item => `<item><title>${xml(item.title)}</title><link>${xml(`${siteUrl}/blog/${item.slug}`)}</link><description>${xml(item.excerpt)}</description><pubDate>${new Date(item.publishedAt).toUTCString()}</pubDate><guid>${xml(`${siteUrl}/blog/${item.slug}`)}</guid></item>`).join('')}</channel></rss>`
})
