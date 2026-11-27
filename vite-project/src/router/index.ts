/**
 * 应用路由配置
 * 定义首页、系统管理、账号设置等全部页面路由及其权限元数据，
 * 并注册登录状态检查、会话过期提示、流程中断确认等全局导航守卫。
 */
import {createRouter, createWebHistory, type RouteRecordRaw} from 'vue-router'
import {checkLogin} from '@/api/login'
import {ElMessageBox} from 'element-plus'
import {useUserStore} from '@/store'

// 定义路由规则
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/home',
    component: () => import('../view/layout/HomeLayout.vue'),
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../view/layout/HomeView.vue'),
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('../view/system/UserManagementView.vue'),
        meta: {
          title: '用户管理',
          description: '管理系统中的用户账号与资料。',
          menu: true,
        },
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('../view/system/RoleManagementView.vue'),
        meta: {
          title: '角色管理',
          description: '管理角色定义与系统权限分配。',
          menu: true,
        },
      },
      {
        path: 'system/menus',
        name: 'system-menus',
        component: () => import('../view/system/MenuManagementView.vue'),
        meta: {
          title: '菜单管理',
          description: '管理后台菜单结构与导航配置。',
          menu: true,
        },
      },
      {
        path: 'system/permissions',
        name: 'system-permissions',
        component: () => import('../view/system/PermissionManagementView.vue'),
        meta: {
          title: '权限管理',
          description: '管理系统权限定义与职权标识。',
          menu: true,
        },
      },
    ],
  },
  {
    path: '/settings',
    component: () => import('../view/settings/SettingsLayout.vue'),
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: { name: 'settings-profile' },
      },
      {
        path: 'profile',
        name: 'settings-profile',
        component: () => import('../view/settings/ProfileSettingsView.vue'),
      },
      {
        path: 'binding',
        name: 'settings-email',
        component: () => import('../view/settings/EmailSettingsView.vue'),
      },
      {
        path: 'password',
        name: 'settings-password',
        component: () => import('../view/settings/PasswordSettingsView.vue'),
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../view/LoginView.vue'),
    meta: {
      requiresAuth: false,
    },
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,

  // 滚动行为
  // 每次切换路由时，滚动到顶部
  scrollBehavior() {
    return { top: 0 }
  },
})

async function showLoginRequired() {
  await ElMessageBox.alert('请先登录后再访问此页面。', '需要登录', {
    confirmButtonText: '前往登录',
    closeOnClickModal: false,
    closeOnPressEscape: false,
    showClose: false,
    center: true,
    customClass: 'nova-login-message',
    modalClass: 'nova-login-backdrop',
    type: 'warning',
  })
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 登录/选职流程成功时 isSessionReady 已置位：直接信任本地会话标记，
  // 短路 /auth/isLogin 网络请求，避免登录后的立即跳转被偶发的鉴权请求拦截；
  // 页面刷新或登出后该标记归零，守卫重新走服务端校验。
  let isFullyLoggedIn = userStore.isSessionReady

  if (!isFullyLoggedIn) {
    try {
      const res = await checkLogin() // 后端在此处同时校验会话登录状态和已选择的角色 ID。
      isFullyLoggedIn = res.data.data === true
    } catch (error) {
      console.error('检查登录状态失败', error)
    }
  }

  if (isFullyLoggedIn) {
    await userStore.loadPermissions()
    await userStore.loadMenus()

    if (to.name === 'login') return { name: 'home' }

    // 菜单受控路由：比对用户菜单列表（路由名），未授权的菜单禁止进入。
    if (to.meta.menu && !userStore.canAccessMenu(String(to.name))) {
      return { name: 'home' }
    }

    return true
  }

  if (!to.meta.requiresAuth) return true

  await showLoginRequired()
  return { name: 'login' }
})

export default router
