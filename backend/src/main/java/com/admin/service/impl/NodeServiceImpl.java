package com.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.common.dto.NodeDto;
import com.admin.common.dto.NodeUpdateDto;
import com.admin.common.dto.PageDto;
import com.admin.common.lang.R;
import com.admin.entity.Node;
import com.admin.entity.Tunnel;
import com.admin.mapper.NodeMapper;
import com.admin.mapper.TunnelMapper;
import com.admin.service.NodeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * <p>
 * 节点服务实现类
 * 提供节点的增删改查功能，包括节点创建、更新、删除和查询操作
 * </p>
 *
 * @author QAQ
 * @since 2025-06-03
 */
@Service
public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements NodeService {

    // ========== 常量定义 ==========
    
    /** 节点默认状态：启用 */
    private static final int NODE_STATUS_ACTIVE = 0;
    
    /** 成功响应消息 */
    private static final String SUCCESS_CREATE_MSG = "节点创建成功";
    private static final String SUCCESS_UPDATE_MSG = "节点更新成功";
    private static final String SUCCESS_DELETE_MSG = "节点删除成功";
    
    /** 错误响应消息 */
    private static final String ERROR_CREATE_MSG = "节点创建失败";
    private static final String ERROR_UPDATE_MSG = "节点更新失败";
    private static final String ERROR_DELETE_MSG = "节点删除失败";
    private static final String ERROR_NODE_NOT_FOUND = "节点不存在";
    
    /** 隧道使用检查相关消息 */
    private static final String ERROR_IN_NODE_IN_USE = "该节点还有 %d 个隧道作为入口节点在使用，请先删除相关隧道";
    private static final String ERROR_OUT_NODE_IN_USE = "该节点还有 %d 个隧道作为出口节点在使用，请先删除相关隧道";

    // ========== 依赖注入 ==========
    
    @Resource
    private TunnelMapper tunnelMapper;

    @Value("${server-addr}")
    private String serverAddr;

    // ========== 公共接口实现 ==========

    /**
     * 创建新节点
     * 
     * @param nodeDto 节点创建数据传输对象
     * @return 创建结果响应
     */
    @Override
    public R createNode(NodeDto nodeDto) {
        Node node = buildNewNode(nodeDto);
        boolean result = this.save(node);
        return result ? R.ok(SUCCESS_CREATE_MSG) : R.err(ERROR_CREATE_MSG);
    }



    /**
     * 获取所有节点列表
     * 注意：返回结果中会隐藏节点密钥信息
     * 
     * @return 包含所有节点的响应对象
     */
    @Override
    public R getAllNodes() {
        List<Node> nodeList = this.list();
        hideNodeSecrets(nodeList);
        return R.ok(nodeList);
    }

    /**
     * 更新节点信息
     * 
     * @param nodeUpdateDto 节点更新数据传输对象
     * @return 更新结果响应
     */
    @Override
    public R updateNode(NodeUpdateDto nodeUpdateDto) {
        // 1. 验证节点是否存在
        if (!isNodeExists(nodeUpdateDto.getId())) {
            return R.err(ERROR_NODE_NOT_FOUND);
        }

        // 2. 构建更新对象并执行更新
        Node updateNode = buildUpdateNode(nodeUpdateDto);
        boolean result = this.updateById(updateNode);
        
        return result ? R.ok(SUCCESS_UPDATE_MSG) : R.err(ERROR_UPDATE_MSG);
    }

    /**
     * 删除节点
     * 删除前会检查是否有隧道正在使用该节点
     * 
     * @param id 节点ID
     * @return 删除结果响应
     */
    @Override
    public R deleteNode(Long id) {
        // 1. 验证节点是否存在
        if (!isNodeExists(id)) {
            return R.err(ERROR_NODE_NOT_FOUND);
        }

        // 2. 检查节点使用情况
        R usageCheckResult = checkNodeUsage(id);
        if (usageCheckResult.getCode() != 0) {
            return usageCheckResult;
        }

        // 3. 执行删除操作
        boolean result = this.removeById(id);
        return result ? R.ok(SUCCESS_DELETE_MSG) : R.err(ERROR_DELETE_MSG);
    }

    /**
     * 根据ID获取节点信息
     * 
     * @param id 节点ID
     * @return 节点对象
     * @throws RuntimeException 当节点不存在时抛出异常
     */
    @Override
    public Node getNodeById(Long id) {
        Node node = this.getById(id);
        if (node == null) {
            throw new RuntimeException(ERROR_NODE_NOT_FOUND);
        }
        return node;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建新节点对象
     * 
     * @param nodeDto 节点创建DTO
     * @return 构建完成的节点对象
     */
    private Node buildNewNode(NodeDto nodeDto) {
        Node node = new Node();
        BeanUtils.copyProperties(nodeDto, node);
        
        // 设置默认属性
        node.setSecret(IdUtil.simpleUUID());
        node.setStatus(NODE_STATUS_ACTIVE);
        
        // 设置时间戳
        long currentTime = System.currentTimeMillis();
        node.setCreatedTime(currentTime);
        node.setUpdatedTime(currentTime);
        
        return node;
    }

    /**
     * 构建节点更新对象
     * 
     * @param nodeUpdateDto 节点更新DTO
     * @return 构建完成的更新对象
     */
    private Node buildUpdateNode(NodeUpdateDto nodeUpdateDto) {
        Node node = new Node();
        node.setId(nodeUpdateDto.getId());
        node.setName(nodeUpdateDto.getName());
        node.setPort(nodeUpdateDto.getPort());
        node.setUpdatedTime(System.currentTimeMillis());
        return node;
    }

    /**
     * 隐藏节点列表中的密钥信息
     * 
     * @param nodeList 节点列表
     */
    private void hideNodeSecrets(List<Node> nodeList) {
        nodeList.forEach(node -> node.setSecret(null));
    }

    /**
     * 检查节点是否存在
     * 
     * @param nodeId 节点ID
     * @return 节点是否存在
     */
    private boolean isNodeExists(Long nodeId) {
        return this.getById(nodeId) != null;
    }

    /**
     * 检查节点使用情况
     * 验证是否有隧道正在使用该节点作为入口或出口节点
     * 
     * @param nodeId 节点ID
     * @return 检查结果响应
     */
    private R checkNodeUsage(Long nodeId) {
        // 检查入口节点使用情况
        R inNodeCheckResult = checkInNodeUsage(nodeId);
        if (inNodeCheckResult.getCode() != 0) {
            return inNodeCheckResult;
        }

        // 检查出口节点使用情况
        return checkOutNodeUsage(nodeId);
    }

    /**
     * 检查节点作为入口节点的使用情况
     * 
     * @param nodeId 节点ID
     * @return 检查结果响应
     */
    private R checkInNodeUsage(Long nodeId) {
        QueryWrapper<Tunnel> query = new QueryWrapper<>();
        query.eq("in_node_id", nodeId);
        
        long tunnelCount = tunnelMapper.selectCount(query);
        if (tunnelCount > 0) {
            String errorMsg = String.format(ERROR_IN_NODE_IN_USE, tunnelCount);
            return R.err(errorMsg);
        }
        
        return R.ok();
    }

    /**
     * 检查节点作为出口节点的使用情况
     * 
     * @param nodeId 节点ID
     * @return 检查结果响应
     */
    private R checkOutNodeUsage(Long nodeId) {
        QueryWrapper<Tunnel> query = new QueryWrapper<>();
        query.eq("out_node_id", nodeId);
        
        long tunnelCount = tunnelMapper.selectCount(query);
        if (tunnelCount > 0) {
            String errorMsg = String.format(ERROR_OUT_NODE_IN_USE, tunnelCount);
            return R.err(errorMsg);
        }
        
        return R.ok();
    }

    /**
     * 获取节点安装命令
     * 根据节点信息生成对应的安装命令
     * 
     * @param id 节点ID
     * @return 包含安装命令的响应对象
     */
    @Override
    public R getInstallCommand(Long id) {
        // 1. 验证节点是否存在
        Node node = this.getById(id);
        if (node == null) {
            return R.err(ERROR_NODE_NOT_FOUND);
        }

        // 2. 构建安装命令
        String installCommand = buildInstallCommand(node);
        
        return R.ok(installCommand);
    }

    /**
     * 构建节点安装命令
     * 
     * @param node 节点对象
     * @return 格式化的安装命令
     */
    private String buildInstallCommand(Node node) {
        StringBuilder command = new StringBuilder();
        
        // 第一部分：下载安装脚本  
        command.append("curl -L https://raw.githubusercontent.com/bqlpfy/forward-panel/refs/heads/main/deployment/install.sh")
               .append(" -o ./install.sh && chmod +x ./install.sh && ");
        
        // 第二部分：执行安装脚本（去掉-u参数）
        command.append("./install.sh")
               .append(" -a ").append(serverAddr)          // 服务器地址
               .append(" -p ").append(node.getPort())      // 节点端口
               .append(" -s ").append(node.getSecret());   // 节点密钥
        
        return command.toString();
    }
}
