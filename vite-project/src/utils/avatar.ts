/**
 * 头像地址解析工具
 * 将后端返回的相对头像路径解析为可直接展示的完整 URL，
 * 供头像裁剪、导航栏、个人资料等场景复用。
 */

export function resolveAvatarUrl(value: string | null | undefined) {
  const avatar = value?.trim()
  if (!avatar) return ''

  if (/^(?:https?:|data:|blob:|\/\/)/i.test(avatar)) return avatar

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, '')
  if (!apiBaseUrl) return avatar

  return `${apiBaseUrl}/${avatar.replace(/^\/+/, '')}`
}
