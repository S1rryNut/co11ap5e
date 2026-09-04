<script setup lang="ts">
// 兜底路由：未匹配的地址统一走品牌 404 页（error.vue）
throw createError({ statusCode: 404, statusMessage: 'Page not found', fatal: true })
</script>

<template>
  <div />
</template>
