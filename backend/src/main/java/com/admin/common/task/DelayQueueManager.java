package com.admin.common.task;


import com.admin.mapper.UserMapper;
import com.admin.mapper.ForwardMapper;
import com.admin.mapper.UserTunnelMapper;
import com.admin.mapper.TunnelMapper;
import com.admin.mapper.NodeMapper;
import com.admin.service.UserService;
import com.admin.service.UserTunnelService;
import com.admin.entity.Forward;
import com.admin.entity.Tunnel;
import com.admin.entity.Node;
import com.admin.entity.UserTunnel;
import com.admin.common.dto.GostDto;
import com.admin.common.utils.GostUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class DelayQueueManager implements CommandLineRunner {

    private final DelayQueue<DelayTask> delayQueue = new DelayQueue<>();

    @Autowired
    UserMapper userMapper;

    @Resource
    ForwardMapper forwardMapper;

    @Resource
    UserTunnelMapper userTunnelMapper;

    @Resource
    TunnelMapper tunnelMapper;

    @Resource
    NodeMapper nodeMapper;

    /**
     * 加入到延时队列中
     *
     * @param task
     */
    public void put(DelayTask task) {
        log.info("加入延时任务：{}", task);
        delayQueue.put(task);
    }

    /**
     * 取消延时任务
     *
     * @param task
     * @return
     */
    public boolean remove(DelayTask task) {
        log.info("取消延时任务：{}", task);
        return delayQueue.remove(task);
    }

    /**
     * 取消延时任务
     *
     * @param taskid
     * @return
     */
    public boolean remove(String taskid) {
        return remove(new DelayTask(new TaskBase(taskid), 0));
    }

    /**
     * 取消延时任务
     *
     * @param taskid
     * @return
     */
    public boolean remove_a(String taskid) {
        return remove(new DelayTask(new TaskBase(taskid), 0));
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("初始化延时队列");
        Executors.newSingleThreadExecutor().execute(new Thread(this::excuteThread));
        
        // 初始化用户账号到期延时任务
        initUserExpirationTasks();
        
        // 初始化用户隧道到期延时任务
        initUserTunnelExpirationTasks();
    }

    /**
     * 延时任务执行线程
     */
    private void excuteThread() {
        while (true) {
            try {
                DelayTask task = delayQueue.take();
                processTask(task);
            } catch (InterruptedException e) {
                break;
            }
        }
    }


    /**
     * 内部执行延时任务
     *
     * @param task
     */
    private void processTask(DelayTask task) {
        log.info("执行延时任务：{}", task.getData().toString());
        TaskBase data = task.getData();
        switch (data.getType()){
            case "1": // 账号到期延迟任务
                handleUserExpiration(data.getData());
                break;
            case "2": // 隧道到期延迟任务
                handleUserTunnelExpiration(data.getData());
                break;
            default:
                log.error("未知延时任务类型：{}", data.getType());
                break;
        }
    }

    /**
     * 处理用户账号到期
     * 
     * @param userId 用户ID
     */
    private void handleUserExpiration(String userId) {
        try {
            log.info("处理用户账号到期，用户ID：{}", userId);
            Long userIdLong = Long.parseLong(userId);
            
            // 获取用户信息
            com.admin.entity.User user = userMapper.selectById(userIdLong);
            if (user == null) {
                log.warn("用户不存在，用户ID：{}", userId);
                return;
            }
            
            // 检查用户是否确实已过期
            if (user.getExpTime() != null && user.getExpTime() > System.currentTimeMillis()) {
                log.info("用户未过期，无需处理，用户ID：{}，过期时间：{}", userId, user.getExpTime());
                return;
            }
            
            // 禁用用户账号
            user.setStatus(0); // 设置为禁用状态
            user.setUpdatedTime(System.currentTimeMillis());

            int i = userMapper.updateById(user);
            if (i != 0) {
                log.info("用户账号已禁用，用户ID：{}", userId);
                
                // 清理用户相关的活跃连接和服务
                cleanupUserServices(userIdLong);
            } else {
                log.error("禁用用户账号失败，用户ID：{}", userId);
            }
            
        } catch (Exception e) {
            log.error("处理用户账号到期异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 清理用户相关服务
     * 
     * @param userId 用户ID
     */
    private void cleanupUserServices(Long userId) {
        try {
            log.info("暂停用户相关转发服务，用户ID：{}", userId);
            
            // 获取用户的所有转发
            QueryWrapper<Forward> forwardQuery = new QueryWrapper<>();
            forwardQuery.eq("user_id", userId);
            List<Forward> userForwards = forwardMapper.selectList(forwardQuery);
            
            log.info("找到用户转发数量：{}，用户ID：{}", userForwards.size(), userId);
            
            for (Forward forward : userForwards) {
                try {
                    // 暂停转发服务
                    pauseForwardService(forward, userId);
                } catch (Exception e) {
                    log.error("暂停转发服务失败，转发ID：{}，用户ID：{}，错误：{}", forward.getId(), userId, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("清理用户服务失败，用户ID：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 暂停转发服务
     * 
     * @param forward 转发对象
     * @param userId 用户ID
     */
    private void pauseForwardService(Forward forward, Long userId) {
        try {
            Tunnel tunnel = tunnelMapper.selectById(forward.getTunnelId());
            if (tunnel == null) {
                log.warn("隧道不存在，跳过暂停，转发ID：{}，隧道ID：{}", forward.getId(), forward.getTunnelId());
                return;
            }

            Node inNode = nodeMapper.selectById(tunnel.getInNodeId());
            if (inNode == null) {
                log.warn("入口节点不存在，跳过暂停，转发ID：{}，节点ID：{}", forward.getId(), tunnel.getInNodeId());
                return;
            }

            // 获取用户隧道关系
            UserTunnel userTunnel = getUserTunnelRelation(userId, tunnel.getId());
            if (userTunnel == null) {
                log.warn("用户隧道关系不存在，跳过暂停，用户ID：{}，隧道ID：{}", userId, tunnel.getId());
                return;
            }

            String serviceName = buildServiceName(forward.getId(), userId, userTunnel.getId());
            String nodeAddress = buildNodeAddress(inNode);

            // 暂停主服务
            GostDto result = GostUtil.PauseService(nodeAddress, serviceName, inNode.getSecret());
            
            // 隧道转发需要同时暂停远端服务
            if (tunnel.getType() == 2) { // TUNNEL_TYPE_TUNNEL_FORWARD
                Node outNode = nodeMapper.selectById(tunnel.getOutNodeId());
                if (outNode != null) {
                    String outNodeAddress = buildNodeAddress(outNode);
                    GostDto remoteResult = GostUtil.PauseRemoteService(outNodeAddress, serviceName, outNode.getSecret());
                    if (!"OK".equals(remoteResult.getMsg())) {
                        log.warn("暂停远端服务失败，转发ID：{}，用户ID：{}，服务名：{}，结果：{}", 
                                forward.getId(), userId, serviceName, remoteResult.getMsg());
                    }
                }
            }
            
            if ( "OK".equals(result.getMsg())) {
                forward.setStatus(0);
                forwardMapper.updateById(forward);
                log.info("成功暂停转发服务，转发ID：{}，用户ID：{}，服务名：{}", forward.getId(), userId, serviceName);
            } else {
                log.warn("暂停转发服务失败，转发ID：{}，用户ID：{}，服务名：{}，结果：{}", 
                        forward.getId(), userId, serviceName, result.getMsg());
            }
            
        } catch (Exception e) {
            log.error("暂停转发服务异常，转发ID：{}，用户ID：{}，错误：{}", forward.getId(), userId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户隧道关系
     * 
     * @param userId 用户ID
     * @param tunnelId 隧道ID
     * @return 用户隧道关系对象
     */
    private UserTunnel getUserTunnelRelation(Long userId, Long tunnelId) {
        try {
            QueryWrapper<UserTunnel> query = new QueryWrapper<>();
            query.eq("user_id", userId).eq("tunnel_id", tunnelId);
            return userTunnelMapper.selectOne(query);
        } catch (Exception e) {
            log.error("获取用户隧道关系失败，用户ID：{}，隧道ID：{}，错误：{}", userId, tunnelId, e.getMessage());
            return null;
        }
    }

    /**
     * 构建服务名称
     * 
     * @param forwardId 转发ID
     * @param userId 用户ID
     * @param userTunnelId 用户隧道ID
     * @return 服务名称
     */
    private String buildServiceName(Long forwardId, Long userId, Integer userTunnelId) {
        return forwardId + "_" + userId + "_" + userTunnelId;
    }

    /**
     * 构建节点地址
     * 
     * @param node 节点对象
     * @return 节点地址字符串
     */
    private String buildNodeAddress(Node node) {
        return node.getIp() + ":" + node.getPort();
    }
    
    /**
     * 初始化用户账号到期延时任务
     * 查询所有非管理员的正常用户，为有到期时间且未过期的用户创建延时任务
     */
    private void initUserExpirationTasks() {
        try {
            log.info("开始初始化用户账号到期延时任务");
            QueryWrapper<com.admin.entity.User> userQuery = new QueryWrapper<>();
            userQuery.ne("role_id", 0) // 排除管理员用户
                    .eq("status", 1) // 只查询启用状态的用户
                    .isNotNull("exp_time") // 只查询有到期时间的用户
                    .orderBy(true, true, "exp_time"); // 按到期时间排序
            
            List<com.admin.entity.User> users = userMapper.selectList(userQuery);
            


            for (com.admin.entity.User user : users) {
                scheduleUserExpirationTask(user);
            }
        } catch (Exception e) {
            log.error("初始化用户账号到期延时任务失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 安排用户到期延时任务
     * 
     * @param user 用户对象
     */
    private void scheduleUserExpirationTask(com.admin.entity.User user) {
        try {
            if (user.getExpTime() != null && user.getExpTime() > System.currentTimeMillis()) {
                // 创建延时任务
                TaskBase taskBase = new TaskBase(user.getId().toString());
                taskBase.setType("1"); // 账号到期延迟任务
                
                long delayTime = user.getExpTime() - System.currentTimeMillis();
                DelayTask delayTask = new DelayTask(taskBase, delayTime);
                
                put(delayTask);
                
                log.debug("已添加用户到期延时任务，用户ID：{}，到期时间：{}，剩余时间：{}ms", 
                        user.getId(), user.getExpTime(), delayTime);
            }
        } catch (Exception e) {
            log.error("添加用户到期延时任务失败，用户ID：{}，错误：{}", user.getId(), e.getMessage(), e);
        }
    }

    /**
     * 处理用户隧道到期
     * 
     * @param userTunnelId 用户隧道ID
     */
    private void handleUserTunnelExpiration(String userTunnelId) {
        try {
            log.info("处理用户隧道到期，用户隧道ID：{}", userTunnelId);
            Integer userTunnelIdInt = Integer.parseInt(userTunnelId);
            
            // 获取用户隧道信息
            UserTunnel userTunnel = userTunnelMapper.selectById(userTunnelIdInt);
            if (userTunnel == null) {
                log.warn("用户隧道不存在，用户隧道ID：{}", userTunnelId);
                return;
            }
            
            // 检查用户隧道是否确实已过期
            if (userTunnel.getExpTime() != null && userTunnel.getExpTime() > System.currentTimeMillis()) {
                log.info("用户隧道未过期，无需处理，用户隧道ID：{}，过期时间：{}", userTunnelId, userTunnel.getExpTime());
                return;
            }
            
                         log.info("用户隧道已过期，开始处理，用户隧道ID：{}，用户ID：{}，隧道ID：{}", 
                     userTunnelId, userTunnel.getUserId(), userTunnel.getTunnelId());
             
             // 暂停该用户在该隧道上的所有转发服务
             cleanupUserTunnelServices(userTunnel);
             
             // 禁用过期的用户隧道权限（设置status为0）
             userTunnel.setStatus(0);
             int updateResult = userTunnelMapper.updateById(userTunnel);
             if (updateResult > 0) {
                 log.info("已禁用过期的用户隧道权限，用户隧道ID：{}", userTunnelId);
             } else {
                 log.error("禁用过期用户隧道权限失败，用户隧道ID：{}", userTunnelId);
             }
            
        } catch (Exception e) {
            log.error("处理用户隧道到期异常，用户隧道ID：{}，错误：{}", userTunnelId, e.getMessage(), e);
        }
    }
    
    /**
     * 清理用户隧道相关服务
     * 
     * @param userTunnel 用户隧道对象
     */
    private void cleanupUserTunnelServices(UserTunnel userTunnel) {
        try {
            log.info("暂停用户隧道相关转发服务，用户ID：{}，隧道ID：{}", userTunnel.getUserId(), userTunnel.getTunnelId());
            
            // 获取该用户在该隧道上的所有转发
            QueryWrapper<Forward> forwardQuery = new QueryWrapper<>();
            forwardQuery.eq("user_id", userTunnel.getUserId())
                       .eq("tunnel_id", userTunnel.getTunnelId());
            List<Forward> userTunnelForwards = forwardMapper.selectList(forwardQuery);
            
            log.info("找到用户隧道转发数量：{}，用户ID：{}，隧道ID：{}", 
                    userTunnelForwards.size(), userTunnel.getUserId(), userTunnel.getTunnelId());
            
            for (Forward forward : userTunnelForwards) {
                try {
                    // 暂停转发服务
                    pauseUserTunnelForwardService(forward, userTunnel);
                } catch (Exception e) {
                    log.error("暂停用户隧道转发服务失败，转发ID：{}，用户ID：{}，隧道ID：{}，错误：{}", 
                            forward.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("清理用户隧道服务失败，用户ID：{}，隧道ID：{}，错误：{}", 
                    userTunnel.getUserId(), userTunnel.getTunnelId(), e.getMessage(), e);
        }
    }
    
    /**
     * 暂停用户隧道转发服务
     * 
     * @param forward 转发对象
     * @param userTunnel 用户隧道对象
     */
    private void pauseUserTunnelForwardService(Forward forward, UserTunnel userTunnel) {
        try {
            Tunnel tunnel = tunnelMapper.selectById(forward.getTunnelId());
            if (tunnel == null) {
                log.warn("隧道不存在，跳过暂停，转发ID：{}，隧道ID：{}", forward.getId(), forward.getTunnelId());
                return;
            }

            Node inNode = nodeMapper.selectById(tunnel.getInNodeId());
            if (inNode == null) {
                log.warn("入口节点不存在，跳过暂停，转发ID：{}，节点ID：{}", forward.getId(), tunnel.getInNodeId());
                return;
            }

            String serviceName = buildServiceName(forward.getId(), Long.valueOf(userTunnel.getUserId()), userTunnel.getId());
            String nodeAddress = buildNodeAddress(inNode);

            // 暂停服务
            GostDto result = GostUtil.PauseService(nodeAddress, serviceName, inNode.getSecret());
            
            // 隧道转发需要同时暂停远端服务
            if (tunnel.getType() == 2) { // TUNNEL_TYPE_TUNNEL_FORWARD
                Node outNode = nodeMapper.selectById(tunnel.getOutNodeId());
                if (outNode != null) {
                    String outNodeAddress = buildNodeAddress(outNode);
                    GostDto remoteResult = GostUtil.PauseRemoteService(outNodeAddress, serviceName, outNode.getSecret());
                    if (!"OK".equals(remoteResult.getMsg())) {
                        log.warn("暂停远端服务失败，转发ID：{}，用户ID：{}，隧道ID：{}，服务名：{}，结果：{}", 
                                forward.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(), serviceName, remoteResult.getMsg());
                    }
                }
            }
            
            if ("OK".equals(result.getMsg())) {
                forward.setStatus(0);
                forwardMapper.updateById(forward);
                log.info("成功暂停用户隧道转发服务，转发ID：{}，用户ID：{}，隧道ID：{}，服务名：{}", 
                        forward.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(), serviceName);
            } else {
                log.warn("暂停用户隧道转发服务失败，转发ID：{}，用户ID：{}，隧道ID：{}，服务名：{}，结果：{}", 
                        forward.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(), serviceName, result.getMsg());
            }
            
        } catch (Exception e) {
            log.error("暂停用户隧道转发服务异常，转发ID：{}，用户ID：{}，隧道ID：{}，错误：{}", 
                    forward.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(), e.getMessage(), e);
        }
    }
    
    /**
     * 初始化用户隧道到期延时任务
     * 查询所有有到期时间且未过期的用户隧道权限，为其创建延时任务
     */
    private void initUserTunnelExpirationTasks() {
        try {
            log.info("开始初始化用户隧道到期延时任务");
                         QueryWrapper<UserTunnel> userTunnelQuery = new QueryWrapper<>();
             userTunnelQuery.eq("status", 1); // 按到期时间排序
            
            List<UserTunnel> userTunnels = userTunnelMapper.selectList(userTunnelQuery);
            
            int taskCount = 0;

            for (UserTunnel userTunnel : userTunnels) {
                scheduleUserTunnelExpirationTask(userTunnel);
            }
            
            log.info("完成初始化用户隧道到期延时任务，总计查询：{}，添加任务：{}", userTunnels.size(), taskCount);
        } catch (Exception e) {
            log.error("初始化用户隧道到期延时任务失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 安排用户隧道到期延时任务
     * 
     * @param userTunnel 用户隧道对象
     */
    private void scheduleUserTunnelExpirationTask(UserTunnel userTunnel) {
        // 创建延时任务
        TaskBase taskBase = new TaskBase(userTunnel.getId().toString());
        taskBase.setType("2"); // 隧道到期延迟任务

        long delayTime = userTunnel.getExpTime() - System.currentTimeMillis();
        DelayTask delayTask = new DelayTask(taskBase, delayTime);

        put(delayTask);

        log.debug("已添加用户隧道到期延时任务，用户隧道ID：{}，用户ID：{}，隧道ID：{}，到期时间：{}，剩余时间：{}ms",
                userTunnel.getId(), userTunnel.getUserId(), userTunnel.getTunnelId(),
                userTunnel.getExpTime(), delayTime);
    }
    
    /**
     * 添加用户隧道到期延时任务（公共方法，供其他服务调用）
     * 
     * @param userTunnel 用户隧道对象
     */
    public void addUserTunnelExpirationTask(UserTunnel userTunnel) {
        scheduleUserTunnelExpirationTask(userTunnel);
    }
    
    /**
     * 移除用户隧道到期延时任务（公共方法，供其他服务调用）
     * 
     * @param userTunnelId 用户隧道ID
     */
    public void removeUserTunnelExpirationTask(Integer userTunnelId) {
        String taskId = userTunnelId.toString();
        boolean removed = remove(taskId);
        if (removed) {
            log.info("已移除用户隧道到期延时任务，用户隧道ID：{}", userTunnelId);
        } else {
            log.debug("未找到需要移除的用户隧道到期延时任务，用户隧道ID：{}", userTunnelId);
        }
    }
}