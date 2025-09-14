- 特别说明：

- 原作者已删库，仅为fork纪念版。

---

## Nppass转发面板

本项目基于 [go-gost/gost](https://github.com/go-gost/gost) 和 [go-gost/x](https://github.com/go-gost/x) 两个开源库，实现了转发面板。

---
## 文档地址：
- [文档地址](https://brunuhville.github.io/flux-panel)
---

### Docker Compose部署
#### 快速部署

面板端：
```bash
curl -L https://raw.githubusercontent.com/planessr/Nppass/refs/heads/main/panel_install.sh -o panel_install.sh && chmod +x panel_install.sh && ./panel_install.sh

```

节点端：
```bash
curl -L https://raw.githubusercontent.com/planessr/Nppass/refs/heads/main/install.sh -o install.sh && chmod +x install.sh && ./install.sh

```

节点端安装说明（手动替换下载链接为下面的地址）：
```
curl -L https://raw.githubusercontent.com/planessr/Nppass/refs/heads/main/install.sh -o ./install.sh && chmod +x ./install.sh && ./install.sh -a 面板机IP:6365 -s 密钥KEY
```



#### 默认管理员账号

- **账号**: admin_user
- **密码**: admin_user

> ⚠️ 首次登录后请立即修改默认密码！