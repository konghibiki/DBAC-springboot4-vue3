<!--
  用户单元格组件
  在用户表格中展示用户的头像占位字、用户名与 ID，
  供用户管理列表等场景复用，统一用户信息的展示样式。
-->
<script lang="ts" setup>
import type {UserInfo} from '@/api/user'
import {getAvatarText} from '@/utils/user'

defineProps<{
  user: UserInfo
}>()
</script>

<template>
  <div class="user-cell">
    <!-- 头像占位字 -->
    <span class="user-cell__avatar">{{ getAvatarText(user) }}</span>
    <!-- 用户名与 ID 信息 -->
    <div>
      <p class="user-cell__name">{{ user.userName || '未设置昵称' }}</p>
      <p class="user-cell__id">ID {{ user.userId }}</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell__avatar {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  background: #f2eee5;
  color: #967c56;
  font-size: 13px;
  font-weight: 700;
}

.user-cell__name,
.user-cell__id {
  margin: 0;
}

.user-cell__name {
  color: #414958;
  font-weight: 600;
}

.user-cell__id {
  margin-top: 4px;
  color: #a2a7b1;
  font-size: 10px;
}
</style>
