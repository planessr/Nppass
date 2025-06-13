import Vue from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import { installPermissionDirectives } from '@/directives/permission'
import { isAdmin, hasRole, isLoggedIn, getCurrentUserRoleId } from '@/utils/auth'
import config from '@/config/env'


// 注册Element UI
Vue.use(ElementUI)

// 注册权限指令
installPermissionDirectives(Vue)

// 添加全局权限方法
Vue.prototype.$isAdmin = isAdmin
Vue.prototype.$hasRole = hasRole
Vue.prototype.$isLoggedIn = isLoggedIn
Vue.prototype.$getCurrentUserRoleId = getCurrentUserRoleId

// 注册axios
const base_url = config.API_BASE_URL
Vue.prototype.baseUrl = base_url
Vue.prototype.$axios = axios
axios.defaults.baseURL = base_url
Vue.config.productionTip = false


Vue.filter('dateFormat',function(originVal){
  const dt = new Date(originVal)
  const y = dt.getFullYear()
  const m = (dt.getMonth() + 1 + '').padStart(2,'0')
  const d = (dt.getDate() + '').padStart(2,'0')
  const hh = (dt.getHours()+'').padStart(2,'0')
  const mm = (dt.getMinutes()+'').padStart(2,'0')
  const ss = (dt.getSeconds()+'').padStart(2,'0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
})

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
