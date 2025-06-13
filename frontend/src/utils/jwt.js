/**
 * JWT工具类 - 前端版本
 */

/**
 * 从JWT Token中获取payload
 * @param {string} token JWT Token
 * @returns {object|null} payload数据
 */
function getPayloadFromToken(token) {
  try {
    if (!token) return null;
    
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    
    const encodedPayload = parts[1];
    const decodedPayload = atob(encodedPayload);
    return JSON.parse(decodedPayload);
  } catch (error) {
    return null;
  }
}

/**
 * 从JWT Token中获取用户ID
 * @param {string} token JWT Token
 * @returns {number|null} 用户ID
 */
export function getUserIdFromToken(token) {
  const payload = getPayloadFromToken(token);
  return payload ? parseInt(payload.sub) : null;
}

/**
 * 从JWT Token中获取用户角色ID
 * @param {string} token JWT Token
 * @returns {number|null} 角色ID
 */
export function getRoleIdFromToken(token) {
  const payload = getPayloadFromToken(token);
  return payload ? payload.role_id : null;
}

/**
 * 从JWT Token中获取用户名
 * @param {string} token JWT Token
 * @returns {string|null} 用户名
 */
export function getUsernameFromToken(token) {
  const payload = getPayloadFromToken(token);
  return payload ? payload.user : null;
}

/**
 * 验证token是否过期
 * @param {string} token JWT Token
 * @returns {boolean} 是否有效
 */
export function isTokenValid(token) {
  const payload = getPayloadFromToken(token);
  if (!payload) return false;
  
  const now = Math.floor(Date.now() / 1000);
  return payload.exp > now;
}

// JwtUtil对象，提供便捷的静态方法调用
export const JwtUtil = {
  /**
   * 从localStorage获取token并解析用户ID
   * @returns {number|null} 用户ID
   */
  getUserIdFromToken() {
    const token = localStorage.getItem('token');
    return getUserIdFromToken(token);
  },
  
  /**
   * 从localStorage获取token并解析角色ID
   * @returns {number|null} 角色ID
   */
  getRoleIdFromToken() {
    const token = localStorage.getItem('token');
    return getRoleIdFromToken(token);
  },
  
  /**
   * 从localStorage获取token并解析用户名
   * @returns {string|null} 用户名
   */
  getUsernameFromToken() {
    const token = localStorage.getItem('token');
    return getUsernameFromToken(token);
  },
  
  /**
   * 验证localStorage中的token是否有效
   * @returns {boolean} 是否有效
   */
  isTokenValid() {
    const token = localStorage.getItem('token');
    return isTokenValid(token);
  }
}; 