<script setup lang="ts">
const props = withDefaults(defineProps<{
  phrases: string[]
  typeSpeed?: number
  eraseSpeed?: number
  holdMs?: number
}>(), {
  typeSpeed: 85,
  eraseSpeed: 42,
  holdMs: 1900
})

const text = ref('')
let timer: ReturnType<typeof setTimeout> | undefined
let canceled = false

const delay = (ms: number) => new Promise<void>((resolve) => { timer = setTimeout(resolve, ms) })

const typePhrase = (phrase: string) => new Promise<void>((resolve) => {
  let i = 0
  const step = () => {
    if (canceled) return resolve()
    i++
    text.value = phrase.slice(0, i)
    if (i < phrase.length) timer = setTimeout(step, props.typeSpeed)
    else timer = setTimeout(resolve, props.holdMs)
  }
  step()
})

const erasePhrase = () => new Promise<void>((resolve) => {
  let i = text.value.length
  const step = () => {
    if (canceled) return resolve()
    i--
    text.value = text.value.slice(0, i)
    if (i > 0) timer = setTimeout(step, props.eraseSpeed)
    else resolve()
  }
  step()
})

const loop = async () => {
  while (!canceled) {
    for (const phrase of props.phrases) {
      await typePhrase(phrase)
      if (canceled) return
      await erasePhrase()
      if (canceled) return
    }
  }
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    text.value = props.phrases[0] || ''
    return
  }
  loop()
})
onUnmounted(() => {
  canceled = true
  if (timer) clearTimeout(timer)
})
</script>

<template>
  <p class="typewriter" aria-live="polite">
    <span class="typewriter-text">{{ text }}</span><span class="typewriter-caret" aria-hidden="true" />
  </p>
</template>
