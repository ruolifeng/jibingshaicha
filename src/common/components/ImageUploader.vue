<script lang="ts" setup>
/**
 * 图片附件上传组件（V15）
 *
 * 用途：首次入户随访、后续随访记录、督导表等，上传最多 10 张照片作为附件。
 *
 * 用法：
 *   <ImageUploader v-model="form.attachmentUrls" />
 *
 * v-model 绑定的 attachmentUrls：
 *   - 在表单中以 JSON 字符串形式存储：'["url1","url2",...]'
 *   - 也兼容直接传入 string[] 数组
 *   - 空值（null/""/[]）均合法
 */
import type { UploadFile, UploadFiles, UploadProps, UploadRawFile, UploadRequestOptions } from "element-plus"
import { parseAttachmentUrls, parseUploadApiResponse, uploadAttachmentFile } from "@@/utils/attachment"
import { getToken } from "@@/utils/cache/cookies"
import { Plus, ZoomIn } from "@element-plus/icons-vue"

interface Props {
  /** v-model 绑定值：可以是 JSON 字符串或 string[] */
  modelValue?: string | string[] | null
  /** 最少张数（默认 0；推荐随访场景设 2）。仅做 UI 提示，不强制阻止保存 */
  min?: number
  /** 最多张数（默认 10） */
  max?: number
  /** 单张图片大小上限 MB（默认 10MB） */
  maxSizeMB?: number
  /** 是否禁用 */
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  min: 0,
  max: 10,
  maxSizeMB: 10,
  disabled: false
})

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void
}>()

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

function handleHttpUpload(options: UploadRequestOptions) {
  return uploadAttachmentFile(options)
}

interface LocalFile { name: string, url: string, uid: number }

/** 内部维护的 fileList，结构对齐 el-upload */
const fileList = ref<LocalFile[]>([])

function toLocalFile(file: UploadFile): LocalFile {
  return {
    name: file.name,
    url: file.url || "",
    uid: file.uid
  }
}

function sameUrlList(a: string[], b: string[]) {
  return a.length === b.length && a.every((url, index) => url === b[index])
}

/** 是否为已落库的服务端 URL（排除 picture-card 预览用的 blob:） */
function isPersistedUrl(url?: string) {
  return Boolean(url) && !url!.startsWith("blob:")
}

/** 已上传成功的张数 */
function uploadedCount() {
  return fileList.value.filter(f => isPersistedUrl(f.url)).length
}

/** 当前占用的上传位（含上传中占位，不含已剔除的失败项） */
function usedSlotCount() {
  return fileList.value.length
}

/** 同步 v-model → fileList（仅在外部值变化时更新，避免覆盖上传中的列表） */
watch(
  () => props.modelValue,
  (v) => {
    const urls = parseAttachmentUrls(v)
    const pendingFiles = fileList.value.filter(f => !isPersistedUrl(f.url))
    const persistedFiles = fileList.value.filter(f => isPersistedUrl(f.url))
    const persistedUrls = persistedFiles.map(f => f.url)

    if (pendingFiles.length > 0) {
      if (sameUrlList(urls, persistedUrls)) return
      fileList.value = [
        ...urls.map((url, idx) => {
          const existing = persistedFiles.find(f => f.url === url)
          return existing || {
            name: url.split("/").pop()?.split("?")[0] || `图片${idx + 1}`,
            url,
            uid: Date.now() + idx
          }
        }),
        ...pendingFiles
      ]
      return
    }

    if (sameUrlList(urls, persistedUrls)) return
    fileList.value = urls.map((url, idx) => ({
      name: url.split("/").pop()?.split("?")[0] || `图片${idx + 1}`,
      url,
      uid: Date.now() + idx
    }))
  },
  { immediate: true }
)

/** fileList → v-model（统一以 JSON 字符串发出；上传中暂不回写，避免清空列表） */
function emitChange(force = false) {
  const urls = fileList.value.map(f => f.url).filter(isPersistedUrl)
  const hasPending = fileList.value.some(f => !isPersistedUrl(f.url))
  if (!force && hasPending) return
  emit("update:modelValue", urls.length ? JSON.stringify(urls) : "")
}

/** 刷新展示列表：保留上传中/成功的文件，仅排除失败项 */
function refreshDisplayFiles(uploadFiles: UploadFiles) {
  fileList.value = uploadFiles
    .filter(file => file.status !== "fail")
    .map(toLocalFile)
}

// ==================== 上传 hooks ====================

const beforeUpload = ((file: UploadRawFile, uploadFiles?: UploadFiles) => {
  const isImage = file.type.startsWith("image/")
    || /\.(png|jpe?g|gif|webp|bmp|svg|heic|heif)$/i.test(file.name)
  if (!isImage) {
    ElMessage.error("仅支持图片格式")
    return false
  }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 ${props.maxSizeMB}MB`)
    return false
  }
  const persisted = uploadedCount()
  const batch = uploadFiles ?? []
  const indexInBatch = batch.findIndex(f => f.uid === file.uid)
  const batchIndex = indexInBatch >= 0 ? indexInBatch : batch.length
  if (persisted + batchIndex + 1 > props.max) {
    if (batchIndex === 0 && persisted >= props.max) {
      ElMessage.warning(`最多上传 ${props.max} 张图片`)
    }
    return false
  }
  return true
}) as UploadProps["beforeUpload"]

const onExceed: UploadProps["onExceed"] = () => {
  ElMessage.warning(`最多上传 ${props.max} 张图片`)
}

const onSuccess: UploadProps["onSuccess"] = (response, uploadFile, uploadFiles) => {
  const result = parseUploadApiResponse(response)
  if (result.ok && result.url) {
    uploadFile.url = result.url
    uploadFile.status = "success"
    refreshDisplayFiles(uploadFiles)
    emitChange()
    return
  }
  uploadFile.status = "fail"
  refreshDisplayFiles(uploadFiles)
  emitChange()
  ElMessage.error(result.msg || "上传失败")
}

const onChange: UploadProps["onChange"] = (_uploadFile, uploadFiles) => {
  refreshDisplayFiles(uploadFiles)
}

const onError: UploadProps["onError"] = (_error, _uploadFile, uploadFiles) => {
  refreshDisplayFiles(uploadFiles)
  emitChange()
  ElMessage.error("图片上传失败，请重试")
}

function handleRemove(uploadFile: UploadFile) {
  fileList.value = fileList.value.filter(f => f.uid !== uploadFile.uid && f.url !== uploadFile.url)
  emitChange(true)
}

// ==================== 预览 ====================

const previewVisible = ref(false)
const previewIndex = ref(0)

const previewUrlList = computed(() =>
  fileList.value.map(f => f.url).filter((url): url is string => isPersistedUrl(url) || Boolean(url))
)

function handlePreview(uploadFile: UploadFile | LocalFile) {
  const url = uploadFile.url || ""
  if (!url) return
  const index = previewUrlList.value.indexOf(url)
  previewIndex.value = index >= 0 ? index : 0
  previewVisible.value = true
}

const canUpload = computed(() => !props.disabled && usedSlotCount() < props.max)
</script>

<template>
  <div class="image-uploader">
    <el-upload
      :http-request="handleHttpUpload"
      :headers="uploadHeaders"
      :file-list="fileList"
      :limit="max"
      multiple
      list-type="picture-card"
      accept="image/*"
      name="file"
      :before-upload="beforeUpload"
      :on-exceed="onExceed"
      :on-success="onSuccess"
      :on-change="onChange"
      :on-error="onError"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :disabled="disabled"
    >
      <template #default>
        <el-icon v-if="canUpload">
          <Plus />
        </el-icon>
      </template>
      <template #file="{ file }">
        <div class="upload-thumb">
          <img v-if="file.url" class="upload-thumb__img" :src="file.url" :alt="file.name">
          <div v-else class="upload-thumb__placeholder">
            上传中...
          </div>
          <div v-if="file.url" class="upload-thumb__mask">
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
      <span>请上传 {{ max }} 张图片（支持批量选择）</span>
      <span class="image-uploader__count">已上传 {{ uploadedCount() }} / {{ max }}</span>
    </div>

    <el-image-viewer
      v-if="previewVisible && previewUrlList.length"
      :url-list="previewUrlList"
      :initial-index="previewIndex"
      teleported
      @close="previewVisible = false"
    />
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

  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
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
</style>
