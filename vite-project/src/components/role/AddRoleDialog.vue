<!--
  新增角色弹窗组件
  提供新增角色的表单弹窗：角色名称与职权分配，
  角色等级固定锁定为 2（管理员），不可修改，
  仅展示当前用户有权分配的职权供多选（精确权限、*.动作、模块.* 均可分配），
  提交后调用 /role/add 接口，成功后派发 success 事件供父组件刷新列表。
-->
<script lang="ts" setup>
import {computed, reactive, ref, watch} from 'vue'
import {
  ElCheckbox,
  ElCheckboxGroup,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {addRole, getSelfPermissions} from '@/api/user'
import {getRequestErrorMessage} from '@/utils/error'
import {buildSelfPermissionItems, groupPermissions, type PermissionItem,} from '@/utils/permissions'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

interface AddRoleForm {
  roleName: string
  roleLevel: number
  rolePermissions: string[]
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const isAddingRole = ref(false)

// 当前用户职权分配选项（/api/permission/self 返回），打开弹窗时实时刷新，
// 直接以接口返回的 permissionName 作为显示名、permissionCode 作为编码，
// 完全随当前用户职权动态调整，不依赖任何写死清单。
const selfPermissionItems = ref<PermissionItem[]>([])

const addRoleForm = reactive<AddRoleForm>({
  roleName: '',
  roleLevel: 2,
  rolePermissions: [],
})

// 当前用户有权分配的权限，以 /api/permission/self 返回的条目为准分区展示
const grantableGroups = computed(() =>
  groupPermissions(selfPermissionItems.value),
)

// 新增角色表单校验规则
const addRoleFormRules: FormRules<AddRoleForm> = {
  roleName: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入角色名称'))
        } else if (text.length > 18) {
          callback(new Error('角色名称不能超过18个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  rolePermissions: [
    {
      validator: (_rule, value: string[], callback) => {
        callback(value.length > 0 ? undefined : new Error('请至少选择一个职权'))
      },
      trigger: 'change',
    },
  ],
}

// 打开弹窗时重置表单，并从 /api/permission/self 实时刷新当前用户职权
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    addRoleForm.roleName = ''
    addRoleForm.roleLevel = 2
    addRoleForm.rolePermissions = []
    formRef.value?.clearValidate()
    await loadSelfPermissions()
  },
)

// 实时获取当前用户职权（/api/permission/self），作为职权分配选项的基准
async function loadSelfPermissions() {
  try {
    const response = await getSelfPermissions()
    selfPermissionItems.value = buildSelfPermissionItems(
      response.data.data ?? [],
    )
  } catch {
    // 接口暂不可用时保持空职权，可分配集合为空
    selfPermissionItems.value = []
  }
}

function close() {
  emit('update:modelValue', false)
}

// 校验并提交新增角色
async function submitAddRole() {
  if (!formRef.value || isAddingRole.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isAddingRole.value = true
  try {
    await addRole({
      roleName: addRoleForm.roleName.trim(),
      rolePermissions: [...addRoleForm.rolePermissions],
      roleLevel: addRoleForm.roleLevel,
    })
    ElMessage.success('角色添加成功')
    close()
    formRef.value.clearValidate()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '添加角色失败，请稍后重试'))
  } finally {
    isAddingRole.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    align-center
    class="add-role-dialog"
    draggable
    title="新增角色"
    width="600"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 新增角色表单 -->
    <ElForm
      ref="formRef"
      :model="addRoleForm"
      :rules="addRoleFormRules"
      label-position="top"
    >
      <!-- 角色名称与固定权限等级 -->
      <div class="add-role-grid">
        <ElFormItem label="角色名称" prop="roleName">
          <ElInput
            v-model="addRoleForm.roleName"
            maxlength="18"
            placeholder="请输入角色名称（不超过18个字符）"
          />
        </ElFormItem>
        <ElFormItem label="权限等级" prop="roleLevel">
          <ElInput
            :model-value="`等级 ${addRoleForm.roleLevel}（管理员）`"
            disabled
          />
        </ElFormItem>
      </div>

      <!-- 职权分配：仅展示当前用户有权分配的权限 -->
      <ElFormItem
        class="permission-form-item"
        label="职权分配"
        prop="rolePermissions"
      >
        <ElCheckboxGroup
          v-model="addRoleForm.rolePermissions"
          class="permission-options"
        >
          <section
            v-for="group in grantableGroups"
            :key="group.group"
            class="permission-group"
          >
            <h4 class="permission-group__title">{{ group.group }}</h4>
            <ElCheckbox
              v-for="permission in group.permissions"
              :key="permission.code"
              :value="permission.name"
              class="permission-option"
            >
              <span class="permission-option__label">
                <span class="permission-option__name">
                  {{ permission.name }}
                </span>
                <span class="permission-option__code">
                  （{{ permission.code }}）
                </span>
              </span>
            </ElCheckbox>
          </section>
        </ElCheckboxGroup>
      </ElFormItem>
    </ElForm>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :loading="isAddingRole"
        confirm-text="确认添加"
        loading-text="保存中…"
        @cancel="close"
        @confirm="submitAddRole"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.add-role-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(.add-role-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.add-role-dialog .el-dialog__title) {
  color: #293343;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

:global(.add-role-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.add-role-dialog .el-dialog__body) {
  padding: 24px 24px 10px;
}

:global(.add-role-dialog .el-dialog__footer) {
  padding: 15px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.add-role-dialog .el-form-item) {
  margin-bottom: 19px;
}

:deep(.add-role-dialog .el-form-item__label) {
  padding-bottom: 7px;
  color: #596171;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

:deep(.add-role-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.add-role-dialog .el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.add-role-dialog .el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.add-role-dialog .el-input.is-disabled .el-input__wrapper) {
  background: #f4f5f6;
  box-shadow: 0 0 0 1px #eaecf0 inset;
}

:deep(.add-role-dialog .el-input__inner) {
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
}

:deep(.add-role-dialog .el-input__inner::placeholder) {
  color: #a7acb6;
}

:deep(.add-role-dialog .el-form-item__error) {
  padding-top: 5px;
  color: #b46a6a;
  font-size: 11px;
}

.add-role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 16px;
}

.permission-form-item {
  margin-top: 8px;
}

.permission-options {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 14px;
  padding: 12px 14px 4px;
  border: 1px solid #eef0f3;
  border-radius: 11px;
  background: #fcfcfd;
}

.permission-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  &__title {
    width: 100%;
    margin: 0;
    color: #8b929e;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
}

.permission-option {
  height: 40px;
  margin: 0;
  border-radius: 9px;

  :deep(.el-checkbox__input) {
    margin-top: 3px;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner),
  :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
    border-color: #d58a32;
    background-color: #d58a32;
  }

  :deep(.el-checkbox__label) {
    display: inline-flex;
    align-items: baseline;
    gap: 8px;
    color: #414958;
    font-size: 12px;
    font-weight: 500;
  }

  &__name {
    white-space: nowrap;
  }

  &__code {
    color: #a6abb5;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 10px;
  }

  :deep(.el-checkbox.is-bordered) {
    border-color: #e3e6eb;
  }
}

@media (max-width: 640px) {
  .add-role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
