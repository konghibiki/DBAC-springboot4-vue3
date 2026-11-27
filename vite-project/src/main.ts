/**
 * 应用入口脚本
 * 创建 Vue 应用实例，引入 Element Plus 全局样式，
 * 并挂载路由与 Pinia 状态管理后启动应用。
 */
import {createApp} from 'vue'
import 'element-plus/dist/index.css'
// import './style.css'
import App from './App.vue'

import router from '@/router'
import pinia from '@/store/index'

createApp(App).use(router).use(pinia).mount('#app')
