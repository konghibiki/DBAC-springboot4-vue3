<!--
  新增菜单弹窗组件
  提供新增菜单的表单弹窗：菜单名称、路由路径、路由名称、组件路径、
  权限标识（从 /permission/listOfList 拉取带 list 的权限做单选）、
  图标（可空）与路由缓存开关，
  提交后调用 /menu/add 接口，成功后派发 success 事件供父组件刷新列表。
-->
<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {addMenu, getPermissionListOfList} from '@/api/menu'
import {getRequestErrorMessage} from '@/utils/error'
import ActionDialogFooter from '@/components/common/ActionDialogFooter.vue'

interface AddMenuForm {
  menuName: string
  routePath: string
  routeName: string
  componentPath: string
  permissionCode: string
  icon: string
  keepAlive: boolean
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const isAddingMenu = ref(false)
const permissionOptions = ref<string[]>([])
const permissionLoading = ref(false)

const addMenuForm = reactive<AddMenuForm>({
  menuName: '',
  routePath: '',
  routeName: '',
  componentPath: '',
  permissionCode: '',
  icon: '',
  keepAlive: false,
})

// 只保留带 list 后缀的权限标识作为下拉选项
const visiblePermissionOptions = computed(() =>
  permissionOptions.value.filter((code) => code.endsWith('.list')),
)

// 新增菜单表单校验规则
const addMenuFormRules: FormRules<AddMenuForm> = {
  menuName: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入菜单名称'))
        } else if (text.length > 20) {
          callback(new Error('菜单名称不能超过20个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  routePath: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入路由路径'))
        } else if (text.length > 200) {
          callback(new Error('路由路径不能超过200个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  routeName: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入路由名称'))
        } else if (text.length > 50) {
          callback(new Error('路由名称不能超过50个字符'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  componentPath: [
    {
      validator: (_rule, value: string, callback) => {
        const text = value?.trim() ?? ''
        if (!text) {
          callback(new Error('请输入组件路径'))
        } else if (text.length > 200) {
          callback(new Error('组件路径不能超过200个字符'))
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
        callback(value.trim() ? undefined : new Error('请选择一个权限标识'))
      },
      trigger: 'change',
    },
  ],
}

// 拉取所有带 list 的权限选项
async function fetchPermissionOptions() {
  permissionLoading.value = true
  try {
    const response = await getPermissionListOfList()
    permissionOptions.value = response.data.data ?? []
  } catch (error) {
    permissionOptions.value = []
    ElMessage.error(getRequestErrorMessage(error, '获取权限选项失败'))
  } finally {
    permissionLoading.value = false
  }
}

// 打开弹窗时重置表单并刷新权限选项
watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    addMenuForm.menuName = ''
    addMenuForm.routePath = ''
    addMenuForm.routeName = ''
    addMenuForm.componentPath = ''
    addMenuForm.permissionCode = ''
    addMenuForm.icon = ''
    addMenuForm.keepAlive = false
    formRef.value?.clearValidate()
    void fetchPermissionOptions()
  },
)

onMounted(() => {
  if (props.modelValue) void fetchPermissionOptions()
})

function close() {
  emit('update:modelValue', false)
}

// 校验并提交新增菜单
async function submitAddMenu() {
  if (!formRef.value || isAddingMenu.value) return
  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isAddingMenu.value = true
  try {
    await addMenu({
      menuName: addMenuForm.menuName.trim(),
      routePath: addMenuForm.routePath.trim(),
      routeName: addMenuForm.routeName.trim(),
      componentPath: addMenuForm.componentPath.trim(),
      permissionCode: addMenuForm.permissionCode.trim(),
      keepAlive: addMenuForm.keepAlive,
      icon: addMenuForm.icon.trim() || undefined,
    })
    ElMessage.success('菜单添加成功')
    close()
    formRef.value.clearValidate()
    emit('success')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '添加菜单失败，请稍后重试'))
  } finally {
    isAddingMenu.value = false
  }
}
</script>

<template>
  <ElDialog
    :close-on-click-modal="false"
    :model-value="modelValue"
    align-center
    class="add-menu-dialog"
    draggable
    title="新增菜单"
    width="620"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 新增菜单表单 -->
    <ElForm
      ref="formRef"
      :model="addMenuForm"
      :rules="addMenuFormRules"
      label-position="top"
    >
      <!-- 菜单名称与权限标识 -->
      <div class="add-menu-grid">
        <ElFormItem label="菜单名称" prop="menuName">
          <ElInput
            v-model="addMenuForm.menuName"
            maxlength="20"
            placeholder="请输入菜单名称（不超过20个字符）"
          />
        </ElFormItem>
        <ElFormItem label="权限标识" prop="permissionCode">
          <ElSelect
            v-model="addMenuForm.permissionCode"
            :loading="permissionLoading"
            placeholder="请选择带 list 的权限"
          >
            <ElOption
              v-for="code in visiblePermissionOptions"
              :key="code"
              :label="code"
              :value="code"
            />
          </ElSelect>
        </ElFormItem>
      </div>

      <!-- 路由路径与路由名称 -->
      <div class="add-menu-grid">
        <ElFormItem label="路由路径" prop="routePath">
          <ElInput
            v-model="addMenuForm.routePath"
            maxlength="200"
            placeholder="请输入路由路径（不超过200个字符）"
          />
        </ElFormItem>
        <ElFormItem label="路由名称" prop="routeName">
          <ElInput
            v-model="addMenuForm.routeName"
            maxlength="50"
            placeholder="请输入路由名称（不超过50个字符）"
          />
        </ElFormItem>
      </div>

      <!-- 组件路径 -->
      <ElFormItem label="组件路径" prop="componentPath">
        <ElInput
          v-model="addMenuForm.componentPath"
          maxlength="200"
          placeholder="请输入组件路径（不超过200个字符）"
        />
      </ElFormItem>

      <!-- 图标与路由缓存 -->
      <div class="add-menu-grid">
        <ElFormItem label="图标" prop="icon">
          <ElInput
            v-model="addMenuForm.icon"
            maxlength="100"
            placeholder="请输入图标名称（可选）"
          />
        </ElFormItem>
        <ElFormItem label="路由缓存" prop="keepAlive">
          <ElRadioGroup
            v-model="addMenuForm.keepAlive"
            class="keep-alive-group"
          >
            <ElRadio :value="true">是</ElRadio>
            <ElRadio :value="false">否</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
      </div>
    </ElForm>

    <!-- 弹窗底部操作 -->
    <template #footer>
      <ActionDialogFooter
        :loading="isAddingMenu"
        confirm-text="确认添加"
        loading-text="保存中…"
        @cancel="close"
        @confirm="submitAddMenu"
      />
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.add-menu-dialog) {
  overflow: hidden;
  border: 1px solid #eee9df;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(26 35 50 / 18%);
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(.add-menu-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid #f0f1f3;
}

:global(.add-menu-dialog .el-dialog__title) {
  color: #293343;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

:global(.add-menu-dialog .el-dialog__headerbtn) {
  top: 8px;
  right: 8px;
  width: 38px;
  height: 38px;
}

:global(.add-menu-dialog .el-dialog__body) {
  padding: 24px 24px 10px;
}

:global(.add-menu-dialog .el-dialog__footer) {
  padding: 15px 24px 22px;
  border-top: 1px solid #f0f1f3;
}

:deep(.add-menu-dialog .el-form-item) {
  margin-bottom: 19px;
}

:deep(.add-menu-dialog .el-form-item__label) {
  padding-bottom: 7px;
  color: #596171;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

:deep(.add-menu-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.add-menu-dialog .el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.add-menu-dialog .el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.add-menu-dialog .el-input__inner) {
  color: #343c4b;
  font-family: inherit;
  font-size: 13px;
}

:deep(.add-menu-dialog .el-input__inner::placeholder) {
  color: #a7acb6;
}

:deep(.add-menu-dialog .el-select__wrapper) {
  min-height: 42px;
  border-radius: 10px;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #e3e6eb inset;
  font-size: 13px;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease;
}

:deep(.add-menu-dialog .el-select__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #c9c1b2 inset;
}

:deep(.add-menu-dialog .el-select__wrapper.is-focused) {
  background: #fff;
  box-shadow: 0 0 0 1px #b3976f inset;
}

:deep(.add-menu-dialog .el-select__selected-item) {
  color: #343c4b;
  font-family: inherit;
}

:deep(.add-menu-dialog .el-select__placeholder) {
  color: #a7acb6;
}

:deep(.add-menu-dialog .el-select__caret) {
  color: #a0a5af;
}

:deep(.add-menu-dialog .el-form-item__error) {
  padding-top: 5px;
  color: #b46a6a;
  font-size: 11px;
}

.add-menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 16px;
}

:deep(.add-menu-dialog .keep-alive-group .el-radio) {
  height: 42px;
  margin-right: 12px;
  color: #343c4b;
  font-size: 13px;
}

:deep(.add-menu-dialog .keep-alive-group .el-radio__inner) {
  border-color: #d5d8de;
}

:deep(
  .add-menu-dialog .keep-alive-group .el-radio.is-checked .el-radio__inner
) {
  border-color: #d58a32;
  background-color: #d58a32;
}

@media (max-width: 640px) {
  .add-menu-grid {
    grid-template-columns: 1fr;
  }
}
</style>
