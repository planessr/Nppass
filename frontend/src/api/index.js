import Network from './network';

export const login = (data) => Network.post("/user/login", data)

// 用户CRUD操作 - 全部使用POST请求
export const createUser = (data) => Network.post("/user/create", data)
export const getAllUsers = (pageData = {}) => Network.post("/user/list", pageData)
export const updateUser = (data) => Network.post("/user/update", data)
export const deleteUser = (id) => Network.post("/user/delete", { id })
export const getUserPackageInfo = () => Network.post("/user/package")

// 节点CRUD操作 - 全部使用POST请求
export const createNode = (data) => Network.post("/node/create", data)
export const getNodeList = () => Network.post("/node/list")
export const updateNode = (data) => Network.post("/node/update", data)
export const deleteNode = (id) => Network.post("/node/delete", { id })
export const getNodeInstallCommand = (id) => Network.post("/node/install", { id })

// 隧道CRUD操作 - 全部使用POST请求
export const createTunnel = (data) => Network.post("/tunnel/create", data)
export const getTunnelList = () => Network.post("/tunnel/list")
export const getTunnelById = (id) => Network.post("/tunnel/get", { id })
export const deleteTunnel = (id) => Network.post("/tunnel/delete", { id })

// 用户隧道权限管理操作 - 全部使用POST请求
export const assignUserTunnel = (data) => Network.post("/tunnel/user/assign", data)
export const getUserTunnelList = (queryData = {}) => Network.post("/tunnel/user/list", queryData)
export const removeUserTunnel = (params) => Network.post("/tunnel/user/remove", params)
export const updateUserTunnelFlow = (params) => Network.post("/tunnel/user/updateFlow", params)
export const updateUserTunnel = (data) => Network.post("/tunnel/user/update", data)
export const userTunnel = () => Network.post("/tunnel/user/tunnel")

// 转发CRUD操作 - 全部使用POST请求
export const createForward = (data) => Network.post("/forward/create", data)
export const getForwardList = () => Network.post("/forward/list")
export const updateForward = (data) => Network.post("/forward/update", data)
export const deleteForward = (id) => Network.post("/forward/delete", { id })

// 转发服务控制操作 - 通过Java后端接口
export const pauseForwardService = (forwardId) => Network.post("/forward/pause", { id: forwardId })
export const resumeForwardService = (forwardId) => Network.post("/forward/resume", { id: forwardId })

// 限速规则CRUD操作 - 全部使用POST请求
export const createSpeedLimit = (data) => Network.post("/speed-limit/create", data)
export const getSpeedLimitList = () => Network.post("/speed-limit/list")
export const updateSpeedLimit = (data) => Network.post("/speed-limit/update", data)
export const deleteSpeedLimit = (id) => Network.post("/speed-limit/delete", { id })

// 修改密码接口
export const updatePassword = (data) => Network.post("/user/updatePassword", data)

