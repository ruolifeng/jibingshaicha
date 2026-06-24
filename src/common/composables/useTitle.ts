import { APP_TITLE } from "@@/constants/app"

/** 项目标题（统一使用品牌常量，不依赖 .env 旧值） */
const VITE_APP_TITLE = APP_TITLE

/** 动态标题 */
const dynamicTitle = ref<string>("")

/** 设置标题 */
function setTitle(title?: string) {
  dynamicTitle.value = title ? `${VITE_APP_TITLE} | ${title}` : VITE_APP_TITLE
}

// 监听标题变化
watch(dynamicTitle, (value, oldValue) => {
  if (document && value !== oldValue) {
    document.title = value
  }
})

/** 标题 Composable */
export function useTitle() {
  return { setTitle }
}
