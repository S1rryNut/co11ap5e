import type { AiNewsItem, Article, ArticleNeighbors, ArticleSummary, GameAccount, GameEntry, LearningLog, LearningLogInput, LearningProgress, LearningResource, MessageComment, MessageLikeResult, NowProfile, Project, PublicMessage, Resume, SearchResult, UserAuthResponse, UserView } from '~/types'

export const useSiteApi = () => {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiBase : config.public.apiBase

  const adminToken = () => import.meta.client ? sessionStorage.getItem('personal-site-admin-token') : ''
  const userToken = () => import.meta.client ? localStorage.getItem('folded-user-token') : ''
  const request = <T>(path: string, options: Parameters<typeof $fetch<T>>[1] = {}) => {
    const token = adminToken() || userToken()
    return $fetch<T>(path, {
      baseURL,
      credentials: 'include',
      ...options,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers as Record<string, string> || {})
      }
    })
  }

  // 写操作需先取 CSRF token 再带上 X-XSRF-TOKEN 头
  const csrfHeader = async (): Promise<Record<string, string>> => {
    await request('/public/csrf')
    const cookie = document.cookie.split('; ').find(c => c.startsWith('XSRF-TOKEN='))
    if (!cookie) throw new Error('csrf token missing')
    return { 'X-XSRF-TOKEN': decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) }
  }

  return {
    getArticles: (query = '', category = '', excludeCategory = '') => {
      const params = new URLSearchParams()
      if (query) params.set('q', query)
      if (category) params.set('category', category)
      if (excludeCategory) params.set('excludeCategory', excludeCategory)
      const suffix = params.size ? `?${params.toString()}` : ''
      return request<ArticleSummary[]>(`/public/articles${suffix}`)
    },
    getArticle: (slug: string) => request<Article>(`/public/articles/${encodeURIComponent(slug)}`),
    getArticleNeighbors: (slug: string) => request<ArticleNeighbors>(`/public/articles/${encodeURIComponent(slug)}/adjacent`),
    getRelatedArticles: (slug: string) => request<ArticleSummary[]>(`/public/articles/${encodeURIComponent(slug)}/related`),
    getProjects: () => request<Project[]>('/public/projects'),
    getProject: (slug: string) => request<Project>(`/public/projects/${encodeURIComponent(slug)}`),
    getGames: () => request<GameEntry[]>('/public/games'),
    getGame: (slug: string) => request<GameEntry>(`/public/games/${encodeURIComponent(slug)}`),
    getViewCounts: (paths: string[]) => request<Record<string, number>>(`/public/metrics/views?paths=${encodeURIComponent(paths.join(','))}`),
    search: (query: string) => request<SearchResult[]>(`/public/search?q=${encodeURIComponent(query)}`),
    getAiNews: () => request<AiNewsItem[]>('/public/ai/news'),
    getLearningResources: () => request<LearningResource[]>('/public/ai/learning'),
    getLearningResource: (slug: string) => request<LearningResource>(`/public/ai/learning/${encodeURIComponent(slug)}`),
    getLearningProgress: () => request<LearningProgress[]>('/public/learning/progress'),
    getLearningLogs: () => request<LearningLog[]>('/public/learning/logs'),
    addLearningLog: async (body: LearningLogInput) =>
      request<LearningLog>('/public/learning/logs', { method: 'POST', headers: await csrfHeader(), body }),
    getAdminLearningLogs: () => request<LearningLog[]>('/admin/learning-logs'),
    updateLearningLog: async (id: number, body: LearningLogInput) =>
      request<LearningLog>(`/admin/learning-logs/${id}`, { method: 'PUT', headers: await csrfHeader(), body }),
    deleteLearningLog: async (id: number) =>
      request<void>(`/admin/learning-logs/${id}`, { method: 'DELETE', headers: await csrfHeader() }),
    updateLearningProgress: async (id: number, body: { status: string; note: string }) =>
      request<LearningProgress>(`/public/learning/progress/${id}`, { method: 'PUT', headers: await csrfHeader(), body }),
    getResume: () => request<Resume>('/public/resume'),
    getGameAccounts: () => request<GameAccount[]>('/public/game-accounts'),
    getNowProfile: () => request<NowProfile>('/public/now'),
    getMessages: () => request<PublicMessage[]>('/public/messages'),
    likeMessage: async (id: number) => request<MessageLikeResult>(`/public/messages/${id}/like`, { method: 'POST', headers: await csrfHeader() }),
    unlikeMessage: async (id: number) => request<MessageLikeResult>(`/public/messages/${id}/like`, { method: 'DELETE', headers: await csrfHeader() }),
    getMessageComments: (id: number) => request<MessageComment[]>(`/public/messages/${id}/comments`),
    addMessageComment: async (id: number, body: { name: string; content: string }) =>
      request<MessageComment>(`/public/messages/${id}/comments`, { method: 'POST', headers: await csrfHeader(), body }),
    sendAuthCode: async (email: string) =>
      request<{ registered: boolean }>('/public/auth/code', { method: 'POST', headers: await csrfHeader(), body: { email } }),
    authLogin: async (body: { email: string; code: string; nickname?: string }) => {
      const res = await request<UserAuthResponse>('/public/auth/login', { method: 'POST', headers: await csrfHeader(), body })
      if (import.meta.client) localStorage.setItem('folded-user-token', res.token)
      return res
    },
    authLogout: async () => {
      if (import.meta.client) {
        const t = localStorage.getItem('folded-user-token')
        if (t) { try { await request('/public/auth/logout', { method: 'POST', headers: await csrfHeader() }) } catch { /* ignore */ } }
        localStorage.removeItem('folded-user-token')
      }
    },
    getMe: () => request<UserView | null>('/public/auth/me'),
    request
  }
}
