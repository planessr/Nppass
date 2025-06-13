import { getRoleIdFromToken, isTokenValid } from './jwt'

/**
 * 权限工具类
 */

/**
 * 获取当前用户的token
 * @returns {string|null} token
 */
export function getToken() {
  return localStorage.getItem('token')
}

/**
 * 获取当前用户的角色ID
 * @returns {number|null} 角色ID
 */
export function getCurrentUserRoleId() {
  const token = getToken()
  if (!token || !isTokenValid(token)) {
    return null
  }
  return getRoleIdFromToken(token)
}

/**
 * 判断当前用户是否是管理员
 * @returns {boolean} 是否是管理员
 */
export function isAdmin() {
  const roleId = getCurrentUserRoleId()
  return roleId === 0
}

/**
 * 判断当前用户是否有指定角色
 * @param {number} targetRoleId 目标角色ID
 * @returns {boolean} 是否有指定角色
 */
export function hasRole(targetRoleId) {
  const roleId = getCurrentUserRoleId()
  return roleId === targetRoleId
}

/**
 * 判断当前用户是否已登录且token有效
 * @returns {boolean} 是否已登录
 */
export function isLoggedIn() {
  const token = getToken()
  return token && isTokenValid(token)
}

/**
 * 权限检查装饰器函数
 * @param {Function} fn 要执行的函数
 * @param {string} errorMsg 权限不足时的错误提示
 * @returns {Function} 包装后的函数
 */
export function requireAdmin(fn, errorMsg = '权限不足，仅管理员可操作') {
  return function(...args) {
    if (!isAdmin()) {
      // 可以在这里集成消息提示组件
      console.warn(errorMsg)
      return false
    }
    return fn.apply(this, args)
  }
} 