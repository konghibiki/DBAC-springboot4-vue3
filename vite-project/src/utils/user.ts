/**
 * 用户领域工具函数
 * 提供性别编码、账号状态、职能字段等在用户管理相关页面中复用的转换逻辑。
 */
import type {UserInfo} from '@/api/user'

/** 性别选项的展示文案与表单编码。 */
export const GENDER_OPTIONS = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
] as const

/** 将后端返回的性别描述转换为表单使用的数字编码（0 保密 / 1 男 / 2 女）。 */
export function getGenderCode(gender: unknown) {
  if (gender === '男' || gender === 1 || gender === '1') return 1
  if (gender === '女' || gender === 2 || gender === '2') return 2
  return 0
}

/** 将后端返回的账号状态转换为表单选项：'正常用户' 或 '禁用用户'。 */
export function getStatusValue(status: unknown): '正常用户' | '禁用用户' {
  if (status === 1 || status === '1' || status === '禁用用户') return '禁用用户'
  return '正常用户'
}

/** 将后端可能返回的字符串或字符串数组职能字段统一为数组。 */
export function toRoleArray(role: UserInfo['role']) {
  if (Array.isArray(role)) return [...role]
  return role ? [role] : []
}

/** 生成用户头像的占位文字：优先取用户名首字母，否则取邮箱首字母。 */
export function getAvatarText(value: unknown) {
  const user = value as UserInfo
  return (user.userName || user.email || '?').trim().charAt(0).toUpperCase()
}
