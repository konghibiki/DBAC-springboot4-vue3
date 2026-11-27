/**
 * 登录/注册表单组合式函数
 * 封装认证页面表单的字段状态、邮箱域名建议、字段级校验与错误收集逻辑，
 * 供登录页面复用，保持表单交互行为一致。
 */
import {computed, reactive, ref} from 'vue'
import axios from 'axios'
import {getEmailValidationError, getPasswordValidationError,} from '@/utils/authValidation'

export type AuthFieldName =
  'username' | 'email' | 'password' | 'confirmPassword'

export interface AuthFormValues {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const fieldNames: AuthFieldName[] = [
  'username',
  'email',
  'password',
  'confirmPassword',
]

const emailDomains = [
  'qq.com',
  'gmail.com',
  '163.com',
  '126.com',
  'outlook.com',
  'hotmail.com',
  'icloud.com',
]

export function useAuthForm() {
  const values = reactive<AuthFormValues>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })

  const fieldState = reactive<
    Record<AuthFieldName, { focused: boolean; touched: boolean }>
  >({
    username: { focused: false, touched: false },
    email: { focused: false, touched: false },
    password: { focused: false, touched: false },
    confirmPassword: { focused: false, touched: false },
  })

  const serverErrors = reactive<Record<AuthFieldName, string>>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })

  const submitError = ref('')

  function validateUsername(value: string) {
    const username = value.trim()
    if (!username) return '请输入用户名'
    if (username.length > 18) return '用户名不能超过18位'
    return ''
  }

  function validateEmail(value: string) {
    return getEmailValidationError(value)
  }

  function validatePassword(value: string) {
    return getPasswordValidationError(value)
  }

  function validateConfirmPassword(value: string) {
    if (!value) return '请再次输入密码'
    if (value !== values.password) return '两次输入的密码不一致'
    return ''
  }

  const validationErrors = computed<Record<AuthFieldName, string>>(() => ({
    username: validateUsername(values.username),
    email: validateEmail(values.email),
    password: validatePassword(values.password),
    confirmPassword: validateConfirmPassword(values.confirmPassword),
  }))

  const emailSuggestions = computed(() => {
    const value = values.email.trim()
    const atIndex = value.indexOf('@')
    const localPart = atIndex < 0 ? value : value.slice(0, atIndex)
    const domainQuery =
      atIndex < 0 ? '' : value.slice(atIndex + 1).toLowerCase()

    if (!fieldState.email.focused || !localPart || /\s/.test(localPart)) {
      return []
    }

    return emailDomains
      .filter(
        (domain) => domain.startsWith(domainQuery) && domain !== domainQuery,
      )
      .map((domain) => `${localPart}@${domain}`)
  })

  function getError(field: AuthFieldName) {
    const state = fieldState[field]
    if (state.focused || !state.touched) return ''
    return serverErrors[field] || validationErrors.value[field]
  }

  function markFocused(field: AuthFieldName) {
    fieldState[field].focused = true
  }

  function markBlurred(field: AuthFieldName) {
    fieldState[field].focused = false
    fieldState[field].touched = true
  }

  function clearServerError(field: AuthFieldName) {
    serverErrors[field] = ''
    submitError.value = ''

    if (field === 'password') {
      serverErrors.confirmPassword = ''
    }
  }

  function clearServerErrors() {
    fieldNames.forEach((field) => {
      serverErrors[field] = ''
    })
    submitError.value = ''
  }

  function resetValidation() {
    clearServerErrors()
    fieldNames.forEach((field) => {
      fieldState[field].focused = false
      fieldState[field].touched = false
    })
  }

  function validateFields(fields: AuthFieldName[]) {
    clearServerErrors()
    fields.forEach((field) => {
      fieldState[field].touched = true
    })
    return fields.every((field) => !validationErrors.value[field])
  }

  function getRequestErrorMessage(error: unknown) {
    if (axios.isAxiosError<{ msg?: string }>(error)) {
      return error.response?.data?.msg || error.message
    }
    return error instanceof Error ? error.message : '请求失败，请稍后重试'
  }

  function getErrorField(message: string): AuthFieldName | undefined {
    if (/用户名|昵称|username/i.test(message)) return 'username'
    if (/确认密码|重复密码|confirm.?password/i.test(message)) {
      return 'confirmPassword'
    }
    if (/邮箱|email/i.test(message)) return 'email'
    if (/密码|password/i.test(message)) return 'password'
    return undefined
  }

  function applyRequestError(error: unknown) {
    const message = getRequestErrorMessage(error)
    const field = getErrorField(message)

    if (field) {
      serverErrors[field] = message
      fieldState[field].touched = true
      return
    }

    submitError.value = message
  }

  function selectEmailSuggestion(suggestion: string) {
    values.email = suggestion
    fieldState.email.focused = false
    fieldState.email.touched = true
    clearServerError('email')
  }

  return {
    values,
    fieldState,
    submitError,
    emailSuggestions,
    getError,
    markFocused,
    markBlurred,
    clearServerError,
    resetValidation,
    validateFields,
    applyRequestError,
    selectEmailSuggestion,
  }
}
