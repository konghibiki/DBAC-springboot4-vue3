/**
 * 系统权限目录工具模块
 * 角色添加/修改弹窗中的职权分配不再写死在前端，完全以当前用户职权为准：
 * - 优先使用 /api/permission/all 动态目录（权限编码 + 显示名 + 分组）做名称与分组映射；
 * - 可分配集合由 /api/permission/self 返回的当前用户职权为准（后端已展开通配符）；
 * - 目录接口不可用时，直接以 /api/permission/self 返回的职权码构建选项，不写死任何清单。
 */
import {ref} from 'vue'
import {hasPermission} from '@/store/modules/user'
import {getAllPermissions, type PermissionInfo} from '@/api/user'

export interface PermissionItem {
  /** 权限编码，用于与当前用户已拥有的权限进行通配匹配 */
  code: string
  /** 权限名称，作为提交给后端 rolePermissions 数组的取值，同时用于界面展示 */
  name: string
  /** 权限所属分组，用于界面分区展示 */
  group: string
}

/** 常见权限模块的分组中文名；未知模块回退为首段大写 */
const moduleGroupNames: Record<string, string> = {
  user: '用户管理',
  role: '角色管理',
  menu: '菜单管理',
  permission: '权限管理',
  system: '系统管理',
  home: '首页',
  file: '文件管理',
  message: '消息管理',
  log: '日志管理',
}

// 根据权限码推导展示分组：*.xxx 归入全局权限，其余按首段模块归类
function deriveGroup(code: string): string {
  const [moduleName] = code.split('.')
  if (!moduleName || moduleName === '*') return '全局权限'
  const known = moduleGroupNames[moduleName]
  if (known) return known
  return moduleName.charAt(0).toUpperCase() + moduleName.slice(1)
}

/**
 * 将后端 /api/permission/all 返回的目录转换为前端条目，
 * 分组由权限码首段推断（含通配权限）。目录来源动态，不写死清单。
 */
export function buildPermissionDirectory(
  list: PermissionInfo[],
): PermissionItem[] {
  const result: PermissionItem[] = []
  const seen = new Set<string>()
  for (const item of list) {
    const code = item.permissionCode?.trim()
    if (!code || seen.has(code)) continue
    seen.add(code)
    result.push({
      code,
      name: item.permissionName?.trim() || code,
      group: deriveGroup(code),
    })
  }
  return result
}

/**
 * 动态权限目录缓存与并发去重加载。
 * 目录加载失败（无 permission.list 权限或接口异常）时保持 null，
 * 可分配集合会直接以 /api/permission/self 返回的当前用户职权构建，不写死清单。
 */
const directoryCache = ref<PermissionItem[] | null>(null)
let directoryRequest: Promise<PermissionItem[] | null> | null = null
let directoryLoaded = false

export function getPermissionDirectory(): PermissionItem[] | null {
  return directoryCache.value
}

export function loadPermissionDirectory(
  force = false,
): Promise<PermissionItem[] | null> {
  if (directoryLoaded && !force) return Promise.resolve(directoryCache.value)
  if (force) {
    directoryLoaded = false
    directoryRequest = null
    directoryCache.value = null
  }
  if (!directoryRequest) {
    directoryRequest = getAllPermissions()
      .then((response) => {
        const items = buildPermissionDirectory(response.data.data ?? [])
        directoryCache.value = items
        directoryLoaded = true
        return items
      })
      .catch(() => {
        // 目录不可用时置为 null，可分配集合回退到当前用户职权
        directoryCache.value = null
        directoryLoaded = true
        return null
      })
      .finally(() => {
        directoryRequest = null
      })
  }
  return directoryRequest
}

/**
 * 汇总当前用户有权分配的职权：
 * 优先基于后端动态目录（directory）映射名称与分组，
 * 再按通配匹配规则筛出用户权限能够覆盖的条目（与后端 PermissionUtils 一致）；
 * 目录不可用时直接以 /api/permission/self 返回的当前用户职权构建，不写死任何清单。
 */
export function getGrantablePermissions(
  userPermissions: string[],
  directory: PermissionItem[] | null = null,
): PermissionItem[] {
  if (directory && directory.length > 0) {
    return directory.filter((item) => hasPermission(userPermissions, item.code))
  }
  return buildSelfPermissionItems(userPermissions)
}

/**
 * 将 /api/permission/self 返回的当前用户职权构建为权限条目列表。
 * 优先使用接口返回的 permissionName 与 permissionCode（对象形态），
 * 保持「名称（编码）」的显示契约；兼容纯权限码字符串数组形态。
 */
export function buildSelfPermissionItems(
  list: Array<PermissionInfo | string>,
): PermissionItem[] {
  const result: PermissionItem[] = []
  const seen = new Set<string>()
  for (const item of list) {
    if (typeof item === 'string') {
      const code = item.trim()
      if (!code || seen.has(code)) continue
      seen.add(code)
      result.push({ code, name: code, group: deriveGroup(code) })
    } else if (item && typeof item === 'object') {
      const code = item.permissionCode?.trim()
      if (!code || seen.has(code)) continue
      seen.add(code)
      const name = item.permissionName?.trim() || code
      result.push({ code, name, group: deriveGroup(code) })
    }
  }
  return result
}

/**
 * 按目录顺序对权限分组，供界面分区标题渲染 */
export function groupPermissions(
  permissions: PermissionItem[],
): Array<{ group: string; permissions: PermissionItem[] }> {
  const result: Array<{ group: string; permissions: PermissionItem[] }> = []
  for (const item of permissions) {
    const group = result.find((entry) => entry.group === item.group)
    if (group) {
      group.permissions.push(item)
    } else {
      result.push({ group: item.group, permissions: [item] })
    }
  }
  return result
}

/**
 * 将接口返回的角色已有权限解析为可勾选的权限名称列表。
 * 兼容两种常见返回形态：
 *  - 字符串数组（可能是 permissionName，也可能是 permissionCode）
 *  - 对象数组（permissionName / permissionCode 字段）
 * 名称映射优先使用动态目录；目录不可用时直接保留原始值
 * （此时可分配选项以当前用户职权 /api/permission/self 构建），
 * 以保证修改表单回显与提交给后端的 rolePermissions 取值完全一致。
 */
export function resolveRolePermissionNames(
  data: unknown,
  directory: PermissionItem[] | null = null,
  fallbackItems: PermissionItem[] | null = null,
): string[] {
  const items =
    directory && directory.length > 0
      ? directory
      : fallbackItems && fallbackItems.length > 0
        ? fallbackItems
        : null
  if (!Array.isArray(data)) return []
  const names: string[] = []
  for (const item of data) {
    if (typeof item === 'string') {
      names.push(toPermissionName(item, items))
    } else if (item && typeof item === 'object') {
      const record = item as Record<string, unknown>
      const nameValue = record.permissionName ?? record.name
      const codeValue = record.permissionCode ?? record.code
      if (typeof nameValue === 'string' && nameValue.trim()) {
        names.push(nameValue.trim())
      } else if (typeof codeValue === 'string' && codeValue.trim()) {
        names.push(toPermissionName(codeValue, items))
      }
    }
  }
  return [...new Set(names)]
}

// 将单个值解析为权限名称：原名命中直接返回，code 命中映射为 name（目录不存在时原样返回）
function toPermissionName(
  value: string,
  items: PermissionItem[] | null,
): string {
  const text = value.trim()
  if (!items || items.length === 0) return text
  const byName = items.find((item) => item.name === text)
  if (byName) return byName.name
  const byCode = items.find(
    (item) => item.code.toLowerCase() === text.toLowerCase(),
  )
  return byCode ? byCode.name : text
}
