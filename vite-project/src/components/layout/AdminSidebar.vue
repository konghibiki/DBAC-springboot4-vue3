<!--
  后台管理侧边栏组件
  提供系统管理菜单的展开/收起与当前选中态导航，
  直接访问子页面时自动展开对应菜单分组，保证导航上下文可感知。
-->
<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {useUserStore} from '@/store'

const route = useRoute()
const userStore = useUserStore()
const isSystemExpanded = ref(true)

// 挂载时不强制刷新菜单，复用路由守卫已加载的缓存，
// 避免清空菜单缓存造成 canAccessMenu 短暂失效，引发登录跳转被误拦截的竞态。
onMounted(() => {
  void userStore.loadMenus()
})

// 系统管理子菜单仅展示当前用户被授权的菜单。
const systemChildren = computed(() =>
  ['system-users', 'system-roles', 'system-menus', 'system-permissions'].filter(
    (name) => userStore.canAccessMenu(name),
  ),
)

const isSystemActive = computed(() =>
  systemChildren.value.includes(String(route.name)),
)

// 直接访问系统管理子页面时自动展开菜单，保持导航上下文可见。
watch(
  isSystemActive,
  (active) => {
    if (active) isSystemExpanded.value = true
  },
  { immediate: true },
)
</script>

<template>
  <aside aria-label="后台管理导航" class="admin-sidebar">
    <p class="admin-sidebar__caption">工作台</p>
    <!-- 系统管理子菜单可折叠，并随当前路由同步高亮项。 -->
    <nav aria-label="主要导航" class="admin-sidebar__menu">
      <RouterLink
        :aria-current="route.name === 'home' ? 'page' : undefined"
        :class="{ 'admin-sidebar__item--active': route.name === 'home' }"
        :to="{ name: 'home' }"
        class="admin-sidebar__item"
      >
        <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
          <path
            d="m3.5 10 8.5-6.5 8.5 6.5v9a1.5 1.5 0 0 1-1.5 1.5h-14A1.5 1.5 0 0 1 3.5 19v-9Z"
            stroke="currentColor"
            stroke-linejoin="round"
            stroke-width="1.6"
          />
          <path
            d="M9 20.5v-6a3 3 0 0 1 6 0v6"
            stroke="currentColor"
            stroke-linejoin="round"
            stroke-width="1.6"
          />
        </svg>
        <span>首页</span>
      </RouterLink>

      <div v-if="systemChildren.length > 0" class="admin-sidebar__group">
        <button
          :aria-expanded="isSystemExpanded"
          :class="{
            'admin-sidebar__item--active': isSystemActive,
            'admin-sidebar__item--expanded': isSystemExpanded,
          }"
          aria-controls="admin-system-submenu"
          class="admin-sidebar__item admin-sidebar__group-button"
          type="button"
          @click="isSystemExpanded = !isSystemExpanded"
        >
          <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
            <rect
              height="16"
              rx="2"
              stroke="currentColor"
              stroke-width="1.6"
              width="18"
              x="3"
              y="4"
            />
            <path
              d="M8 4v16M3 9h18M3 15h18"
              stroke="currentColor"
              stroke-width="1.4"
            />
          </svg>
          <span>系统管理</span>
          <svg
            aria-hidden="true"
            class="admin-sidebar__chevron"
            fill="none"
            viewBox="0 0 16 16"
          >
            <path
              d="m4 6 4 4 4-4"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
          </svg>
        </button>

        <Transition name="admin-submenu">
          <div
            v-show="isSystemExpanded"
            id="admin-system-submenu"
            class="admin-sidebar__children"
          >
            <RouterLink
              v-if="userStore.canAccessMenu('system-users')"
              :aria-current="route.name === 'system-users' ? 'page' : undefined"
              :class="{
                'admin-sidebar__child--active': route.name === 'system-users',
              }"
              :to="{ name: 'system-users' }"
              class="admin-sidebar__child"
            >
              <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                <circle
                  cx="9"
                  cy="8"
                  r="3.25"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="M3.5 19a5.5 5.5 0 0 1 11 0M16 5.2a3.25 3.25 0 0 1 0 6.3M17 14a5 5 0 0 1 3.5 4.8"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="1.5"
                />
              </svg>
              用户管理
            </RouterLink>
            <RouterLink
              v-if="userStore.canAccessMenu('system-roles')"
              :aria-current="route.name === 'system-roles' ? 'page' : undefined"
              :class="{
                'admin-sidebar__child--active': route.name === 'system-roles',
              }"
              :to="{ name: 'system-roles' }"
              class="admin-sidebar__child"
            >
              <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                <path
                  d="m12 3 2.4 1.4 2.8-.1.9 2.6 2.2 1.7-.8 2.7.8 2.7-2.2 1.7-.9 2.6-2.8-.1L12 20l-2.4-1.4-2.8.1-.9-2.6-2.2-1.7.8-2.7-.8-2.7 2.2-1.7.9-2.6 2.8.1L12 3Z"
                  stroke="currentColor"
                  stroke-linejoin="round"
                  stroke-width="1.5"
                />
                <circle
                  cx="12"
                  cy="11.5"
                  r="2.5"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="M8.8 17a3.4 3.4 0 0 1 6.4 0"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="1.5"
                />
              </svg>
              角色管理
            </RouterLink>
            <RouterLink
              v-if="userStore.canAccessMenu('system-menus')"
              :aria-current="route.name === 'system-menus' ? 'page' : undefined"
              :class="{
                'admin-sidebar__child--active': route.name === 'system-menus',
              }"
              :to="{ name: 'system-menus' }"
              class="admin-sidebar__child"
            >
              <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                <path
                  d="M9 6h11M9 12h11M9 18h11"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="1.6"
                />
                <circle cx="4.5" cy="6" fill="currentColor" r="1" />
                <circle cx="4.5" cy="12" fill="currentColor" r="1" />
                <circle cx="4.5" cy="18" fill="currentColor" r="1" />
              </svg>
              菜单管理
            </RouterLink>
            <RouterLink
              v-if="userStore.canAccessMenu('system-permissions')"
              :aria-current="
                route.name === 'system-permissions' ? 'page' : undefined
              "
              :class="{
                'admin-sidebar__child--active':
                  route.name === 'system-permissions',
              }"
              :to="{ name: 'system-permissions' }"
              class="admin-sidebar__child"
            >
              <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                <path
                  d="M9.5 11.5a2.5 2.5 0 1 0 5 0 2.5 2.5 0 0 0-5 0Z"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="M9.2 15.8c1.9 1.5 3.7 1.5 5.6 0l.7.4a2.2 2.2 0 0 1 3-3l-.1-.6c.9-1.9.9-3.7 0-5.6l.6-.7a2.2 2.2 0 0 0-3-3l-.6.1c-1.9-.9-3.7-.9-5.6 0l-.7-.4a2.2 2.2 0 0 0-3 3l.1.6c-.9 1.9-.9 3.7 0 5.6l-.6.7a2.2 2.2 0 0 0 3 3l.6-.1Z"
                  stroke="currentColor"
                  stroke-linejoin="round"
                  stroke-width="1.5"
                />
              </svg>
              权限管理
            </RouterLink>
          </div>
        </Transition>
      </div>
    </nav>
  </aside>
</template>

<style lang="scss" scoped>
.admin-sidebar {
  position: sticky;
  top: 76px;
  box-sizing: border-box;
  height: calc(100vh - 76px);
  overflow-y: auto;
  padding: 24px 12px;
  border-right: 1px solid #eceef1;
  background: rgb(255 255 255 / 94%);

  &__caption {
    margin: 0 10px 12px;
    color: #a0a5b0;
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 0.1em;
  }

  &__menu {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  &__item {
    display: flex;
    width: 100%;
    min-height: 44px;
    align-items: center;
    gap: 11px;
    padding: 0 12px;
    border: 0;
    border-radius: 9px;
    background: transparent;
    color: #687080;
    cursor: pointer;
    font: inherit;
    font-size: 13px;
    text-align: left;
    text-decoration: none;
    transition:
      background-color 160ms ease,
      color 160ms ease;

    > svg:first-child {
      width: 19px;
      height: 19px;
      flex: 0 0 auto;
    }

    &:hover,
    &:focus-visible {
      outline: none;
      background: #f8f6f1;
      color: #806541;
    }

    &--active {
      background: #f5f2eb;
      color: #806541;
      font-weight: 600;
    }
  }

  &__group-button {
    font-family: inherit;
  }

  &__chevron {
    width: 15px;
    height: 15px;
    margin-left: auto;
    transition: transform 160ms ease;
  }

  &__group-button.admin-sidebar__item--expanded .admin-sidebar__chevron {
    transform: rotate(180deg);
  }

  &__children {
    display: flex;
    flex-direction: column;
    gap: 3px;
    margin: 4px 0 6px 21px;
    padding-left: 12px;
    border-left: 1px solid #ebe8e1;
  }

  &__child {
    display: flex;
    min-height: 38px;
    align-items: center;
    gap: 9px;
    padding: 0 10px;
    border-radius: 8px;
    color: #7d8491;
    font-size: 12px;
    text-decoration: none;
    transition:
      background-color 160ms ease,
      color 160ms ease;

    > svg {
      width: 16px;
      height: 16px;
      flex: 0 0 auto;
    }

    &:hover,
    &:focus-visible {
      outline: none;
      background: #faf8f4;
      color: #806541;
    }

    &--active {
      background: #faf8f4;
      color: #806541;
      font-weight: 600;
    }
  }
}

.admin-submenu-enter-active,
.admin-submenu-leave-active {
  overflow: hidden;
  transition:
    max-height 180ms ease,
    opacity 160ms ease;
}

.admin-submenu-enter-from,
.admin-submenu-leave-to {
  max-height: 0;
  opacity: 0;
}

.admin-submenu-enter-to,
.admin-submenu-leave-from {
  max-height: 150px;
  opacity: 1;
}

@media (max-width: 760px) {
  .admin-sidebar {
    position: static;
    height: auto;
    overflow: visible;
    padding: 12px 16px;
    border-right: 0;
    border-bottom: 1px solid #eceef1;

    &__caption {
      display: none;
    }

    &__menu {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 6px;
    }

    &__group {
      grid-column: 1 / -1;
    }

    &__children {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 4px;
      margin: 5px 0 0;
      padding: 0;
      border-left: 0;
    }

    &__child {
      justify-content: center;
      gap: 6px;
      padding: 0 6px;
      font-size: 11px;

      > svg {
        width: 15px;
        height: 15px;
      }
    }
  }
}
</style>
