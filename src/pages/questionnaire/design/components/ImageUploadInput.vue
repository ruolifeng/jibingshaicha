<script lang="ts" setup>
/**
 * 图片上传输入框
 * 支持：
 *  - 点击选择本地文件
 *  - 拖拽图片文件到区域
 *  - 粘贴剪贴板中的图片（Ctrl+V / 截图后直接粘贴）
 *  - 展示已有图片预览（URL 或 base64 均可）
 * 输出：将图片转为 base64 dataURL 后通过 update:modelValue 抛出
 */
import { Upload } from "@element-plus/icons-vue"

const props = defineProps<{
  modelValue: string
  placeholder?: string
  /** 图片最大尺寸（MB），默认 2 */
  maxSizeMb?: number
}>()

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void
}>()

const MAX_MB = computed(() => props.maxSizeMb ?? 2)
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)

/** 读取 File 并转为 base64 */
function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    if (file.size > MAX_MB.value * 1024 * 1024) {
      reject(new Error(`图片不能超过 ${MAX_MB.value}MB`))
      return
    }
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error("读取文件失败"))
    reader.readAsDataURL(file)
  })
}

async function processFile(file: File) {
  if (!file.type.startsWith("image/")) {
    ElMessage.warning("请上传图片文件")
    return
  }
  try {
    const dataUrl = await fileToBase64(file)
    emit("update:modelValue", dataUrl)
  } catch (e: any) {
    ElMessage.error(e.message || "图片处理失败")
  }
}

// 点击选择
function handleClick() {
  fileInputRef.value?.click()
}
function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) processFile(file)
  // 重置 input 以便下次选同一文件也能触发
  if (fileInputRef.value) fileInputRef.value.value = ""
}

// 拖拽
function handleDragOver(e: DragEvent) {
  e.preventDefault()
  isDragOver.value = true
}
function handleDragLeave() {
  isDragOver.value = false
}
function handleDrop(e: DragEvent) {
  e.preventDefault()
  isDragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) processFile(file)
}

// 粘贴
function handlePaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of Array.from(items)) {
    if (item.type.startsWith("image/")) {
      const file = item.getAsFile()
      if (file) {
        processFile(file)
        break
      }
    }
  }
}

// 清除
function handleClear(e: MouseEvent) {
  e.stopPropagation()
  emit("update:modelValue", "")
}
</script>

<template>
  <div class="img-upload-wrap">
    <!-- 隐藏的 file input -->
    <input ref="fileInputRef" type="file" accept="image/*" style="display: none" @change="handleFileChange">

    <!-- 已有图片：展示预览 + 替换/清除操作 -->
    <div v-if="modelValue" class="img-preview-box">
      <img :src="modelValue" alt="预览" class="img-preview-img">
      <div class="img-preview-actions">
        <el-button size="small" type="primary" plain @click="handleClick">
          替换
        </el-button>
        <el-button size="small" type="danger" plain @click="handleClear">
          清除
        </el-button>
      </div>
    </div>

    <!-- 无图片：上传区域 -->
    <div
      v-else
      class="img-upload-zone"
      :class="{ 'is-dragover': isDragOver }"
      tabindex="0"
      @click="handleClick"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
      @paste="handlePaste"
      @keydown.enter="handleClick"
    >
      <el-icon size="22" class="upload-icon">
        <Upload />
      </el-icon>
      <div class="upload-hint">
        <span>拖拽 / 粘贴</span>
        <span class="upload-hint-sub">或点击选择图片（{{ maxSizeMb ?? 2 }}MB 以内）</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.img-upload-wrap {
  width: 100%;
}

.img-upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  padding: 16px 10px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;
  outline: none;

  &:hover,
  &:focus {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  &.is-dragover {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-8);
  }
}

.upload-icon {
  color: var(--el-color-primary-light-3);
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  pointer-events: none;
}

.upload-hint-sub {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.img-preview-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.img-preview-img {
  max-width: 100%;
  max-height: 120px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  object-fit: contain;
  background: #f9fafb;
}

.img-preview-actions {
  display: flex;
  gap: 8px;
}
</style>
