// 环境变量配置
const config = {
  // 优先使用运行时配置，然后是环境变量，最后是默认值
  API_BASE_URL: window.APP_CONFIG?.VUE_APP_API_URL || process.env.VUE_APP_API_BASE_URL || "http://127.0.0.1:6365/api/v1",
  
  // WebSocket地址配置
  WS_BASE_URL: window.APP_CONFIG?.VUE_APP_WS_URL || process.env.VUE_APP_WS_BASE_URL || "ws://127.0.0.1:6365",
  
  // 其他配置
  REQUEST_TIMEOUT: process.env.VUE_APP_REQUEST_TIMEOUT || 60000
}

export default config 