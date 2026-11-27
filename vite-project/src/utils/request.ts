/**
 * Axios 请求实例与统一拦截器
 * 配置接口基础地址、超时时间，并统一处理请求中的加载指示、
 * Cookie 携带、响应结构解包与错误消息提示，
 * 供所有 API 模块复用，保证请求行为一致。
 */
import axios, {type AxiosResponse} from 'axios'
import {ElLoading} from 'element-plus'

interface ApiResponse<T = unknown> {
  code: string
  msg: string
  data: T | null
  success: boolean
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
  // 自动携带 Cookie
  withCredentials: true,
})

let activeRequestCount = 0
let loadingTimer: ReturnType<typeof setTimeout> | undefined
let loadingInstance: ReturnType<typeof ElLoading.service> | undefined

function startLoading() {
  activeRequestCount += 1

  if (activeRequestCount === 1) {
    loadingTimer = setTimeout(() => {
      loadingTimer = undefined
      if (activeRequestCount > 0) {
        loadingInstance = ElLoading.service({
          fullscreen: true,
          text: '正在处理，请稍候…',
          customClass: 'nova-loading-mask',
        })
      }
    }, 300)
  }
}

function stopLoading() {
  activeRequestCount = Math.max(0, activeRequestCount - 1)

  if (activeRequestCount === 0) {
    if (loadingTimer) {
      clearTimeout(loadingTimer)
      loadingTimer = undefined
    }

    loadingInstance?.close()
    loadingInstance = undefined
  }
}

request.interceptors.request.use((config) => {
  // if (typeof localStorage !== 'undefined') {
  //   const token = localStorage.getItem('token')
  //   if (token) config.headers.set('token', token)
  // }

  startLoading()
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    stopLoading()

    if (response.data.success === false) {
      return Promise.reject(new Error(response.data.msg || '请求失败'))
    }

    return response
  },
  (error: unknown) => {
    stopLoading()

    if (axios.isAxiosError<ApiResponse>(error)) {
      const message = error.response?.data?.msg
      if (message) error.message = message
    }

    return Promise.reject(error)
  },
)

export default request
