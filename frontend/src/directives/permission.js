import { isAdmin, hasRole, isLoggedIn } from '@/utils/auth'

/**
 * 权限指令
 * 使用方法：
 * v-admin - 只有管理员可见
 * v-role="0" - 只有指定角色可见
 * v-auth - 只有已登录用户可见
 */

// 管理员权限指令
export const adminDirective = {
  inserted(el, binding) {
    if (!isAdmin()) {
      // 移除元素
      el.parentNode && el.parentNode.removeChild(el)
    }
  },
  
  update(el, binding) {
    // 动态更新权限状态
    if (!isAdmin()) {
      el.style.display = 'none'
    } else {
      el.style.display = ''
    }
  }
}

// 角色权限指令
export const roleDirective = {
  inserted(el, binding) {
    const targetRole = binding.value
    if (!hasRole(targetRole)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  },
  
  update(el, binding) {
    const targetRole = binding.value
    if (!hasRole(targetRole)) {
      el.style.display = 'none'
    } else {
      el.style.display = ''
    }
  }
}

// 登录权限指令
export const authDirective = {
  inserted(el, binding) {
    if (!isLoggedIn()) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  },
  
  update(el, binding) {
    if (!isLoggedIn()) {
      el.style.display = 'none'
    } else {
      el.style.display = ''
    }
  }
}

// 批量注册指令的函数
export function installPermissionDirectives(Vue) {
  Vue.directive('admin', adminDirective)
  Vue.directive('role', roleDirective)
  Vue.directive('auth', authDirective)
} 