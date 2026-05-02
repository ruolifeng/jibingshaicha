import type { App, Directive } from "vue"
import { useUserStore } from "@/pinia/stores/user"

/**
 * @name 权限指令
 * @description 支持两种用法：
 *   v-permission="'latent:track'" — 单个权限编码
 *   v-permission="['latent:track', 'latent:referral']" — 拥有其中任一即可
 */
const permission: Directive = {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()

    if (!value) return

    let codes: string[]
    if (typeof value === "string") {
      codes = [value]
    } else if (Array.isArray(value)) {
      codes = value
    } else {
      throw new TypeError("v-permission 参数必须是字符串或字符串数组")
    }

    if (codes.length === 0) return

    const has = codes.some(code => userStore.hasPermission(code))
    if (!has) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function installPermissionDirective(app: App) {
  app.directive("permission", permission)
}
