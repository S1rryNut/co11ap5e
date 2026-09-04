<script setup lang="ts">
import { GitFork as Github, Mail, Rss } from '@lucide/vue'
</script>

<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div>
        <strong>折叠思维</strong>
        <p>记录构建、学习和持续思考。</p>
      </div>
      <div class="footer-links">
        <a href="mailto:hello@co11ap5e.example" aria-label="电子邮箱" title="电子邮箱"><Mail :size="18" /></a>
        <a href="https://github.com" target="_blank" rel="noreferrer" aria-label="GitHub" title="GitHub"><Github :size="18" /></a>
        <a href="/rss.xml" aria-label="RSS" title="RSS"><Rss :size="18" /></a>
      </div>
    </div>
    <div class="footer-meta"><span>© {{ new Date().getFullYear() }} 折叠思维 · 内容保留署名权</span><span class="footer-signature">Co11ap5e · Folded to unfold better.</span></div>
  </footer>
</template>
