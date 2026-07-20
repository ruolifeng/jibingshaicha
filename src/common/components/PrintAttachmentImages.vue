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

<!-- 样式由 print-forms.css 提供，与实际打印一致 -->
