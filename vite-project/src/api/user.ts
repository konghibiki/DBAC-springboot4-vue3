/**
 * 用户相关接口与类型定义
 * 声明用户信息、职能、分页数据结构，以及用户列表、搜索、
 * 增删改查、个人资料、邮箱与密码等全部接口，
 * 供用户管理、设置与登录相关页面共用。
 */
import request from '@/utils/request'

export interface UserInfo {
  userId: number
  email: string
  role: string | string[] | null
  userName: string | null
  avatar: string | null
  gender: string
  createTime: string
  status?: string | number | null
}

export interface UserRole {
  roleId: number
  roleName: string
}

export interface RoleInfo {
  roleId: number
  roleName: string
  roleLevel: number
}

export interface UserPage<T = UserInfo> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AddUserPayload {
  email: string
  password: string
  userName: string
  gender: number
  role: string[]
}

export interface UpdateUserPayload {
  userId: number
  email: string | null
  password: string | null
  userName: string | null
  gender: number
  status: '正常用户' | '禁用用户'
  role: string[]
}

export interface RoleUpdatePayload {
  roleId: number
  roleName: string
  rolePermissions: string[]
  roleLevel: number
}

// 获取当前账号可选择的职能。
export const getUserRoleList = () =>
  request.get<{ data: UserRole[] }>('/role/self')

// 获取当前登录用户的个人资料
export const getUserSelfInfo = () =>
  request.get<{ data: UserInfo }>('/user/self-info')

// 分页获取用户列表。
export const getUserPage = (pageNum: number, pageSize: number) =>
  request.get<{ data: UserPage }>(`/user/all/${pageNum}`, {
    params: { pageSize },
  })

// 分页筛选用户，支持按关键词与角色名组合查询；两者均可为空。
export const searchUserPage = (
  findInfo: string,
  pageNum: number,
  pageSize: number,
  roleName: string,
) =>
  request.get<{ data: UserPage }>(`/user/info/${pageNum}`, {
    params: { findInfo: findInfo || '', roleName: roleName || '', pageSize },
  })

// 获取全部角色。
export const getAllRoleList = () =>
  request.get<{ data: UserRole[] }>('/role/list')

// 分页获取全部角色列表。
export const getRolePage = (pageNum: number, pageSize: number) =>
  request.get<{ data: UserPage<RoleInfo> }>(`/role/all/${pageNum}`, {
    params: { pageSize },
  })

// 分页模糊搜索角色（支持按角色名称或角色 ID 搜索）。
export const searchRolePage = (
  findInfo: string,
  pageNum: number,
  pageSize: number,
) =>
  request.get<{ data: UserPage<RoleInfo> }>(`/role/find/${pageNum}`, {
    params: { findInfo, pageSize },
  })

// 获取当前登录用户在所选角色下的权限。
export const getSelfPermissions = () =>
  request.get<{ data: Array<PermissionInfo | string> }>('/permission/self')

export interface PermissionInfo {
  permissionId: number
  permissionName: string
  permissionCode: string
  permissionType: string
  permissionDesc: string
}

// 获取系统完整权限目录（含权限编码与名称），用于角色弹窗动态构建权限选项。
export const getAllPermissions = () =>
  request.get<{ data: PermissionInfo[] }>('/permission/all')

// 分页获取权限列表，接口返回 MyBatis-Plus 分页对象。
export const getPermissionPage = (pageNum: number, pageSize: number) =>
  request.get<{ data: UserPage<PermissionInfo> }>(
    `/permission/all/${pageNum}`,
    {
      params: { pageSize },
    },
  )

// 分页搜索权限，支持按权限 ID、权限标识与权限名称匹配。
export const searchPermissionPage = (
  findInfo: string,
  pageNum: number,
  pageSize: number,
) =>
  request.get<{ data: UserPage<PermissionInfo> }>(
    `/permission/search/${pageNum}`,
    {
      params: { findInfo, pageSize },
    },
  )

export interface PermissionPayload {
  permissionId?: number
  permissionCode: string
  permissionName: string
  permissionType: string
  description?: string
}

// 新增权限。
export const addPermission = (data: PermissionPayload) =>
  request.post<void>('/permission/add', data)

// 修改权限。
export const updatePermission = (data: PermissionPayload) =>
  request.post<void>('/permission/update', data)

// 删除权限，并校验当前账号密码。
export const deletePermission = (permissionId: number, password: string) =>
  request.post<void>('/permission/delete', null, {
    params: { permissionId, password },
  })

// 修改角色名称与权限。
export const updateRole = (data: RoleUpdatePayload) =>
  request.post<void>('/role/update', data)

export interface RoleAddPayload {
  roleName: string
  rolePermissions: string[]
  roleLevel: number
}

// 新增角色，角色等级固定为 2。
export const addRole = (data: RoleAddPayload) =>
  request.post<void>('/role/add', data)

// 删除角色，并校验当前账号密码。
export const deleteRole = (roleId: number, password: string) =>
  request.delete<void>(`/role/delete`, { params: { roleId, password } })

// 获取指定角色已拥有的职权，用于修改弹窗回显。
export const getRolePermissions = (roleId: number) =>
  request.get<{ data: string[] | Array<Record<string, string>> }>(
    `/permission/role`,
    { params: { roleId } },
  )

// 添加用户。
export const addUser = (data: AddUserPayload) =>
  request.post<void>('/user/add', data)

// 修改用户。
export const updateUser = (data: UpdateUserPayload) =>
  request.put<void>('/user/update', data)

// 删除用户，并校验当前账号密码。
export const deleteUser = (userId: number, password: string) =>
  request.delete<void>(`/user/delete`, { params: { userId, password } })

// 使用 FormData 提交个人资料与可选的裁剪后头像。
export const updateUserProfile = (formData: FormData) =>
  request.put<{ data: UserInfo }>('/user/profile', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

// 更新当前账号绑定邮箱。
export const updateBoundEmail = (email: string) =>
  request.put('/auth/email', { email })

// 修改当前账号密码。
export const updatePassword = (data: {
  currentPassword: string
  newPassword: string
}) => request.put('/auth/password', data)
