<script lang="ts" setup>
/**
 * 电子签名画板
 * - 鼠标/触屏绘制
 * - 清除、撤销（撤销最后一笔）
 * - 完成后将 canvas 内容导出为 base64 dataURL，通过 update:modelValue 抛出
 * modelValue 为空串代表未签名，非空代表签名图像 dataURL
 */

const props = defineProps<{
  modelValue: string
  /** 笔触颜色，默认 #222222 */
  penColor?: string
  /** 笔触宽度，默认 2 */
  penWidth?: number
  /** 画板高度（px），默认 160 */
  height?: number
  /** 画板背景色，默认 #ffffff */
  bgColor?: string
  /** 只读（已签名后展示用） */
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const isDrawing = ref(false)
const hasDrawn = ref(false)

// 历史快照，用于撤销
const history = ref<ImageData[]>([])

const penColor = computed(() => props.penColor || "#222222")
const penWidth = computed(() => props.penWidth || 2)
const bgColor = computed(() => props.bgColor || "#ffffff")
const padHeight = computed(() => props.height || 160)

function getCtx() {
  return canvasRef.value?.getContext("2d") ?? null
}

function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext("2d")!
  // 使用设备像素比让签名在高分屏上清晰
  const dpr = window.devicePixelRatio || 1
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * dpr
  canvas.height = padHeight.value * dpr
  ctx.scale(dpr, dpr)
  ctx.fillStyle = bgColor.value
  ctx.fillRect(0, 0, rect.width, padHeight.value)

  // 如果已有签名值，绘回去
  if (props.modelValue) {
    const img = new Image()
    img.onload = () => ctx.drawImage(img, 0, 0, rect.width, padHeight.value)
    img.src = props.modelValue
    hasDrawn.value = true
  }
}

function getPos(e: MouseEvent | TouchEvent): { x: number, y: number } {
  const canvas = canvasRef.value!
  const rect = canvas.getBoundingClientRect()
  if (e instanceof TouchEvent) {
    const t = e.touches[0] || e.changedTouches[0]
    return { x: t.clientX - rect.left, y: t.clientY - rect.top }
  }
  return { x: (e as MouseEvent).clientX - rect.left, y: (e as MouseEvent).clientY - rect.top }
}

function saveSnapshot() {
  const ctx = getCtx()
  const canvas = canvasRef.value
  if (!ctx || !canvas) return
  history.value.push(ctx.getImageData(0, 0, canvas.width, canvas.height))
  // 最多保留 20 步
  if (history.value.length > 20) history.value.shift()
}

function startDraw(e: MouseEvent | TouchEvent) {
  if (props.readonly) return
  e.preventDefault()
  saveSnapshot()
  isDrawing.value = true
  const ctx = getCtx()
  if (!ctx) return
  const { x, y } = getPos(e)
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.strokeStyle = penColor.value
  ctx.lineWidth = penWidth.value
  ctx.lineCap = "round"
  ctx.lineJoin = "round"
}

function draw(e: MouseEvent | TouchEvent) {
  if (!isDrawing.value || props.readonly) return
  e.preventDefault()
  const ctx = getCtx()
  if (!ctx) return
  const { x, y } = getPos(e)
  ctx.lineTo(x, y)
  ctx.stroke()
  hasDrawn.value = true
}

function endDraw(e: MouseEvent | TouchEvent) {
  if (!isDrawing.value) return
  e.preventDefault()
  isDrawing.value = false
  exportValue()
}

function exportValue() {
  const canvas = canvasRef.value
  if (!canvas || !hasDrawn.value) return
  emit("update:modelValue", canvas.toDataURL("image/png"))
}

function clearSignature() {
  const canvas = canvasRef.value
  const ctx = getCtx()
  if (!ctx || !canvas) return
  const rect = canvas.getBoundingClientRect()
  ctx.fillStyle = bgColor.value
  ctx.fillRect(0, 0, rect.width, padHeight.value)
  hasDrawn.value = false
  history.value = []
  emit("update:modelValue", "")
}

function undo() {
  const ctx = getCtx()
  const canvas = canvasRef.value
  if (!ctx || !canvas || !history.value.length) return
  const prev = history.value.pop()!
  ctx.putImageData(prev, 0, 0)
  // 检查是否还有内容（通过判断历史栈是否清空）
  if (history.value.length === 0) {
    hasDrawn.value = false
    emit("update:modelValue", "")
  } else {
    exportValue()
  }
}

onMounted(() => {
  nextTick(initCanvas)
})

// 画板宽度可能变化时重新初始化
watch(() => props.modelValue, (v: string, old: string) => {
  if (v === old) return
  // 外部清空时重置画布
  if (!v) {
    const ctx = getCtx()
    const canvas = canvasRef.value
    if (!ctx || !canvas) return
    const rect = canvas.getBoundingClientRect()
    ctx.fillStyle = bgColor.value
    ctx.fillRect(0, 0, rect.width, padHeight.value)
    hasDrawn.value = false
    history.value = []
  }
})
</script>

<template>
  <div class="sig-pad-wrap">
    <canvas
      ref="canvasRef"
      class="sig-canvas"
      :style="{ height: `${padHeight}px`, cursor: readonly ? 'default' : 'crosshair' }"
      @mousedown="startDraw"
      @mousemove="draw"
      @mouseup="endDraw"
      @mouseleave="endDraw"
      @touchstart.prevent="startDraw"
      @touchmove.prevent="draw"
      @touchend.prevent="endDraw"
    />
    <div v-if="!readonly" class="sig-toolbar">
      <span class="sig-hint">{{ modelValue ? '已签名' : '请在上方区域手写签名' }}</span>
      <div style="display: flex; gap: 8px">
        <el-button size="small" plain :disabled="!history.length" @click="undo">
          撤销
        </el-button>
        <el-button size="small" type="danger" plain :disabled="!modelValue" @click="clearSignature">
          清除
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.sig-pad-wrap {
  width: 100%;
  border: 1.5px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background: v-bind(bgColor);
  user-select: none;
  touch-action: none;
}

.sig-canvas {
  display: block;
  width: 100%;
}

.sig-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: #fafafa;
}

.sig-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
