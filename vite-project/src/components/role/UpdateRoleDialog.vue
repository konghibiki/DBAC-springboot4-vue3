<!--
  修改角色弹窗组件
  提供修改角色的表单弹窗：回显角色名称、权限等级，
  仅展示当前用户有权分配的职权供多选（精确权限、*.动作、模块.* 均可分配），
  角色等级固定不可修改，提交后调用 /role/update 接口，
  成功后派发 success 事件供父组件刷新列表。
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
import {getRolePermissions, getSelfPermissions, type RoleInfo, updateRole,} from '@/api/user'
import {getRequestErrorMessage} from '@/utils/error'
import {
  buildSelfPermissionItems,
  groupPermissions,
  type PermissionItem,
  resolveRolePermissionNames,
} from '@/utils/permissions'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

interface UpdateRoleForm {
  roleName: string
  roleLevel: number
  rolePermissions: string[]
}

const props = defineProps<{
  modelValue: boolean
  role: RoleInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const isUpdatingRole = ref(false)

// 当前用户职权分配选项（/api/permission/self 返回），打开弹窗时实时刷新，
// 直接以接口返回的 permissionName 作为显示名、permissionCode 作为编码，
// 完全随当前用户职权动态调整，不依赖任何写死清单。
const selfPermissionItems = ref<PermissionItem[]>([])

const updateRoleForm = reactive<UpdateRoleForm>({
  roleName: '',
  roleLevel: 0,
  rolePermissions: [],
})

// 级别文案：1 最高等级 / 2 管理员 / 3 普通用户
function levelText(level: number): string {
  switch (level) {
    case 1:
      return '最高等级'
    case 2:
      return '管理员'
    case 3:
      return '普通用户'
    default:
      return `等级 ${level}`
  }
}

// 当前用户有权分配的职权，以 /api/permission/self 返回的条目为准，
// 直接按权限条目分区展示，不再依赖权限码通配过滤
const grantableGroups = computed(() =>
  groupPermissions(selfPermissionItems.value),
)

// 修改角色表单校验规则
const updateRoleFormRules: FormRules<UpdateRoleForm> = {
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

// 打开弹窗时回显角色信息、清空历史校验，
// 实时刷新当前用户职权与权限目录后回显该角色已持有的职权
watch(
  () => [props.modelValue, props.role] as const,
  async ([visible, role]) => {
    if (!visible || !role) return
    updateRoleForm.roleName = role.roleName
    updateRoleForm.roleLevel = role.roleLevel
    updateRoleForm.rolePermissions = []
    formRef.value?.clearValidate()
    // 每次打开都从 /api/permission/self 实时获取当前用户职权，作为职权分配选项
    await loadSelfPermissions()
    // 再从 /api/permission/role 获取该角色已有的职权，回显其中可勾选的部分
    await loadRolePermissionNames(role.roleId)
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

// 通过 /permission/role 获取被修改角色已有的职权并回显到表单
// 仅回显当前用户职权（/permission/self）里真实存在的项，
// 角色独有而当前用户没有的职权不显示也不勾选
async function loadRolePermissionNames(roleId: number) {
  try {
    const response = await getRolePermissions(roleId)
    const grantableNames = new Set(
      selfPermissionItems.value.map((item) => item.name),
    )
    updateRoleForm.rolePermissions = resolveRolePermissionNames(
      response.data.data,
      null,
      selfPermissionItems.value,
    ).filter((name) => grantableNames.has(name))
  } catch {
    // 加载失败时静默清空，避免残留的旧数据干扰回显
    updateRoleForm.rolePermissions = []
  }
}

function close() {
  emit('update:modelValue', false)
}

// 校验并提交修改角色
async function submitUpdateRole() {
  if (!formRef.value || isUpdatingRole.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isUpdatingRole.value = true
  try {
    await updateRole({
      roleId: props.role?.roleId ?? 0,
      roleName: updateRoleForm.roleName.trim(),
      rolePermissions: [...updateRoleForm.rolePermissions],
      roleLevel: updateRoleForm.roleLevel,
    })
    ElMessage.success('角色修改成功')
    close()
    formRef.value.clearValidate()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '修改角色失败，请稍后重试'))
  } finally {
    isUpdatingRole.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    align-center
    class="update-role-dialog"
    draggable
    title="修改角色"
    width="600"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 修改角色表单 -->
    <ElForm
      ref="formRef"
      :model="updateRoleForm"
      :rules="updateRoleFormRules"
      label-position="top"
    >
      <!-- 角色名称与权限等级 -->
      <div class="update-role-grid">
        <ElFormItem label="角色名称" prop="roleName">
          <ElInput
            v-model="updateRoleForm.roleName"
            maxlength="18"
            placeholder="请输入角色名称（不超过18个字符）"
          />
        </ElFormItem>
        <ElFormItem label="权限等级" prop="roleLevel">
          <ElInput
            :model-value="`等级 ${updateRoleForm.roleLevel}（${levelText(updateRoleForm.roleLevel)}）`"
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
          v-model="updateRoleForm.rolePermissions"
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
        :loading="isUpdatingRole"
        confirm-text="确认修改"
        loading-text="保存中…"
        @cancel="close"
        @confirm="submitUpdateRole"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.update-role-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(.update-role-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.update-role-dialog .el-dialog__title) {
  color: #293343;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

:global(.update-role-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.update-role-dialog .el-dialog__body) {
  padding: 24px 24px 10px;
}

:global(.update-role-dialog .el-dialog__footer) {
  padding: 15px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.update-role-dialog .el-form-item) {
  margin-bottom: 19px;
}

:deep(.update-role-dialog .el-form-item__label) {
  padding-bottom: 7px;
  color: #596171;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

:deep(.update-role-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.update-role-dialog .el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.update-role-dialog .el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.update-role-dialog .el-input.is-disabled .el-input__wrapper) {
  background: #f4f5f6;
  box-shadow: 0 0 0 1px #eaecf0 inset;
}

:deep(.update-role-dialog .el-input__inner) {
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
}

:deep(.update-role-dialog .el-input__inner::placeholder) {
  color: #a7acb6;
}

:deep(.update-role-dialog .el-form-item__error) {
  padding-top: 5px;
  color: #b46a6a;
  font-size: 11px;
}

.update-role-grid {
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
  .update-role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
