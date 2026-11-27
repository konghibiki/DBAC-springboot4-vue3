<!--
  账号设置布局页面
  提供设置页面的面包屑、标题与侧栏导航（个人资料 / 邮箱 / 密码），
  主体区域通过 RouterView 渲染各设置子页面，支持页面间自由切换。
-->
<script lang="ts" setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import TopNavigationBar from '@/components/layout/TopNavigationBar.vue'

const route = useRoute()

// 当前路由决定侧栏选中项和面包屑末级标题。
const settingItems = [
  { name: 'settings-profile', label: '修改个人信息', mark: '改' },
  { name: 'settings-email', label: '更改绑定邮箱', mark: '绑' },
  { name: 'settings-password', label: '修改密码', mark: '密' },
] as const

const currentTitle = computed(
  () =>
    settingItems.find((item) => item.name === route.name)?.label ?? '账号设置',
)
</script>

<template>
  <div class="settings-page">
    <TopNavigationBar />

    <main class="settings-content">
      <!-- 面包屑保留首页入口，并标明当前所在的设置页面。 -->
      <el-breadcrumb class="settings-breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ name: 'home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>账号设置</el-breadcrumb-item>
        <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section class="settings-heading">
        <div>
          <p class="settings-heading__eyebrow">ACCOUNT SETTINGS</p>
          <h1>账号设置</h1>
          <p>管理你的个人资料、登录邮箱和账号安全。</p>
        </div>
        <span aria-hidden="true" class="settings-heading__mark">{{
          settingItems.find((item) => item.name === route.name)?.mark
        }}</span>
      </section>

      <div class="settings-layout">
        <!-- 侧栏路由让用户可在各设置页面之间直接切换。 -->
        <aside aria-label="设置导航" class="settings-sidebar">
          <p class="settings-sidebar__title">个人设置</p>
          <nav>
            <RouterLink
              v-for="item in settingItems"
              :key="item.name"
              :aria-current="route.name === item.name ? 'page' : undefined"
              :class="{
                'settings-sidebar__link--active': route.name === item.name,
              }"
              :to="{ name: item.name }"
              class="settings-sidebar__link"
            >
              <span class="settings-sidebar__icon">{{ item.mark }}</span>
              <span>{{ item.label }}</span>
              <svg aria-hidden="true" fill="none" viewBox="0 0 16 16">
                <path
                  d="m6 3 5 5-5 5"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.5"
                />
              </svg>
            </RouterLink>
          </nav>
          <div class="settings-sidebar__note">
            <span aria-hidden="true"></span>
            <p>请妥善保管你的账号信息。</p>
          </div>
        </aside>

        <section class="settings-panel">
          <RouterView />
        </section>
      </div>
    </main>
  </div>
</template>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 86% 0%, rgb(233 226 213 / 20%), transparent 32%),
    #f7f8fa;
  color: #293343;
}

.settings-content {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 27px 0 64px;
}

.settings-breadcrumb {
  margin-bottom: 24px;
  font-size: 11px;
}

.settings-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 26px;

  h1 {
    margin: 5px 0 4px;
    color: #293343;
    font-size: 26px;
    font-weight: 600;
    letter-spacing: -0.035em;
  }

  > div > p:last-child {
    margin: 0;
    color: #9096a3;
    font-size: 12px;
    line-height: 1.7;
  }

  &__eyebrow {
    margin: 0;
    color: #b3976f;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.18em;
  }

  &__mark {
    display: grid;
    width: 48px;
    height: 48px;
    place-items: center;
    border: 1px solid #e8e2d7;
    border-radius: 15px;
    background: #fff;
    box-shadow: 0 5px 14px rgb(34 42 57 / 4%);
    color: #b3976f;
    font-size: 21px;
    font-weight: 600;
  }
}

.settings-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
}

.settings-sidebar,
.settings-panel {
  border: 1px solid #eceef1;
  border-radius: 15px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 7px 22px rgb(34 42 57 / 3%);
}

.settings-sidebar {
  position: sticky;
  top: 96px;
  padding: 15px 10px 12px;

  &__title {
    margin: 0;
    padding: 4px 11px 12px;
    color: #9da2ad;
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 0.08em;
  }

  nav {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__link {
    display: flex;
    min-height: 44px;
    align-items: center;
    gap: 10px;
    padding: 0 11px;
    border-radius: 8px;
    color: #687080;
    font-size: 12px;
    text-decoration: none;
    transition:
      background-color 160ms ease,
      color 160ms ease;

    > svg {
      width: 14px;
      height: 14px;
      margin-left: auto;
      color: #a9aeb8;
      opacity: 0;
      transition: opacity 160ms ease;
    }

    &:hover {
      background: #f8f6f1;
      color: #806541;

      > svg {
        opacity: 1;
      }
    }

    &--active {
      background: #f5f2eb;
      color: #806541;
      font-weight: 600;

      > svg {
        color: #b3976f;
        opacity: 1;
      }
    }
  }

  &__icon {
    display: grid;
    width: 24px;
    height: 24px;
    place-items: center;
    border-radius: 7px;
    background: #f0eee8;
    color: #9d855f;
    font-size: 10px;
    font-weight: 600;
  }

  &__note {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin: 18px 4px 0;
    padding: 13px 8px 2px;
    border-top: 1px solid #f0f0f1;

    span {
      width: 5px;
      height: 5px;
      flex: 0 0 auto;
      margin-top: 5px;
      border-radius: 50%;
      background: #b3976f;
    }

    p {
      margin: 0;
      color: #a1a6b0;
      font-size: 12px;
      line-height: 1.6;
    }
  }
}

.settings-panel {
  min-height: 530px;
  padding: clamp(22px, 3.5vw, 36px);
}

@media (max-width: 760px) {
  .settings-content {
    width: min(100% - 32px, 600px);
    padding-top: 20px;
  }

  .settings-heading {
    margin-bottom: 19px;
  }

  .settings-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }

  .settings-sidebar {
    position: static;
    padding: 10px;

    &__title,
    &__note {
      display: none;
    }

    nav {
      flex-direction: row;
      gap: 5px;
    }

    &__link {
      min-height: 40px;
      flex: 1;
      justify-content: center;
      gap: 6px;
      padding: 0 6px;
      font-size: 10px;

      > svg {
        display: none;
      }
    }

    &__icon {
      width: 20px;
      height: 20px;
      flex: 0 0 auto;
      font-size: 9px;
    }
  }

  .settings-panel {
    min-height: 0;
    padding: 23px 18px;
  }
}

@media (max-width: 390px) {
  .settings-sidebar__link {
    flex-direction: column;
    min-height: 55px;
    gap: 3px;
  }
}
</style>
