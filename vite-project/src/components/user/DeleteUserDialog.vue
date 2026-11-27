<!--
  删除用户弹窗组件
  展示待删除用户的确认卡片，并要求输入当前账号密码以增强安全性，
  密码错误时在表单内提示；删除成功后派发 success 事件，
  由父组件负责分页与列表刷新逻辑。
-->
<script lang="ts" setup>
import {ref, watch} from 'vue'
import {ElDialog, ElInput, ElMessage} from 'element-plus'
import {deleteUser, type UserInfo} from '@/api/user'
import {getRequestErrorMessage} from '@/utils/error'
import {getAvatarText} from '@/utils/user'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

const props = defineProps<{
  modelValue: boolean
  target: UserInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const deletePassword = ref('')
const deletePasswordError = ref('')
const isDeletingUser = ref(false)

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
async function submitDeleteUser() {
  const user = props.target
  if (!user || !deletePassword.value || isDeletingUser.value) return

  isDeletingUser.value = true
  try {
    await deleteUser(user.userId, deletePassword.value)
    ElMessage.success('用户删除成功')
    close()
    emit('success')
  } catch (error) {
    const message = getRequestErrorMessage(error, '删除用户失败，请稍后重试')
    if (message.includes('密码错误')) {
      deletePasswordError.value = message
    } else {
      ElMessage.error(message)
    }
  } finally {
    isDeletingUser.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :close-on-press-escape="!isDeletingUser"
    :model-value="modelValue"
    :width="480"
    align-center
    class="delete-user-dialog"
    draggable
    title="确认删除用户"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="target" class="delete-confirmation">
      <!-- 危险操作提示 -->
      <div class="delete-confirmation__warning">
        <span aria-hidden="true" class="delete-confirmation__icon">!</span>
        <p>此操作不可撤销，请确认要删除以下用户。</p>
      </div>
      <!-- 待删除用户卡片 -->
      <div class="delete-user-card">
        <span class="delete-user-card__avatar">
          {{ getAvatarText(target) }}
        </span>
        <div class="delete-user-card__details">
          <strong>{{ target.userName || '未设置昵称' }}</strong>
          <span>{{ target.email }}</span>
          <small>用户 ID：{{ target.userId }}</small>
        </div>
      </div>
      <!-- 密码确认输入框 -->
      <label class="delete-password-label" for="delete-user-password">
        输入当前账号密码以确认
      </label>
      <ElInput
        id="delete-user-password"
        v-model="deletePassword"
        autocomplete="current-password"
        placeholder="请输入您的登录密码"
        show-password
        type="password"
        @input="deletePasswordError = ''"
        @keyup.enter="submitDeleteUser"
      />
      <p v-if="deletePasswordError" class="delete-password-error" role="alert">
        {{ deletePasswordError }}
      </p>
    </div>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :cancel-disabled="isDeletingUser"
        :confirm-disabled="!deletePassword"
        :loading="isDeletingUser"
        confirm-text="确认删除"
        confirm-type="danger"
        loading-text="删除中…"
        @cancel="close"
        @confirm="submitDeleteUser"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.delete-user-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
}

:global(.delete-user-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.delete-user-dialog .el-dialog__title) {
  color: #293343;
  font-size: 18px;
  font-weight: 600;
}

:global(.delete-user-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.delete-user-dialog .el-dialog__body) {
  padding: 22px 24px 10px;
}

:global(.delete-user-dialog .el-dialog__footer) {
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
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #d58a32;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.delete-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #eceef1;
  border-radius: 11px;
  background: #fcfcfb;
}

.delete-user-card__avatar {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 11px;
  background: #f2eee5;
  color: #967c56;
  font-size: 15px;
  font-weight: 700;
}

.delete-user-card__details {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;

  strong {
    color: #414958;
    font-size: 13px;
  }

  span,
  small {
    overflow: hidden;
    color: #858c99;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.delete-password-label {
  margin-bottom: -10px;
  color: #616978;
  font-size: 12px;
  font-weight: 600;
}

.delete-password-error {
  margin: 0 0 0 2px;
  color: #d03050;
  font-size: 11px;
  line-height: 1.5;
}
</style>
