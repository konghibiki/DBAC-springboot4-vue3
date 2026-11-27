/**
 * 时间格式化工具
 * 提供通用日期时间字符串的展示格式化，供列表、细节页复用。
 */

/**
 * 将后端返回的时间字符串格式化为便于阅读的形式。
 * 例如 "2026-11-22T10:30:00" -> "2026-11-22 10:30"。
 */
export function formatDateTime(value: unknown) {
  return String(value ?? '')
    .replace('T', ' ')
    .replace(/:\d{2}$/, '')
}
