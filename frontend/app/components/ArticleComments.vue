<script setup lang="ts">
/**
 * giscus 评论组件（基于 GitHub Discussions）
 * 未配置时（repo/repoId/category/categoryId 任一为空）不渲染。
 * 主题跟随站点明暗模式。
 */
const props = defineProps<{ term: string }>()

const config = useRuntimeConfig().public.giscus
const enabled = computed(() => Boolean(config.repo && config.repoId && config.category && config.categoryId))
const { theme } = useTheme()

const host = ref<HTMLElement | null>(null)
let scriptEl: HTMLScriptElement | null = null

const giscusTheme = computed(() => (theme.value === 'dark' ? 'dark' : 'light'))

const mountScript = () => {
  if (!host.value || scriptEl || document.querySelector('script[data-giscus]')) return
  scriptEl = document.createElement('script')
  scriptEl.src = 'https://giscus.app/client.js'
  scriptEl.async = true
  scriptEl.crossOrigin = 'anonymous'
  scriptEl.dataset.giscus = ''
  scriptEl.dataset.repo = config.repo
  scriptEl.dataset.repoId = config.repoId
  scriptEl.dataset.category = config.category
  scriptEl.dataset.categoryId = config.categoryId
  scriptEl.dataset.mapping = 'pathname'
  scriptEl.dataset.strict = '0'
  scriptEl.dataset.reactionsEnabled = '1'
  scriptEl.dataset.emitMetadata = '0'
  scriptEl.dataset.inputPosition = 'top'
  scriptEl.dataset.theme = giscusTheme.value
  scriptEl.dataset.lang = 'zh-CN'
  host.value.appendChild(scriptEl)
}

const updateTheme = (t: string) => {
  const iframe = host.value?.querySelector<HTMLIFrameElement>('iframe.giscus-frame')
  iframe?.contentWindow?.postMessage({ giscus: { setConfig: { theme: t } } }, 'https://giscus.app')
}

watch(theme, (v) => updateTheme(v === 'dark' ? 'dark' : 'light'))

onMounted(() => {
  if (enabled.value) {
    mountScript()
    // giscus iframe 就绪后同步一次主题
    const timer = window.setInterval(() => {
      const iframe = host.value?.querySelector<HTMLIFrameElement>('iframe.giscus-frame')
      if (iframe) {
        updateTheme(giscusTheme.value)
        window.clearInterval(timer)
      }
    }, 500)
    onScopeDispose(() => window.clearInterval(timer))
  }
})
</script>

<template>
  <section v-if="enabled" class="article-comments" aria-label="文章评论">
    <h2>评论</h2>
    <div ref="host" />
  </section>
</template>
