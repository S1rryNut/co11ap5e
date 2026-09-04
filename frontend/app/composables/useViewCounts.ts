import { reactive } from 'vue'

// 文章阅读量：模块级共享响应式缓存 + 批量合并请求
// 同一渲染周期内对多个 path 的读取会合并成一次 /public/metrics/views 请求
const counts = reactive<Record<string, number>>({})
const loading = new Set<string>()
let queue: string[] = []
let timer: ReturnType<typeof setTimeout> | null = null

const flush = async () => {
  const paths = [...new Set(queue)]
  queue = []
  timer = null
  if (!paths.length) return
  try {
    const res = await useSiteApi().getViewCounts(paths)
    for (const p of paths) counts[p] = res[p] ?? 0
  } catch {
    for (const p of paths) counts[p] = 0
  } finally {
    for (const p of paths) loading.delete(p)
  }
}

export function useViewCounts(paths: string[]) {
  if (import.meta.client) {
    for (const p of paths) {
      if (!(p in counts) && !loading.has(p)) {
        loading.add(p)
        queue.push(p)
      }
    }
    if (queue.length && !timer) timer = setTimeout(flush, 80)
  }
  return counts
}
