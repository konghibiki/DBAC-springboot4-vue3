<!--
  顶部导航栏组件
  展示品牌标识、当前用户信息与头像下拉菜单（用户中心、邮箱设置、退出登录），
  加载并监听共享用户资料变化，提供统一的顶部导航交互。
-->
<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {logout} from '@/api/login'
import {useUserStore} from '@/store'
import {resolveAvatarUrl} from '@/utils/avatar'

type UserMenuAction = 'profile' | 'binding' | 'password' | 'logout'

const router = useRouter()
const userStore = useUserStore()
const isLoadingProfile = ref(false)
const isLoggingOut = ref(false)
const avatarFailed = ref(false)

// #region 用户资料与头像
const avatarUrl = computed(() => resolveAvatarUrl(userStore.userInfo?.avatar))
const displayName = computed(() => {
  const name = userStore.userInfo?.userName?.trim()
  if (name) return name
  if (isLoadingProfile.value) return '正在加载…'
  return 'NOVA 用户'
})

watch(avatarUrl, () => {
  avatarFailed.value = false
})

async function loadUserProfile() {
  if (userStore.userInfo || isLoadingProfile.value) return

  isLoadingProfile.value = true
  try {
    await userStore.loadUserInfo()
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '获取用户信息失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isLoadingProfile.value = false
  }
}
// #endregion

// #region 用户菜单与退出登录
function handleMenuAction(action: UserMenuAction) {
  if (action === 'logout') {
    void handleLogout()
    return
  }

  const actionRoutes: Record<Exclude<UserMenuAction, 'logout'>, string> = {
    profile: 'settings-profile',
    binding: 'settings-email',
    password: 'settings-password',
  }
  void router.push({ name: actionRoutes[action] })
}

async function handleLogout() {
  if (isLoggingOut.value) return

  try {
    await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '继续使用',
      closeOnClickModal: false,
      closeOnPressEscape: true,
      customClass: 'nova-login-message',
      modalClass: 'nova-login-backdrop',
      type: 'warning',
    })
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    ElMessage.error('无法打开退出确认框')
    return
  }

  isLoggingOut.value = true
  try {
    await logout()
    userStore.clearUserInfo()
    await router.replace({ name: 'login' })
    ElMessage.success('已退出登录')
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '退出登录失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isLoggingOut.value = false
  }
}
// #endregion

onMounted(() => {
  void loadUserProfile()
})
</script>

<template>
  <header class="top-navigation">
    <!-- 品牌入口：点击后回到首页工作台。 -->
    <RouterLink :to="{ name: 'home' }" class="top-navigation__brand">
      <span aria-hidden="true" class="top-navigation__brand-mark">
        <svg fill="none" viewBox="0 0 32 32">
          <path
            d="M16 3.5 19.8 12.2 28.5 16l-8.7 3.8L16 28.5l-3.8-8.7L3.5 16l8.7-3.8L16 3.5Z"
            fill="currentColor"
          />
        </svg>
      </span>
      <span class="top-navigation__brand-name">NOVA</span>
      <span aria-hidden="true" class="top-navigation__divider"></span>
      <span class="top-navigation__title">后台管理页面</span>
    </RouterLink>

    <!-- 用户入口包含悬停菜单与独立的快捷退出按钮。 -->
    <div class="top-navigation__actions">
      <el-dropdown
        placement="bottom-end"
        popper-class="nova-user-dropdown-popper"
        trigger="hover"
        @command="handleMenuAction"
      >
        <button
          :aria-label="`用户菜单，当前用户：${displayName}`"
          class="user-trigger"
          type="button"
        >
          <el-avatar
            v-if="avatarUrl && !avatarFailed"
            :size="42"
            :src="avatarUrl"
            class="user-trigger__avatar"
            @error="avatarFailed = true"
          />
          <el-avatar
            v-else
            :size="42"
            class="user-trigger__avatar user-trigger__avatar--fallback"
          >
            <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
              <circle cx="12" cy="8" r="3.5" />
              <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
            </svg>
          </el-avatar>
          <span class="user-trigger__name">{{ displayName }}</span>
          <svg
            aria-hidden="true"
            class="user-trigger__chevron"
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

        <template #dropdown>
          <el-dropdown-menu class="user-dropdown-menu">
            <div class="user-dropdown-menu__summary">
              <strong>{{ displayName }}</strong>
              <span>{{ userStore.userInfo?.email || '欢迎使用 NOVA' }}</span>
            </div>
            <el-dropdown-item command="profile">
              <span aria-hidden="true" class="user-dropdown-menu__icon">
                改
              </span>
              修改用户信息
            </el-dropdown-item>
            <el-dropdown-item command="binding">
              <span aria-hidden="true" class="user-dropdown-menu__icon"
                >绑</span
              >
              更改绑定
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <span aria-hidden="true" class="user-dropdown-menu__icon"
                >密</span
              >
              修改密码
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <span
                aria-hidden="true"
                class="user-dropdown-menu__icon user-dropdown-menu__icon--logout"
              >
                退
              </span>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-button
        :loading="isLoggingOut"
        class="top-navigation__logout"
        plain
        @click="handleLogout"
      >
        退出登录
      </el-button>
    </div>
  </header>
</template>

<style lang="scss" scoped>
.top-navigation {
  position: sticky;
  z-index: 20;
  top: 0;
  display: flex;
  min-height: 76px;
  align-items: center;
  justify-content: space-between;
  padding: 0 clamp(22px, 5vw, 72px);
  border-bottom: 1px solid rgb(228 230 234 / 78%);
  background: rgb(255 255 255 / 90%);
  backdrop-filter: blur(18px);
}

.top-navigation__brand,
.top-navigation__actions,
.user-trigger {
  display: inline-flex;
  align-items: center;
}

.top-navigation__brand {
  gap: 12px;
  color: #293343;
  text-decoration: none;
}

.top-navigation__brand-mark {
  display: inline-flex;
  width: 30px;
  height: 30px;
  color: #b3976f;

  svg {
    width: 100%;
    height: 100%;
  }
}

.top-navigation__brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.top-navigation__divider {
  width: 1px;
  height: 22px;
  margin: 0 3px 0 5px;
  background: #e2e4e9;
}

.top-navigation__title {
  color: #606777;
  font-size: 14px;
  font-weight: 500;
}

.top-navigation__actions {
  gap: 18px;
}

.user-trigger {
  gap: 10px;
  padding: 7px 10px 7px 7px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: #343c4b;
  cursor: pointer;
  font: inherit;
  transition:
    background-color 180ms ease,
    border-color 180ms ease;

  &:hover,
  &:focus-visible {
    border-color: #eee9df;
    outline: none;
    background: #faf8f4;
  }

  &:hover .user-trigger__avatar,
  &:focus-visible .user-trigger__avatar {
    transform: scale(1.1);
  }
}

.user-trigger__avatar {
  flex: 0 0 auto;
  border: 2px solid #fff;
  background: #f0ece4;
  box-shadow: 0 2px 9px rgb(34 42 57 / 13%);
  transition: transform 180ms ease;
}

.user-trigger__avatar--fallback {
  color: #9e855f;

  svg {
    width: 22px;
    height: 22px;
    stroke: currentColor;
    stroke-width: 1.5;
    stroke-linecap: round;
  }
}

.user-trigger__name {
  max-width: 160px;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-trigger__chevron {
  width: 15px;
  height: 15px;
  color: #9a9eaa;
}

.top-navigation__logout {
  height: 36px;
  padding: 0 15px;
  border-color: #e7e2d8;
  border-radius: 8px;
  color: #6d6252;
  font-size: 12px;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;

  &:hover {
    border-color: #d7c6a9;
    background: #faf8f4;
    color: #806541;
  }
}

:global(.nova-user-dropdown-popper) {
  min-width: 230px;
  padding: 7px;
  border: 1px solid #eee9df !important;
  border-radius: 14px !important;
  box-shadow: 0 16px 38px rgb(34 42 57 / 15%) !important;
}

:global(.nova-user-dropdown-popper .user-dropdown-menu) {
  border: 0;
  box-shadow: none;
}

:global(.user-dropdown-menu__summary) {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 11px 12px 13px;
  border-bottom: 1px solid #f0eee9;

  strong {
    overflow: hidden;
    color: #293343;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    overflow: hidden;
    color: #999eaa;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

:global(.nova-user-dropdown-popper .el-dropdown-menu__item) {
  min-height: 40px;
  gap: 10px;
  border-radius: 7px;
  color: #555d6c;
  font-size: 12px;
}

:global(
  .nova-user-dropdown-popper .el-dropdown-menu__item:not(.is-disabled):hover
) {
  background: #f7f4ee;
  color: #806541;
}

.user-dropdown-menu__icon {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 7px;
  background: #f4f1eb;
  color: #9e855f;
  font-size: 10px;
  font-weight: 600;
}

.user-dropdown-menu__icon--logout {
  background: #fff1ef;
  color: #bc675b;
}

@media (max-width: 640px) {
  .top-navigation {
    min-height: 66px;
    padding: 0 16px;
  }

  .top-navigation__brand {
    gap: 8px;
  }

  .top-navigation__brand-mark {
    width: 26px;
    height: 26px;
  }

  .top-navigation__brand-name {
    font-size: 13px;
  }

  .top-navigation__divider {
    margin: 0 1px;
  }

  .top-navigation__title {
    font-size: 12px;
  }

  .top-navigation__actions {
    gap: 6px;
  }

  .top-navigation__logout {
    width: 34px;
    padding: 0;
    overflow: hidden;
    font-size: 0;

    &::after {
      content: '退';
      font-size: 12px;
    }
  }

  .user-trigger {
    gap: 6px;
    padding-right: 5px;
  }

  .user-trigger__name {
    max-width: 70px;
    font-size: 12px;
  }

  .user-trigger__chevron {
    width: 12px;
    height: 12px;
  }
}

@media (max-width: 390px) {
  .top-navigation__brand-name,
  .top-navigation__divider {
    display: none;
  }

  .user-trigger__name {
    max-width: 64px;
  }
}
</style>
