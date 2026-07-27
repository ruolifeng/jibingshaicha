import type { ComputedRef } from "vue"
import { computed, reactive } from "vue"

/**
 * 表头 Excel 式筛选：按字段缓存服务端 distinct 值，打开下拉时异步加载。
 */
export function useColumnDistinct(fetchApi: (field: string) => Promise<string[]>) {
  const cache = reactive<Record<string, string[]>>({})
  const computedCache = new Map<string, ComputedRef<string[]>>()
  const loadingFields = new Set<string>()

  async function load(field: string) {
    if (loadingFields.has(field)) return
    loadingFields.add(field)
    try {
      cache[field] = await fetchApi(field)
    } catch {
      if (!cache[field]) {
        cache[field] = []
      }
    } finally {
      loadingFields.delete(field)
    }
  }

  function sourceValues(field: string): ComputedRef<string[]> {
    let ref = computedCache.get(field)
    if (!ref) {
      ref = computed(() => cache[field] ?? [])
      computedCache.set(field, ref)
    }
    return ref
  }

  /** 筛选范围变化时清空缓存，下次打开下拉重新拉取 */
  function clearCache() {
    Object.keys(cache).forEach((key) => {
      delete cache[key]
    })
  }

  return { load, sourceValues, clearCache }
}
