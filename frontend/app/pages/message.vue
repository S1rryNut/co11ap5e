<script setup lang="ts">
import { CheckCircle2, Heart, LogOut, MailCheck, MessageCircle, MessageSquareText, Send, User } from '@lucide/vue'
import type { MessageComment, PublicMessage, UserView } from '~/types'

const api = useSiteApi()
const form = reactive({ name: '', email: '', content: '' })
const sending = ref(false)
const sent = ref(false)
const error = ref('')

// 最近公开留言（脱敏：称呼 + 内容 + 时间）
const { data: recent, refresh } = await useAsyncData('public-messages',
  () => api.getMessages(), { default: () => [] as PublicMessage[] })

// ---- 邮箱登录（验证码认证，前台仍可匿名）----
const currentUser = ref<UserView | null>(null)
const authOpen = ref(false)
const authBusy = ref(false)
const authError = ref('')
const authSent = ref(false)
const authRegistered = ref(false)
const authForm = reactive({ email: '', code: '', nickname: '' })

onMounted(async () => {
  if (!import.meta.client) return
  const token = localStorage.getItem('folded-user-token')
  if (token) {
    try { currentUser.value = await api.getMe() } catch { localStorage.removeItem('folded-user-token') }
  }
})
const sendCode = async () => {
  if (!authForm.email.trim()) { authError.value = '请先填写邮箱'; return }
  authBusy.value = true; authError.value = ''
  try {
    const res = await api.sendAuthCode(authForm.email.trim())
    authRegistered.value = res.registered
    authSent.value = true
  } catch { authError.value = '验证码发送失败，请稍后重试' } finally { authBusy.value = false }
}
const doLogin = async () => {
  authBusy.value = true; authError.value = ''
  try {
    const res = await api.authLogin({
      email: authForm.email.trim(),
      code: authForm.code.trim(),
      nickname: (!authRegistered.value && authForm.nickname.trim()) ? authForm.nickname.trim() : undefined
    })
    currentUser.value = res.user
    authOpen.value = false; authSent.value = false; authRegistered.value = false
    authForm.email = ''; authForm.code = ''; authForm.nickname = ''
  } catch { authError.value = '登录失败，请检查验证码是否正确' } finally { authBusy.value = false }
}
const doLogout = async () => {
  await api.authLogout(); currentUser.value = null
}

// ---- 点赞 ----
const LIKED_KEY = 'folded-liked-messages'
const likedMap = reactive<Record<number, boolean>>({})
if (import.meta.client) {
  try {
    const stored = JSON.parse(localStorage.getItem(LIKED_KEY) || '{}')
    Object.assign(likedMap, stored)
  } catch { /* ignore */ }
}
const persistLikes = () => {
  if (import.meta.client) localStorage.setItem(LIKED_KEY, JSON.stringify(likedMap))
}
const toggleLike = async (item: PublicMessage) => {
  try {
    const already = likedMap[item.id]
    const result = already
      ? await api.unlikeMessage(item.id)
      : await api.likeMessage(item.id)
    likedMap[item.id] = result.liked
    item.likeCount = result.likeCount
    persistLikes()
  } catch { /* 静默失败，不打断浏览 */ }
}

// ---- 评论 ----
const expandedId = ref<number | null>(null)
const commentsByMessage = reactive<Record<number, MessageComment[]>>({})
const commentForm = reactive({ name: '', content: '' })
const commentSending = ref(false)
const commentError = ref('')

const toggleComments = async (item: PublicMessage) => {
  if (expandedId.value === item.id) { expandedId.value = null; return }
  expandedId.value = item.id
  if (!commentsByMessage[item.id]) {
    try { commentsByMessage[item.id] = await api.getMessageComments(item.id) } catch { commentsByMessage[item.id] = [] }
  }
}
const submitComment = async (item: PublicMessage) => {
  // 已登录用户用注册昵称，未登录用户用填写的称呼
  const name = currentUser.value ? currentUser.value.nickname : commentForm.name.trim()
  if (!name || !commentForm.content.trim()) return
  commentSending.value = true; commentError.value = ''
  try {
    const created = await api.addMessageComment(item.id, { name, content: commentForm.content.trim() })
    commentsByMessage[item.id] = [...(commentsByMessage[item.id] || []), created]
    item.commentCount++
    commentForm.content = ''
  } catch { commentError.value = '评论发送失败，请稍后重试。' } finally { commentSending.value = false }
}

const formatTime = (iso: string) => {
  const d = new Date(iso)
  const now = new Date()
  const sameYear = d.getFullYear() === now.getFullYear()
  const pad = (n: number) => String(n).padStart(2, '0')
  const date = sameYear ? `${d.getMonth() + 1}-${pad(d.getDate())}` : `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const submit = async () => {
  sending.value = true; error.value = ''
  try {
    await api.request('/public/csrf')
    const cookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))
    if (!cookie) throw new Error('csrf')
    await api.request('/public/messages', { method: 'POST', headers: { 'X-XSRF-TOKEN': decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) }, body: form })
    sent.value = true; form.name = ''; form.email = ''; form.content = ''
    refresh()
  } catch { error.value = '提交失败，请稍后重试。' } finally { sending.value = false }
}
useSeoMeta({ title: '留言', description: '给折叠思维留言。' })
</script>

<template>
  <div class="page-wrap section-pad">
    <header class="page-header content-wrap">
      <p class="eyebrow">Message</p>
      <h1>给我留言</h1>
      <p>项目交流、技术讨论或其他想说的话，都可以写在这里。</p>
    </header>
    <section class="content-wrap message-layout">
      <aside class="message-intro">
        <span class="message-intro-icon"><MessageSquareText :size="26" /></span>
        <h2>保持联系</h2>
        <p class="message-fold">把想说的话折叠成一句留言，我来展开回应。</p>
        <ul class="message-facts">
          <li>项目、技术、安全或 AI 相关话题都欢迎</li>
          <li>称呼与留言内容会公开展示，邮箱仅用于私密回复、不会公开</li>
          <li>看到留言后我会尽量回复</li>
        </ul>
      </aside>
      <form class="message-form" @submit.prevent="submit">
        <div class="message-card">
          <label>称呼<input v-model="form.name" required maxlength="80" autocomplete="name" placeholder="怎么称呼你"></label>
          <label>邮箱（可选）<input v-model="form.email" type="email" maxlength="200" autocomplete="email" placeholder="用于回复，不公开"></label>
          <label class="message-content-field">留言内容
            <textarea v-model="form.content" required maxlength="2000" rows="7" placeholder="写下你想说的话…"></textarea>
            <span class="message-count">{{ form.content.length }}/2000</span>
          </label>
        </div>
        <div class="message-actions">
          <button class="primary-button" type="submit" :disabled="sending"><Send :size="17" />{{ sending ? '发送中' : '发送留言' }}</button>
          <p class="message-privacy">提交即表示同意公开显示称呼与内容，邮箱仅用于联系。</p>
        </div>
        <p v-if="sent" class="message-success"><CheckCircle2 :size="17" />留言已发送，我会尽快查看。</p>
        <p v-if="error" class="message-error">{{ error }}</p>
      </form>
    </section>

    <section class="content-wrap message-wall">
      <div class="message-wall-head">
        <h2><MessageCircle :size="20" /> 最近留言</h2>
        <div class="message-wall-head-right">
          <span v-if="recent.length" class="message-wall-count">{{ recent.length }} 条</span>
          <span v-if="currentUser" class="message-user-chip"><User :size="13" />{{ currentUser.nickname }}<button type="button" class="message-user-logout" title="退出登录" @click="doLogout"><LogOut :size="13" /></button></span>
          <button v-else type="button" class="message-auth-btn" :class="{ open: authOpen }" @click="authOpen = !authOpen"><User :size="14" />{{ authOpen ? '取消' : '邮箱登录' }}</button>
        </div>
      </div>
      <div v-if="authOpen" class="message-auth">
        <div class="message-auth-title"><MailCheck :size="16" /> 邮箱验证码登录</div>
        <p class="message-auth-tip">不注册也能匿名留言；登录后评论会带上你的昵称和「已认证」标识。验证码由系统发送到你的邮箱，5 分钟内有效。</p>
        <div class="message-auth-row">
          <input v-model="authForm.email" type="email" placeholder="你的邮箱" maxlength="254" :disabled="authSent">
          <button v-if="!authSent" class="message-auth-btn-small" type="button" :disabled="authBusy" @click="sendCode">{{ authBusy ? '发送中' : '发送验证码' }}</button>
          <span v-else class="message-auth-sent">已发送，请查收邮箱</span>
        </div>
        <template v-if="authSent">
          <input v-model="authForm.code" placeholder="6 位验证码" maxlength="6" inputmode="numeric" class="message-auth-code">
          <input v-if="!authRegistered" v-model="authForm.nickname" placeholder="昵称（首次登录必填）" maxlength="80">
        </template>
        <div class="message-auth-row">
          <button class="primary-button" type="button" :disabled="authBusy || !authSent" @click="doLogin"><MailCheck :size="15" />{{ authBusy ? '登录中' : '登录' }}</button>
          <span v-if="authSent" class="message-auth-resend" @click="authSent = false; sendCode()">重新发送</span>
        </div>
        <p v-if="authError" class="message-error">{{ authError }}</p>
      </div>
      <p v-if="!recent.length" class="message-wall-empty">还没有留言，来做第一个吧。</p>
      <ul v-else class="message-wall-list">
        <li v-for="item in recent" :key="item.id">
          <div class="message-wall-meta"><strong>{{ item.name }}</strong><span class="message-wall-meta-right"><span v-if="item.pinned" class="message-pin">置顶</span><time :datetime="item.createdAt">{{ formatTime(item.createdAt) }}</time></span></div>
          <p>{{ item.content }}</p>
          <div class="message-wall-actions">
            <button class="message-like-btn" :class="{ liked: likedMap[item.id] }" type="button" :aria-pressed="likedMap[item.id]" @click="toggleLike(item)">
              <Heart :size="15" :fill="likedMap[item.id] ? 'currentColor' : 'none'" />{{ item.likeCount }}
            </button>
            <button class="message-comment-btn" :class="{ active: expandedId === item.id }" type="button" :aria-expanded="expandedId === item.id" @click="toggleComments(item)">
              <MessageCircle :size="15" />{{ item.commentCount }}
            </button>
          </div>
          <div v-if="expandedId === item.id" class="message-comments">
            <p v-if="!commentsByMessage[item.id]?.length" class="message-comments-empty">还没有评论，来聊两句。</p>
            <ul v-else class="message-comments-list">
              <li v-for="comment in commentsByMessage[item.id]" :key="comment.id">
                <div class="message-comments-meta">
                  <strong>{{ comment.name }}</strong><span v-if="comment.isAuthor" class="message-author">作者</span><span v-else-if="comment.verified" class="message-verified">已认证</span><time :datetime="comment.createdAt">{{ formatTime(comment.createdAt) }}</time>
                </div>
                <p>{{ comment.content }}</p>
              </li>
            </ul>
            <form class="message-comment-form" @submit.prevent="submitComment(item)">
              <input v-if="!currentUser" v-model="commentForm.name" maxlength="80" placeholder="你的称呼" aria-label="称呼">
              <p v-else class="message-comment-as">以 <strong>{{ currentUser.nickname }}</strong> 身份评论（已认证）</p>
              <div class="message-comment-input">
                <input v-model="commentForm.content" maxlength="500" placeholder="写下评论…" aria-label="评论内容">
                <button class="message-comment-send" type="submit" :disabled="commentSending"><Send :size="14" /></button>
              </div>
              <p v-if="commentError" class="message-error">{{ commentError }}</p>
            </form>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>
