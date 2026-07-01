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

  &__title {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #1a3a6b;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  &__item {
    margin: 0;
    text-align: center;
    page-break-inside: avoid;
    break-inside: avoid;

    img {
      width: 100%;
      max-width: 180px;
      max-height: 140px;
      object-fit: contain;
      border: 1px solid #ddd;
      display: block;
      margin: 0 auto;
    }

    figcaption {
      margin-top: 4px;
      font-size: 12px;
      color: #606266;
    }
  }
}
</style>
