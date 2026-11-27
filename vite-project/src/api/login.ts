/**
 * 登录认证接口模块
 * 提供登录、登录后选择职能、注册、登录状态检查与退出登录等接口定义，
 * 供登录页面、路由守卫与用户状态管理复用。
 */
import request from '@/utils/request'

// 登录
export const login = (data: {
  email: string
  password: string
  rememberMe: boolean
}) => request.post<{ data: boolean }>('/auth/login', data)

// 选择职能后完成登录。
export const loginWithRole = (roleId: number) =>
  request.post('/auth/loginWithRole', roleId)

// 注册
export const register = (data: {
  email: string
  password: string
  userName: string
}) => {
  return request.post('/auth/register', data)
}

// 检查登录状态
export function checkLogin() {
  return request.get<{ data: boolean }>('/auth/isLogin')
}

// 结束当前登录会话
export const logout = () => request.post<void>('/auth/logout')
