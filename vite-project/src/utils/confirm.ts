/**
 * 确认弹窗工具
 * 封装 Element Plus 消息确认框的通用配置，统一提示样式与交互行为，
 * 供删除、状态切换、职能变更、退出登录等危险操作复用。
 */
import {ElMessageBox} from 'element-plus'

interface ConfirmOptions {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}

/**
 * 弹出确认对话框。
 * 用户确认后 resolve；取消或关闭时按 Element Plus 的约定 reject（action 为 'cancel' / 'close'）。
 */
export async function showConfirm(options: ConfirmOptions) {
  await ElMessageBox.confirm(options.message, options.title, {
    confirmButtonText: options.confirmText ?? '确认',
    cancelButtonText: options.cancelText ?? '取消',
    closeOnClickModal: false,
    closeOnPressEscape: true,
    customClass: 'nova-login-message',
    modalClass: 'nova-login-backdrop',
    type: 'warning',
  })
}
