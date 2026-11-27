<!--
  后台管理布局框架
  组合顶部导航栏与侧边栏，提供内容区域的 RouterView 出口，
  首页与所有系统管理子页面共用此布局。
-->
<script lang="ts" setup>
import TopNavigationBar from '@/components/layout/TopNavigationBar.vue'
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
</script>

<template>
  <div class="admin-layout">
    <TopNavigationBar />
    <div class="admin-layout__body">
      <AdminSidebar />
      <!-- 首页与管理子页面共用此内容区域。 -->
      <main class="admin-layout__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-layout {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 86% 0%, rgb(233 226 213 / 24%), transparent 32%),
    #f7f8fa;
  color: #293343;

  &__body {
    display: grid;
    min-height: calc(100vh - 76px);
    grid-template-columns: 248px minmax(0, 1fr);
    align-items: start;
  }

  &__main {
    min-width: 0;
  }
}

@media (max-width: 760px) {
  .admin-layout__body {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
