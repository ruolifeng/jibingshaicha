<script lang="ts" setup>
/**
 * 图片附件上传组件（V15）
 *
 * 用途：首次入户随访、后续随访记录中，上传 2~6 张照片作为附件。
 *
 * 用法：
 *   <ImageUploader v-model="form.attachmentUrls" :min="2" :max="6" />
 *
 * v-model 绑定的 attachmentUrls：
 *   - 在表单中以 JSON 字符串形式存储：'["url1","url2",...]'
 *   - 也兼容直接传入 string[] 数组
 *   - 空值（null/""/[]）均合法
 */
import type { UploadFile, UploadProps } from "element-plus"
import { Plus, ZoomIn } from "@element-plus/icons-vue"
import { getToken } from "@@/utils/cache/cookies"

interface Props {
  /** v-model 绑定值：可以是 JSON 字符串或 string[] */
  modelValue?: string | string[] | null
  /** 最少张数（默认 0；推荐随访场景设 2）。仅做 UI 提示，不强制阻止保存 */
  min?: number
  /** 最多张数（默认 6） */
  max?: number
  /** 单张图片大小上限 MB（默认 10MB） */
  maxSizeMB?: number
  /** 是否禁用 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  min: 0,
  max: 6,
  maxSizeMB: 10,
  disabled: false
})

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void
}>()

const uploadAction = `${import.meta.env.VITE_BASE_URL}/file/upload`
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

/** 将 v-model 输入规范化为 string[] */
function parseValue(v: Props["modelValue"]): string[] {
  if (!v) return []
  if (Array.isArray(v)) return v.filter(Boolean)
  try {
    const parsed = JSON.parse(v)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    // 兼容历史脏数据：单个 URL 字符串
    return v ? [v] : []
  }
}

/** 内部维护的 fileList，结构对齐 el-upload */
const fileList = ref<{ name: string, url: string, uid?: number }[]>([])

/** 同步 v-model → fileList */
watch(
  () => props.modelValue,
  (v) => {
    const urls = parseValue(v)
    fileList.value = urls.map((url, idx) => ({
      name: extractFileName(url) || `图片${idx + 1}`,
      url,
      uid: idx
    }))
  },
  { immediate: true }
)

/** fileList → v-model（统一以 JSON 字符串发出） */
function emitChange() {
  const urls = fileList.value.map(f => f.url).filter(Boolean)
  emit("update:modelValue", urls.length ? JSON.stringify(urls) : "")
}

function extractFileName(url: string): string {
  try {
    const u = new URL(url, "http://x")
    const name = u.searchParams.get("name")
    if (name) return decodeURIComponent(name)
  } catch { /* ignore */ }
  return url.split("/").pop() || url
}

// ==================== 上传 hooks ====================

const beforeUpload: UploadProps["beforeUpload"] = (file) => {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("仅支持图片格式")
    return false
  }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 ${props.maxSizeMB}MB`)
    return false
  }
  if (fileList.value.length >= props.max) {
    ElMessage.warning(`最多上传 ${props.max} 张图片`)
    return false
  }
  return true
}

const onSuccess: UploadProps["onSuccess"] = (response, uploadFile) => {
  if (response.code === 200) {
    fileList.value.push({
      name: uploadFile.name,
      url: import.meta.env.VITE_BASE_URL + response.data,
      uid: uploadFile.uid
    })
    emitChange()
  } else {
    ElMessage.error(response.msg || "上传失败")
  }
}

const onError: UploadProps["onError"] = () => {
  ElMessage.error("图片上传失败，请重试")
}

function handleRemove(uploadFile: UploadFile) {
  const idx = fileList.value.findIndex(f => f.url === uploadFile.url || f.name === uploadFile.name)
  if (idx >= 0) {
    fileList.value.splice(idx, 1)
    emitChange()
  }
}

// ==================== 预览 ====================

const previewVisible = ref(false)
const previewUrl = ref("")

function handlePreview(uploadFile: UploadFile) {
  previewUrl.value = uploadFile.url || ""
  previewVisible.value = true
}

const canUpload = computed(() => !props.disabled && fileList.value.length < props.max)
</script>

<template>
  <div class="image-uploader">
    <el-upload
      :action="uploadAction"
      :headers="uploadHeaders"
      :file-list="fileList"
      list-type="picture-card"
      accept="image/*"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :disabled="disabled"
    >
      <template #default>
        <el-icon v-if="canUpload"><Plus /></el-icon>
      </template>
      <template #file="{ file }">
        <div class="upload-thumb">
          <img class="upload-thumb__img" :src="file.url" :alt="file.name">
          <div class="upload-thumb__mask">
            <el-icon class="upload-thumb__icon" @click.stop="handlePreview(file)">
              <ZoomIn />
            </el-icon>
            <el-icon class="upload-thumb__icon" @click.stop="handleRemove(file)">
              <i-ep-delete />
            </el-icon>
          </div>
        </div>
      </template>
    </el-upload>

    <div class="image-uploader__tip">
      <span>请上传 {{ min || 2 }}~{{ max }} 张图片</span>
      <span class="image-uploader__count">已上传 {{ fileList.length }} / {{ max }}</span>
    </div>

    <el-dialog v-model="previewVisible" title="" width="60vw" align-center>
      <img v-if="previewUrl" class="preview-img" :src="previewUrl" alt="preview">
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.image-uploader {
  width: 100%;

  &__tip {
    display: flex;
    justify-content: space-between;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    margin-top: 4px;
  }

  &__count {
    color: var(--el-color-primary);
  }
}

.upload-thumb {
  position: relative;
  width: 100%;
  height: 100%;

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 6px;
  }

  &__mask {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    transition: opacity 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 14px;
    border-radius: 6px;
  }

  &:hover &__mask {
    opacity: 1;
  }

  &__icon {
    color: #fff;
    font-size: 18px;
    cursor: pointer;
  }
}

.preview-img {
  width: 100%;
  height: auto;
  max-height: 80vh;
  object-fit: contain;
  display: block;
  margin: 0 auto;
}
</style>
