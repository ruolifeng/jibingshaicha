<script lang="ts" setup>
import QRCode from "qrcode"
import type { QuestionnaireConfig, QuestionnaireField, QuestionnaireFieldGroup } from "@/common/constants/questionnaire"
import { FIELD_TYPE_LABELS, FIELD_TYPE_OPTIONS, QUESTIONNAIRE_CODE, SCREENING_FIELD_KEYS } from "@/common/constants/questionnaire"
import {
  exportQuestionnaireSubmissionsApi,
  getQuestionnaireConfigApi,
  getQuestionnaireSubmissionsApi,
  updateQuestionnaireConfigApi,
  updateQuestionnaireEnabledApi
} from "../apis/questionnaire"

defineOptions({ name: "QuestionnairePanel" })

const questionnaireCode = QUESTIONNAIRE_CODE

const loading = ref(false)
const saving = ref(false)
const config = ref<QuestionnaireConfig | null>(null)
const editMode = ref(false)
const editForm = reactive({
  title: "",
  subtitle: "",
  groups: [] as QuestionnaireFieldGroup[]
})

const qrCodeDataUrl = ref("")
const qrCodeVisible = ref(false)
const questionnaireUrl = computed(() => `${window.location.origin}/#/questionnaire/${questionnaireCode}`)

const submissionLoading = ref(false)
const submissionList = ref<Record<string, unknown>[]>([])
const submissionTotal = ref(0)
const submissionQuery = reactive({
  page: 1,
  size: 10,
  name: "",
  idNumber: ""
})

const fieldDialogVisible = ref(false)
const editingField = ref<QuestionnaireField | null>(null)
const editingGroupIndex = ref(-1)
const editingFieldIndex = ref(-1)
const fieldForm = reactive({
  key: "",
  label: "",
  type: "input" as QuestionnaireField["type"],
  required: false,
  optionsText: "",
  showWhenField: "",
  showWhenValue: ""
})

function cloneGroups(groups: QuestionnaireFieldGroup[]) {
  return JSON.parse(JSON.stringify(groups)) as QuestionnaireFieldGroup[]
}

async function loadConfig() {
  loading.value = true
  try {
    const { data } = await getQuestionnaireConfigApi(questionnaireCode)
    config.value = data
    editForm.title = data.title
    editForm.subtitle = data.subtitle
    editForm.groups = cloneGroups(data.groups)
  } finally {
    loading.value = false
  }
}

async function loadSubmissions() {
  submissionLoading.value = true
  try {
    const { data } = await getQuestionnaireSubmissionsApi(questionnaireCode, {
      page: submissionQuery.page,
      size: submissionQuery.size,
      name: submissionQuery.name || undefined,
      idNumber: submissionQuery.idNumber || undefined
    })
    submissionList.value = data.records || []
    submissionTotal.value = data.total || 0
  } finally {
    submissionLoading.value = false
  }
}

async function handleToggleEnabled(value: string | number | boolean) {
  const enabled = value === true
  saving.value = true
  try {
    await updateQuestionnaireEnabledApi(questionnaireCode, enabled)
    if (config.value) config.value.enabled = enabled
    ElMessage.success(`问卷已${enabled ? "开启" : "关闭"}`)
  } catch {
    if (config.value) config.value.enabled = !enabled
  } finally {
    saving.value = false
  }
}

function startEdit() {
  if (!config.value) return
  editForm.title = config.value.title
  editForm.subtitle = config.value.subtitle
  editForm.groups = cloneGroups(config.value.groups)
  editMode.value = true
}

function cancelEdit() {
  editMode.value = false
}

async function saveConfig() {
  if (!editForm.title.trim()) {
    ElMessage.warning("请填写问卷标题")
    return
  }
  const keys = editForm.groups.flatMap(g => g.fields.map(f => f.key))
  if (keys.length === 0) {
    ElMessage.warning("请至少保留一个问卷题目")
    return
  }
  if (new Set(keys).size !== keys.length) {
    ElMessage.warning("存在重复的字段标识，请修改后再保存")
    return
  }
  saving.value = true
  try {
    const payload: QuestionnaireConfig = {
      code: questionnaireCode,
      title: editForm.title.trim(),
      subtitle: editForm.subtitle.trim(),
      enabled: config.value?.enabled ?? true,
      populationType: config.value?.populationType || "school",
      groups: editForm.groups
    }
    await updateQuestionnaireConfigApi(questionnaireCode, payload)
    editMode.value = false
    ElMessage.success("问卷配置已保存")
    await loadConfig()
  } finally {
    saving.value = false
  }
}

function addGroup() {
  editForm.groups.push({ group: "新分组", fields: [] })
}

function removeGroup(index: number) {
  editForm.groups.splice(index, 1)
}

function openFieldDialog(groupIndex: number, fieldIndex?: number) {
  editingGroupIndex.value = groupIndex
  if (fieldIndex !== undefined) {
    editingFieldIndex.value = fieldIndex
    const field = editForm.groups[groupIndex].fields[fieldIndex]
    editingField.value = field
    fieldForm.key = field.key
    fieldForm.label = field.label
    fieldForm.type = field.type
    fieldForm.required = !!field.required
    fieldForm.optionsText = (field.options || []).join(" / ")
    fieldForm.showWhenField = field.showWhen?.field || ""
    fieldForm.showWhenValue = field.showWhen?.value || ""
  } else {
    editingFieldIndex.value = -1
    editingField.value = null
    fieldForm.key = ""
    fieldForm.label = ""
    fieldForm.type = "input"
    fieldForm.required = false
    fieldForm.optionsText = ""
    fieldForm.showWhenField = ""
    fieldForm.showWhenValue = ""
  }
  fieldDialogVisible.value = true
}

function saveField() {
  if (!fieldForm.key.trim() || !fieldForm.label.trim()) {
    ElMessage.warning("请填写字段标识和题目名称")
    return
  }
  if (!SCREENING_FIELD_KEYS.includes(fieldForm.key.trim())) {
    ElMessage.warning("字段标识需与筛查表字段一致，否则提交后无法入库统计")
    return
  }
  const duplicateKey = editForm.groups.some((group, gi) =>
    group.fields.some((f, fi) =>
      f.key === fieldForm.key.trim()
      && !(gi === editingGroupIndex.value && fi === editingFieldIndex.value)
    )
  )
  if (duplicateKey) {
    ElMessage.warning("字段标识已存在，请使用不同的标识")
    return
  }
  const field: QuestionnaireField = {
    key: fieldForm.key.trim(),
    label: fieldForm.label.trim(),
    type: fieldForm.type,
    required: fieldForm.required,
    options: fieldForm.type === "select"
      ? fieldForm.optionsText.split(/[/|，,]/).map(s => s.trim()).filter(Boolean)
      : undefined,
    showWhen: fieldForm.showWhenField && fieldForm.showWhenValue
      ? { field: fieldForm.showWhenField.trim(), value: fieldForm.showWhenValue.trim() }
      : undefined
  }
  const group = editForm.groups[editingGroupIndex.value]
  if (editingFieldIndex.value >= 0) {
    group.fields[editingFieldIndex.value] = field
  } else {
    group.fields.push(field)
  }
  fieldDialogVisible.value = false
}

function removeField(groupIndex: number, fieldIndex: number) {
  editForm.groups[groupIndex].fields.splice(fieldIndex, 1)
}

async function generateQRCode() {
  try {
    qrCodeDataUrl.value = await QRCode.toDataURL(questionnaireUrl.value, {
      width: 300,
      margin: 2,
      color: { dark: "#000000", light: "#ffffff" }
    })
    qrCodeVisible.value = true
  } catch {
    ElMessage.error("二维码生成失败")
  }
}

function downloadQRCode() {
  if (!qrCodeDataUrl.value) return
  const a = document.createElement("a")
  a.href = qrCodeDataUrl.value
  a.download = "学生筛查问卷二维码.png"
  a.click()
  ElMessage.success("二维码已下载")
}

async function handleExportSubmissions() {
  try {
    const blob = await exportQuestionnaireSubmissionsApi(questionnaireCode, {
      name: submissionQuery.name || undefined,
      idNumber: submissionQuery.idNumber || undefined
    })
    const url = URL.createObjectURL(new Blob([blob as BlobPart], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }))
    const a = document.createElement("a")
    a.href = url
    a.download = "问卷提交记录.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

function fieldCount(groups: QuestionnaireFieldGroup[]) {
  return groups.reduce((sum, g) => sum + g.fields.length, 0)
}

const displayGroups = computed(() => editMode.value ? editForm.groups : (config.value?.groups || []))

onMounted(async () => {
  await loadConfig()
  await loadSubmissions()
})
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16" class="mb-4">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <el-icon color="#409eff" size="18">
                  <Document />
                </el-icon>
                <span>学生筛查问卷管理</span>
              </div>
              <div class="card-actions">
                <el-button v-if="!editMode" type="primary" link @click="startEdit">
                  编辑问卷
                </el-button>
                <template v-else>
                  <el-button type="primary" :loading="saving" @click="saveConfig">
                    保存配置
                  </el-button>
                  <el-button @click="cancelEdit">
                    取消
                  </el-button>
                </template>
              </div>
            </div>
          </template>

          <el-form v-if="editMode" label-width="100px" class="mb-3">
            <el-form-item label="问卷标题">
              <el-input v-model="editForm.title" />
            </el-form-item>
            <el-form-item label="问卷说明">
              <el-input v-model="editForm.subtitle" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>

          <el-descriptions v-else :column="2" border>
            <el-descriptions-item label="问卷名称">
              {{ config?.title }}
            </el-descriptions-item>
            <el-descriptions-item label="适用对象">
              在校学生（学校人群）
            </el-descriptions-item>
            <el-descriptions-item label="问卷状态">
              <el-switch
                :model-value="config?.enabled"
                :loading="saving"
                active-text="开启填写"
                inactive-text="关闭填写"
                @change="handleToggleEnabled"
              />
            </el-descriptions-item>
            <el-descriptions-item label="问卷题目数量">
              {{ fieldCount(config?.groups || []) }} 题（{{ config?.groups?.length || 0 }} 组）
            </el-descriptions-item>
            <el-descriptions-item label="填写地址" :span="2">
              <el-tag type="info" class="mr-2">
                {{ questionnaireUrl }}
              </el-tag>
              <el-button size="small" type="primary" @click="generateQRCode">
                生成二维码
              </el-button>
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="!editMode && config && !config.enabled"
            title="问卷已关闭：扫描二维码后将看到「问卷填写已关闭」提示，无法提交数据。"
            type="warning"
            show-icon
            :closable="false"
            class="mt-3"
          />
          <el-alert
            v-else-if="!editMode"
            title="问卷已开启：个人可通过扫描二维码在手机上填写并提交，数据将自动进入统计分析。"
            type="success"
            show-icon
            :closable="false"
            class="mt-3"
          />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="qr-card">
          <template #header>
            <span class="font-bold">二维码预览</span>
          </template>
          <div class="qr-preview">
            <template v-if="qrCodeDataUrl">
              <img :src="qrCodeDataUrl" alt="问卷二维码" class="qr-image">
              <el-button type="primary" link size="small" @click="downloadQRCode">
                下载二维码
              </el-button>
            </template>
            <template v-else>
              <el-icon size="48" color="#dcdfe6">
                <Picture />
              </el-icon>
              <p class="qr-tip">
                点击「生成二维码」
              </p>
              <el-button type="primary" size="small" @click="generateQRCode">
                生成二维码
              </el-button>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb-4">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon color="#67c23a" size="18">
              <List />
            </el-icon>
            <span>{{ editMode ? "编辑问卷字段" : "问卷字段预览" }}</span>
          </div>
          <el-button v-if="editMode" type="primary" size="small" @click="addGroup">
            新增分组
          </el-button>
        </div>
      </template>

      <div v-for="(group, groupIndex) in displayGroups" :key="`${group.group}-${groupIndex}`" class="field-group">
        <div class="group-title-row">
          <el-input
            v-if="editMode"
            v-model="group.group"
            size="small"
            style="width: 240px"
          />
          <span v-else class="group-title">{{ group.group }}</span>
          <div v-if="editMode" class="group-actions">
            <el-button type="primary" link size="small" @click="openFieldDialog(groupIndex)">
              新增题目
            </el-button>
            <el-button type="danger" link size="small" @click="removeGroup(groupIndex)">
              删除分组
            </el-button>
          </div>
        </div>

        <el-table :data="group.fields" border size="small">
          <el-table-column prop="label" label="题目" min-width="180" />
          <el-table-column prop="key" label="字段标识" width="160" />
          <el-table-column label="输入类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'select' ? 'primary' : row.type === 'date' ? 'success' : 'info'">
                {{ FIELD_TYPE_LABELS[row.type] || row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="是否必填" width="100">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                {{ row.required ? "必填" : "选填" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="选项值" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.options">{{ row.options.join(" / ") }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="editMode" label="操作" width="120" fixed="right">
            <template #default="{ $index }">
              <el-button type="primary" link size="small" @click="openFieldDialog(groupIndex, $index)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click="removeField(groupIndex, $index)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon color="#e6a23c" size="18">
              <Tickets />
            </el-icon>
            <span>问卷提交记录</span>
          </div>
          <el-button type="success" size="small" @click="handleExportSubmissions">
            导出 Excel
          </el-button>
        </div>
      </template>

      <el-form :model="submissionQuery" inline class="mb-3">
        <el-form-item label="姓名">
          <el-input v-model="submissionQuery.name" clearable placeholder="姓名筛选" style="width: 140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="submissionQuery.idNumber" clearable placeholder="证件号筛选" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submissionQuery.page = 1; loadSubmissions()">
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="submissionLoading" :data="submissionList" border stripe max-height="420">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="idNumber" label="证件号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="schoolName" label="学校名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="district" label="区县" width="100" />
        <el-table-column prop="infectionResult" label="感染筛查结果" min-width="120" show-overflow-tooltip />
        <el-table-column prop="chestXrayResult" label="胸片结果" width="100" />
        <el-table-column prop="createTime" label="提交时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="submissionQuery.page"
          v-model:page-size="submissionQuery.size"
          :total="submissionTotal"
          layout="total, prev, pager, next"
          @current-change="loadSubmissions"
          @size-change="loadSubmissions"
        />
      </div>
    </el-card>

    <el-dialog v-model="qrCodeVisible" title="学生筛查问卷二维码" width="400px" align-center>
      <div class="qr-dialog">
        <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" alt="问卷二维码" class="qr-dialog-image">
        <p class="qr-dialog-tip">
          扫码后可在手机端填写并提交筛查问卷
        </p>
        <p class="qr-dialog-url">
          {{ questionnaireUrl }}
        </p>
      </div>
      <template #footer>
        <el-button type="primary" @click="downloadQRCode">
          下载二维码
        </el-button>
        <el-button @click="qrCodeVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldDialogVisible" :title="editingFieldIndex >= 0 ? '编辑题目' : '新增题目'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="字段标识">
          <el-input v-model="fieldForm.key" placeholder="如 name、phone、schoolName" />
          <p class="field-key-tip">
            需使用筛查表字段：{{ SCREENING_FIELD_KEYS.join("、") }}
          </p>
        </el-form-item>
        <el-form-item label="题目名称">
          <el-input v-model="fieldForm.label" placeholder="显示给填写者的题目" />
        </el-form-item>
        <el-form-item label="输入类型">
          <el-select v-model="fieldForm.type" style="width: 100%">
            <el-option v-for="opt in FIELD_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="fieldForm.required" />
        </el-form-item>
        <el-form-item v-if="fieldForm.type === 'select'" label="选项值">
          <el-input v-model="fieldForm.optionsText" placeholder="多个选项用 / 分隔，如：有 / 无" />
        </el-form-item>
        <el-form-item label="条件显示">
          <div class="show-when-row">
            <el-input v-model="fieldForm.showWhenField" placeholder="依赖字段，如 hasChestXray" />
            <span>=</span>
            <el-input v-model="fieldForm.showWhenValue" placeholder="显示条件值，如 是" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fieldDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="saveField">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mt-3 { margin-top: 12px; }
.mr-2 { margin-right: 8px; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.qr-card {
  height: 100%;
}

.qr-preview {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.qr-image {
  width: 160px;
  height: 160px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.qr-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.field-group {
  margin-bottom: 24px;
}

.group-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.group-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  background: #f5f7fa;
  padding: 8px 12px;
  border-left: 4px solid #409eff;
  border-radius: 0 4px 4px 0;
  flex: 1;
}

.group-actions {
  display: flex;
  gap: 8px;
  margin-left: 12px;
}

.text-muted {
  color: #c0c4cc;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.qr-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-dialog-image {
  width: 280px;
  height: 280px;
}

.qr-dialog-tip {
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.qr-dialog-url {
  font-size: 12px;
  color: #c0c4cc;
  word-break: break-all;
  text-align: center;
}

.show-when-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.field-key-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.field-key-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
