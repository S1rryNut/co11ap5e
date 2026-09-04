export interface ArticleSummary {
  id: number
  slug: string
  title: string
  excerpt: string
  category: string
  tags: string[]
  publishedAt: string
  readingMinutes: number
}

export interface Article extends ArticleSummary {
  content: string
}

export interface ArticleNeighbors {
  prev: ArticleSummary | null
  next: ArticleSummary | null
}

export interface ArticleVersion extends Article {
  articleId: number
  createdAt: string

  published: boolean
}

export interface Project {
  id: number
  slug: string
  name: string
  summary: string
  stack: string[]
  background: string
  responsibility: string
  workflow: string[]
  architecture: string
  highlights: string[]
  retrospective: string
  repositoryUrl?: string
  liveUrl?: string
  featured: boolean
  sortOrder?: number
}

export interface GameAccount {
  id: number
  platform: string
  handle: string
  url: string
  description: string
  sortOrder: number
}

export interface GameEntry {
  id: number
  slug: string
  title: string
  coverUrl?: string
  platform: string
  status: 'PLAYING' | 'COMPLETED' | 'WISHLIST' | 'DROPPED' | 'LIBRARY'
  score?: number
  hoursPlayed: number
  completedOn?: string
  verdict: string
  reviewArticleSlug?: string
  sortOrder: number
}

export interface SearchResult {
  type: 'ARTICLE' | 'PROJECT' | 'LEARNING' | 'AI_NEWS' | 'GAME'
  title: string
  summary: string
  url: string
  meta: string
  external: boolean
}

export interface PageMetric {
  path: string
  views: number
  averageRenderMs: number
  maxRenderMs: number
}

export interface DailyMetric {
  date: string
  views: number
  visitors: number
}

export interface MetricsSummary {
  viewsToday: number
  viewsThirtyDays: number
  visitorsToday: number
  visitorsThirtyDays: number
  averageRenderMs: number
  topPages: PageMetric[]
  dailyViews: DailyMetric[]
}

export interface Message {
  id: number
  name: string
  email?: string
  content: string
  createdAt: string
}

// 公开留言（脱敏视图：不含邮箱）
export interface PublicMessage {
  id: number
  name: string
  content: string
  createdAt: string
  pinned?: boolean
  likeCount: number
  commentCount: number
}

export interface MessageComment {
  id: number
  messageId: number
  name: string
  content: string
  createdAt: string
  isAuthor: boolean
  verified: boolean
}

export interface MessageLikeResult {
  messageId: number
  likeCount: number
  liked: boolean
}

export interface UserView {
  id: number
  email: string
  nickname: string
  isOwner: boolean
}

export interface UserAuthResponse {
  token: string
  user: UserView
}

export interface AiNewsItem {
  id: number
  title: string
  summary: string
  sourceName: string
  sourceUrl: string
  publishedAt: string
  topics: string[]
}

export interface LearningResource {
  id: number
  slug: string
  title: string
  description: string
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  topic: string
  url?: string
  content?: string
  sortOrder: number
}

export interface LearningProgress {
  id: number
  resourceId: number
  slug: string
  title: string
  topic: string
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE'
  note: string
  sortOrder: number
}

export interface LearningLog {
  id: number
  station: number
  section: number
  logDate: string | null
  title: string
  content: string
  tags: string[]
  createdAt: string
}

export interface LearningLogInput {
  station: number
  section: number
  logDate: string | null
  title: string
  content: string
  tags: string[]
}

export interface Resume {
  displayName: string
  headline: string
  location: string
  email: string
  summary: string
  skills: string[]
  experiences: Array<{
    organization: string
    role: string
    period: string
    description: string
  }>
  education: Array<{
    institution: string
    major: string
    period: string
  }>
}

export interface NowProfile {
  headline: string
  updatedAt: string
  building: string[]
  learning: string[]
  playing: string[]
  reading: string[]
  outlook?: string[]
}
