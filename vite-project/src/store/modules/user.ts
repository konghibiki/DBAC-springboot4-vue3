/**
 * 用户状态管理模块
 * 集中管理当前登录用户的资料、权限列表与登录会话状态，
 * 提供权限归一化/匹配辅助函数以及资料、权限的加载与清理能力，
 * 供导航栏、用户管理、设置等多页面共享使用。
 */
import {defineStore} from 'pinia'
import {ref} from 'vue'
import {getSelfPermissions, getUserSelfInfo, type PermissionInfo, type UserInfo} from '@/api/user'
import {getMenuSelf} from '@/api/menu'

export function normalizePermissionCode(permission: string) {
  return permission.trim().toLowerCase()
}

export function normalizePermissionList(
  permissions: Array<PermissionInfo | string> = [],
) {
  return [...new Set(
    permissions
      .map((permission) =>
        typeof permission === 'string'
          ? permission
          : permission?.permissionCode ?? ''
      )
      .map(normalizePermissionCode)
      .filter(Boolean),
  )]
}

export function hasPermission(
  permissions: string[],
  requiredPermission: string,
) {
  const normalized = normalizePermissionList(permissions)
  const required = normalizePermissionCode(requiredPermission)
  if (!required) return false

  if (normalized.includes('*') || normalized.includes('*.*')) return true
  if (normalized.includes(required)) return true

  const separatorIndex = required.lastIndexOf('.')
  if (separatorIndex < 0) return false

  const moduleName = required.slice(0, separatorIndex)
  const actionName = required.slice(separatorIndex + 1)
  return (
    normalized.includes(`${moduleName}.*`) ||
    normalized.includes(`*.${actionName}`)
  )
}

export const useUserStore = defineStore(
  'CONST_USE_PINIA_USER_STORE_PERSIST',
  () => {
    const userInfo = ref<UserInfo | null>(null)
    const permissions = ref<string[]>([])
    // 当前登录用户拥有的菜单路由名列表；null 表示尚未成功加载（未进行菜单比对）。
    const menuRouteNames = ref<string[] | null>(null)
    // 会话就绪标记：登录成功且权限加载完成后置为 true，
    // 供路由守卫短路 /auth/isLogin 校验，避免登录发起的跳转被并发守卫拦截。
    const isSessionReady = ref(false)
    let permissionRequest: Promise<string[]> | null = null
    let permissionsLoaded = false
    let profileRequest: Promise<UserInfo> | null = null
    let profileRequestVersion = 0

    // 登出后清除内存中的用户资料，避免切换账号时显示旧信息。
    function clearUserInfo() {
      userInfo.value = null
      profileRequestVersion += 1
      profileRequest = null
      permissions.value = []
      permissionRequest = null
      permissionsLoaded = false
      menuRouteNames.value = null
      menuRequest = null
      isSessionReady.value = false
    }

    // 登录流程（含职能选择）全部成功后调用，供路由守卫短路 /auth/isLogin。
    function markSessionReady() {
      isSessionReady.value = true
    }

    async function loadPermissions(force = false) {
      if (permissionsLoaded && !force) return permissions.value
      if (force) {
        permissionsLoaded = false
        permissionRequest = null
      }
      if (!permissionRequest) {
        permissionRequest = getSelfPermissions()
          .then((response) => {
            const normalized = normalizePermissionList(response.data.data ?? [])
            permissions.value = normalized
            permissionsLoaded = true
            return normalized
          })
          .catch(() => {
            // 权限接口暂不可用（如后端 500）时按“无权限”处理，导航放行，
            // 避免一次权限拉取失败导致整个路由守卫中断。
            permissions.value = []
            permissionsLoaded = true
            return permissions.value
          })
          .finally(() => {
            permissionRequest = null
          })
      }
      return permissionRequest
    }

    function can(requiredPermission: string) {
      return hasPermission(permissions.value, requiredPermission)
    }

    // 并发页面共用同一个菜单请求，避免侧边栏与路由守卫重复拉取；
    // 菜单路由列表加载失败时保持 null，守卫对菜单比对采用「放行」策略以防误锁。
    let menuRequest: Promise<string[] | null> | null = null
    async function loadMenus(force = false): Promise<string[] | null> {
      if (menuRouteNames.value && !force) return menuRouteNames.value
      if (force) {
        menuRouteNames.value = null
        menuRequest = null
      }
      if (!menuRequest) {
        menuRequest = getMenuSelf()
          .then((response) => {
            menuRouteNames.value = response.data.data ?? []
            return menuRouteNames.value
          })
          .catch(() => {
            // 接口暂不可用（如后端 500）时按“未加载”处理，导航放行，
            // 避免一次菜单拉取失败导致整个路由守卫中断。
            menuRouteNames.value = null
            return null
          })
          .finally(() => {
            menuRequest = null
          })
      }
      return menuRequest
    }

    // Unknown menu state is denied; the server remains the authorization boundary.
    function canAccessMenu(routeName: string) {
      if (!menuRouteNames.value) return false
      return menuRouteNames.value.includes(routeName)
    }

    // 并发页面共用同一个资料请求，避免导航栏与设置页重复拉取。
    async function loadUserInfo() {
      if (userInfo.value) return userInfo.value

      if (!profileRequest) {
        const requestVersion = profileRequestVersion
        profileRequest = getUserSelfInfo()
          .then((response) => {
            const info = response.data.data
            if (requestVersion === profileRequestVersion) {
              userInfo.value = info
            }
            return info
          })
          .finally(() => {
            if (requestVersion === profileRequestVersion) {
              profileRequest = null
            }
          })
      }

      return profileRequest
    }

    return {
      userInfo,
      permissions,
      menuRouteNames,
      isSessionReady,
      markSessionReady,
      clearUserInfo,
      loadUserInfo,
      loadPermissions,
      loadMenus,
      canAccessMenu,
      can,
    }
  },
)
