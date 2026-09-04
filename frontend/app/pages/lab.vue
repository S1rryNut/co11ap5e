<script setup lang="ts">
import { Braces, Check, Clipboard, CodeXml, FileKey2, GitCompareArrows, Hash } from '@lucide/vue'
import { sha256, sha384, sha512 } from '@noble/hashes/sha2.js'

type Tool = 'hash' | 'json' | 'certificate' | 'prompt'

const activeTool = ref<Tool>('hash')
const copied = ref('')
const tools = [
  { key: 'hash' as const, label: 'Hash / 编码', icon: Hash },
  { key: 'json' as const, label: 'JSON', icon: Braces },
  { key: 'certificate' as const, label: '证书', icon: FileKey2 },
  { key: 'prompt' as const, label: 'Prompt 对比', icon: GitCompareArrows }
]

const hashInput = ref('')
const hashAlgorithm = ref<'SHA-256' | 'SHA-384' | 'SHA-512'>('SHA-256')
const hashOutput = ref('')
const base64Output = ref('')
const hashError = ref('')

const bytesToHex = (bytes: Uint8Array) => Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
const bytesToBase64 = (bytes: Uint8Array) => {
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  return btoa(binary)
}

const calculateHash = () => {
  hashError.value = ''
  const bytes = new TextEncoder().encode(hashInput.value)
  const algorithms = { 'SHA-256': sha256, 'SHA-384': sha384, 'SHA-512': sha512 }
  hashOutput.value = bytesToHex(algorithms[hashAlgorithm.value](bytes))
  base64Output.value = bytesToBase64(bytes)
}

const decodeBase64 = () => {
  hashError.value = ''
  try {
    const binary = atob(hashInput.value.trim().replace(/-/g, '+').replace(/_/g, '/'))
    hashOutput.value = new TextDecoder().decode(Uint8Array.from(binary, char => char.charCodeAt(0)))
    base64Output.value = ''
  } catch {
    hashError.value = 'Base64 内容无效。'
  }
}

const jsonInput = ref('')
const jsonOutput = ref('')
const jsonError = ref('')
const formatJson = (compact = false) => {
  jsonError.value = ''
  try {
    jsonOutput.value = JSON.stringify(JSON.parse(jsonInput.value), null, compact ? 0 : 2)
  } catch (error) {
    jsonOutput.value = ''
    jsonError.value = error instanceof Error ? error.message : 'JSON 内容无效。'
  }
}

const certificateInput = ref('')
const certificateError = ref('')
const certificateInfo = ref<{ type: string, bytes: number, sha256: string } | null>(null)
const inspectCertificate = () => {
  certificateError.value = ''
  certificateInfo.value = null
  const match = certificateInput.value.trim().match(/^-----BEGIN ([A-Z0-9 ]+)-----([\s\S]+?)-----END \1-----$/)
  if (!match) {
    certificateError.value = '未识别到有效的 PEM 数据。'
    return
  }
  const [, pemType, pemBody] = match
  if (!pemType || !pemBody) return
  try {
    const binary = atob(pemBody.replace(/\s/g, ''))
    const bytes = Uint8Array.from(binary, char => char.charCodeAt(0))
    const digest = sha256(bytes)
    certificateInfo.value = {
      type: pemType,
      bytes: bytes.length,
      sha256: bytesToHex(digest).match(/.{2}/g)?.join(':').toUpperCase() || ''
    }
  } catch {
    certificateError.value = 'PEM 的 Base64 数据无效。'
  }
}

const promptA = ref('')
const promptB = ref('')
const promptStats = (value: string) => ({
  chars: value.length,
  lines: value ? value.split(/\r?\n/).length : 0,
  words: value.trim() ? value.trim().split(/\s+/).length : 0
})

const copyText = async (value: string, key: string) => {
  if (!value) return
  await navigator.clipboard.writeText(value)
  copied.value = key
  window.setTimeout(() => { if (copied.value === key) copied.value = '' }, 1500)
}

useSeoMeta({ title: '项目实验室', description: '折叠思维 的浏览器端工程工具集合。' })
</script>

<template>
  <div class="page-wrap section-pad lab-page">
    <header class="page-header content-wrap">
      <p class="eyebrow">Lab</p>
      <h1>项目实验室</h1>
      <p>面向日常开发与安全实践的浏览器端工具。</p>
    </header>

    <div class="content-wrap lab-layout">
      <nav class="lab-tabs" aria-label="工具选择">
        <button v-for="tool in tools" :key="tool.key" type="button" :class="{ active: activeTool === tool.key }" @click="activeTool = tool.key">
          <component :is="tool.icon" :size="18" />
          <span>{{ tool.label }}</span>
        </button>
      </nav>

      <section v-if="activeTool === 'hash'" class="lab-workspace">
        <header><div><p class="eyebrow">Encoder</p><h2>Hash / 编码转换</h2></div><CodeXml :size="25" /></header>
        <label class="lab-field wide">输入<textarea v-model="hashInput" rows="10" spellcheck="false" /></label>
        <div class="lab-controls">
          <label class="lab-field">算法<select v-model="hashAlgorithm"><option>SHA-256</option><option>SHA-384</option><option>SHA-512</option></select></label>
          <button class="primary-button" type="button" @click="calculateHash">计算</button>
          <button class="secondary-button" type="button" @click="decodeBase64">Base64 解码</button>
        </div>
        <p v-if="hashError" class="lab-error">{{ hashError }}</p>
        <div v-if="hashOutput" class="lab-result"><div><strong>结果</strong><button class="icon-button" type="button" title="复制结果" @click="copyText(hashOutput, 'hash')"><Check v-if="copied === 'hash'" :size="17" /><Clipboard v-else :size="17" /></button></div><code>{{ hashOutput }}</code></div>
        <div v-if="base64Output" class="lab-result"><div><strong>Base64</strong><button class="icon-button" type="button" title="复制 Base64" @click="copyText(base64Output, 'base64')"><Check v-if="copied === 'base64'" :size="17" /><Clipboard v-else :size="17" /></button></div><code>{{ base64Output }}</code></div>
      </section>

      <section v-else-if="activeTool === 'json'" class="lab-workspace">
        <header><div><p class="eyebrow">Formatter</p><h2>JSON 格式化</h2></div><Braces :size="25" /></header>
        <div class="lab-columns">
          <label class="lab-field">输入<textarea v-model="jsonInput" rows="18" spellcheck="false" /></label>
          <label class="lab-field">输出<textarea :value="jsonOutput" rows="18" readonly spellcheck="false" /></label>
        </div>
        <div class="lab-controls"><button class="primary-button" type="button" @click="formatJson(false)">格式化</button><button class="secondary-button" type="button" @click="formatJson(true)">压缩</button><button class="icon-button" type="button" title="复制结果" @click="copyText(jsonOutput, 'json')"><Check v-if="copied === 'json'" :size="17" /><Clipboard v-else :size="17" /></button></div>
        <p v-if="jsonError" class="lab-error">{{ jsonError }}</p>
      </section>

      <section v-else-if="activeTool === 'certificate'" class="lab-workspace">
        <header><div><p class="eyebrow">PEM Inspector</p><h2>证书检查</h2></div><FileKey2 :size="25" /></header>
        <label class="lab-field wide">PEM<textarea v-model="certificateInput" rows="14" spellcheck="false" /></label>
        <div class="lab-controls"><button class="primary-button" type="button" @click="inspectCertificate">检查</button></div>
        <p v-if="certificateError" class="lab-error">{{ certificateError }}</p>
        <dl v-if="certificateInfo" class="certificate-result"><div><dt>类型</dt><dd>{{ certificateInfo.type }}</dd></div><div><dt>DER 字节</dt><dd>{{ certificateInfo.bytes }}</dd></div><div><dt>SHA-256 指纹</dt><dd><code>{{ certificateInfo.sha256 }}</code></dd></div></dl>
      </section>

      <section v-else class="lab-workspace">
        <header><div><p class="eyebrow">Comparator</p><h2>Prompt 对比</h2></div><GitCompareArrows :size="25" /></header>
        <div class="lab-columns prompt-columns">
          <label class="lab-field">版本 A<textarea v-model="promptA" rows="18" /></label>
          <label class="lab-field">版本 B<textarea v-model="promptB" rows="18" /></label>
        </div>
        <div class="prompt-metrics"><div><strong>A</strong><span>{{ promptStats(promptA).chars }} 字符</span><span>{{ promptStats(promptA).words }} 词</span><span>{{ promptStats(promptA).lines }} 行</span></div><div><strong>B</strong><span>{{ promptStats(promptB).chars }} 字符</span><span>{{ promptStats(promptB).words }} 词</span><span>{{ promptStats(promptB).lines }} 行</span></div></div>
      </section>
    </div>
  </div>
</template>
