<!--
  删除权限弹窗组件
  展示待删除权限的确认卡片，并要求输入当前账号密码以增强安全性，
  密码错误时在表单内提示；删除成功后派发 success 事件，
  由父组件负责分页与列表刷新逻辑。
-->
<script lang="ts" setup>
import {ref, watch} from 'vue'
import {ElDialog, ElInput, ElMessage} from 'element-plus'
import {deletePermission, type PermissionInfo} from '@/api/user'
import {getRequestErrorMessage} from '@/utils/error'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

const props = defineProps<{
  modelValue: boolean
  target: PermissionInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const deletePassword = ref('')
const deletePasswordError = ref('')
const isDeletingPermission = ref(false)

// 打开弹窗时重置密码与错误提示
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      deletePassword.value = ''
      deletePasswordError.value = ''
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

// 校验密码并执行删除
async function submitDeletePermission() {
  const permission = props.target
  if (!permission || !deletePassword.value || isDeletingPermission.value) return

  isDeletingPermission.value = true
  try {
    await deletePermission(permission.permissionId, deletePassword.value)
    ElMessage.success('权限删除成功')
    close()
    emit('success')
  } catch (error) {
    const message = getRequestErrorMessage(error, '删除权限失败，请稍后重试')
    if (message.includes('密码错误')) {
      deletePasswordError.value = message
    } else {
      ElMessage.error(message)
    }
  } finally {
    isDeletingPermission.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :close-on-press-escape="!isDeletingPermission"
    :model-value="modelValue"
    align-center
    class="delete-permission-dialog"
    draggable
    title="确认删除权限"
    width="480"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="target" class="delete-confirmation">
      <!-- 危险操作提示 -->
      <div class="delete-confirmation__warning">
        <span aria-hidden="true" class="delete-confirmation__icon">!</span>
        <p>此操作不可撤销，请确认要删除以下权限。</p>
      </div>
      <!-- 待删除权限卡片 -->
      <div class="delete-permission-card">
        <span class="delete-permission-card__avatar">
          {{ (target.permissionName || 'P').slice(0, 1) }}
        </span>
        <div class="delete-permission-card__details">
          <strong>{{ target.permissionName }}</strong>
          <span
            >权限标识：{{ target.permissionCode || '—' }} · 类型：{{
              target.permissionType || '—'
            }}</span
          >
          <small>权限 ID：{{ target.permissionId }}</small>
        </div>
      </div>
      <!-- 密码确认输入框 -->
      <label class="delete-password-label" for="delete-permission-password">
        输入当前账号密码以确认
      </label>
      <ElInput
        id="delete-permission-password"
        v-model="deletePassword"
        autocomplete="current-password"
        placeholder="请输入您的登录密码"
        show-password
        type="password"
        @input="deletePasswordError = ''"
        @keyup.enter="submitDeletePermission"
      />
      <p v-if="deletePasswordError" class="delete-password-error" role="alert">
        {{ deletePasswordError }}
      </p>
    </div>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :cancel-disabled="isDeletingPermission"
        :confirm-disabled="!deletePassword"
        :loading="isDeletingPermission"
        confirm-text="确认删除"
        confirm-type="danger"
        loading-text="删除中…"
        @cancel="close"
        @confirm="submitDeletePermission"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.delete-permission-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
}

:global(.delete-permission-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.delete-permission-dialog .el-dialog__title) {
  color: #293343;
  font-size: 18px;
  font-weight: 600;
}

:global(.delete-permission-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.delete-permission-dialog .el-dialog__body) {
  padding: 22px 24px 10px;
}

:global(.delete-permission-dialog .el-dialog__footer) {
  padding: 14px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

.delete-confirmation {
  display: flex;
  flex-direction: column;
  gap: 16px;

  :deep(.el-input__wrapper) {
    min-height: 42px;
    border-radius: 9px;
    box-shadow: 0 0 0 1px #e3e6eb inset;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #b3976f inset;
  }
}

.delete-confirmation__warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #f0e2d4;
  border-radius: 10px;
  background: #fcf7f1;
  color: #9c6840;

  p {
    margin: 0;
    font-size: 12px;
  }
}

.delete-confirmation__icon {
  display: grid;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  background: #f5e2d4;
  color: #c07a47;
  font-size: 14px;
  font-weight: 700;
}

.delete-permission-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px;
  border: 1px solid #eceef1;
  border-radius: 11px;
  background: #fafbfc;

  &__avatar {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 9px;
    background: #f2eee5;
    color: #967c56;
    font-size: 13px;
    font-weight: 700;
  }

  &__details {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 3px;

    strong {
      overflow: hidden;
      color: #393f4d;
      font-size: 13px;
      font-weight: 600;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span {
      overflow: hidden;
      color: #787f8e;
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    small {
      color: #a0a5af;
      font-size: 10px;
    }
  }
}

.delete-password-label {
  display: block;
  padding-bottom: 6px;
  color: #596171;
  font-size: 12px;
  font-weight: 500;
}

.delete-password-error {
  margin: 0;
  color: #b46a6a;
  font-size: 11px;
}
</style>
