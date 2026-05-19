<script lang="ts" setup>
import { importSpecialDiseaseApi } from "./apis/index"

defineOptions({ name: "SpecialDiseaseImport" })

const uploading = ref(false)
const importResult = ref<{ count: number } | null>(null)

async function handleFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 格式的专病表文件")
    return
  }

  uploading.value = true
  importResult.value = null
  try {
    const { data } = await importSpecialDiseaseApi(file)
    importResult.value = { count: data as unknown as number }
    ElMessage.success(`导入成功，共创建 ${data} 条患者记录`)
  } catch {
    ElMessage.error("导入失败，请确认文件格式正确")
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="text-lg font-bold">专病网导入</span>
      </template>

      <el-alert
        type="info"
        :closable="false"
          title="上传专病/病案信息表（.xlsx），系统自动提取字段并创建患者记录，提取后可直接进入「填写通知单→首次随访→后续随访→服药管理」流程。"
        style="margin-bottom: 20px"
      />

      <el-descriptions :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="来源标签">
          <el-tag type="warning">专病网</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提取字段">
          患者姓名、身份证号、性别、出生日期、年龄、联系电话、人群分类、现详细住址、户籍地址、现管单位、诊断结果（病原学阴/阳性）
        </el-descriptions-item>
      </el-descriptions>

      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        drag
        style="width: 100%"
      >
        <el-icon style="font-size: 48px; color: #409eff; margin-bottom: 12px">
          <upload-filled />
        </el-icon>
        <div style="font-size: 16px; color: #606266">
          拖拽专病表文件到此处，或
          <span style="color: #409eff; cursor: pointer">点击上传</span>
        </div>
        <div style="font-size: 12px; color: #909399; margin-top: 8px">
          支持 .xlsx / .xls 格式
        </div>
      </el-upload>

      <div v-if="uploading" style="text-align: center; margin-top: 16px">
        <el-icon class="is-loading" style="font-size: 24px">
          <loading />
        </el-icon>
        <span style="margin-left: 8px; color: #606266">正在解析并导入，请稍候...</span>
      </div>
    </el-card>

    <el-card v-if="importResult" shadow="never">
      <template #header>
        <span class="text-lg font-bold">导入结果</span>
      </template>
      <el-result
        icon="success"
        :title="`成功导入 ${importResult.count} 条患者记录`"
        sub-title="导入的患者数据来源标签为「专病网」，可在「患者管理 → 通知单」页面进行后续操作。"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/patient-management/notice')">
            前往患者管理
          </el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>
