<!--
  删除菜单弹窗组件
  展示待删除菜单的确认卡片，并要求输入当前账号密码以增强安全性，
  密码错误时在表单内提示；删除成功后派发 success 事件，
  由父组件负责分页与列表刷新逻辑。
-->
<script lang="ts" setup>
import {ref, watch} from 'vue'
import {ElDialog, ElInput, ElMessage} from 'element-plus'
import {deleteMenu, type MenuInfo} from '@/api/menu'
import {getRequestErrorMessage} from '@/utils/error'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

const props = defineProps<{
  modelValue: boolean
  target: MenuInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const deletePassword = ref('')
const deletePasswordError = ref('')
const isDeletingMenu = ref(false)

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
async function submitDeleteMenu() {
  const menu = props.target
  if (!menu || !deletePassword.value || isDeletingMenu.value) return

  isDeletingMenu.value = true
  try {
    await deleteMenu(menu.menuId, deletePassword.value)
    ElMessage.success('菜单删除成功')
    close()
    emit('success')
  } catch (error) {
    const message = getRequestErrorMessage(error, '删除菜单失败，请稍后重试')
    if (message.includes('密码错误')) {
      deletePasswordError.value = message
    } else {
      ElMessage.error(message)
    }
  } finally {
    isDeletingMenu.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :close-on-press-escape="!isDeletingMenu"
    :model-value="modelValue"
    align-center
    class="delete-menu-dialog"
    draggable
    title="确认删除菜单"
    width="480"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="target" class="delete-confirmation">
      <!-- 危险操作提示 -->
      <div class="delete-confirmation__warning">
        <span aria-hidden="true" class="delete-confirmation__icon">!</span>
        <p>此操作不可撤销，请确认要删除以下菜单。</p>
      </div>
      <!-- 待删除菜单卡片 -->
      <div class="delete-menu-card">
        <span class="delete-menu-card__avatar">
          {{ target.menuName.slice(0, 1) }}
        </span>
        <div class="delete-menu-card__details">
          <strong>{{ target.menuName }}</strong>
          <span>路由：{{ target.routeName || target.routePath }}</span>
          <small>菜单 ID：{{ target.menuId }}</small>
        </div>
      </div>
      <!-- 密码确认输入框 -->
      <label class="delete-password-label" for="delete-menu-password">
        输入当前账号密码以确认
      </label>
      <ElInput
        id="delete-menu-password"
        v-model="deletePassword"
        autocomplete="current-password"
        placeholder="请输入您的登录密码"
        show-password
        type="password"
        @input="deletePasswordError = ''"
        @keyup.enter="submitDeleteMenu"
      />
      <p v-if="deletePasswordError" class="delete-password-error" role="alert">
        {{ deletePasswordError }}
      </p>
    </div>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :cancel-disabled="isDeletingMenu"
        :confirm-disabled="!deletePassword"
        :loading="isDeletingMenu"
        confirm-text="确认删除"
        confirm-type="danger"
        loading-text="删除中…"
        @cancel="close"
        @confirm="submitDeleteMenu"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.delete-menu-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
}

:global(.delete-menu-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.delete-menu-dialog .el-dialog__title) {
  color: #293343;
  font-size: 18px;
  font-weight: 600;
}

:global(.delete-menu-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.delete-menu-dialog .el-dialog__body) {
  padding: 22px 24px 10px;
}

:global(.delete-menu-dialog .el-dialog__footer) {
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

.delete-menu-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #eceef1;
  border-radius: 11px;
  background: #fcfcfb;
}

.delete-menu-card__avatar {
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

.delete-menu-card__details {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;

  strong {
    color: #414958;
    font-size: 14px;
  }

  span {
    overflow: hidden;
    color: #616978;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: #a0a5af;
    font-size: 11px;
  }
}

.delete-password-label {
  color: #596171;
  font-size: 12px;
  font-weight: 600;
}

.delete-password-error {
  margin: 0;
  color: #b46a6a;
  font-size: 11px;
}
</style>
