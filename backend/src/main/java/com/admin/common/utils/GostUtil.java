package com.admin.common.utils;

import com.admin.common.dto.GostDto;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class GostUtil {

    private static final String API_BASE_URL = "/api/config/";
    private static final String LIMITERS_ENDPOINT = "limiters";
    private static final String SERVICES_ENDPOINT = "services";
    private static final String CHAINS_ENDPOINT = "chains";
    /**
     * 添加限流器配置
     * @param addr 服务器地址
     * @param name 限流器名称
     * @param speed 限速值（MB）
     * @param secret 认证密钥
     * @return 请求结果
     */
    public static GostDto AddLimiters(String addr, Long name, String speed, String secret) {
        JSONObject data = createLimiterData(name, speed);
        String url = buildUrl(addr, LIMITERS_ENDPOINT);
        return HttpUtils.post(url, data, secret);
    }

    /**
     * 更新限流器配置
     * @param addr 服务器地址
     * @param name 限流器名称
     * @param speed 限速值（MB）
     * @param secret 认证密钥
     * @return 请求结果
     */
    public static GostDto UpdateLimiters(String addr, Long name, String speed, String secret) {
        JSONObject data = createLimiterData(name, speed);
        String url = buildUrl(addr, LIMITERS_ENDPOINT + "/" + name);
        return HttpUtils.put(url, data, secret);
    }

    /**
     * 删除限流器配置
     * @param addr 服务器地址
     * @param name 限流器名称
     * @param secret 认证密钥
     * @return 请求结果
     */
    public static GostDto DeleteLimiters(String addr, Long name, String secret) {
        String url = buildUrl(addr, LIMITERS_ENDPOINT + "/" + name);
        return HttpUtils.delete(url, secret);
    }

    /**
     * 创建限流器数据
     */
    private static JSONObject createLimiterData(Long name, String speed) {
        JSONObject data = new JSONObject();
        data.put("name", name.toString());
        JSONArray limits = new JSONArray();
        limits.add("$ " + speed + "MB " + speed + "MB");
        data.put("limits", limits);
        return data;
    }


    /**
     * 添加服务配置（支持端口转发和隧道转发）
     * @param addr 服务器地址
     * @param name 服务名称
     * @param in_port 监听端口
     * @param limiter 限流器ID
     * @param remoteAddr 远程地址（端口转发时使用）
     * @param secret 认证密钥
     * @param fow_type 转发类型：1=端口转发，2=隧道转发
     * @return 请求结果
     */
    public static GostDto AddService(String addr, String name, Integer in_port, Integer limiter, String remoteAddr, String secret, Integer fow_type) {
        JSONArray services = new JSONArray();
        String[] protocols = {"tcp", "udp"};
        for (String protocol : protocols) {
            JSONObject service = createServiceConfig(name, in_port, limiter, remoteAddr, protocol, fow_type);
            services.add(service);
        }
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch");
        return HttpUtils.post(url, services, secret);
    }

    /**
     * 更新服务配置（批量更新TCP和UDP服务）
     * @param addr 服务器地址
     * @param name 服务名称
     * @param in_port 监听端口
     * @param limiter 限流器ID
     * @param remoteAddr 远程地址（端口转发时使用）
     * @param secret 认证密钥
     * @param fow_type 转发类型：1=端口转发，2=隧道转发
     * @return 请求结果
     */
    public static GostDto UpdateService(String addr, String name, Integer in_port, Integer limiter, String remoteAddr, String secret, Integer fow_type) {
        JSONArray services = new JSONArray();
        String[] protocols = {"tcp", "udp"};
        for (String protocol : protocols) {
            JSONObject service = createServiceConfig(name, in_port, limiter, remoteAddr, protocol, fow_type);
            services.add(service);
        }
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch");
        return HttpUtils.put(url, services, secret);
    }

    /**
     * 删除服务配置（批量删除TCP和UDP服务）
     * @param addr 服务器地址
     * @param name 服务名称
     * @param secret 认证密钥
     * @return 请求结果
     */
    public static GostDto DeleteService(String addr, String name, String secret) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch");
        return HttpUtils.delete(url, data, secret);
    }


    public static GostDto PauseService(String addr, String name, String secret) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch/pause");
        return HttpUtils.post(url, data, secret);
    }

    public static GostDto ResumeService(String addr, String name, String secret) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch/resume");
        return HttpUtils.post(url, data, secret);
    }

    public static GostDto PauseRemoteService(String addr, String name, String secret) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tls");
        data.put("services", services);
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch/pause");
        return HttpUtils.post(url, data, secret);
    }

    public static GostDto ResumeRemoteService(String addr, String name, String secret) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tls");
        data.put("services", services);
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/batch/resume");
        return HttpUtils.post(url, data, secret);
    }

    public static GostDto AddChains(String addr, String name,  String remoteAddr, String secret) {
        JSONObject dialer = new JSONObject();
        dialer.put("type", "tls");

        JSONObject connector = new JSONObject();
        connector.put("type", "relay");

        JSONObject node = new JSONObject();
        node.put("name", "node-" + name);
        node.put("addr", remoteAddr);
        node.put("connector", connector);
        node.put("dialer", dialer);

        JSONArray nodes = new JSONArray();
        nodes.add(node);

        JSONObject hop = new JSONObject();
        hop.put("name", "hop-" + name);
        hop.put("nodes", nodes);

        JSONArray hops = new JSONArray();
        hops.add(hop);

        JSONObject data = new JSONObject();
        data.put("name", name + "_chains");
        data.put("hops", hops);

        String url = buildUrl(addr, CHAINS_ENDPOINT);
        return HttpUtils.post(url, data, secret);
    }

    public static GostDto UpdateChains(String addr, String name,  String remoteAddr, String secret) {
        JSONObject dialer = new JSONObject();
        dialer.put("type", "tls");

        JSONObject connector = new JSONObject();
        connector.put("type", "relay");

        JSONObject node = new JSONObject();
        node.put("name", "node-" + name);
        node.put("addr", remoteAddr);
        node.put("connector", connector);
        node.put("dialer", dialer);

        JSONArray nodes = new JSONArray();
        nodes.add(node);

        JSONObject hop = new JSONObject();
        hop.put("name", "hop-" + name);
        hop.put("nodes", nodes);

        JSONArray hops = new JSONArray();
        hops.add(hop);

        JSONObject data = new JSONObject();
        data.put("name", name + "_chains");
        data.put("hops", hops);

        String url = buildUrl(addr, CHAINS_ENDPOINT + "/" + name + "_chains");
        return HttpUtils.put(url, data, secret);
    }


    public static GostDto DeleteChains(String addr, String name, String secret) {
        String url = buildUrl(addr, CHAINS_ENDPOINT + "/" + name + "_chains");
        return HttpUtils.delete(url, secret);
    }

    public static GostDto AddRemoteService(String addr, String name,  Integer out_port, String remoteAddr, String secret) {
        JSONObject data = new JSONObject();
        data.put("name", name + "_tls");
        data.put("addr", ":"+out_port);
        JSONObject handler = new JSONObject();
        handler.put("type", "relay");
        data.put("handler", handler);
        JSONObject listener = new JSONObject();
        listener.put("type", "tls");
        data.put("listener", listener);
        JSONObject forwarder = new JSONObject();
        JSONArray nodes = new JSONArray();
        JSONObject node = new JSONObject();
        node.put("name", name + "_node");
        node.put("addr", remoteAddr);
        nodes.add(node);
        forwarder.put("nodes", nodes);
        data.put("forwarder", forwarder);
        String url = buildUrl(addr, SERVICES_ENDPOINT);
        return HttpUtils.post(url, data, secret);
    }


    public static GostDto UpdateRemoteService(String addr, String name,  Integer out_port, String remoteAddr, String secret) {
        JSONObject data = new JSONObject();
        data.put("name", name + "_tls");
        data.put("addr", ":"+out_port);
        JSONObject handler = new JSONObject();
        handler.put("type", "relay");
        data.put("handler", handler);
        JSONObject listener = new JSONObject();
        listener.put("type", "tls");
        data.put("listener", listener);
        JSONObject forwarder = new JSONObject();
        JSONArray nodes = new JSONArray();
        JSONObject node = new JSONObject();
        node.put("name", name + "_node");
        node.put("addr", remoteAddr);
        nodes.add(node);
        forwarder.put("nodes", nodes);
        data.put("forwarder", forwarder);
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/" +  name + "_tls");
        return HttpUtils.put(url, data, secret);
    }



    public static GostDto DeleteRemoteService(String addr, String name, String secret) {
        String url = buildUrl(addr, SERVICES_ENDPOINT + "/" +  name + "_tls");
        return HttpUtils.delete(url, secret);
    }


    
    /**
     * 创建单个服务配置
     */
    private static JSONObject createServiceConfig(String name, Integer in_port, Integer limiter, String remoteAddr, String protocol, Integer fow_type) {
        JSONObject service = new JSONObject();
        service.put("name", name + "_" + protocol);
        service.put("addr", ":" +in_port);
        
        // 添加限流器配置
        if (limiter != null) {
            service.put("limiter", limiter.toString());
        }
        
        // 配置处理器
        JSONObject handler = createHandler(protocol, name, fow_type);
        service.put("handler", handler);
        
        // 配置监听器
        JSONObject listener = createListener(protocol);
        service.put("listener", listener);
        
        // 端口转发需要配置转发器
        if (isPortForwarding(fow_type)) {
            JSONObject forwarder = createForwarder(protocol, remoteAddr);
            service.put("forwarder", forwarder);
        }
        
        return service;
    }
    
    /**
     * 创建处理器配置
     */
    private static JSONObject createHandler(String protocol, String name, Integer fow_type) {
        JSONObject handler = new JSONObject();
        handler.put("type", protocol);
        
        // 隧道转发需要添加链配置
        if (isTunnelForwarding(fow_type)) {
            handler.put("chain", name + "_chains");
        }
        
        return handler;
    }
    
    /**
     * 创建监听器配置
     */
    private static JSONObject createListener(String protocol) {
        JSONObject listener = new JSONObject();
        listener.put("type", protocol);
        return listener;
    }
    
    /**
     * 创建转发器配置
     */
    private static JSONObject createForwarder(String protocol, String remoteAddr) {
        JSONObject forwarder = new JSONObject();
        JSONArray nodes = new JSONArray();
        JSONObject node = new JSONObject();
        node.put("name", protocol);
        node.put("addr", remoteAddr);
        nodes.add(node);
        forwarder.put("nodes", nodes);
        return forwarder;
    }
    
    /**
     * 判断是否为端口转发
     */
    private static boolean isPortForwarding(Integer fow_type) {
        return fow_type != null && fow_type == 1;
    }
    
    /**
     * 判断是否为隧道转发
     */
    private static boolean isTunnelForwarding(Integer fow_type) {
        return fow_type != null && fow_type != 1;
    }


    /**
     * 构建API URL
     */
    private static String buildUrl(String addr, String endpoint) {
        return "http://" + addr + API_BASE_URL + endpoint;
    }
}
