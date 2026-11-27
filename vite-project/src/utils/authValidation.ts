/**
 * 登录与账号安全校验工具
 * 提供邮箱格式、密码强度（6-18 位且包含字母与数字）的校验函数，
 * 返回面向用户的中文错误消息，供登录、注册及设置页面共用。
 */

const emailPattern =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&#._-]+$/

export function getEmailValidationError(value: string) {
  const email = value.trim()
  if (!email) return '请输入邮箱地址'
  if (email.length > 254) return '邮箱地址不能超过254位'
  if (!emailPattern.test(email)) return '请输入有效的邮箱地址'
  return ''
}

export function getPasswordValidationError(value: string) {
  if (!value) return '请输入密码'
  if (value.length < 6) return '密码至少需要6位'
  if (value.length > 18) return '密码不能超过18位'
  if (!passwordPattern.test(value)) {
    return '密码需包含英文字母和数字，且只能使用提示中的字符'
  }
  return ''
}
