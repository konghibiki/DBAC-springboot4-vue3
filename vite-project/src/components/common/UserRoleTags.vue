<!--
  用户职能标签组件
  将用户的职能列表渲染为小标签，并处理"未分配"的空状态展示，
  供用户表格等场景复用，统一职能信息的展示样式。
-->
<script lang="ts" setup>
import type {UserInfo} from '@/api/user'

defineProps<{
  role: UserInfo['role']
}>()
</script>

<template>
  <div v-if="Array.isArray(role) ? role.length > 0 : !!role" class="role-tags">
    <!-- 遍历渲染职能标签 -->
    <span
      v-for="roleName in Array.isArray(role) ? role : role ? [role] : []"
      :key="roleName"
      class="role-tag"
    >
      {{ roleName }}
    </span>
  </div>
  <!-- 未分配职能的空状态 -->
  <span v-else class="role-tags__empty">未分配</span>
</template>

<style lang="scss" scoped>
.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.role-tag {
  padding: 4px 8px;
  border-radius: 6px;
  background: #f3f0e8;
  color: #92774f;
  font-size: 10px;
  font-weight: 600;
}

.role-tags__empty {
  color: #a2a7b1;
  font-size: 12px;
}
</style>
