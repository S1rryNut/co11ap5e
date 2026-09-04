// v-reveal 滚动渐入指令：元素进入视口后从隐藏淡入上移
// 无 JS / 减少动态效果偏好时保持始终可见，不隐藏内容
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('reveal', {
    mounted(el: HTMLElement) {
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
      // 首屏已可见的元素直接显示，避免可见→隐藏→可见的闪烁
      const rect = el.getBoundingClientRect()
      if (rect.top < window.innerHeight && rect.bottom > 0) {
        el.classList.add('revealed')
        return
      }
      const io = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            el.classList.add('revealed')
            io.unobserve(el)
          }
        })
      }, { threshold: 0.12, rootMargin: '0px 0px -48px 0px' })
      ;(el as any).__reveal_io = io
      el.classList.add('reveal-init')
      io.observe(el)
    },
    unmounted(el: HTMLElement) {
      ;(el as any).__reveal_io?.disconnect?.()
    }
  })
})
