# 常见问题


## 面板管理脚本

安装、更新、卸载面板均使用同一条命令，运行后在菜单中选择对应操作：

```bash
curl -L https://raw.githubusercontent.com/BrunuhVille/flux-panel/refs/heads/main/panel_install.sh -o panel_install.sh && chmod +x panel_install.sh && ./panel_install.sh
```

## 节点管理脚本

节点的“更新”和“卸载”可直接使用以下命令，安装脚本请前往面板系统的“节点管理”页面复制获取：

```bash
curl -L https://raw.githubusercontent.com/BrunuhVille/flux-panel/refs/heads/main/install.sh -o ./install.sh && chmod +x ./install.sh && ./install.sh
```

## 日志查看

### 查看后端服务日志

当后端服务出现问题时，可以通过以下命令查看实时日志：

```bash
docker logs -f springboot-backend
```

按 `Ctrl+C` 退出日志查看

### 查看节点日志

当转发功能出现问题时，可以通过以下命令查看gost服务日志：

```bash
journalctl -u gost -f
```

按 `Ctrl+C` 退出日志查看

---

### 屏蔽 http/tls/socks 协议

⚠️注意：目前已知bug，开启屏蔽后，转发udp会失效，只能转发tcp

屏蔽指定协议的方法，只需要在入口执行。出口无需执行

1️⃣ 编辑配置文件

打开 /etc/gost/config.json，添加以下字段：
```
"http": 1,
"tls": 1,
"socks": 1
```

2️⃣ 重启服务
```
sudo systemctl restart gost
```

3️⃣ 检查状态（可选）
```
sudo systemctl status gost
```

- 说明：
  - 0 或不添加 → 不屏蔽对应协议
  - 1 → 屏蔽对应协议

完整的json文件
```
{
  "addr":"127.0.0.1：6365",
  "secret":"doraemon",
  "http":1,
  "tls":1,
  "socks":1
}
```
