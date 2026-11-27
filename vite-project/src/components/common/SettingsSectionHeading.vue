<!--
  设置区块标题组件
  用于账号设置页面（个人资料、邮箱、密码等）内部的区块标题展示，
  统一眉行文案、标题、描述与角标的布局样式，供多个设置页面复用。
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
  <header class="settings-section-heading">
    <!-- 标题主区域：眉行、主标题与描述 -->
    <div>
      <p class="settings-section-heading__eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <!-- 右侧角标区域 -->
    <span class="settings-section-heading__badge">{{ badge }}</span>
  </header>
</template>

<style lang="scss" scoped>
.settings-section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 26px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f1f3;

  &__eyebrow {
    margin: 0;
    color: #b3976f;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.16em;
  }

  h2 {
    margin: 5px 0;
    color: #293343;
    font-size: 19px;
    font-weight: 600;
  }

  p:last-child {
    margin: 0;
    color: #969ca8;
    font-size: 11px;
    line-height: 1.6;
  }

  &__badge {
    display: grid;
    width: 34px;
    height: 34px;
    place-items: center;
    border-radius: 10px;
    background: #f7f4ee;
    color: #b3976f;
    font-size: 10px;
    font-weight: 700;
  }
}
</style>
