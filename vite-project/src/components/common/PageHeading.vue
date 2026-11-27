<!--
  页面通用标题组件
  用于展示页面级标题区域，包含眉行文案（eyebrow）、主标题、描述文案与右上角角标，
  统一后台管理各页面的头部视觉规范。
-->
<script lang="ts" setup>
defineProps<{
  eyebrow: string
  title: string
  description: string
  badge: string
}>()
</script>

<template>
  <header class="page-heading">
    <!-- 标题主区域：眉行、主标题与描述 -->
    <div>
      <p class="page-heading__eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>
    <!-- 右侧角标区域 -->
    <span class="page-heading__badge">{{ badge }}</span>
  </header>
</template>

<style lang="scss" scoped>
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eceef1;

  &__eyebrow {
    margin: 0;
    color: #b3976f;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.16em;
  }

  h1 {
    margin: 5px 0;
    color: #293343;
    font-size: 24px;
    font-weight: 600;
  }

  p:last-child {
    margin: 0;
    color: #969ca8;
    font-size: 12px;
  }

  &__badge {
    display: inline-flex;
    min-width: max-content;
    height: 38px;
    box-sizing: border-box;
    align-items: center;
    justify-content: center;
    padding: 0 12px;
    border-radius: 999px;
    background: #f5f2eb;
    color: #a68b62;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.04em;
    white-space: nowrap;
  }
}
</style>
