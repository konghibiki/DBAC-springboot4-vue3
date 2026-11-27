<!--
  修改用户弹窗组件
  提供修改用户的表单弹窗：回显用户基础资料，支持编辑邮箱、密码、用户名、
  性别、账号状态（正常/禁用）与职能，
  状态切换与职能变更均需二次确认，成功后派发 success 事件供父组件刷新列表。
-->
<script lang="ts" setup>
import {reactive, ref, watch} from 'vue'
import {ElDialog, ElForm, ElFormItem, ElInput, ElMessage, type FormInstance, type FormRules,} from 'element-plus'
import {updateUser, type UserInfo} from '@/api/user'
import {getEmailValidationError, getPasswordValidationError,} from '@/utils/authValidation'
import {getRequestErrorMessage} from '@/utils/error'
import {showConfirm} from '@/utils/confirm'
import {getGenderCode, getStatusValue, toRoleArray} from '@/utils/user'
import {useRoleList} from '@/composables/useRoleList'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'
import GenderRadioGroup from '@/components/common/GenderRadioGroup.vue'
import RoleCheckboxGroup from '@/components/common/RoleCheckboxGroup.vue'

interface UpdateUserForm {
  userId: number
  email: string
  password: string
  userName: string
  gender: number
  status: '正常用户' | '禁用用户'
  role: string[]
}

const props = defineProps<{
  modelValue: boolean
  user: UserInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const { roles, isLoading: isLoadingRoles, loadRoles } = useRoleList()
const formRef = ref<FormInstance>()
const isUpdatingUser = ref(false)
const isConfirmingStatusChange = ref(false)
const originalUpdateRoles = ref<string[]>([])

const updateForm = reactive<UpdateUserForm>({
  userId: 0,
  email: '',
  password: '',
  userName: '',
  gender: 0,
  status: '正常用户',
  role: [],
})

// 修改用户表单校验规则
const updateFormRules: FormRules<UpdateUserForm> = {
  email: [
    {
      validator: (_rule, value: string, callback) => {
        const error = value?.trim() ? getEmailValidationError(value) : ''
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        const error = value ? getPasswordValidationError(value) : ''
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
  userName: [
    {
      validator: (_rule, value: string, callback) => {
        callback(
          value && value.trim().length > 18
            ? new Error('用户名不能超过18位')
            : undefined,
        )
      },
      trigger: ['blur', 'change'],
    },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  status: [{ required: true, message: '请选择账号状态', trigger: 'change' }],
  role: [
    {
      validator: (_rule, value: string[], callback) => {
        callback(
          value.length > 0 ? undefined : new Error('请至少选择一个用户职能'),
        )
      },
      trigger: 'change',
    },
  ],
}

// 打开弹窗时回显用户信息并加载职能列表
watch(
  () => [props.modelValue, props.user] as const,
  ([visible, user]) => {
    if (!visible || !user) return
    const status = getStatusValue(user)
    const roles = toRoleArray(user.role)

    updateForm.userId = user.userId
    updateForm.email = user.email
    updateForm.password = ''
    updateForm.userName = user.userName ?? ''
    updateForm.gender = getGenderCode(user.gender)
    updateForm.status = status
    updateForm.role = roles
    originalUpdateRoles.value = [...roles]
    formRef.value?.clearValidate()
    void loadRoles()
  },
)

function close() {
  emit('update:modelValue', false)
}

// 判断职能是否发生变化
function rolesChanged() {
  const original = [...originalUpdateRoles.value].sort()
  const current = [...updateForm.role].sort()
  return (
    original.length !== current.length ||
    original.some((role, index) => role !== current[index])
  )
}

// 切换账号状态（需二次确认）
async function changeUpdateStatus() {
  if (isConfirmingStatusChange.value) return

  const nextStatus = updateForm.status === '正常用户' ? '禁用用户' : '正常用户'
  isConfirmingStatusChange.value = true
  try {
    await showConfirm({
      title: '确认账号状态',
      message:
        nextStatus === '禁用用户'
          ? `确定将用户“${updateForm.userName || updateForm.email}”设置为禁用吗？`
          : `确定恢复用户“${updateForm.userName || updateForm.email}”的正常状态吗？`,
      confirmText: '确认切换',
    })
    updateForm.status = nextStatus
  } catch {
    // 取消时保持当前状态不变
  } finally {
    isConfirmingStatusChange.value = false
  }
}

// 校验并提交修改用户：职能变更时需二次确认
async function submitUpdateUser() {
  if (!formRef.value || isUpdatingUser.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  if (rolesChanged()) {
    try {
      await showConfirm({
        title: '确认职能修改',
        message: `确定修改用户“${updateForm.userName}”的职能吗？`,
        confirmText: '确认修改',
      })
    } catch {
      return
    }
  }

  isUpdatingUser.value = true
  try {
    await updateUser({
      userId: updateForm.userId,
      email: updateForm.email.trim() || null,
      password: updateForm.password || null,
      userName: updateForm.userName.trim() || null,
      gender: updateForm.gender,
      status: updateForm.status,
      role: updateForm.role,
    })
    ElMessage.success('用户修改成功')
    close()
    formRef.value.clearValidate()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '修改用户失败，请稍后重试'))
  } finally {
    isUpdatingUser.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    :width="620"
    align-center
    class="update-user-dialog"
    draggable
    title="修改用户"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 修改用户表单 -->
    <ElForm
      ref="formRef"
      :model="updateForm"
      :rules="updateFormRules"
      label-position="top"
    >
      <div class="update-user-grid">
        <!-- 账号信息：邮箱与密码 -->
        <ElFormItem label="邮箱地址" prop="email">
          <ElInput
            v-model="updateForm.email"
            autocomplete="email"
            maxlength="254"
            placeholder="请输入用户邮箱"
          />
        </ElFormItem>
        <ElFormItem label="登录密码" prop="password">
          <ElInput
            v-model="updateForm.password"
            autocomplete="new-password"
            maxlength="18"
            placeholder="请输入新的登录密码"
            show-password
            type="password"
          />
        </ElFormItem>
        <!-- 基础资料：用户名与性别 -->
        <ElFormItem label="用户名" prop="userName">
          <ElInput
            v-model="updateForm.userName"
            maxlength="18"
            placeholder="请输入 1-18 位用户名"
          />
        </ElFormItem>
        <ElFormItem label="性别" prop="gender">
          <GenderRadioGroup v-model="updateForm.gender" />
        </ElFormItem>
      </div>

      <!-- 账号状态切换 -->
      <ElFormItem label="账号状态" prop="status">
        <button
          :aria-checked="updateForm.status === '正常用户'"
          :class="[
            'status-switch',
            {
              'status-switch--disabled': updateForm.status === '禁用用户',
              'status-switch--confirming': isConfirmingStatusChange,
            },
          ]"
          :disabled="isConfirmingStatusChange || isUpdatingUser"
          aria-label="切换账号状态"
          role="switch"
          type="button"
          @click="changeUpdateStatus"
        >
          <span class="status-switch__label status-switch__label--normal">
            正常
          </span>
          <span class="status-switch__label status-switch__label--disabled">
            禁用
          </span>
          <span class="status-switch__thumb">
            {{ updateForm.status === '正常用户' ? '正常' : '禁用' }}
          </span>
        </button>
      </ElFormItem>

      <!-- 职能分配 -->
      <ElFormItem class="role-form-item" label="用户职能" prop="role">
        <RoleCheckboxGroup
          v-model="updateForm.role"
          :loading="isLoadingRoles"
          :roles="roles"
        />
      </ElFormItem>
    </ElForm>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :loading="isUpdatingUser"
        confirm-text="确认修改"
        loading-text="保存中…"
        @cancel="close"
        @confirm="submitUpdateUser"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.update-user-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(.update-user-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.update-user-dialog .el-dialog__title) {
  color: #293343;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

:global(.update-user-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.update-user-dialog .el-dialog__body) {
  padding: 24px 24px 10px;
}

:global(.update-user-dialog .el-dialog__footer) {
  padding: 15px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.update-user-dialog .el-form-item) {
  margin-bottom: 19px;
}

:deep(.update-user-dialog .el-form-item__label) {
  padding-bottom: 7px;
  color: #596171;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

:deep(.update-user-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.update-user-dialog .el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.update-user-dialog .el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.update-user-dialog .el-input__inner) {
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
}

:deep(.update-user-dialog .el-input__inner::placeholder) {
  color: #a7acb6;
}

:deep(.update-user-dialog .el-form-item__error) {
  padding-top: 5px;
  color: #b46a6a;
  font-size: 11px;
}

.update-user-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 16px;
}

.status-switch {
  position: relative;
  display: grid;
  width: min(150px, 100%);
  height: 40px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: center;
  padding: 5px;
  overflow: hidden;
  border: 1px solid #e4e6ea;
  border-radius: 999px;
  background: #eef0f3;
  box-shadow: inset 0 2px 5px rgb(35 43 57 / 7%);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 15px;
  font-weight: 650;
  line-height: 1;
  transition: background-color 200ms ease;

  &--disabled {
    background: #f0e9e7;

    .status-switch__thumb {
      border-color: #e6d9d4;
      color: #9d625e;
    }
  }

  &--confirming,
  &:disabled {
    cursor: wait;
    opacity: 0.8;
  }

  &:focus-visible {
    outline: 3px solid rgb(179 151 111 / 30%);
    outline-offset: 3px;
  }

  &__label,
  &__thumb {
    display: grid;
    place-items: center;
    height: 100%;
    border-radius: 999px;
  }

  &__label {
    position: relative;
    z-index: 1;
    color: #858b96;
    transition: color 180ms ease;
  }

  &__thumb {
    position: absolute;
    top: 3px;
    left: 2px;
    z-index: 2;
    width: calc(50% - 4px);
    height: calc(100% - 8px);
    border: 1px solid #dfe3e8;
    background: linear-gradient(180deg, #fff 0%, #fbfbfc 100%);
    box-shadow:
      0 3px 8px rgb(35 43 57 / 14%),
      inset 0 1px 0 #fff;
    color: #547961;
    transition:
      transform 240ms cubic-bezier(0.2, 0.8, 0.2, 1),
      border-color 180ms ease,
      color 180ms ease;
  }

  &--disabled &__thumb {
    transform: translateX(100%);
  }
}

.role-form-item {
  margin-top: 8px;
}

@media (max-width: 760px) {
  .update-user-grid {
    grid-template-columns: 1fr;
  }
}
</style>
