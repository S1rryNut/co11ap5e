// 代码块复制按钮的共享事件处理（事件委托，兼容 v-html 注入的 HTML）
export function useCodeCopy() {
  const fallbackCopy = (text: string) => {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }

  const onContentClick = (e: MouseEvent) => {
    const target = e.target as HTMLElement
    const btn = target.closest<HTMLElement>('.code-copy')
    if (!btn || !e.currentTarget) return
    const codeEl = btn.closest<HTMLElement>('.code-block')?.querySelector<HTMLElement>('pre code')
    if (!codeEl) return
    const text = codeEl.innerText
    const feedback = () => {
      const prev = btn.textContent
      btn.classList.add('copied')
      btn.textContent = '已复制'
      setTimeout(() => { btn.textContent = prev; btn.classList.remove('copied') }, 1500)
    }
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(feedback).catch(() => { try { fallbackCopy(text); feedback() } catch { /* 忽略 */ } })
    } else {
      try { fallbackCopy(text); feedback() } catch { /* 忽略 */ }
    }
  }

  return { onContentClick }
}
