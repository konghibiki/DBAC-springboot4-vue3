/**
 * 请求错误消息提取工具
 * 统一将接口调用抛出的异常转换成可读的中文提示，
 * 避免每个页面重复编写 try/catch 中的错误消息处理逻辑。
 */

/**
 * 提取错误对象中面向用户的提示消息。
 * @param error 捕获到的异常对象
 * @param fallback 无法解析错误类型时的兜底消息
 */
export function getRequestErrorMessage(
  error: unknown,
  fallback = '请求失败，请稍后重试',
) {
  return error instanceof Error ? error.message : fallback
}
