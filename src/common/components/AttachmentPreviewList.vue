<script lang="ts" setup>
import { getAttachmentLabel, isImageAttachment, parseAttachmentUrls } from "@@/utils/attachment"
import { Download, ZoomIn } from "@element-plus/icons-vue"

const props = defineProps<{
  urls?: string | string[] | null
}>()

const list = computed(() => parseAttachmentUrls(props.urls))
const imageUrls = computed(() => list.value.filter(isImageAttachment))
const previewVisible = ref(false)
const previewIndex = ref(0)

function openPreview(url: string) {
  const index = imageUrls.value.indexOf(url)
  previewIndex.value = index >= 0 ? index : 0
  previewVisible.value = true
}
</script>

<template>
  <div v-if="list.length" class="attachment-preview-list">
    <div v-for="(url, index) in list" :key="`${url}-${index}`" class="attachment-preview-list__item">
      <div v-if="isImageAttachment(url)" class="attachment-preview-list__thumb" @click="openPreview(url)">
        <img :src="url" :alt="getAttachmentLabel(url, index)">
        <div class="attachment-preview-list__mask">
          <el-icon>
            <ZoomIn />
          </el-icon>
        </div>
      </div>
      <div class="attachment-preview-list__meta">
        <span class="attachment-preview-list__name">{{ getAttachmentLabel(url, index) }}</span>
        <div class="attachment-preview-list__actions">
          <el-button v-if="isImageAttachment(url)" type="primary" link size="small" @click="openPreview(url)">
            查看
          </el-button>
          <el-link :href="url" target="_blank" rel="noopener noreferrer" type="primary" :underline="false">
            <el-icon class="mr-1">
              <Download />
            </el-icon>
            下载
          </el-link>
        </div>
      </div>
    </div>
  </div>
  <span v-else>-</span>

  <!-- 支持缩放、左右旋转（组件自动导入会带上样式） -->
  <el-image-viewer
    v-if="previewVisible && imageUrls.length"
    :url-list="imageUrls"
    :initial-index="previewIndex"
    teleported
    @close="previewVisible = false"
  />
</template>

<style lang="scss" scoped>
.attachment-preview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__thumb {
    position: relative;
    width: 72px;
    height: 72px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    border: 1px solid var(--el-border-color-lighter);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &__thumb:hover &__mask {
    opacity: 1;
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__name {
    font-size: 13px;
    color: var(--el-text-color-regular);
    word-break: break-all;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}
</style>
