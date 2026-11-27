<!--
  修改权限弹窗组件
  提供修改权限的表单弹窗：打开时回显权限名称、权限标识、权限类型与权限描述，
  表单结构与新增权限保持一致，提交后调用 /permission/update 接口，
  并携带 permissionId，成功后派发 success 事件供父组件刷新列表。
-->
<script lang="ts" setup>
import {reactive, ref, watch} from 'vue'
import {
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {type PermissionInfo, updatePermission} from '@/api/user'
import {getRequestErrorMessage} from '@/utils/error'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

interface UpdatePermissionForm {
  permissionName: string
  permissionCode: string
  permissionType: string
  description: string
}

const props = defineProps<{
  modelValue: boolean
  permission: PermissionInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const isUpdatingPermission = ref(false)

const updatePermissionForm = reactive<UpdatePermissionForm>({
  permissionName: '',
  permissionCode: '',
  permissionType: '',
  description: '',
})

// 修改权限表单校验规则
const updatePermissionFormRules: FormRules<UpdatePermissionForm> = {
  permissionName: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入权限名称'))
        } else if (text.length > 100) {
          callback(new Error('权限名称不能超过100个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  permissionCode: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入权限标识'))
        } else if (text.length > 100) {
          callback(new Error('权限标识不能超过100个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  permissionType: [
    {
      validator: (_rule, value: string, callback) => {
        callback(value.trim() ? undefined : new Error('请选择权限类型'))
      },
      trigger: 'change',
    },
  ],
  description: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (text.length > 255) {
          callback(new Error('权限描述不能超过255个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
}

// 打开弹窗时回显权限信息
watch(
  () => [props.modelValue, props.permission] as const,
  ([visible, permission]) => {
    if (!visible || !permission) return
    updatePermissionForm.permissionName = permission.permissionName ?? ''
    updatePermissionForm.permissionCode = permission.permissionCode ?? ''
    updatePermissionForm.permissionType = permission.permissionType ?? ''
    updatePermissionForm.description = permission.permissionDesc ?? ''
    formRef.value?.clearValidate()
  },
)

function close() {
  emit('update:modelValue', false)
}

// 校验并提交修改权限
async function submitUpdatePermission() {
  if (!formRef.value || isUpdatingPermission.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isUpdatingPermission.value = true
  try {
    await updatePermission({
      permissionId: props.permission?.permissionId ?? 0,
      permissionName: updatePermissionForm.permissionName.trim(),
      permissionCode: updatePermissionForm.permissionCode.trim(),
      permissionType: updatePermissionForm.permissionType.trim(),
      description: updatePermissionForm.description.trim() || undefined,
    })
    ElMessage.success('权限修改成功')
    close()
    formRef.value.clearValidate()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '修改权限失败，请稍后重试'))
  } finally {
    isUpdatingPermission.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    align-center
    class="update-permission-dialog"
    draggable
    title="修改权限"
    width="620"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 修改权限表单 -->
    <ElForm
      ref="formRef"
      :model="updatePermissionForm"
      :rules="updatePermissionFormRules"
      label-position="top"
    >
      <!-- 权限名称与权限标识 -->
      <div class="update-permission-grid">
        <!-- 权限名称 -->
        <ElFormItem label="权限名称" prop="permissionName">
          <ElInput
            v-model="updatePermissionForm.permissionName"
            maxlength="100"
            placeholder="请输入权限名称（不超过100个字符）"
          />
        </ElFormItem>

        <!-- 权限标识 -->
        <ElFormItem label="权限标识" prop="permissionCode">
          <ElInput
            v-model="updatePermissionForm.permissionCode"
            maxlength="100"
            placeholder="请输入权限标识（不超过100个字符）"
          />
        </ElFormItem>
      </div>

      <!-- 权限类型 -->
      <ElFormItem label="权限类型" prop="permissionType">
        <ElSelect
          v-model="updatePermissionForm.permissionType"
          placeholder="请选择权限类型"
        >
          <ElOption label="BUTTON" value="BUTTON" />
          <ElOption label="API" value="API" />
        </ElSelect>
      </ElFormItem>

      <!-- 权限描述 -->
      <ElFormItem label="权限描述" prop="description">
        <ElInput
          v-model="updatePermissionForm.description"
          :rows="3"
          maxlength="255"
          placeholder="请输入权限描述（可选，不超过255个字符）"
          type="textarea"
        />
      </ElFormItem>
    </ElForm>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :loading="isUpdatingPermission"
        confirm-text="确认修改"
        loading-text="保存中…"
        @cancel="close"
        @confirm="submitUpdatePermission"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.update-permission-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(.update-permission-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.update-permission-dialog .el-dialog__title) {
  color: #293343;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

:global(.update-permission-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.update-permission-dialog .el-dialog__body) {
  padding: 24px 24px 10px;
}

:global(.update-permission-dialog .el-dialog__footer) {
  padding: 15px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.update-permission-dialog .el-form-item) {
  margin-bottom: 19px;
}

:deep(.update-permission-dialog .el-form-item__label) {
  padding-bottom: 7px;
  color: #596171;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

:deep(.update-permission-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.update-permission-dialog .el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.update-permission-dialog .el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.update-permission-dialog .el-input__inner) {
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
}

:deep(.update-permission-dialog .el-input__inner::placeholder) {
  color: #a7acb6;
}

:deep(.update-permission-dialog .el-textarea__inner) {
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
  padding: 10px 12px;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.update-permission-dialog .el-textarea__inner:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.update-permission-dialog .el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.update-permission-dialog .el-textarea__inner::placeholder) {
  color: #a7acb6;
}

:deep(.update-permission-dialog .el-select) {
  width: 100%;
}

:deep(.update-permission-dialog .el-select__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  font-size: 13px;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.update-permission-dialog .el-select__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.update-permission-dialog .el-select__wrapper.is-focused) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.update-permission-dialog .el-select__selected-item) {
  color: #343c4b;
  font-family: inherit;
}

:deep(.update-permission-dialog .el-select__placeholder) {
  color: #a7acb6;
}

:deep(.update-permission-dialog .el-select__caret) {
  color: #a0a5af;
}

:deep(.update-permission-dialog .el-form-item__error) {
  padding-top: 5px;
  color: #b46a6a;
  font-size: 11px;
}

.update-permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 16px;
}

@media (max-width: 640px) {
  .update-permission-grid {
    grid-template-columns: 1fr;
  }
}
</style>
