package com.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.common.dto.ForwardDto;
import com.admin.common.dto.ForwardUpdateDto;
import com.admin.common.dto.ForwardWithTunnelDto;
import com.admin.common.dto.GostDto;
import com.admin.common.lang.R;
import com.admin.common.utils.GostUtil;
import com.admin.common.utils.JwtUtil;
import com.admin.entity.*;
import com.admin.mapper.ForwardMapper;
import com.admin.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 端口转发服务实现类
 * </p>
 *
 * @author QAQ
 * @since 2025-06-03
 */
@Service
public class ForwardServiceImpl extends ServiceImpl<ForwardMapper, Forward> implements ForwardService {

    // 常量定义
    private static final String GOST_SUCCESS_MSG = "OK";
    private static final String GOST_NOT_FOUND_MSG = "not found";
    private static final int ADMIN_ROLE_ID = 0;
    private static final int TUNNEL_TYPE_PORT_FORWARD = 1;
    private static final int TUNNEL_TYPE_TUNNEL_FORWARD = 2;
    private static final int FORWARD_STATUS_ACTIVE = 1;
    private static final int FORWARD_STATUS_PAUSED = 0;
    private static final int FORWARD_STATUS_ERROR = -1;
    private static final int TUNNEL_STATUS_ACTIVE = 1;
    private static final int FLOW_TYPE_UPLOAD_ONLY = 1;
    private static final long BYTES_TO_GB = 1024L * 1024L * 1024L;

    @Resource
    @Lazy
    private TunnelService tunnelService;

    @Resource
    UserTunnelService userTunnelService;

    @Resource
    UserService userService;

    @Resource
    NodeService nodeService;

    @Override
    public R createForward(ForwardDto forwardDto) {
        // 1. 获取当前用户信息
        UserInfo currentUser = getCurrentUserInfo();
        
        // 2. 检查隧道是否存在和可用
        Tunnel tunnel = validateTunnel(forwardDto.getTunnelId());
        if (tunnel == null) {
            return R.err("隧道不存在");
        }
        if (tunnel.getStatus() != TUNNEL_STATUS_ACTIVE) {
            return R.err("隧道已禁用，无法创建转发");
        }

        // 3. 普通用户权限和限制检查
        UserPermissionResult permissionResult = checkUserPermissions(currentUser, tunnel, null);
        if (permissionResult.isHasError()) {
            return R.err(permissionResult.getErrorMessage());
        }

        // 4. 分配端口
        PortAllocation portAllocation = allocatePorts(tunnel);
        if (portAllocation.isHasError()) {
            return R.err(portAllocation.getErrorMessage());
        }

        // 5. 创建并保存Forward对象
        Forward forward = createForwardEntity(forwardDto, currentUser, portAllocation);
        if (!this.save(forward)) {
            return R.err("端口转发创建失败");
        }

        // 6. 调用Gost服务创建转发
        R gostResult = createGostServices(forward, tunnel, permissionResult.getLimiter());
        if (gostResult.getCode() != 0) {
            return gostResult;
        }

        return R.ok();
    }

    @Override
    public R getAllForwards() {
        UserInfo currentUser = getCurrentUserInfo();
        
        List<ForwardWithTunnelDto> forwardList;
        if (currentUser.getRoleId() != ADMIN_ROLE_ID) {
            forwardList = baseMapper.selectForwardsWithTunnelByUserId(currentUser.getUserId());
        } else {
            forwardList = baseMapper.selectAllForwardsWithTunnel();
        }
        
        return R.ok(forwardList);
    }

    @Override
    public R updateForward(ForwardUpdateDto forwardUpdateDto) {
        // 1. 获取当前用户信息
        UserInfo currentUser = getCurrentUserInfo();
        
        // 2. 检查转发是否存在
        Forward existForward = validateForwardExists(forwardUpdateDto.getId(), currentUser);
        if (existForward == null) {
            return R.err("转发不存在");
        }

        // 3. 检查隧道是否存在和可用
        Tunnel tunnel = validateTunnel(forwardUpdateDto.getTunnelId());
        if (tunnel == null) {
            return R.err("隧道不存在");
        }
        if (tunnel.getStatus() != TUNNEL_STATUS_ACTIVE) {
            return R.err("隧道已禁用，无法更新转发");
        }

        // 4. 检查权限和限制（仅当隧道发生变化时）
        UserPermissionResult permissionResult = null;
        if (isTunnelChanged(existForward, forwardUpdateDto)) {
            permissionResult = checkUserPermissions(currentUser, tunnel, forwardUpdateDto.getId());
            if (permissionResult.isHasError()) {
                return R.err(permissionResult.getErrorMessage());
            }
        }

        // 5. 更新Forward对象
        Forward updatedForward = updateForwardEntity(forwardUpdateDto, existForward, tunnel);

        // 6. 调用Gost服务更新转发
        R gostResult = updateGostServices(updatedForward, tunnel, 
                                        permissionResult != null ? permissionResult.getLimiter() : null);
        if (gostResult.getCode() != 0) {
            return gostResult;
        }

        // 7. 保存更新
        boolean result = this.updateById(updatedForward);
        return result ? R.ok("端口转发更新成功") : R.err("端口转发更新失败");
    }

    @Override
    public R deleteForward(Long id) {
        // 1. 获取当前用户信息
        UserInfo currentUser = getCurrentUserInfo();
        
        // 2. 检查转发是否存在
        Forward forward = validateForwardExists(id, currentUser);
        if (forward == null) {
            return R.err("端口转发不存在");
        }

        // 3. 获取隧道信息
        Tunnel tunnel = validateTunnel(forward.getTunnelId());
        if (tunnel == null) {
            return R.err("隧道不存在");
        }

        // 4. 权限检查（仅普通用户需要）
        if (currentUser.getRoleId() != ADMIN_ROLE_ID) {
            if (!hasUserTunnelPermission(currentUser.getUserId(), tunnel.getId().intValue())) {
                return R.err("你没有该隧道权限");
            }
        }

        // 5. 调用Gost服务删除转发
        R gostResult = deleteGostServices(forward, tunnel);
        if (gostResult.getCode() != 0) {
            return gostResult;
        }

        // 6. 删除转发记录
        boolean result = this.removeById(id);
        if (result) {
            // 归还用户转发条数（普通用户才需要归还）
            returnUserForwardQuota(currentUser);
            return R.ok("端口转发删除成功");
        } else {
            return R.err("端口转发删除失败");
        }
    }

    @Override
    public R pauseForward(Long id) {
        return changeForwardStatus(id, FORWARD_STATUS_PAUSED, "暂停", "PauseService");
    }

    @Override
    public R resumeForward(Long id) {
        return changeForwardStatus(id, FORWARD_STATUS_ACTIVE, "恢复", "ResumeService");
    }

    /**
     * 改变转发状态（暂停/恢复）
     */
    private R changeForwardStatus(Long id, int targetStatus, String operation, String gostMethod) {
        // 1. 获取当前用户信息
        UserInfo currentUser = getCurrentUserInfo();
        
        // 2. 检查转发是否存在
        Forward forward = validateForwardExists(id, currentUser);
        if (forward == null) {
            return R.err("转发不存在");
        }

        // 3. 获取隧道信息
        Tunnel tunnel = validateTunnel(forward.getTunnelId());
        if (tunnel == null) {
            return R.err("隧道不存在");
        }

        // 4. 恢复服务时需要额外检查
        if (targetStatus == FORWARD_STATUS_ACTIVE) {
            if (tunnel.getStatus() != TUNNEL_STATUS_ACTIVE) {
                return R.err("隧道已禁用，无法恢复服务");
            }
            
            // 普通用户需要检查流量和账户状态
            if (currentUser.getRoleId() != ADMIN_ROLE_ID) {
                R flowCheckResult = checkUserFlowLimits(currentUser.getUserId(), tunnel);
                if (flowCheckResult.getCode() != 0) {
                    return flowCheckResult;
                }
            }
        }

        // 5. 权限检查（仅普通用户需要）
        if (currentUser.getRoleId() != ADMIN_ROLE_ID) {
            if (!hasUserTunnelPermission(currentUser.getUserId(), tunnel.getId().intValue())) {
                return R.err("你没有该隧道权限");
            }
        }

        // 6. 调用Gost服务
        Node node = nodeService.getNodeById(tunnel.getInNodeId());
        if (node == null) {
            return R.err("节点不存在");
        }

        String serviceName = buildServiceName(forward.getId(), forward.getUserId(), forward.getTunnelId());
        GostDto gostResult;
        
        if ("PauseService".equals(gostMethod)) {
            gostResult = GostUtil.PauseService(node.getIp() + ":" + node.getPort(), serviceName, node.getSecret());
            
            // 隧道转发需要同时暂停远端服务
            if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
                Node outNode = nodeService.getNodeById(tunnel.getOutNodeId());
                if (outNode != null) {
                    GostDto remoteResult = GostUtil.PauseRemoteService(outNode.getIp() + ":" + outNode.getPort(), serviceName, outNode.getSecret());
                    if (!isGostOperationSuccess(remoteResult)) {
                        return R.err(operation + "远端服务失败：" + remoteResult.getMsg());
                    }
                }
            }
        } else {
            gostResult = GostUtil.ResumeService(node.getIp() + ":" + node.getPort(), serviceName, node.getSecret());
            
            // 隧道转发需要同时恢复远端服务
            if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
                Node outNode = nodeService.getNodeById(tunnel.getOutNodeId());
                if (outNode != null) {
                    GostDto remoteResult = GostUtil.ResumeRemoteService(outNode.getIp() + ":" + outNode.getPort(), serviceName, outNode.getSecret());
                    if (!isGostOperationSuccess(remoteResult)) {
                        return R.err(operation + "远端服务失败：" + remoteResult.getMsg());
                    }
                }
            }
        }

        if (!isGostOperationSuccess(gostResult)) {
            return R.err(operation + "服务失败：" + gostResult.getMsg());
        }

        // 7. 更新转发状态
        forward.setStatus(targetStatus);
        forward.setUpdatedTime(System.currentTimeMillis());
        boolean result = this.updateById(forward);
        
        return result ? R.ok("服务已" + operation) : R.err("更新状态失败");
    }

    /**
     * 获取当前用户信息
     */
    private UserInfo getCurrentUserInfo() {
        Integer userId = JwtUtil.getUserIdFromToken();
        Integer roleId = JwtUtil.getRoleIdFromToken();
        String userName = JwtUtil.getNameFromToken();
        return new UserInfo(userId, roleId, userName);
    }

    /**
     * 验证隧道是否存在
     */
    private Tunnel validateTunnel(Integer tunnelId) {
        return tunnelService.getById(tunnelId);
    }

    /**
     * 验证转发是否存在且用户有权限访问
     */
    private Forward validateForwardExists(Long forwardId, UserInfo currentUser) {
        Forward forward = this.getById(forwardId);
        if (forward == null) {
            return null;
        }
        
        // 普通用户只能操作自己的转发
        if (currentUser.getRoleId() != ADMIN_ROLE_ID && 
            !Objects.equals(currentUser.getUserId(), forward.getUserId())) {
            return null;
        }
        
        return forward;
    }

    /**
     * 检查用户权限和限制
     */
    private UserPermissionResult checkUserPermissions(UserInfo currentUser, Tunnel tunnel, Long excludeForwardId) {
        if (currentUser.getRoleId() == ADMIN_ROLE_ID) {
            return UserPermissionResult.success(null);
        }

        // 获取用户信息
        User userInfo = userService.getById(currentUser.getUserId());
        if (userInfo.getExpTime() != null && userInfo.getExpTime() <= System.currentTimeMillis()) {
            return UserPermissionResult.error("当前账号已到期");
        }

        // 检查用户隧道权限
        UserTunnel userTunnel = getUserTunnel(currentUser.getUserId(), tunnel.getId().intValue());
        if (userTunnel == null) {
            return UserPermissionResult.error("你没有该隧道权限");
        }
        
        // 检查隧道权限到期时间
        if (userTunnel.getExpTime() != null && userTunnel.getExpTime() <= System.currentTimeMillis()) {
            return UserPermissionResult.error("该隧道权限已到期");
        }

        // 流量限制检查
        if (userInfo.getFlow() <= 0) {
            return UserPermissionResult.error("用户总流量已用完");
        }
        if (userTunnel.getFlow() <= 0) {
            return UserPermissionResult.error("该隧道流量已用完");
        }

        // 转发数量限制检查
        R quotaCheckResult = checkForwardQuota(currentUser.getUserId(), tunnel.getId().intValue(), userTunnel, userInfo, excludeForwardId);
        if (quotaCheckResult.getCode() != 0) {
            return UserPermissionResult.error(quotaCheckResult.getMsg());
        }

        return UserPermissionResult.success(userTunnel.getSpeedId());
    }

    /**
     * 检查用户转发数量限制
     */
    private R checkForwardQuota(Integer userId, Integer tunnelId, UserTunnel userTunnel, User userInfo, Long excludeForwardId) {
        // 检查用户总转发数量限制
        long userForwardCount = this.count(new QueryWrapper<Forward>().eq("user_id", userId));
        if (userForwardCount >= userInfo.getNum()) {
            return R.err("用户总转发数量已达上限，当前限制：" + userInfo.getNum() + "个");
        }

        // 检查用户在该隧道的转发数量限制
        QueryWrapper<Forward> tunnelQuery = new QueryWrapper<Forward>()
                .eq("user_id", userId)
                .eq("tunnel_id", tunnelId);
        
        if (excludeForwardId != null) {
            tunnelQuery.ne("id", excludeForwardId);
        }
        
        long tunnelForwardCount = this.count(tunnelQuery);
        if (tunnelForwardCount >= userTunnel.getNum()) {
            return R.err("该隧道转发数量已达上限，当前限制：" + userTunnel.getNum() + "个");
        }

        return R.ok();
    }

    /**
     * 检查用户流量限制
     */
    private R checkUserFlowLimits(Integer userId, Tunnel tunnel) {
        User userInfo = userService.getById(userId);
        if (userInfo.getExpTime() != null && userInfo.getExpTime() <= System.currentTimeMillis()) {
            return R.err("当前账号已到期");
        }

        UserTunnel userTunnel = getUserTunnel(userId, tunnel.getId().intValue());
        if (userTunnel == null) {
            return R.err("你没有该隧道权限");
        }
        
        // 检查隧道权限到期时间
        if (userTunnel.getExpTime() != null && userTunnel.getExpTime() <= System.currentTimeMillis()) {
            return R.err("该隧道权限已到期，无法恢复服务");
        }

        // 检查用户总流量限制
        if (userInfo.getFlow() * BYTES_TO_GB <= userInfo.getInFlow() + userInfo.getOutFlow()) {
            return R.err("用户总流量已用完，无法恢复服务");
        }

        // 检查隧道流量限制
        long tunnelFlow = (tunnel.getFlow() == FLOW_TYPE_UPLOAD_ONLY) ? 
                         userTunnel.getOutFlow() : 
                         userTunnel.getInFlow() + userTunnel.getOutFlow();
        
        if (userTunnel.getFlow() * BYTES_TO_GB <= tunnelFlow) {
            return R.err("该隧道流量已用完，无法恢复服务");
        }

        return R.ok();
    }

    /**
     * 分配端口
     */
    private PortAllocation allocatePorts(Tunnel tunnel) {
        Integer inPort = allocateInPort(tunnel);
        if (inPort == null) {
            return PortAllocation.error("隧道入口端口已满，无法分配新端口");
        }

        Integer outPort = null;
        if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
            outPort = allocateOutPort(tunnel);
            if (outPort == null) {
                return PortAllocation.error("隧道出口端口已满，无法分配新端口");
            }
        }

        return PortAllocation.success(inPort, outPort);
    }

    /**
     * 创建Forward实体对象
     */
    private Forward createForwardEntity(ForwardDto forwardDto, UserInfo currentUser, PortAllocation portAllocation) {
        Forward forward = new Forward();
        forward.setStatus(FORWARD_STATUS_ACTIVE);
        forward.setInPort(portAllocation.getInPort());
        forward.setOutPort(portAllocation.getOutPort());
        forward.setUserId(currentUser.getUserId());
        forward.setUserName(currentUser.getUserName());
        forward.setCreatedTime(System.currentTimeMillis());
        forward.setUpdatedTime(System.currentTimeMillis());
        BeanUtils.copyProperties(forwardDto, forward);
        return forward;
    }

    /**
     * 更新Forward实体对象
     */
    private Forward updateForwardEntity(ForwardUpdateDto forwardUpdateDto, Forward existForward, Tunnel tunnel) {
        Forward forward = new Forward();
        BeanUtils.copyProperties(forwardUpdateDto, forward);

        // 如果隧道ID发生变化，需要重新分配端口
        if (!existForward.getTunnelId().equals(forwardUpdateDto.getTunnelId())) {
            PortAllocation portAllocation = allocatePorts(tunnel);
            forward.setInPort(portAllocation.getInPort());
            forward.setOutPort(portAllocation.getOutPort());
        } else {
            // 隧道未变化，保持原端口
            forward.setInPort(existForward.getInPort());
            forward.setOutPort(existForward.getOutPort());
        }

        forward.setUpdatedTime(System.currentTimeMillis());
        return forward;
    }

    /**
     * 创建Gost服务
     */
    private R createGostServices(Forward forward, Tunnel tunnel, Integer limiter) {
        String serviceName = buildServiceName(forward.getId(), forward.getUserId(), forward.getTunnelId());
        Node inNode = nodeService.getNodeById(tunnel.getInNodeId());

        // 隧道转发需要创建链和远程服务
        if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
            R chainResult = createChainService(inNode, serviceName, tunnel.getOutIp(), forward.getOutPort());
            if (chainResult.getCode() != 0) {
                updateForwardStatusToError(forward);
                return chainResult;
            }

            R remoteResult = createRemoteService(tunnel.getOutNodeId().intValue(), serviceName, forward);
            if (remoteResult.getCode() != 0) {
                updateForwardStatusToError(forward);
                return remoteResult;
            }
        }

        // 创建主服务
        R serviceResult = createMainService(inNode, serviceName, forward, limiter, tunnel.getType());
        if (serviceResult.getCode() != 0) {
            updateForwardStatusToError(forward);
            return serviceResult;
        }

        return R.ok();
    }

    /**
     * 更新Gost服务
     */
    private R updateGostServices(Forward forward, Tunnel tunnel, Integer limiter) {
        String serviceName = buildServiceName(forward.getId(), forward.getUserId(), forward.getTunnelId());
        Node inNode = nodeService.getNodeById(tunnel.getInNodeId());

        // 隧道转发需要更新链和远程服务
        if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
            R chainResult = updateChainService(inNode, serviceName, tunnel.getOutIp(), forward.getOutPort());
            if (chainResult.getCode() != 0) {
                updateForwardStatusToError(forward);
                return chainResult;
            }

            R remoteResult = updateRemoteService(tunnel.getOutNodeId().intValue(), serviceName, forward);
            if (remoteResult.getCode() != 0) {
                updateForwardStatusToError(forward);
                return remoteResult;
            }
        }

        // 更新主服务
        R serviceResult = updateMainService(inNode, serviceName, forward, limiter, tunnel.getType());
        if (serviceResult.getCode() != 0) {
            updateForwardStatusToError(forward);
            return serviceResult;
        }

        return R.ok();
    }

    /**
     * 删除Gost服务
     */
    private R deleteGostServices(Forward forward, Tunnel tunnel) {
        String serviceName = buildServiceName(forward.getId(), forward.getUserId(), forward.getTunnelId());
        Node inNode = nodeService.getNodeById(tunnel.getInNodeId());

        // 删除主服务
        GostDto serviceResult = GostUtil.DeleteService(inNode.getIp() + ":" + inNode.getPort(), serviceName, inNode.getSecret());
        if (!isGostOperationSuccess(serviceResult)) {
            return R.err(serviceResult.getMsg());
        }

        // 隧道转发需要删除链和远程服务
        if (tunnel.getType() == TUNNEL_TYPE_TUNNEL_FORWARD) {
            GostDto chainResult = GostUtil.DeleteChains(inNode.getIp() + ":" + inNode.getPort(), serviceName, inNode.getSecret());
            if (!isGostOperationSuccess(chainResult)) {
                return R.err(chainResult.getMsg());
            }

            Node outNode = nodeService.getNodeById(tunnel.getOutNodeId());
            GostDto remoteResult = GostUtil.DeleteRemoteService(outNode.getIp() + ":" + outNode.getPort(), serviceName, outNode.getSecret());
            if (!isGostOperationSuccess(remoteResult)) {
                return R.err(remoteResult.getMsg());
            }
        }

        return R.ok();
    }

    /**
     * 创建链服务
     */
    private R createChainService(Node inNode, String serviceName, String outIp, Integer outPort) {
        String remoteAddr = outIp + ":" + outPort;
        GostDto result = GostUtil.AddChains(inNode.getIp() + ":" + inNode.getPort(), serviceName, remoteAddr, inNode.getSecret());
        return isGostOperationSuccess(result) ? R.ok() : R.err(result.getMsg());
    }

    /**
     * 创建远程服务
     */
    private R createRemoteService(Integer outNodeId, String serviceName, Forward forward) {
        Node outNode = nodeService.getNodeById(outNodeId.longValue());
        GostDto result = GostUtil.AddRemoteService(outNode.getIp() + ":" + outNode.getPort(), 
                                                  serviceName, forward.getOutPort(), 
                                                  forward.getRemoteAddr(), outNode.getSecret());
        return isGostOperationSuccess(result) ? R.ok() : R.err(result.getMsg());
    }

    /**
     * 创建主服务
     */
    private R createMainService(Node inNode, String serviceName, Forward forward, Integer limiter, Integer tunnelType) {
        GostDto result = GostUtil.AddService(inNode.getIp() + ":" + inNode.getPort(), serviceName, 
                                           forward.getInPort(), limiter, forward.getRemoteAddr(), 
                                           inNode.getSecret(), tunnelType);
        return isGostOperationSuccess(result) ? R.ok() : R.err(result.getMsg());
    }

    /**
     * 更新链服务
     */
    private R updateChainService(Node inNode, String serviceName, String outIp, Integer outPort) {

        // 创建新链
        String remoteAddr = outIp + ":" + outPort;
        GostDto createResult = GostUtil.UpdateChains(inNode.getIp() + ":" + inNode.getPort(), serviceName, remoteAddr, inNode.getSecret());
        if (createResult.getMsg().contains(GOST_NOT_FOUND_MSG)) {
            createResult = GostUtil.AddChains(inNode.getIp() + ":" + inNode.getPort(), serviceName, remoteAddr, inNode.getSecret());
        }
        return isGostOperationSuccess(createResult) ? R.ok() : R.err(createResult.getMsg());
    }

    /**
     * 更新远程服务
     */
    private R updateRemoteService(Integer outNodeId, String serviceName, Forward forward) {
        Node outNode = nodeService.getNodeById(outNodeId.longValue());
        // 创建新远程服务
        GostDto createResult = GostUtil.UpdateRemoteService(outNode.getIp() + ":" + outNode.getPort(),
                                                        serviceName, forward.getOutPort(), 
                                                        forward.getRemoteAddr(), outNode.getSecret());
        if (createResult.getMsg().contains(GOST_NOT_FOUND_MSG)) {
            createResult = GostUtil.AddRemoteService(outNode.getIp() + ":" + outNode.getPort(),
                    serviceName, forward.getOutPort(),
                    forward.getRemoteAddr(), outNode.getSecret());
        }
        return isGostOperationSuccess(createResult) ? R.ok() : R.err(createResult.getMsg());
    }

    /**
     * 更新主服务
     */
    private R updateMainService(Node inNode, String serviceName, Forward forward, Integer limiter, Integer tunnelType) {
        GostDto result = GostUtil.UpdateService(inNode.getIp() + ":" + inNode.getPort(), serviceName, 
                                              forward.getInPort(), limiter, forward.getRemoteAddr(), 
                                              inNode.getSecret(), tunnelType);
        
        if (result.getMsg().contains(GOST_NOT_FOUND_MSG)) {
            result = GostUtil.AddService(inNode.getIp() + ":" + inNode.getPort(), serviceName, 
                                       forward.getInPort(), limiter, forward.getRemoteAddr(), 
                                       inNode.getSecret(), tunnelType);
        }
        
        return isGostOperationSuccess(result) ? R.ok() : R.err(result.getMsg());
    }

    /**
     * 更新转发状态为错误
     */
    private void updateForwardStatusToError(Forward forward) {
        forward.setStatus(FORWARD_STATUS_ERROR);
        this.updateById(forward);
    }

    /**
     * 检查是否有用户隧道权限
     */
    private boolean hasUserTunnelPermission(Integer userId, Integer tunnelId) {
        return getUserTunnel(userId, tunnelId) != null;
    }

    /**
     * 获取用户隧道关系
     */
    private UserTunnel getUserTunnel(Integer userId, Integer tunnelId) {
        return userTunnelService.getOne(new QueryWrapper<UserTunnel>()
                .eq("user_id", userId)
                .eq("tunnel_id", tunnelId));
    }

    /**
     * 检查隧道是否发生变化
     */
    private boolean isTunnelChanged(Forward existForward, ForwardUpdateDto updateDto) {
        return !existForward.getTunnelId().equals(updateDto.getTunnelId());
    }

    /**
     * 归还用户转发配额
     */
    private void returnUserForwardQuota(UserInfo currentUser) {
        if (currentUser.getRoleId() != ADMIN_ROLE_ID) {
            User user = userService.getById(currentUser.getUserId());
            if (user != null) {
                user.setNum(user.getNum() + 1);
                user.setUpdatedTime(System.currentTimeMillis());
                userService.updateById(user);
            }
        }
    }

    /**
     * 检查Gost操作是否成功
     */
    private boolean isGostOperationSuccess(GostDto gostResult) {
        return Objects.equals(gostResult.getMsg(), GOST_SUCCESS_MSG);
    }

    /**
     * 为隧道分配一个可用的入口端口
     */
    private Integer allocateInPort(Tunnel tunnel) {
        // 获取所有使用相同入口节点的隧道
        List<Tunnel> tunnelsWithSameInNode = tunnelService.list(new QueryWrapper<Tunnel>().eq("in_node_id", tunnel.getInNodeId()));
        Set<Long> tunnelIds = tunnelsWithSameInNode.stream()
                .map(Tunnel::getId)
                .collect(Collectors.toSet());

        // 获取这些隧道的所有转发已使用的入口端口
        List<Forward> usedForwards = this.list(new QueryWrapper<Forward>().in("tunnel_id", tunnelIds));
        Set<Integer> usedInPorts = usedForwards.stream()
                .map(Forward::getInPort)
                .filter(port -> port != null)
                .collect(Collectors.toSet());

        // 在隧道端口范围内寻找未使用的端口
        for (int port = tunnel.getInPortSta(); port <= tunnel.getInPortEnd(); port++) {
            if (!usedInPorts.contains(port)) {
                return port;
            }
        }
        return null;
    }

    /**
     * 为隧道分配一个可用的出口端口
     */
    private Integer allocateOutPort(Tunnel tunnel) {
        // 获取所有使用相同出口节点的隧道
        List<Tunnel> tunnelsWithSameOutNode = tunnelService.list(new QueryWrapper<Tunnel>().eq("out_node_id", tunnel.getOutNodeId()));
        Set<Long> tunnelIds = tunnelsWithSameOutNode.stream()
                .map(Tunnel::getId)
                .collect(Collectors.toSet());

        // 获取这些隧道的所有转发已使用的出口端口
        List<Forward> usedForwards = this.list(new QueryWrapper<Forward>().in("tunnel_id", tunnelIds));
        Set<Integer> usedOutPorts = usedForwards.stream()
                .map(Forward::getOutPort)
                .filter(port -> port != null)
                .collect(Collectors.toSet());

        // 在隧道出口端口范围内寻找未使用的端口
        for (int port = tunnel.getOutIpSta(); port <= tunnel.getOutIpEnd(); port++) {
            if (!usedOutPorts.contains(port)) {
                return port;
            }
        }
        return null;
    }

    /**
     * 构建服务名称，确保管理员和用户操作的一致性
     */
    private String buildServiceName(Long forwardId, Integer userId, Integer tunnelId) {
        // 根据userId和tunnelId查询UserTunnel获取正确的user_tunnel_id
        UserTunnel userTunnel = userTunnelService.getOne(new QueryWrapper<UserTunnel>()
            .eq("user_id", userId)
            .eq("tunnel_id", tunnelId));
        
        int userTunnelId = (userTunnel != null) ? userTunnel.getId() : 0;
        
        return forwardId + "_" + userId + "_" + userTunnelId;
    }

    // ========== 内部数据类 ==========

    /**
     * 用户信息封装类
     */
    @Data
    private static class UserInfo {
        private final Integer userId;
        private final Integer roleId;
        private final String userName;

    }

    /**
     * 用户权限检查结果
     */
    @Data
    private static class UserPermissionResult {
        private final boolean hasError;
        private final String errorMessage;
        private final Integer limiter;

        private UserPermissionResult(boolean hasError, String errorMessage, Integer limiter) {
            this.hasError = hasError;
            this.errorMessage = errorMessage;
            this.limiter = limiter;
        }

        public static UserPermissionResult success(Integer limiter) {
            return new UserPermissionResult(false, null, limiter);
        }

        public static UserPermissionResult error(String errorMessage) {
            return new UserPermissionResult(true, errorMessage, null);
        }
    }

    /**
     * 端口分配结果
     */
    @Data
    private static class PortAllocation {
        private final boolean hasError;
        private final String errorMessage;
        private final Integer inPort;
        private final Integer outPort;

        private PortAllocation(boolean hasError, String errorMessage, Integer inPort, Integer outPort) {
            this.hasError = hasError;
            this.errorMessage = errorMessage;
            this.inPort = inPort;
            this.outPort = outPort;
        }

        public static PortAllocation success(Integer inPort, Integer outPort) {
            return new PortAllocation(false, null, inPort, outPort);
        }

        public static PortAllocation error(String errorMessage) {
            return new PortAllocation(true, errorMessage, null, null);
        }
    }
}
