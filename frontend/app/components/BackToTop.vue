<script setup lang="ts">
import { ArrowUp } from '@lucide/vue'

const visible = ref(false)
const onScroll = () => { visible.value = window.scrollY > 560 }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <button class="back-to-top" :class="{ visible }" type="button" aria-label="回到顶部" title="回到顶部" @click="scrollToTop">
    <ArrowUp :size="20" />
  </button>
</template>
