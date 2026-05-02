<script setup lang="ts">
interface Props {
  accept?: string
  tip?: string
}

const props = withDefaults(defineProps<Props>(), {
  accept: ".xlsx,.xls",
  tip: "请上传 .xlsx 或 .xls 格式的 Excel 文件"
})

const emit = defineEmits<{
  upload: [file: File]
}>()

const loading = ref(false)

function handleChange(uploadFile: any) {
  const rawFile = uploadFile.raw as File
  if (!rawFile) return

  const isExcel = rawFile.name.endsWith(".xlsx") || rawFile.name.endsWith(".xls")
  if (!isExcel) {
    ElMessage.error("请上传 Excel 格式文件（.xlsx 或 .xls）")
    return
  }

  emit("upload", rawFile)
}
</script>

<template>
  <el-upload
    :accept="props.accept"
    :auto-upload="false"
    :show-file-list="false"
    :on-change="handleChange"
    :loading="loading"
  >
    <el-button type="primary">
      <el-icon class="mr-1">
        <Upload />
      </el-icon>
      上传 Excel
    </el-button>
    <template #tip>
      <div class="el-upload__tip">
        {{ props.tip }}
      </div>
    </template>
  </el-upload>
</template>
