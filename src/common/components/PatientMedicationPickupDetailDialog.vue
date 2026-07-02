<script lang="ts" setup>
import { formatMedicationPickupDrugs, formatMedicationPickupQuantities } from "@@/utils/medicationPickup"

defineProps<{
  visible: boolean
  record: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`${patientName || ''} — 领药记录详情`"
    width="640px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-descriptions v-if="record" :column="1" border>
      <el-descriptions-item label="第几次领药">
        第 {{ record.pickupSeq }} 次
      </el-descriptions-item>
      <el-descriptions-item label="领取时间">
        {{ record.pickupTime || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="药品及用量">
        {{ formatMedicationPickupDrugs(record.drugs) || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="领取数量">
        {{ formatMedicationPickupQuantities(record.drugs, record.quantity, record.quantityUnit) || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="发药单位">
        {{ record.dispensingUnit || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="备注">
        {{ record.remarks || "-" }}
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
    </template>
  </el-dialog>
</template>
