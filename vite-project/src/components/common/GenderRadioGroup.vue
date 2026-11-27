<!--
  用户表单性别选择组件
  封装"保密 / 男 / 女"三个性别选项的单选按钮组，
  供新增用户、修改用户等表单弹窗复用，统一性别字段的交互与样式。
-->
<script lang="ts" setup>
import {GENDER_OPTIONS} from '@/utils/user'

const model = defineModel<number>({ required: true })
</script>

<template>
  <ElRadioGroup v-model="model" class="gender-options">
    <ElRadio
      v-for="option in GENDER_OPTIONS"
      :key="option.value"
      :value="option.value"
      border
    >
      {{ option.label }}
    </ElRadio>
  </ElRadioGroup>
</template>

<style lang="scss" scoped>
.gender-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;

  :deep(.el-radio) {
    width: 100%;
    margin: 0;
  }

  :deep(.el-radio.is-bordered) {
    height: 40px;
  }

  :deep(.el-radio__input.is-checked .el-radio__inner) {
    border-color: #d58a32;
    background-color: #d58a32;
  }

  :deep(.el-radio__input.is-checked + .el-radio__label) {
    color: #b87525;
  }

  :deep(.el-radio__input.is-focus .el-radio__inner) {
    box-shadow: 0 0 0 2px rgba(213, 138, 50, 0.3);
  }

  :deep(.el-radio.is-checked) {
    border-color: #b3976f;
    background: #faf8f4;
  }
}
</style>
