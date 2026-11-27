<!--
  弹窗底部操作按钮组件
  封装 Element Plus 弹窗（dialog）的右下角常见操作按钮：
  取消（muted）与确认（primary/danger）两个按钮，
  统一新增、修改、删除等各类弹窗的操作交互与加载中样式。
-->
<script lang="ts" setup>
withDefaults(
  defineProps<{
    cancelText?: string
    confirmText: string
    confirmType?: 'primary' | 'danger'
    cancelDisabled?: boolean
    confirmDisabled?: boolean
    loading?: boolean
    loadingText?: string
  }>(),
  {
    cancelText: '取消',
    confirmType: 'primary',
    cancelDisabled: false,
    confirmDisabled: false,
    loading: false,
    loadingText: '提交中…',
  },
)

const emit = defineEmits<{
  cancel: []
  confirm: []
}>()
</script>

<template>
  <div class="dialog-actions">
    <!-- 取消操作 -->
    <button
      :disabled="cancelDisabled || loading"
      class="dialog-button dialog-button--muted"
      type="button"
      @click="emit('cancel')"
    >
      {{ cancelText }}
    </button>
    <!-- 确认/危险操作 -->
    <button
      :class="[
        'dialog-button',
        confirmType === 'danger'
          ? 'dialog-button--danger'
          : 'dialog-button--primary',
      ]"
      :disabled="confirmDisabled || loading"
      type="button"
      @click="emit('confirm')"
    >
      {{ loading ? loadingText : confirmText }}
    </button>
  </div>
</template>

<style lang="scss" scoped>
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.dialog-button {
  height: 40px;
  min-width: 88px;
  padding: 0 16px;
  border-radius: 9px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 600;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }

  &--muted {
    border: 1px solid #e3e5e9;
    background: #fff;
    color: #747c8a;
  }

  &--primary {
    border: 1px solid #293343;
    background: #293343;
    color: #fff;
  }

  &--danger {
    border: 1px solid #b46a6a;
    background: #b46a6a;
    color: #fff;
  }
}
</style>
