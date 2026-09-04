import type { ArticleSummary, GameEntry, Project } from '~/types'

const escapeXml = (value: string) => value.replace(/[<>&'\"]/g, char => ({
  '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;'
}[char] || char))

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const siteUrl = String(config.public.siteUrl).replace(/\/+$/, '')
  let articles: ArticleSummary[] = []
  let projects: Project[] = []
  let games: GameEntry[] = []
  try { articles = await $fetch<ArticleSummary[]>('/public/articles', { baseURL: config.apiBase }) } catch {}
  try { projects = await $fetch<Project[]>('/public/projects', { baseURL: config.apiBase }) } catch {}
  try { games = await $fetch<GameEntry[]>('/public/games', { baseURL: config.apiBase }) } catch {}

  const urls: string[] = []
  const staticPaths = ['/', '/article/tech', '/article/game', '/article/ai', '/games', '/now', '/ai', '/projects', '/message', '/search']
  for (const path of staticPaths) urls.push(`<url><loc>${escapeXml(siteUrl + path)}</loc></url>`)
  for (const item of articles) {
    const lastmod = item.publishedAt ? `<lastmod>${new Date(item.publishedAt).toISOString().slice(0, 10)}</lastmod>` : ''
    urls.push(`<url><loc>${escapeXml(`${siteUrl}/blog/${item.slug}`)}</loc>${lastmod}</url>`)
  }
  for (const item of projects) urls.push(`<url><loc>${escapeXml(`${siteUrl}/projects/${item.slug}`)}</loc></url>`)
  for (const item of games) urls.push(`<url><loc>${escapeXml(`${siteUrl}/games/${item.slug}`)}</loc></url>`)

  setHeader(event, 'content-type', 'application/xml; charset=utf-8')
  return `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${urls.join('')}</urlset>`
})
