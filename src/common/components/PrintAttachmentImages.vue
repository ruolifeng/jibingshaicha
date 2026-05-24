<script lang="ts" setup>
import { isImageAttachment, parseAttachmentUrls } from "@@/utils/attachment"

const props = withDefaults(defineProps<{
  urls?: string | string[] | null
  title?: string
}>(), {
  title: "附件照片"
})

const images = computed(() => parseAttachmentUrls(props.urls).filter(isImageAttachment))
</script>

<template>
  <div v-if="images.length" class="print-attachments">
    <div class="print-attachments__title">
      {{ title }}
    </div>
    <div class="print-attachments__grid">
      <figure v-for="(url, index) in images" :key="`${url}-${index}`" class="print-attachments__item">
        <img :src="url" :alt="`${title}${index + 1}`">
        <figcaption>图{{ index + 1 }}</figcaption>
      </figure>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.print-attachments {
  margin-top: 12px;
  page-break-inside: avoid;

  &__title {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #1a3a6b;
  }

  &__grid {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  &__item {
    margin: 0;
    text-align: center;

    img {
      width: 180px;
      max-height: 140px;
      object-fit: contain;
      border: 1px solid #ddd;
      display: block;
    }

    figcaption {
      margin-top: 4px;
      font-size: 12px;
      color: #606266;
    }
  }
}
</style>
