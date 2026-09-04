import MarkdownIt from 'markdown-it'

export interface TocItem {
  level: number
  text: string
  id: string
}

interface MarkdownEnv {
  toc: TocItem[]
  usedIds: Record<string, number>
}

const renderer = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true
})

const stripInline = (text: string) => text.replace(/[*_`~#]/g, '').trim()

const escapeHtml = (s: string) => s
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

const slugify = (text: string) => {
  const base = stripInline(text).toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
  return base || 'section'
}

// 给标题加锚点 id 和可跳转链接，同时把标题收集进 env.toc 供目录使用
renderer.renderer.rules.heading_open = (tokens: any, idx: number, options: any, env: any, self: any) => {
  const token = tokens[idx]
  const inline = tokens[idx + 1]
  const rawText = inline ? inline.content : ''
  let id = slugify(rawText)
  if (env && env.usedIds) {
    const count = env.usedIds[id] || 0
    env.usedIds[id] = count + 1
    if (count > 0) id = `${id}-${count + 1}`
  }
  token.attrSet('id', id)
  if (env && Array.isArray(env.toc)) {
    env.toc.push({ level: Number(token.tag.slice(1)), text: stripInline(rawText), id })
  }
  // 在标题内容前注入锚点链接（hover 显示 #）
  if (inline && Array.isArray(inline.children) && inline.children.length) {
    const anchor = new (token.constructor as any)('html_inline', '', 0)
    anchor.content = `<a class="heading-anchor" href="#${id}" aria-hidden="true" tabindex="-1"></a>`
    inline.children.unshift(anchor)
  }
  return self.renderToken(tokens, idx, options)
}

// 正文图片默认懒加载
const defaultImageRule = renderer.renderer.rules.image
renderer.renderer.rules.image = (tokens: any, idx: number, options: any, env: any, self: any) => {
  const token = tokens[idx]
  token.attrSet('loading', 'lazy')
  token.attrSet('decoding', 'async')
  return defaultImageRule ? defaultImageRule(tokens, idx, options, env, self) : self.renderToken(tokens, idx, options)
}

// 代码块增强：语言标签 + 行号 + 复制按钮
renderer.renderer.rules.fence = (tokens: any, idx: number, options: any, env: any, self: any) => {
  const token = tokens[idx]
  const info = token.info ? String(token.info).trim().split(/\s+/g)[0] : ''
  const lang = info || ''
  const escaped = escapeHtml(String(token.content))
  const lines = escaped.split('\n')
  if (lines.length && lines[lines.length - 1] === '') lines.pop()
  const body = lines.map(l => `<span class="code-line">${l}</span>`).join('')
  const langLabel = lang ? `<span class="code-lang">${lang}</span>` : '<span class="code-lang is-empty"></span>'
  return `<div class="code-block" data-lang="${escapeHtml(lang)}"><div class="code-header">${langLabel}<button type="button" class="code-copy" aria-label="复制代码">复制</button></div><pre><code class="language-${escapeHtml(lang)}">${body}</code></pre></div>`
}

const makeEnv = (): MarkdownEnv => ({ toc: [], usedIds: {} })

// ---- 专业图标 + 自检清单增强 ----
const ICON_OK = '<svg class="md-ic md-ic-ok" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>'
const ICON_NO = '<svg class="md-ic md-ic-no" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>'
const ICON_TIP = '<svg class="md-ic md-ic-tip" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5"/><path d="M9 18h6"/><path d="M10 22h4"/></svg>'
const ICON_WARN = '<svg class="md-ic md-ic-warn" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>'
const ICON_CHECK_TABLE = '<svg class="md-ic md-ic-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>'
const ICON_X_TABLE = '<svg class="md-ic md-ic-x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>'
const ICON_CELEBRATE = '<svg class="md-ic md-ic-celebrate" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5.8 11.3 2 22l10.7-3.79"/><path d="M4 3h.01"/><path d="M22 8h.01"/><path d="M15 2h.01"/><path d="M22 20h.01"/><path d="m22 2-2.24.75a2.9 2.9 0 0 0-1.96 3.12c.1.86-.57 1.63-1.45 1.63h-.38c-.86 0-1.6.6-1.76 1.44L14 10"/><path d="m22 13-.82-.33c-.86-.34-1.82.2-1.98 1.11c-.11.7-.72 1.22-1.43 1.22H17"/><path d="m11 2 .33.82c.34.86-.2 1.82-1.11 1.98C9.52 4.9 9 5.52 9 6.23V7"/><path d="M11 13c1.93 1.93 2.83 4.17 2 5-.83.83-3.07-.07-5-2-1.93-1.93-2.83-4.17-2-5 .83-.83 3.07.07 5 2Z"/></svg>'
const ICON_FLAG = '<svg class="md-ic md-ic-flag" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" x2="4" y1="22" y2="15"/></svg>'

const enhanceMarkdown = (html: string) => {
  let out = html
  // 自检清单：- [ ] / [x] 任务项 → 可勾选 checkbox
  out = out.replace(/\[ \]/g, '<span class="task-check" data-check="0" role="checkbox" aria-checked="false" tabindex="0"><span class="task-check-box"></span></span>')
  out = out.replace(/\[x\]/gi, '<span class="task-check is-checked" data-check="1" role="checkbox" aria-checked="true" tabindex="0"><span class="task-check-box"></span></span>')
  // 状态/提示/警告图标（徽章样式）
  out = out.replace(/✅/g, `<span class="md-badge md-badge-ok">${ICON_OK}</span>`)
  out = out.replace(/❌/g, `<span class="md-badge md-badge-no">${ICON_NO}</span>`)
  out = out.replace(/💡/g, `<span class="md-badge md-badge-tip">${ICON_TIP}</span>`)
  out = out.replace(/⚠️/g, `<span class="md-badge md-badge-warn">${ICON_WARN}</span>`)
  out = out.replace(/⚠/g, `<span class="md-badge md-badge-warn">${ICON_WARN}</span>`)
  // 表格内轻量对勾/叉（无背景）
  out = out.replace(/✓/g, `<span class="md-cell-ic md-cell-ic-check">${ICON_CHECK_TABLE}</span>`)
  out = out.replace(/✗/g, `<span class="md-cell-ic md-cell-ic-x">${ICON_X_TABLE}</span>`)
  // 里程碑/完成
  out = out.replace(/🎉/g, `<span class="md-badge md-badge-celebrate">${ICON_CELEBRATE}</span>`)
  out = out.replace(/🏁/g, `<span class="md-badge md-badge-flag">${ICON_FLAG}</span>`)
  return out
}

export const useMarkdown = (source: string) => enhanceMarkdown(renderer.render(source || '', makeEnv()))

export const useMarkdownToc = (source: string): TocItem[] => {
  const env = makeEnv()
  renderer.render(source || '', env)
  return env.toc
}
