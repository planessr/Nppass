package com.admin.config;


import com.admin.common.utils.JwtUtil;
import com.admin.common.utils.RedisUtil;
import com.admin.common.utils.WebSocketServer;
import com.admin.entity.Node;
import com.admin.service.NodeService;
import com.admin.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Configuration
@Slf4j
public class WebSocketInterceptor extends HttpSessionHandshakeInterceptor {
    @Resource
    NodeService nodeService;

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception ex) {

    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
        String secret = serverHttpRequest.getServletRequest().getParameter("secret");
        String type = serverHttpRequest.getServletRequest().getParameter("type");
        if (Objects.equals(type, "1")) {
            String client_ip = serverHttpRequest.getServletRequest().getParameter("client_ip");
            Node node = nodeService.getOne(new QueryWrapper<Node>().eq("secret", secret));
            if (node == null) return false;
            attributes.put("id", node.getId());
            node.setStatus(1);
            if (!Objects.equals(node.getIp(), client_ip)){
                node.setIp(client_ip);
            }
            nodeService.updateById(node);
        }else {
            boolean b = JwtUtil.validateToken(secret);
            if (!b) return false;
            attributes.put("id", JwtUtil.getUserIdFromToken(secret));
        }
        attributes.put("type", type);
        return true;
    }


}
