/**
 * 菜单相关接口与类型定义
 * 声明菜单分页数据结构与列表接口，
 * 供菜单管理页面展示后台菜单结构与导航配置。
 */
import request from '@/utils/request'
import type {UserPage} from '@/api/user'

export interface MenuInfo {
  menuId: number
  menuName: string
  routePath: string
  routeName: string
  componentPath: string
  icon: string
  permissionCode: string | null
  keepAlive: number | null
}

export interface MenuAddPayload {
  menuName: string
  routePath: string
  routeName: string
  componentPath: string
  permissionCode: string
  keepAlive: boolean
  icon?: string
}

// 获取当前登录用户拥有的菜单路由名列表（由后端根据职权计算）。
export const getMenuSelf = () => request.get<{ data: string[] }>('/menu/self')

// 分页获取菜单列表。
export const getMenuPage = (pageNum: number, pageSize: number) =>
  request.get<{ data: UserPage<MenuInfo> }>(`/menu/list/${pageNum}`, {
    params: { pageSize },
  })

// 获取全部带 list 的权限标识，供新增菜单的权限下拉框选用。
export const getPermissionListOfList = () =>
  request.get<{ data: string[] }>('/permission/listOfList')

// 新增菜单。
export const addMenu = (data: MenuAddPayload) =>
  request.post<void>('/menu/add', data)

// 修改菜单。
export const updateMenu = (data: MenuAddPayload & { menuId: number }) =>
  request.post<void>('/menu/update', data)

// 删除菜单：需携带当前账号密码确认。
export const deleteMenu = (menuId: number, password: string) =>
  request.post<void>('/menu/delete', null, { params: { menuId, password } })
