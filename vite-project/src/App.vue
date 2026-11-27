<!--
  应用根组件
  使用 Element Plus 配置 Provider 注入中文语言包，
  挂载全局路由出口，是所有页面渲染的容器。
-->
<script lang="ts" setup>
import {ElConfigProvider} from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn.mjs'
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <router-view></router-view>
  </ElConfigProvider>
</template>

<style lang="scss" scoped>
:global(.el-loading-mask.nova-loading-mask) {
  background: rgb(35 43 57 / 28%) !important;
  backdrop-filter: blur(5px);
}

:global(.nova-loading-mask .el-loading-spinner) {
  top: 50%;
  left: 50%;
  width: max-content;
  margin: 0;
  padding: 22px 32px;
  border: 1px solid rgb(255 255 255 / 70%);
  border-radius: 16px;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 18px 50px rgb(26 35 50 / 18%);
  transform: translate(-50%, -50%);
}

:global(.nova-loading-mask .el-loading-spinner .circular) {
  width: 38px;
  height: 38px;
}

:global(.nova-loading-mask .el-loading-spinner .path) {
  stroke: #b3976f;
  stroke-width: 3;
  stroke-linecap: round;
}

:global(.nova-loading-mask .el-loading-spinner .el-loading-text) {
  margin-top: 13px;
  color: #293343;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.04em;
}

:global(.el-overlay.nova-login-backdrop) {
  background: rgb(29 37 51 / 42%);
  backdrop-filter: blur(5px);
}

:global(.el-message-box.nova-login-message) {
  width: min(420px, calc(100vw - 36px));
  padding: 26px 26px 22px;
  border: 1px solid #eee9df;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgb(26 35 50 / 24%);
}

:global(.nova-login-message .el-message-box__header) {
  padding-bottom: 14px;
}

:global(.nova-login-message .el-message-box__title) {
  color: #293343;
  font-size: 18px;
  font-weight: 600;
}

:global(.nova-login-message .el-message-box__status.el-icon) {
  color: #b3976f;
}

:global(.nova-login-message .el-message-box__message) {
  color: #737987;
  font-size: 14px;
  line-height: 1.7;
}

:global(.nova-login-message .el-message-box__btns) {
  justify-content: center;
  padding-top: 22px;
}

:global(.nova-login-message .el-message-box__btns .el-button--primary) {
  min-width: 144px;
  height: 42px;
  border-color: #293343;
  border-radius: 9px;
  background: #293343;
  font-weight: 600;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    transform 160ms ease;
}

:global(.nova-login-message .el-message-box__btns .el-button--primary:hover) {
  border-color: #39475b;
  background: #39475b;
  transform: translateY(-1px);
}
</style>
