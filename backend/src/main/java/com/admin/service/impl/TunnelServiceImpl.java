package com.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.common.dto.TunnelDto;
import com.admin.common.dto.TunnelListDto;

import com.admin.common.lang.R;
import com.admin.common.utils.JwtUtil;
import com.admin.entity.Forward;
import com.admin.entity.Node;
import com.admin.entity.Tunnel;
import com.admin.entity.User;
import com.admin.entity.UserTunnel;
import com.admin.mapper.TunnelMapper;
import com.admin.mapper.UserTunnelMapper;
import com.admin.service.ForwardService;
import com.admin.service.NodeService;
import com.admin.service.TunnelService;
import com.admin.service.UserTunnelService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 隧道服务实现类
 * 提供隧道的增删改查功能，包括隧道创建、删除和用户权限管理
 * 支持端口转发和隧道转发两种模式
 * </p>
 *
 * @author QAQ
 * @since 2025-06-03
 */
@Service
public class TunnelServiceImpl extends ServiceImpl<TunnelMapper, Tunnel> implements TunnelService {

    // ========== 常量定义 ==========
    
    /** 隧道类型常量 */
    private static final int TUNNEL_TYPE_PORT_FORWARD = 1;  // 端口转发
    private static final int TUNNEL_TYPE_TUNNEL_FORWARD = 2; // 隧道转发
    
    /** 隧道状态常量 */
    private static final int TUNNEL_STATUS_ACTIVE = 1;      // 启用状态
    
    /** 用户角色常量 */
    private static final int ADMIN_ROLE_ID = 0;             // 管理员角色ID
    
    /** 成功响应消息 */
    private static final String SUCCESS_CREATE_MSG = "隧道创建成功";
    private static final String SUCCESS_DELETE_MSG = "隧道删除成功";
    
    /** 错误响应消息 */
    private static final String ERROR_CREATE_MSG = "隧道创建失败";
    private static final String ERROR_DELETE_MSG = "隧道删除失败";
    private static final String ERROR_TUNNEL_NOT_FOUND = "隧道不存在";
    private static final String ERROR_TUNNEL_NAME_EXISTS = "隧道名称已存在";
    private static final String ERROR_IN_NODE_NOT_FOUND = "入口节点不存在";
    private static final String ERROR_OUT_NODE_NOT_FOUND = "出口节点不存在";
    private static final String ERROR_OUT_NODE_REQUIRED = "出口节点不能为空";
    private static final String ERROR_OUT_PORT_REQUIRED = "出口端口不能为空";
    private static final String ERROR_SAME_NODE_NOT_ALLOWED = "隧道转发模式下，入口和出口不能是同一个节点";
    private static final String ERROR_IN_PORT_RANGE_INVALID = "入口端口开始不能大于结束端口";
    private static final String ERROR_OUT_PORT_RANGE_INVALID = "出口端口开始不能大于结束端口";
    private static final String ERROR_NO_AVAILABLE_TUNNELS = "暂无可用隧道";
    private static final String ERROR_IN_NODE_IP_INVALID = "入口节点IP地址无效，请先安装节点端的gost（支持IPv4和IPv6）";
    private static final String ERROR_OUT_NODE_IP_INVALID = "出口节点IP地址无效，请先安装节点端的gost（支持IPv4和IPv6）";
    
    /** 使用检查相关消息 */
    private static final String ERROR_FORWARDS_IN_USE = "该隧道还有 %d 个转发在使用，请先删除相关转发";
    private static final String ERROR_USER_PERMISSIONS_IN_USE = "该隧道还有 %d 个用户权限关联，请先取消用户权限分配";

    // ========== 依赖注入 ==========
    
    @Resource
    UserTunnelMapper userTunnelMapper;

    @Resource
    NodeService nodeService;
    
    @Resource
    ForwardService forwardService;
    
    @Resource
    UserTunnelService userTunnelService;

    // ========== 公共接口实现 ==========

    /**
     * 创建隧道
     * 支持端口转发和隧道转发两种模式
     * 
     * @param tunnelDto 隧道创建数据传输对象
     * @return 创建结果响应
     */
    @Override
    public R createTunnel(TunnelDto tunnelDto) {
        // 1. 验证隧道名称唯一性
        R nameValidationResult = validateTunnelNameUniqueness(tunnelDto.getName());
        if (nameValidationResult.getCode() != 0) {
            return nameValidationResult;
        }

        // 2. 验证入口节点和端口
        NodeValidationResult inNodeValidation = validateInNode(tunnelDto);
        if (inNodeValidation.isHasError()) {
            return R.err(inNodeValidation.getErrorMessage());
        }

        // 3. 构建隧道实体
        Tunnel tunnel = buildTunnelEntity(tunnelDto, inNodeValidation.getNode());

        // 4. 根据隧道类型设置出口参数
        R outNodeSetupResult = setupOutNodeParameters(tunnel, tunnelDto);
        if (outNodeSetupResult.getCode() != 0) {
            return outNodeSetupResult;
        }

        // 5. 设置默认属性并保存
        setDefaultTunnelProperties(tunnel);
        boolean result = this.save(tunnel);
        
        return result ? R.ok(SUCCESS_CREATE_MSG) : R.err(ERROR_CREATE_MSG);
    }

    /**
     * 获取所有隧道列表
     * 
     * @return 包含所有隧道的响应对象
     */
    @Override
    public R getAllTunnels() {
        List<Tunnel> tunnelList = this.list();
        return R.ok(tunnelList);
    }

    /**
     * 删除隧道
     * 删除前会检查是否有转发或用户权限在使用该隧道
     * 
     * @param id 隧道ID
     * @return 删除结果响应
     */
    @Override
    public R deleteTunnel(Long id) {
        // 1. 验证隧道是否存在
        if (!isTunnelExists(id)) {
            return R.err(ERROR_TUNNEL_NOT_FOUND);
        }

        // 2. 检查隧道使用情况
        R usageCheckResult = checkTunnelUsage(id);
        if (usageCheckResult.getCode() != 0) {
            return usageCheckResult;
        }

        // 3. 执行删除操作
        boolean result = this.removeById(id);
        return result ? R.ok(SUCCESS_DELETE_MSG) : R.err(ERROR_DELETE_MSG);
    }

    /**
     * 获取用户可用的隧道列表
     * 管理员可以看到所有启用的隧道，普通用户只能看到有权限的启用隧道
     * 
     * @return 用户可用隧道列表响应
     */
    @Override
    public R userTunnel() {
        UserInfo currentUser = getCurrentUserInfo();
        
        // 根据用户角色获取隧道列表
        List<Tunnel> tunnelEntities = getUserAccessibleTunnels(currentUser);
        
        // 转换为DTO并返回
        List<TunnelListDto> tunnelDtos = convertToTunnelListDtos(tunnelEntities);
        return R.ok(tunnelDtos);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取当前用户信息
     * 
     * @return 用户信息对象
     */
    private UserInfo getCurrentUserInfo() {
        Integer roleId = JwtUtil.getRoleIdFromToken();
        Integer userId = JwtUtil.getUserIdFromToken();
        return new UserInfo(userId, roleId);
    }

    /**
     * 验证隧道名称唯一性
     * 
     * @param tunnelName 隧道名称
     * @return 验证结果响应
     */
    private R validateTunnelNameUniqueness(String tunnelName) {
        Tunnel existTunnel = this.getOne(new QueryWrapper<Tunnel>().eq("name", tunnelName));
        if (existTunnel != null) {
            return R.err(ERROR_TUNNEL_NAME_EXISTS);
        }
        return R.ok();
    }

    /**
     * 验证入口节点和端口
     * 
     * @param tunnelDto 隧道创建DTO
     * @return 节点验证结果
     */
    private NodeValidationResult validateInNode(TunnelDto tunnelDto) {
        // 验证入口节点是否存在
        Node inNode = nodeService.getById(tunnelDto.getInNodeId());
        if (inNode == null) {
            return NodeValidationResult.error(ERROR_IN_NODE_NOT_FOUND);
        }

        // 验证入口节点IP地址
        if (!isValidNodeIp(inNode.getIp())) {
            return NodeValidationResult.error(ERROR_IN_NODE_IP_INVALID);
        }

        // 验证入口端口范围
        if (tunnelDto.getInPortSta() > tunnelDto.getInPortEnd()) {
            return NodeValidationResult.error(ERROR_IN_PORT_RANGE_INVALID);
        }

        return NodeValidationResult.success(inNode);
    }

    /**
     * 构建隧道实体对象
     * 
     * @param tunnelDto 隧道创建DTO
     * @param inNode 入口节点
     * @return 构建完成的隧道对象
     */
    private Tunnel buildTunnelEntity(TunnelDto tunnelDto, Node inNode) {
        Tunnel tunnel = new Tunnel();
        BeanUtils.copyProperties(tunnelDto, tunnel);
        
        // 设置入口节点信息
        tunnel.setInNodeId(tunnelDto.getInNodeId());
        tunnel.setInIp(inNode.getIp());
        
        // 设置流量计算类型
        tunnel.setFlow(tunnelDto.getFlow());
        
        return tunnel;
    }

    /**
     * 设置出口节点参数
     * 
     * @param tunnel 隧道对象
     * @param tunnelDto 隧道创建DTO
     * @return 设置结果响应
     */
    private R setupOutNodeParameters(Tunnel tunnel, TunnelDto tunnelDto) {
        if (tunnelDto.getType() == TUNNEL_TYPE_PORT_FORWARD) {
            // 端口转发：出口参数使用入口参数
            return setupPortForwardOutParameters(tunnel, tunnelDto);
        } else {
            // 隧道转发：需要验证出口参数
            return setupTunnelForwardOutParameters(tunnel, tunnelDto);
        }
    }

    /**
     * 设置端口转发的出口参数
     * 
     * @param tunnel 隧道对象
     * @param tunnelDto 隧道创建DTO
     * @return 设置结果响应
     */
    private R setupPortForwardOutParameters(Tunnel tunnel, TunnelDto tunnelDto) {
        tunnel.setOutNodeId(tunnelDto.getInNodeId());
        tunnel.setOutIp(tunnel.getInIp());
        tunnel.setOutIpSta(tunnelDto.getInPortSta());
        tunnel.setOutIpEnd(tunnelDto.getInPortEnd());
        return R.ok();
    }

    /**
     * 设置隧道转发的出口参数
     * 
     * @param tunnel 隧道对象
     * @param tunnelDto 隧道创建DTO
     * @return 设置结果响应
     */
    private R setupTunnelForwardOutParameters(Tunnel tunnel, TunnelDto tunnelDto) {
        // 验证出口节点不能为空
        if (tunnelDto.getOutNodeId() == null) {
            return R.err(ERROR_OUT_NODE_REQUIRED);
        }
        
        // 验证入口和出口不能是同一个节点
        if (tunnelDto.getInNodeId().equals(tunnelDto.getOutNodeId())) {
            return R.err(ERROR_SAME_NODE_NOT_ALLOWED);
        }
        
        // 验证出口节点是否存在
        Node outNode = nodeService.getById(tunnelDto.getOutNodeId());
        if (outNode == null) {
            return R.err(ERROR_OUT_NODE_NOT_FOUND);
        }
        
        // 验证出口节点IP地址
        if (!isValidNodeIp(outNode.getIp())) {
            return R.err(ERROR_OUT_NODE_IP_INVALID);
        }
        
        // 验证出口端口参数
        if (tunnelDto.getOutIpSta() == null || tunnelDto.getOutIpEnd() == null) {
            return R.err(ERROR_OUT_PORT_REQUIRED);
        }
        if (tunnelDto.getOutIpSta() > tunnelDto.getOutIpEnd()) {
            return R.err(ERROR_OUT_PORT_RANGE_INVALID);
        }
        
        // 设置出口参数
        tunnel.setOutNodeId(tunnelDto.getOutNodeId());
        tunnel.setOutIp(outNode.getIp());
        
        return R.ok();
    }

    /**
     * 设置隧道默认属性
     * 
     * @param tunnel 隧道对象
     */
    private void setDefaultTunnelProperties(Tunnel tunnel) {
        tunnel.setStatus(TUNNEL_STATUS_ACTIVE);
        long currentTime = System.currentTimeMillis();
        tunnel.setCreatedTime(currentTime);
        tunnel.setUpdatedTime(currentTime);
    }

    /**
     * 验证节点IP地址是否有效
     * 支持IPv4和IPv6格式
     * 
     * @param ip IP地址字符串
     * @return IP地址是否有效
     */
    private boolean isValidNodeIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        
        // 验证IPv4格式
        if (isValidIpv4(ip)) {
            return true;
        }
        
        // 验证IPv6格式
        return isValidIpv6(ip);
    }
    
    /**
     * 验证IPv4地址格式
     * 
     * @param ip IP地址字符串
     * @return 是否为有效的IPv4地址
     */
    private boolean isValidIpv4(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 验证IPv6地址格式
     * 
     * @param ip IP地址字符串
     * @return 是否为有效的IPv6地址
     */
    private boolean isValidIpv6(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        
        // 简化的IPv6验证，支持标准格式和压缩格式
        try {
            // 移除可能的端口号
            if (ip.startsWith("[") && ip.contains("]:")) {
                ip = ip.substring(1, ip.indexOf("]:"));
            }
            
            // 检查是否包含非法字符
            if (!ip.matches("^[0-9a-fA-F:]+$")) {
                return false;
            }
            
            // 处理双冒号压缩格式
            if (ip.contains("::")) {
                if (ip.indexOf("::") != ip.lastIndexOf("::")) {
                    // 不能有多个双冒号
                    return false;
                }
                // 展开双冒号
                String[] parts = ip.split("::");
                int leftParts = parts[0].isEmpty() ? 0 : parts[0].split(":").length;
                int rightParts = parts.length > 1 && !parts[1].isEmpty() ? parts[1].split(":").length : 0;
                int totalParts = leftParts + rightParts;
                if (totalParts >= 8) {
                    return false;
                }
            } else {
                // 标准格式，必须有8个部分
                String[] parts = ip.split(":");
                if (parts.length != 8) {
                    return false;
                }
                
                // 验证每个部分都是有效的十六进制数
                for (String part : parts) {
                    if (part.length() > 4 || part.isEmpty()) {
                        return false;
                    }
                    Integer.parseInt(part, 16); // 如果不是有效的十六进制会抛出异常
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查隧道是否存在
     * 
     * @param tunnelId 隧道ID
     * @return 隧道是否存在
     */
    private boolean isTunnelExists(Long tunnelId) {
        return this.getById(tunnelId) != null;
    }

    /**
     * 检查隧道使用情况
     * 
     * @param tunnelId 隧道ID
     * @return 检查结果响应
     */
    private R checkTunnelUsage(Long tunnelId) {
        // 检查转发使用情况
        R forwardCheckResult = checkForwardUsage(tunnelId);
        if (forwardCheckResult.getCode() != 0) {
            return forwardCheckResult;
        }

        // 检查用户权限使用情况
        return checkUserPermissionUsage(tunnelId);
    }

    /**
     * 检查转发使用情况
     * 
     * @param tunnelId 隧道ID
     * @return 检查结果响应
     */
    private R checkForwardUsage(Long tunnelId) {
        QueryWrapper<Forward> forwardQuery = new QueryWrapper<>();
        forwardQuery.eq("tunnel_id", tunnelId);
        long forwardCount = forwardService.count(forwardQuery);
        
        if (forwardCount > 0) {
            String errorMsg = String.format(ERROR_FORWARDS_IN_USE, forwardCount);
            return R.err(errorMsg);
        }
        
        return R.ok();
    }

    /**
     * 检查用户权限使用情况
     * 
     * @param tunnelId 隧道ID
     * @return 检查结果响应
     */
    private R checkUserPermissionUsage(Long tunnelId) {
        QueryWrapper<UserTunnel> userTunnelQuery = new QueryWrapper<>();
        userTunnelQuery.eq("tunnel_id", tunnelId);
        long userTunnelCount = userTunnelService.count(userTunnelQuery);
        
        if (userTunnelCount > 0) {
            String errorMsg = String.format(ERROR_USER_PERMISSIONS_IN_USE, userTunnelCount);
            return R.err(errorMsg);
        }
        
        return R.ok();
    }

    /**
     * 获取用户可访问的隧道列表
     * 
     * @param userInfo 用户信息
     * @return 隧道列表
     */
    private List<Tunnel> getUserAccessibleTunnels(UserInfo userInfo) {
        if (userInfo.getRoleId() == ADMIN_ROLE_ID) {
            // 管理员：获取所有启用状态的隧道
            return getActiveTunnels();
        } else {
            // 普通用户：根据权限获取启用状态的隧道
            return getUserAuthorizedTunnels(userInfo.getUserId());
        }
    }

    /**
     * 获取所有启用状态的隧道
     * 
     * @return 启用状态的隧道列表
     */
    private List<Tunnel> getActiveTunnels() {
        return this.list(new QueryWrapper<Tunnel>().eq("status", TUNNEL_STATUS_ACTIVE));
    }

    /**
     * 获取用户有权限的启用隧道
     * 
     * @param userId 用户ID
     * @return 用户有权限的隧道列表
     */
    private List<Tunnel> getUserAuthorizedTunnels(Integer userId) {
        List<UserTunnel> userTunnels = userTunnelMapper.selectList(
            new QueryWrapper<UserTunnel>().eq("user_id", userId)
        );
        
        if (userTunnels.isEmpty()) {
            return java.util.Collections.emptyList(); // 返回空列表
        }
        
        List<Integer> tunnelIds = userTunnels.stream()
                .map(UserTunnel::getTunnelId)
                .collect(Collectors.toList());
                
        return this.list(new QueryWrapper<Tunnel>()
                .in("id", tunnelIds)
                .eq("status", TUNNEL_STATUS_ACTIVE));
    }

    /**
     * 将隧道实体列表转换为DTO列表
     * 
     * @param tunnelEntities 隧道实体列表
     * @return 隧道DTO列表
     */
    private List<TunnelListDto> convertToTunnelListDtos(List<Tunnel> tunnelEntities) {
        return tunnelEntities.stream()
                .map(this::convertToTunnelListDto)
                .collect(Collectors.toList());
    }

    /**
     * 将Tunnel实体转换为TunnelListDto
     * 
     * @param tunnel 隧道实体
     * @return 隧道列表DTO
     */
    private TunnelListDto convertToTunnelListDto(Tunnel tunnel) {
        TunnelListDto dto = new TunnelListDto();
        dto.setId(tunnel.getId().intValue());
        dto.setName(tunnel.getName());
        return dto;
    }

    // ========== 内部数据类 ==========

    /**
     * 用户信息封装类
     */
    @Data
    private static class UserInfo {
        private final Integer userId;
        private final Integer roleId;
    }

    /**
     * 节点验证结果封装类
     */
    @Data
    private static class NodeValidationResult {
        private final boolean hasError;
        private final String errorMessage;
        private final Node node;

        private NodeValidationResult(boolean hasError, String errorMessage, Node node) {
            this.hasError = hasError;
            this.errorMessage = errorMessage;
            this.node = node;
        }

        public static NodeValidationResult success(Node node) {
            return new NodeValidationResult(false, null, node);
        }

        public static NodeValidationResult error(String errorMessage) {
            return new NodeValidationResult(true, errorMessage, null);
        }
    }
}
