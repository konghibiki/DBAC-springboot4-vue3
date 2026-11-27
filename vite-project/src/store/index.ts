/**
 * Pinia 状态管理入口
 * 创建全局状态实例并统一导出各业务模块的 Store，
 * 供组件清单式引入，保证状态管理集中可维护。
 */
import {createPinia} from 'pinia'

import {useUserStore} from './modules/user'

const pinia = createPinia()

export default pinia
export { useUserStore }
