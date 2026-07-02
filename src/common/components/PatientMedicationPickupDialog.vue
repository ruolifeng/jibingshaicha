<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import { MEDICATION_PICKUP_DRUG_OPTIONS, MEDICATION_PICKUP_UNIT_OPTIONS } from "@@/constants/disease"
import { MEDICATION_PICKUP_EDIT_DAYS_LEVEL5, parseMedicationPickupDrugs } from "@@/utils/medicationPickup"
import { saveMedicationPickupApi } from "@/pages/patient-management/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
  initialData?: Record<string, any> | null
}>()
const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()
const CUSTOM_DRUG_VALUE = "__custom__"
let drugRowIdSeed = 0

interface DrugRow {
  id: number
  selectValue: string
  customName: string
  dosage: string
  quantity: number | null
  quantityUnit: string
}

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const isEditMode = computed(() => !!props.initialData?.id)
const formLocked = computed(() =>
  isEditMode.value && props.initialData?.editable === false
)

const dialogTitle = computed(() => {
  const suffix = props.patientRow?.name ? ` — ${props.patientRow.name}` : ""
  if (isEditMode.value) {
    return formLocked.value ? `查看领药记录${suffix}` : `修改领药记录${suffix}`
  }
  return `填写领药记录${suffix}`
})

const pickupForm = reactive({
  pickupTime: "",
  dispensingUnit: "",
  remarks: "",
  drugRows: [] as DrugRow[]
})

const rules: FormRules = {
  pickupTime: [{ required: true, message: "请选择领取时间", trigger: "change" }],
  dispensingUnit: [{ required: true, message: "请填写发药单位", trigger: "blur" }]
}

function createEmptyDrugRow(): DrugRow {
  return { id: ++drugRowIdSeed, selectValue: "", customName: "", dosage: "", quantity: null, quantityUnit: "" }
}

function resetForm() {
  pickupForm.pickupTime = ""
  pickupForm.dispensingUnit = userStore.orgName || ""
  pickupForm.remarks = ""
  pickupForm.drugRows = [createEmptyDrugRow()]
}

function resolveDrugName(row: DrugRow) {
  if (row.selectValue === CUSTOM_DRUG_VALUE) {
    return row.customName.trim()
  }
  return row.selectValue.trim()
}

function buildDrugsPayload() {
  return pickupForm.drugRows
    .map(row => ({
      name: resolveDrugName(row),
      dosage: row.dosage.trim(),
      quantity: row.quantity,
      quantityUnit: row.quantityUnit
    }))
    .filter(item => item.name && item.dosage)
}

function validateDrugRows(): boolean {
  if (!pickupForm.drugRows.length) {
    ElMessage.warning("请至少添加一种药品")
    return false
  }
  for (let i = 0; i < pickupForm.drugRows.length; i++) {
    const row = pickupForm.drugRows[i]
    const name = resolveDrugName(row)
    if (!name) {
      ElMessage.warning(`请选择或填写第 ${i + 1} 种药品名称`)
      return false
    }
    if (!row.dosage.trim()) {
      ElMessage.warning(`请填写第 ${i + 1} 种药品用量`)
      return false
    }
    if (row.quantity == null || row.quantity <= 0) {
      ElMessage.warning(`请填写第 ${i + 1} 种药品的领取数量`)
      return false
    }
    if (!row.quantityUnit) {
      ElMessage.warning(`请选择第 ${i + 1} 种药品的领取数量单位`)
      return false
    }
  }
  return true
}

function fillFromInitial(data: Record<string, any>) {
  resetForm()
  pickupForm.pickupTime = data.pickupTime || ""
  pickupForm.dispensingUnit = data.dispensingUnit || userStore.orgName || ""
  pickupForm.remarks = data.remarks || ""
  const legacyQuantity = data.quantity != null ? Number(data.quantity) : null
  const legacyQuantityUnit = data.quantityUnit || ""
  const drugs = parseMedicationPickupDrugs(data.drugs)
  pickupForm.drugRows = drugs.length
    ? drugs.map((item, index) => ({
        id: ++drugRowIdSeed,
        selectValue: MEDICATION_PICKUP_DRUG_OPTIONS.includes(item.name) ? item.name : CUSTOM_DRUG_VALUE,
        customName: MEDICATION_PICKUP_DRUG_OPTIONS.includes(item.name) ? "" : item.name,
        dosage: item.dosage,
        quantity: item.quantity ?? (index === 0 ? legacyQuantity : null),
        quantityUnit: item.quantityUnit || (index === 0 ? legacyQuantityUnit : "")
      }))
    : [createEmptyDrugRow()]
}

watch(
  () => props.visible,
  async (val) => {
    if (!val) return
    if (props.initialData) {
      fillFromInitial(props.initialData)
    } else {
      resetForm()
    }
    nextTick(() => formRef.value?.clearValidate())
  }
)

function close() {
  emit("update:visible", false)
}

async function handleSave() {
  if (!props.patientRow || formLocked.value || saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !validateDrugRows()) return

  saving.value = true
  try {
    const drugs = buildDrugsPayload()
    const firstDrug = drugs[0]
    await saveMedicationPickupApi({
      id: props.initialData?.id,
      patientId: props.patientRow.id,
      pickupTime: pickupForm.pickupTime,
      quantity: firstDrug?.quantity ?? null,
      quantityUnit: firstDrug?.quantityUnit ?? "",
      dispensingUnit: pickupForm.dispensingUnit.trim(),
      remarks: pickupForm.remarks.trim(),
      drugs: JSON.stringify(drugs)
    })
    ElMessage.success(isEditMode.value ? "领药记录已更新" : "领药记录已保存")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

function addDrugRow() {
  pickupForm.drugRows.push(createEmptyDrugRow())
}

function removeDrugRow(index: number) {
  if (pickupForm.drugRows.length <= 1) {
    ElMessage.warning("至少保留一种药品")
    return
  }
  pickupForm.drugRows.splice(index, 1)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="760px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-alert
      v-if="formLocked"
      type="warning"
      :closable="false"
      show-icon
      :title="`领药记录已超过 ${MEDICATION_PICKUP_EDIT_DAYS_LEVEL5} 天修改期限，仅可查看。如需修改请联系上级管理员。`"
      style="margin-bottom: 16px"
    />

    <el-form
      ref="formRef"
      :model="pickupForm"
      :rules="rules"
      label-width="110px"
      :disabled="formLocked"
    >
      <el-divider content-position="left">
        一、结核药品名称及用量
      </el-divider>

      <div v-for="(row, index) in pickupForm.drugRows" :key="row.id" class="drug-row">
        <el-form-item :label="`药品 ${index + 1}`" required>
          <div class="drug-row-content">
            <el-select
              v-model="row.selectValue"
              placeholder="选择药品"
              filterable
              style="width: 220px"
            >
              <el-option
                v-for="item in MEDICATION_PICKUP_DRUG_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
              <el-option label="手动输入其他药品" :value="CUSTOM_DRUG_VALUE" />
            </el-select>
            <el-input
              v-if="row.selectValue === CUSTOM_DRUG_VALUE"
              v-model="row.customName"
              placeholder="请输入药品名称"
              style="width: 180px"
            />
            <el-input
              v-model="row.dosage"
              placeholder="用量（如 4片/日）"
              style="width: 180px"
            />
            <el-button
              v-if="!formLocked"
              type="danger"
              link
              @click="removeDrugRow(index)"
            >
              删除
            </el-button>
          </div>
        </el-form-item>
      </div>

      <el-form-item v-if="!formLocked">
        <el-button type="primary" link @click="addDrugRow">
          + 添加药品
        </el-button>
      </el-form-item>

      <el-divider content-position="left">
        二、领取数量
      </el-divider>
      <div v-for="(row, index) in pickupForm.drugRows" :key="`quantity-${row.id}`">
        <el-form-item :label="resolveDrugName(row) || `药品 ${index + 1}`" required>
          <div class="quantity-row">
            <el-input-number
              v-model="row.quantity"
              :min="0.01"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 160px"
            />
            <el-select
              v-model="row.quantityUnit"
              placeholder="单位"
              style="width: 120px"
            >
              <el-option
                v-for="item in MEDICATION_PICKUP_UNIT_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </div>
        </el-form-item>
      </div>

      <el-divider content-position="left">
        三、领取时间
      </el-divider>
      <el-form-item label="领取时间" prop="pickupTime">
        <el-date-picker
          v-model="pickupForm.pickupTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 100%"
        />
      </el-form-item>

      <el-divider content-position="left">
        四、发药单位
      </el-divider>
      <el-form-item label="发药单位" prop="dispensingUnit">
        <el-input v-model="pickupForm.dispensingUnit" placeholder="默认当前录入单位" />
      </el-form-item>

      <el-divider content-position="left">
        五、备注
      </el-divider>
      <el-form-item label="备注">
        <el-input
          v-model="pickupForm.remarks"
          type="textarea"
          :rows="3"
          placeholder="特殊情况说明"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">
        关闭
      </el-button>
      <el-button
        v-if="!formLocked"
        type="primary"
        :loading="saving"
        @click="handleSave"
      >
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.drug-row-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.quantity-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
