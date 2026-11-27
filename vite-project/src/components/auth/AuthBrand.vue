<!--
  品牌 Logo 组件
  展示 NOVA 品牌标识（星标 + 文字），支持深色与紧凑两种形态，
  供登录页与导航栏等场景复用以保持品牌视觉统一。
-->
<script lang="ts" setup>
withDefaults(
  defineProps<{
    dark?: boolean
    compact?: boolean
  }>(),
  {
    dark: false,
    compact: false,
  },
)
</script>

<template>
  <span
    :class="{
      'auth-brand--dark': dark,
      'auth-brand--compact': compact,
    }"
    aria-label="NOVA"
    class="auth-brand"
    style="user-select: none"
  >
    <span aria-hidden="true" class="auth-brand__mark">
      <svg fill="none" viewBox="0 0 32 32">
        <path
          d="M16 3.5 19.8 12.2 28.5 16l-8.7 3.8L16 28.5l-3.8-8.7L3.5 16l8.7-3.8L16 3.5Z"
          fill="currentColor"
        />
      </svg>
    </span>
    <span>NOVA</span>
  </span>
</template>

<style lang="scss" scoped>
.auth-brand {
  position: relative;
  z-index: 1;
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.24em;
  text-decoration: none;

  &--dark {
    color: #293343;
  }

  &--compact {
    gap: 8px;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0.2em;

    .auth-brand__mark {
      width: 27px;
      height: 27px;
    }
  }

  &__mark {
    display: inline-grid;
    width: 34px;
    height: 34px;
    place-items: center;
    color: #d7bd93;

    svg {
      width: 100%;
      height: 100%;
    }
  }
}

@media (max-width: 760px) {
  .auth-brand:not(.auth-brand--compact) {
    font-size: 15px;

    .auth-brand__mark {
      width: 28px;
      height: 28px;
    }
  }
}
</style>
