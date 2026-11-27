/**
 * 用户职能列表组合式函数
 * 在模块级别缓存角色列表并共享加载中的请求，
 * 避免新增/修改用户弹窗对同一接口发起重复请求。
 */
import {ref} from 'vue'
import {getAllRoleList, type UserRole} from '@/api/user'

// 模块级共享缓存：角色列表数据与响应式引用在所有实例间复用，
// 避免新增/修改用户弹窗对同一接口发起重复请求，
// 并保证任一实例发起加载完成后，其他实例的职能选项同步更新。
let cachedRoles: UserRole[] | null = null
let pendingRequest: Promise<UserRole[]> | null = null
const sharedRoles = ref<UserRole[]>([])

export function useRoleList() {
  const roles = sharedRoles
  const isLoading = ref(false)

  /**
   * 加载全部角色列表；已缓存或正在请求时复用，避免重复调用接口。
   */
  async function loadRoles() {
    if (cachedRoles) {
      roles.value = cachedRoles
      return cachedRoles
    }
    if (pendingRequest) return pendingRequest

    isLoading.value = true
    pendingRequest = getAllRoleList()
      .then((response) => {
        cachedRoles = response.data.data ?? []
        roles.value = cachedRoles
        return cachedRoles
      })
      .finally(() => {
        isLoading.value = false
        pendingRequest = null
      })

    return pendingRequest
  }

  return { roles, isLoading, loadRoles }
}
