<!--
  头像裁剪组件
  支持图片上传、缩放/拖动裁剪与实时预览，
  对文件类型、体积做前置校验后通过 canvas 输出裁剪结果 Blob，
  供个人资料设置页上传新头像使用。
-->
<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {resolveAvatarUrl} from '@/utils/avatar'

const props = defineProps<{
  currentAvatar?: string | null
}>()

const emit = defineEmits<{
  crop: [blob: Blob | null]
}>()

const canvasSize = 440
const maximumUploadSize = 2 * 1024 * 1024
const acceptedTypes = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
])

interface ImageBounds {
  x: number
  y: number
  width: number
  height: number
  scale: number
}

interface Selection {
  x: number
  y: number
  size: number
}

type ResizeHandle = 'nw' | 'ne' | 'sw' | 'se'

interface PointerState {
  mode: 'move' | 'resize'
  handle: ResizeHandle | null
  hasMoved: boolean
  startX: number
  startY: number
  originalX: number
  originalY: number
  originalSize: number
}

const canvasRef = ref<HTMLCanvasElement>()
const fileInputRef = ref<HTMLInputElement>()
const sourceImage = ref<HTMLImageElement | null>(null)
const imageBounds = ref<ImageBounds | null>(null)
const selection = ref<Selection | null>(null)
const pointerState = ref<PointerState | null>(null)
const previewCanvasRef = ref<HTMLCanvasElement>()
const hasCropPreview = ref(false)
const previewAvatarFailed = ref(false)
const canvasCursor = ref('crosshair')
const selectedFileName = ref('')
const isReadingFile = ref(false)
const currentAvatarUrl = computed(() => resolveAvatarUrl(props.currentAvatar))

let sourceObjectUrl = ''
let imageLoadVersion = 0
let cropVersion = 0
let renderFrame = 0

const resizeHandleHitSize = 40
const dragThreshold = 3
const previewSize = 192

// 按鼠标位置判断当前操作意图，缩放角点优先于选区移动。
function getResizeHandle(position: { x: number; y: number }, crop: Selection) {
  const corners: Array<{ handle: ResizeHandle; x: number; y: number }> = [
    { handle: 'nw', x: crop.x, y: crop.y },
    { handle: 'ne', x: crop.x + crop.size, y: crop.y },
    { handle: 'sw', x: crop.x, y: crop.y + crop.size },
    { handle: 'se', x: crop.x + crop.size, y: crop.y + crop.size },
  ]

  return corners.find(
    (corner) =>
      Math.abs(position.x - corner.x) <= resizeHandleHitSize &&
      Math.abs(position.y - corner.y) <= resizeHandleHitSize,
  )?.handle
}

function getHandleCursor(handle: ResizeHandle) {
  return handle === 'nw' || handle === 'se' ? 'nwse-resize' : 'nesw-resize'
}

function updateHoverCursor(event: PointerEvent) {
  const position = getCanvasPosition(event)
  const crop = selection.value
  if (!position || !crop) {
    canvasCursor.value = 'crosshair'
    return
  }

  const handle = getResizeHandle(position, crop)
  if (handle) {
    canvasCursor.value = getHandleCursor(handle)
    return
  }

  const insideCrop =
    position.x >= crop.x &&
    position.x <= crop.x + crop.size &&
    position.y >= crop.y &&
    position.y <= crop.y + crop.size
  canvasCursor.value = insideCrop ? 'grab' : 'crosshair'
}

// 画布与预览共用同一帧更新，拖动时避免反复编码 PNG。
function drawCanvas() {
  const canvas = canvasRef.value
  const context = canvas?.getContext('2d')
  if (!canvas || !context) return

  context.clearRect(0, 0, canvasSize, canvasSize)
  context.fillStyle = '#f3f4f6'
  context.fillRect(0, 0, canvasSize, canvasSize)

  const image = sourceImage.value
  const bounds = imageBounds.value
  const crop = selection.value

  if (!image || !bounds) {
    context.fillStyle = '#8f96a3'
    context.font = '500 14px sans-serif'
    context.textAlign = 'center'
    context.fillText(
      '上传图片后，在此选择头像范围',
      canvasSize / 2,
      canvasSize / 2,
    )
    return
  }

  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height)
  if (!crop) return

  context.beginPath()
  context.rect(0, 0, canvasSize, canvasSize)
  context.rect(crop.x, crop.y, crop.size, crop.size)
  context.fillStyle = 'rgb(25 33 46 / 58%)'
  context.fill('evenodd')

  context.save()
  context.strokeStyle = '#fff'
  context.lineWidth = 2
  context.strokeRect(crop.x, crop.y, crop.size, crop.size)
  context.strokeStyle = 'rgb(255 255 255 / 60%)'
  context.lineWidth = 1
  for (let division = 1; division < 3; division += 1) {
    const offset = (crop.size * division) / 3
    context.beginPath()
    context.moveTo(crop.x + offset, crop.y)
    context.lineTo(crop.x + offset, crop.y + crop.size)
    context.moveTo(crop.x, crop.y + offset)
    context.lineTo(crop.x + crop.size, crop.y + offset)
    context.stroke()
  }

  const handles = [
    { x: crop.x, y: crop.y },
    { x: crop.x + crop.size, y: crop.y },
    { x: crop.x, y: crop.y + crop.size },
    { x: crop.x + crop.size, y: crop.y + crop.size },
  ]
  handles.forEach(({ x, y }) => {
    context.beginPath()
    context.arc(x, y, 12, 0, Math.PI * 2)
    context.fillStyle = '#fff'
    context.fill()
    context.strokeStyle = '#b3976f'
    context.lineWidth = 2
    context.stroke()
  })
  context.restore()
}

function drawPreview() {
  const canvas = previewCanvasRef.value
  const context = canvas?.getContext('2d')
  const image = sourceImage.value
  const bounds = imageBounds.value
  const crop = selection.value
  if (!canvas || !context || !image || !bounds || !crop) return

  const sourceX = (crop.x - bounds.x) / bounds.scale
  const sourceY = (crop.y - bounds.y) / bounds.scale
  const sourceSize = crop.size / bounds.scale

  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.clearRect(0, 0, previewSize, previewSize)
  context.drawImage(
    image,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    previewSize,
    previewSize,
  )
}

function scheduleRender() {
  if (renderFrame) return

  renderFrame = requestAnimationFrame(() => {
    renderFrame = 0
    drawCanvas()
    drawPreview()
  })
}

function getCanvasPosition(event: PointerEvent) {
  const canvas = canvasRef.value
  const bounds = canvas?.getBoundingClientRect()
  if (!canvas || !bounds) return null

  return {
    x: ((event.clientX - bounds.left) / bounds.width) * canvasSize,
    y: ((event.clientY - bounds.top) / bounds.height) * canvasSize,
  }
}

function beginSelection(event: PointerEvent) {
  const position = getCanvasPosition(event)
  const crop = selection.value
  const bounds = imageBounds.value
  const canvas = canvasRef.value
  if (!position || !crop || !bounds || !canvas) return

  const handle = getResizeHandle(position, crop) ?? null
  const insideCrop =
    position.x >= crop.x &&
    position.x <= crop.x + crop.size &&
    position.y >= crop.y &&
    position.y <= crop.y + crop.size

  if (!insideCrop && !handle) {
    crop.x = Math.min(
      Math.max(position.x - crop.size / 2, bounds.x),
      bounds.x + bounds.width - crop.size,
    )
    crop.y = Math.min(
      Math.max(position.y - crop.size / 2, bounds.y),
      bounds.y + bounds.height - crop.size,
    )
  }

  pointerState.value = {
    mode: handle ? 'resize' : 'move',
    handle,
    hasMoved: false,
    startX: position.x,
    startY: position.y,
    originalX: crop.x,
    originalY: crop.y,
    originalSize: crop.size,
  }
  canvasCursor.value = handle ? getHandleCursor(handle) : 'grabbing'
  canvas.setPointerCapture(event.pointerId)
  event.preventDefault()
  scheduleRender()
}

function moveSelection(event: PointerEvent) {
  const position = getCanvasPosition(event)
  const state = pointerState.value
  const crop = selection.value
  const bounds = imageBounds.value
  if (!position || !crop || !bounds) return
  if (!state) {
    updateHoverCursor(event)
    return
  }

  const deltaX = position.x - state.startX
  const deltaY = position.y - state.startY
  if (!state.hasMoved && Math.hypot(deltaX, deltaY) < dragThreshold) return
  state.hasMoved = true

  if (state.mode === 'move') {
    crop.x = Math.min(
      Math.max(state.originalX + deltaX, bounds.x),
      bounds.x + bounds.width - crop.size,
    )
    crop.y = Math.min(
      Math.max(state.originalY + deltaY, bounds.y),
      bounds.y + bounds.height - crop.size,
    )
  } else {
    const handle = state.handle
    if (!handle) return

    const anchorX = handle.includes('w')
      ? state.originalX + state.originalSize
      : state.originalX
    const anchorY = handle.includes('n')
      ? state.originalY + state.originalSize
      : state.originalY
    const maximumWidth = handle.includes('w')
      ? anchorX - bounds.x
      : bounds.x + bounds.width - anchorX
    const maximumHeight = handle.includes('n')
      ? anchorY - bounds.y
      : bounds.y + bounds.height - anchorY
    const maximumSize = Math.min(maximumWidth, maximumHeight)
    const sizeDelta =
      handle === 'nw'
        ? -(deltaX + deltaY) / 2
        : handle === 'ne'
          ? (deltaX - deltaY) / 2
          : handle === 'sw'
            ? (-deltaX + deltaY) / 2
            : (deltaX + deltaY) / 2
    const minimumSize = Math.min(48, maximumSize)
    crop.size = Math.min(
      maximumSize,
      Math.max(minimumSize, state.originalSize + sizeDelta),
    )
    crop.x = handle.includes('w') ? anchorX - crop.size : anchorX
    crop.y = handle.includes('n') ? anchorY - crop.size : anchorY
  }

  scheduleRender()
}

function finishSelection(event: PointerEvent) {
  const state = pointerState.value
  if (!state) return
  pointerState.value = null
  updateHoverCursor(event)
  scheduleRender()
  if (state.hasMoved || !state.handle) createCropBlob()
}

function handlePointerLeave() {
  if (!pointerState.value) canvasCursor.value = 'crosshair'
}

function setDefaultSelection() {
  const bounds = imageBounds.value
  if (!bounds) return

  const size = Math.min(bounds.width, bounds.height) * 0.68
  selection.value = {
    x: bounds.x + (bounds.width - size) / 2,
    y: bounds.y + (bounds.height - size) / 2,
    size,
  }
}

function createCropBlob() {
  const image = sourceImage.value
  const bounds = imageBounds.value
  const crop = selection.value
  if (!image || !bounds || !crop) return

  const nextVersion = ++cropVersion
  const outputCanvas = document.createElement('canvas')
  outputCanvas.width = 512
  outputCanvas.height = 512
  const context = outputCanvas.getContext('2d')
  if (!context) {
    ElMessage.error('无法生成头像预览，请换一张图片重试')
    return
  }

  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  const sourceX = (crop.x - bounds.x) / bounds.scale
  const sourceY = (crop.y - bounds.y) / bounds.scale
  const sourceSize = crop.size / bounds.scale

  context.drawImage(
    image,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    outputCanvas.width,
    outputCanvas.height,
  )

  outputCanvas.toBlob((blob) => {
    if (nextVersion === cropVersion && blob) emit('crop', blob)
  }, 'image/png')
}

// 校验原始文件后将图片完整缩放到画布中，并初始化选区。
function handleFileChange(event: Event) {
  const input = event.currentTarget
  if (!(input instanceof HTMLInputElement)) return

  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!acceptedTypes.has(file.type)) {
    ElMessage.error('请选择 JPG、PNG、GIF 或 WebP 格式的图片')
    return
  }
  if (file.size > maximumUploadSize) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  const currentVersion = ++imageLoadVersion
  cropVersion += 1
  isReadingFile.value = true
  hasCropPreview.value = false
  previewAvatarFailed.value = false
  emit('crop', null)

  const objectUrl = URL.createObjectURL(file)
  const image = new Image()
  image.onload = () => {
    if (currentVersion !== imageLoadVersion) {
      URL.revokeObjectURL(objectUrl)
      return
    }

    if (sourceObjectUrl) URL.revokeObjectURL(sourceObjectUrl)
    sourceObjectUrl = objectUrl
    sourceImage.value = image
    selectedFileName.value = file.name

    const scale = Math.min(
      canvasSize / image.naturalWidth,
      canvasSize / image.naturalHeight,
    )
    const width = image.naturalWidth * scale
    const height = image.naturalHeight * scale
    imageBounds.value = {
      x: (canvasSize - width) / 2,
      y: (canvasSize - height) / 2,
      width,
      height,
      scale,
    }
    setDefaultSelection()
    hasCropPreview.value = true
    canvasCursor.value = 'crosshair'
    scheduleRender()
    createCropBlob()
    isReadingFile.value = false
  }
  image.onerror = () => {
    URL.revokeObjectURL(objectUrl)
    if (currentVersion !== imageLoadVersion) return
    isReadingFile.value = false
    ElMessage.error('图片读取失败，请选择其他图片')
  }
  image.src = objectUrl
}

onMounted(() => {
  drawCanvas()
})

onBeforeUnmount(() => {
  imageLoadVersion += 1
  cropVersion += 1
  if (renderFrame) cancelAnimationFrame(renderFrame)
  if (sourceObjectUrl) URL.revokeObjectURL(sourceObjectUrl)
})
</script>

<template>
  <section aria-label="头像裁剪与预览" class="avatar-editor">
    <div class="avatar-editor__workspace">
      <div class="avatar-editor__canvas-frame">
        <canvas
          ref="canvasRef"
          :height="canvasSize"
          :style="{ cursor: canvasCursor }"
          :width="canvasSize"
          aria-label="拖动或调整正方形选区以裁剪头像"
          class="avatar-editor__canvas"
          @pointercancel="finishSelection"
          @pointerdown="beginSelection"
          @pointerleave="handlePointerLeave"
          @pointermove="moveSelection"
          @pointerup="finishSelection"
        ></canvas>
      </div>
      <div class="avatar-editor__controls">
        <el-button
          :loading="isReadingFile"
          type="primary"
          @click="fileInputRef?.click()"
        >
          选择图片
        </el-button>
        <input
          ref="fileInputRef"
          accept="image/jpeg,image/png,image/gif,image/webp"
          class="avatar-editor__file-input"
          type="file"
          @change="handleFileChange"
        />
        <span>{{
          selectedFileName || 'JPG / PNG / GIF / WebP，最大 2MB'
        }}</span>
      </div>
      <p class="avatar-editor__hint">
        在选区内拖动可移动；四角仅支持拖动拉伸，单击不会调整。
      </p>
      <aside class="avatar-preview">
        <p class="avatar-preview__eyebrow">LIVE PREVIEW</p>
        <div class="avatar-preview__circle">
          <img
            v-if="!hasCropPreview && currentAvatarUrl && !previewAvatarFailed"
            :src="currentAvatarUrl"
            alt="头像圆形预览"
            @error="previewAvatarFailed = true"
          />
          <svg
            v-else-if="!hasCropPreview"
            aria-hidden="true"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle cx="12" cy="8" r="3.5" />
            <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
          </svg>
          <canvas
            ref="previewCanvasRef"
            :class="{ 'avatar-preview__canvas--visible': hasCropPreview }"
            :height="previewSize"
            :width="previewSize"
            aria-label="当前选区头像预览"
            class="avatar-preview__canvas"
          ></canvas>
        </div>
        <p class="avatar-preview__label">头像预览</p>
        <span class="avatar-preview__note">保存后将应用到个人资料</span>
      </aside>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.avatar-editor {
  width: 100%;

  &__workspace {
    display: grid;
    grid-template-columns: minmax(0, 440px) 150px;
    grid-template-rows: auto auto auto;
    column-gap: 24px;
  }

  &__canvas-frame {
    grid-column: 1;
    grid-row: 1;
    width: min(100%, 440px);
    aspect-ratio: 1;
    overflow: hidden;
    border: 1px solid #e7e9ed;
    border-radius: 12px;
    background: #f3f4f6;
  }

  &__canvas {
    display: block;
    width: 100%;
    height: 100%;
    touch-action: none;
  }

  &__controls {
    grid-column: 1;
    grid-row: 2;
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 14px;

    > span {
      min-width: 0;
      overflow: hidden;
      color: #969ca8;
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__file-input {
    display: none;
  }

  &__hint {
    grid-column: 1;
    grid-row: 3;
    margin: 10px 0 0;
    color: #999eaa;
    font-size: 12px;
    line-height: 1.6;
  }
}

.avatar-preview {
  grid-column: 2;
  grid-row: 1;
  align-self: end;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 12px;
  border: 1px solid #f0eee9;
  border-radius: 13px;
  background: #fcfbf9;

  &__eyebrow {
    margin: 0 0 15px;
    color: #b3976f;
    font-size: 8px;
    font-weight: 700;
    letter-spacing: 0.14em;
  }

  &__circle {
    position: relative;
    display: grid;
    width: 92px;
    height: 92px;
    overflow: hidden;
    place-items: center;
    border: 3px solid #fff;
    border-radius: 50%;
    background: #f0ece4;
    box-shadow: 0 5px 16px rgb(34 42 57 / 12%);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    svg {
      width: 42px;
      height: 42px;
      stroke: #9e855f;
      stroke-width: 1.4;
      stroke-linecap: round;
    }
  }

  &__canvas {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    opacity: 0;
    transition: opacity 100ms linear;

    &--visible {
      opacity: 1;
    }
  }

  &__label {
    margin: 13px 0 0;
    color: #4a5260;
    font-size: 11px;
    font-weight: 600;
  }

  &__note {
    margin-top: 5px;
    color: #a0a5b0;
    font-size: 10px;
    text-align: center;
  }
}

@media (max-width: 600px) {
  .avatar-editor__workspace {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto auto auto auto;
  }

  .avatar-editor__canvas-frame {
    grid-column: 1;
    grid-row: 1;
    margin: 0 auto;
  }

  .avatar-editor__controls {
    grid-column: 1;
    grid-row: 2;
  }

  .avatar-editor__hint {
    grid-column: 1;
    grid-row: 3;
  }

  .avatar-preview {
    grid-column: 1;
    grid-row: 4;
    align-self: stretch;
    width: 100%;
    flex-direction: row;
    justify-content: flex-start;
    gap: 13px;
    padding: 11px 14px;

    &__eyebrow {
      display: none;
    }

    &__circle {
      width: 56px;
      height: 56px;
    }

    &__label {
      margin: 0;
    }

    &__note {
      margin: 0 0 0 auto;
    }
  }
}

@media (min-width: 601px) and (max-width: 900px) {
  .avatar-preview {
    flex-direction: row;
    justify-content: center;
    gap: 8px;
    padding: 10px 8px;

    &__eyebrow,
    &__note {
      display: none;
    }

    &__circle {
      width: 52px;
      height: 52px;
      flex: 0 0 auto;
    }

    &__label {
      margin: 0;
      font-size: 10px;
    }
  }
}
</style>
