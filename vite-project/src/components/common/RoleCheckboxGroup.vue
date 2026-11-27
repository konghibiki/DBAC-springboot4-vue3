<!--
  用户表单职能复选框组件
  将角色列表渲染为多选复选框组，支持加载中状态与空数据提示，
  供新增用户、修改用户等表单弹窗复用，统一职能字段的交互与样式。
-->
<script lang="ts" setup>
import type {UserRole} from '@/api/user'

const model = defineModel<string[]>({ default: () => [] })

defineProps<{
  roles: UserRole[]
  loading?: boolean
  emptyHint?: string
}>()
</script>

<template>
  <ElCheckboxGroup v-model="model" v-loading="loading" class="role-options">
    <ElCheckbox
      v-for="role in roles"
      :key="role.roleId"
      :value="role.roleName"
      border
    >
      {{ role.roleName }}
    </ElCheckbox>
  </ElCheckboxGroup>
  <!-- 空角色数据提示 -->
  <p v-if="emptyHint && roles.length === 0 && !loading" class="role-empty">
    {{ emptyHint }}
  </p>
</template>

<style lang="scss" scoped>
.role-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner),
  :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
    border-color: #d58a32;
    background-color: #d58a32;
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: #b87525;
  }

  :deep(.el-checkbox__input.is-focus .el-checkbox__inner) {
    border-color: #d58a32;
  }

  :deep(.el-checkbox) {
    margin: 0;
  }

  :deep(.el-checkbox.is-bordered) {
    height: 38px;
    border-radius: 9px;
  }

  :deep(.el-checkbox.is-checked) {
    border-color: #b3976f;
    background: #faf8f4;
  }
}

.role-empty {
  width: 100%;
  margin: 6px 0 0;
  color: #a2a7b1;
  font-size: 11px;
}
</style>
