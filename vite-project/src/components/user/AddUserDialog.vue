<!--
  新增用户弹窗组件
  提供新增用户的表单弹窗：邮箱、密码、用户名、性别与职能字段的填写与校验，
  内部通过 useRoleList 加载并缓存职能选项，
  提交成功后派发 success 事件，由父组件负责刷新列表。
-->
<script lang="ts" setup>
import {reactive, ref, watch} from 'vue'
import {ElDialog, ElForm, ElFormItem, ElInput, ElMessage, type FormInstance, type FormRules,} from 'element-plus'
import {addUser} from '@/api/user'
import {getEmailValidationError, getPasswordValidationError,} from '@/utils/authValidation'
import {getRequestErrorMessage} from '@/utils/error'
import {useRoleList} from '@/composables/useRoleList'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'
import GenderRadioGroup from '@/components/common/GenderRadioGroup.vue'
import RoleCheckboxGroup from '@/components/common/RoleCheckboxGroup.vue'

interface AddUserForm {
  email: string
  password: string
  userName: string
  gender: number
  role: string[]
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const { roles, isLoading: isLoadingRoles, loadRoles } = useRoleList()
const formRef = ref<FormInstance>()
const isAddingUser = ref(false)

const addForm = reactive<AddUserForm>({
  email: '',
  password: '',
  userName: '',
  gender: 0,
  role: [],
})

// 新增用户表单校验规则
const addFormRules: FormRules<AddUserForm> = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const error = getEmailValidationError(value)
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const error = getPasswordValidationError(value)
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 18, message: '用户名长度为1-18位', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
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

// 打开弹窗时加载职能列表并重置表单
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resetAddForm()
      void loadRoles()
    }
  },
)

function resetAddForm() {
  addForm.email = ''
  addForm.password = ''
  addForm.userName = ''
  addForm.gender = 0
  addForm.role = []
  formRef.value?.clearValidate()
}

function close() {
  emit('update:modelValue', false)
}

// 校验并提交新增用户
async function submitAddUser() {
  if (!formRef.value || isAddingUser.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isAddingUser.value = true
  try {
    await addUser({
      email: addForm.email.trim(),
      password: addForm.password,
      userName: addForm.userName.trim(),
      gender: addForm.gender,
      role: addForm.role,
    })
    ElMessage.success('用户添加成功')
    close()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '添加用户失败，请稍后重试'))
  } finally {
    isAddingUser.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    :width="620"
    align-center
    class="add-user-dialog"
    draggable
    title="新增用户"
    @closed="resetAddForm"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 新增用户表单 -->
    <ElForm
      ref="formRef"
      :model="addForm"
      :rules="addFormRules"
      label-position="top"
    >
      <div class="add-user-grid">
        <!-- 账号信息：邮箱与密码 -->
        <ElFormItem label="邮箱地址" prop="email">
          <ElInput
            v-model="addForm.email"
            autocomplete="email"
            maxlength="254"
            placeholder="请输入用户邮箱"
          />
        </ElFormItem>
        <ElFormItem label="登录密码" prop="password">
          <ElInput
            v-model="addForm.password"
            autocomplete="new-password"
            maxlength="18"
            placeholder="6-18 位，包含字母和数字"
            show-password
            type="password"
          />
        </ElFormItem>
        <!-- 基础资料：用户名与性别 -->
        <ElFormItem label="用户名" prop="userName">
          <ElInput
            v-model="addForm.userName"
            maxlength="18"
            placeholder="请输入 1-18 位用户名"
          />
        </ElFormItem>
        <ElFormItem label="性别" prop="gender">
          <GenderRadioGroup v-model="addForm.gender" />
        </ElFormItem>
      </div>
      <!-- 职能分配 -->
      <ElFormItem class="role-form-item" label="用户职能" prop="role">
        <RoleCheckboxGroup
          v-model="addForm.role"
          :loading="isLoadingRoles"
          :roles="roles"
          empty-hint="暂无可分配职能"
        />
      </ElFormItem>
    </ElForm>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :loading="isAddingUser"
        confirm-text="确认添加"
        loading-text="添加中…"
        @cancel="close"
        @confirm="submitAddUser"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.add-user-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
}

:global(.add-user-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.add-user-dialog .el-dialog__title) {
  color: #293343;
  font-size: 18px;
  font-weight: 600;
}

:global(.add-user-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.add-user-dialog .el-dialog__body) {
  padding: 22px 24px 8px;
}

:global(.add-user-dialog .el-dialog__footer) {
  padding: 14px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.add-user-dialog .el-form-item__label) {
  color: #616978;
  font-size: 12px;
  font-weight: 600;
}

:deep(.add-user-dialog .el-input__wrapper) {
  min-height: 40px;
  border-radius: 9px;
  box-shadow: 0 0 0 1px #e3e6eb inset;
}

:deep(.add-user-dialog .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #b3976f inset;
}

.add-user-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 14px;
}

.role-form-item {
  margin-top: 4px;
}

@media (max-width: 760px) {
  .add-user-grid {
    grid-template-columns: 1fr;
  }
}
</style>
