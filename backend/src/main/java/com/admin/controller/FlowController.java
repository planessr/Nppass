package com.admin.controller;

import com.admin.common.aop.LogAnnotation;
import com.admin.common.dto.FlowDto;
import com.admin.common.lang.R;
import com.admin.common.utils.GostUtil;
import com.admin.entity.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 流量上报控制器
 * 处理节点上报的流量数据，更新用户和隧道的流量统计
 * 
 * 并发安全解决方案：
 * 1. 使用UpdateWrapper进行数据库层面的原子更新操作，避免读取-修改-写入的竞态条件
 * 2. 使用synchronized锁确保同一用户/隧道的流量更新串行执行
 * 3. 这样可以避免相同用户相同隧道不同转发同时上报时流量统计丢失的问题
 */
@RestController
@RequestMapping("/flow")
@CrossOrigin
public class FlowController extends BaseController {

    // 常量定义
    private static final String SUCCESS_RESPONSE = "ok";
    private static final String ERROR_RESPONSE = "err1";
    private static final String DEFAULT_USER_TUNNEL_ID = "0";
    private static final int FLOW_TYPE_UPLOAD_ONLY = 1;
    private static final int FLOW_TYPE_BIDIRECTIONAL = 2;
    private static final long BYTES_TO_GB = 1024L * 1024L * 1024L;

    // 用于同步相同用户和隧道的流量更新操作
    private static final ConcurrentHashMap<String, Object> USER_LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> TUNNEL_LOCKS = new ConcurrentHashMap<>();


    @RequestMapping("/test")
    @LogAnnotation
    public String test() {
        return "test";
    }
    /**
     * 处理流量数据上报
     * @param flowDataList 流量数据列表
     * @param secret 节点密钥
     * @return 处理结果
     */
    @RequestMapping("/upload")
    @LogAnnotation
    public String uploadFlowData(@RequestBody List<FlowDto> flowDataList, String secret) {
        // 1. 验证节点权限
        if (!isValidNode(secret)) {
            return ERROR_RESPONSE;
        }

        // 2. 过滤有效流量数据
        List<FlowDto> validFlowData = filterValidFlowData(flowDataList);
        if (validFlowData.isEmpty()) {
            return SUCCESS_RESPONSE;
        }

        // 3. 解析服务名称获取ID信息
        String[] serviceIds = parseServiceName(validFlowData.get(0).getN());
        String forwardId = serviceIds[0];
        String userId = serviceIds[1];
        String userTunnelId = serviceIds[2];

        // 4. 计算总流量
        FlowStatistics flowStats = calculateTotalFlow(validFlowData);

        // 5. 获取流量计费类型
        int flowType = getFlowType(forwardId);

        // 6. 更新各项流量统计
        updateForwardFlow(forwardId, flowStats);
        updateUserFlow(userId, flowStats, flowType);
        updateUserTunnelFlow(userTunnelId, flowStats, flowType, forwardId, userId);
        
        // 7. 检查用户总流量限制
        checkUserTotalFlowLimit(userId, userTunnelId);

        return SUCCESS_RESPONSE;
    }

    /**
     * 验证节点是否有效
     */
    private boolean isValidNode(String secret) {
        int nodeCount = nodeService.count(new QueryWrapper<Node>().eq("secret", secret));
        return nodeCount > 0;
    }

    /**
     * 过滤有效的流量数据
     */
    private List<FlowDto> filterValidFlowData(List<FlowDto> flowDataList) {
        return flowDataList.stream()
                .filter(flow -> flow.getU() != null && flow.getD() != null)
                .filter(flow -> flow.getU() > 0 && flow.getD() > 0)
                .collect(Collectors.toList());
    }

    /**
     * 解析服务名称获取ID信息
     */
    private String[] parseServiceName(String serviceName) {
        return serviceName.split("_");
    }

    /**
     * 计算总流量统计
     */
    private FlowStatistics calculateTotalFlow(List<FlowDto> validFlowData) {
        long totalUpload = 0L;
        long totalDownload = 0L;
        
        for (FlowDto flow : validFlowData) {
            totalUpload += flow.getU();
            totalDownload += flow.getD();
        }
        
        return new FlowStatistics(totalUpload, totalDownload);
    }

    /**
     * 获取流量计费类型
     */
    private int getFlowType(String forwardId) {
        int defaultFlowType = FLOW_TYPE_BIDIRECTIONAL;
        
        Forward forward = forwardService.getById(forwardId);
        if (forward != null) {
            Tunnel tunnel = tunnelService.getById(forward.getTunnelId());
            if (tunnel != null) {
                return tunnel.getFlow();
            }
        }
        
        return defaultFlowType;
    }

    /**
     * 更新转发流量统计 - 使用原子操作避免并发问题
     */
    private void updateForwardFlow(String forwardId, FlowStatistics flowStats) {
        UpdateWrapper<Forward> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", forwardId);
        updateWrapper.setSql("in_flow = in_flow + " + flowStats.getDownload());
        updateWrapper.setSql("out_flow = out_flow + " + flowStats.getUpload());
        
        forwardService.update(null, updateWrapper);
    }

    /**
     * 更新用户流量统计 - 使用原子操作避免并发问题
     */
    private void updateUserFlow(String userId, FlowStatistics flowStats, int flowType) {
        // 对相同用户的流量更新进行同步，避免并发覆盖
        synchronized (getUserLock(userId)) {
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userId);
            
            // 使用SQL的原子更新操作，避免读取-修改-写入的并发问题
            if (flowType == FLOW_TYPE_BIDIRECTIONAL) {
                // 双向计费：同时更新上传和下载流量
                updateWrapper.setSql("in_flow = in_flow + " + flowStats.getDownload());
                updateWrapper.setSql("out_flow = out_flow + " + flowStats.getUpload());
            } else {
                // 仅上传计费：只更新上传流量
                updateWrapper.setSql("out_flow = out_flow + " + flowStats.getUpload());
            }
            
            userService.update(null, updateWrapper);
        }
    }

    /**
     * 更新用户隧道流量统计并检查限制
     */
    private void updateUserTunnelFlow(String userTunnelId, FlowStatistics flowStats, 
                                    int flowType, String forwardId, String userId) {
        if (Objects.equals(userTunnelId, DEFAULT_USER_TUNNEL_ID)) {
            return;
        }

        // 对相同用户隧道的流量更新进行同步，避免并发覆盖
        synchronized (getTunnelLock(userTunnelId)) {
            UpdateWrapper<UserTunnel> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userTunnelId);
            updateWrapper.setSql("in_flow = in_flow + " + flowStats.getDownload());
            updateWrapper.setSql("out_flow = out_flow + " + flowStats.getUpload());
            
            boolean updateSuccess = userTunnelService.update(null, updateWrapper);
            if (!updateSuccess) {
                return; // 更新失败，可能记录不存在
            }
        }

        // 重新获取最新的流量数据进行限制检查
        UserTunnel userTunnel = userTunnelService.getById(userTunnelId);
        if (userTunnel != null) {
            checkUserTunnelFlowLimit(userTunnel, flowType, forwardId, userId, userTunnelId);
        }
    }

    /**
     * 检查用户隧道流量限制
     */
    private void checkUserTunnelFlowLimit(UserTunnel userTunnel, int flowType, 
                                        String forwardId, String userId, String userTunnelId) {
        long currentFlow = (flowType == FLOW_TYPE_UPLOAD_ONLY) ? 
                          userTunnel.getOutFlow() : 
                          userTunnel.getInFlow() + userTunnel.getOutFlow();

        long flowLimit = userTunnel.getFlow() * BYTES_TO_GB;
        
        if (flowLimit < currentFlow) {
            pauseServiceDueToTunnelLimit(userTunnel.getTunnelId(), forwardId, userId, userTunnelId);
        }
    }

    /**
     * 因隧道流量超限暂停服务
     */
    private void pauseServiceDueToTunnelLimit(Integer tunnelId, String forwardId, 
                                            String userId, String userTunnelId) {
        Tunnel tunnel = tunnelService.getById(tunnelId);
        if (tunnel != null) {
            Node node = nodeService.getNodeById(tunnel.getInNodeId());
            if (node != null) {
                String serviceName = buildServiceName(forwardId, userId, userTunnelId);
                GostUtil.PauseService(node.getIp() + ":" + node.getPort(), serviceName, node.getSecret());
                
                // 隧道转发需要同时暂停远端服务
                if (tunnel.getType() == 2) { // TUNNEL_TYPE_TUNNEL_FORWARD
                    Node outNode = nodeService.getNodeById(tunnel.getOutNodeId());
                    if (outNode != null) {
                        GostUtil.PauseRemoteService(outNode.getIp() + ":" + outNode.getPort(), serviceName, outNode.getSecret());
                    }
                }
            }
        }

        // 更新转发状态为暂停
        Forward forward = forwardService.getById(forwardId);
        if (forward != null) {
            forward.setStatus(0);
            forwardService.updateById(forward);
        }
    }

    /**
     * 检查用户总流量限制
     */
    private void checkUserTotalFlowLimit(String userId, String userTunnelId) {
        User user = userService.getById(userId);
        if (user == null) {
            return;
        }

        long userFlowLimit = user.getFlow() * BYTES_TO_GB;
        long userCurrentFlow = user.getInFlow() + user.getOutFlow();
        
        if (userFlowLimit < userCurrentFlow) {
            pauseAllUserServices(userId, userTunnelId);
        }
    }

    /**
     * 暂停用户所有服务
     */
    private void pauseAllUserServices(String userId, String userTunnelId) {
        List<Forward> userForwards = forwardService.list(new QueryWrapper<Forward>().eq("user_id", userId));
        
        for (Forward forward : userForwards) {
            Tunnel tunnel = tunnelService.getById(forward.getTunnelId());
            if (tunnel != null) {
                Node node = nodeService.getNodeById(tunnel.getInNodeId());
                if (node != null) {
                    String serviceName = buildServiceName(String.valueOf(forward.getId()), userId, userTunnelId);
                    GostUtil.PauseService(node.getIp() + ":" + node.getPort(), serviceName, node.getSecret());
                    
                    // 隧道转发需要同时暂停远端服务
                    if (tunnel.getType() == 2) { // TUNNEL_TYPE_TUNNEL_FORWARD
                        Node outNode = nodeService.getNodeById(tunnel.getOutNodeId());
                        if (outNode != null) {
                            GostUtil.PauseRemoteService(outNode.getIp() + ":" + outNode.getPort(), serviceName, outNode.getSecret());
                        }
                    }
                }
            }
            
            forward.setStatus(0);
            forwardService.updateById(forward);
        }
    }

    /**
     * 构建服务名称
     */
    private String buildServiceName(String forwardId, String userId, String userTunnelId) {
        return forwardId + "_" + userId + "_" + userTunnelId;
    }

    /**
     * 获取用户锁对象
     */
    private Object getUserLock(String userId) {
        return USER_LOCKS.computeIfAbsent(userId, k -> new Object());
    }

    /**
     * 获取隧道锁对象
     */
    private Object getTunnelLock(String userTunnelId) {
        return TUNNEL_LOCKS.computeIfAbsent(userTunnelId, k -> new Object());
    }

    /**
     * 流量统计数据类
     */
    private static class FlowStatistics {
        private final long upload;
        private final long download;

        public FlowStatistics(long upload, long download) {
            this.upload = upload;
            this.download = download;
        }

        public long getUpload() {
            return upload;
        }

        public long getDownload() {
            return download;
        }
    }
}
