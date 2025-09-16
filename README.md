## Nppass转发面板

高性能gost转发面板！

---
## 文档地址：
- [文档地址](https://brunuhville.github.io/flux-panel)
---

### Docker Compose部署
#### 快速部署

面板端管理脚本：
```bash
curl -L https://raw.githubusercontent.com/planessr/Nppass/refs/heads/main/panel_install.sh -o panel_install.sh && chmod +x panel_install.sh && ./panel_install.sh

```

节点端管理脚本：
```bash
curl -L https://raw.githubusercontent.com/planessr/Nppass/refs/heads/main/install.sh -o install.sh && chmod +x install.sh && ./install.sh

```

## 备份部署

- **使用面板端管理脚本「导出备份」**
- **将sql备份文件改名为「gost.sql」**
- **上传到服务器,使用面板端管理脚本,在gost.sql所在目录,执行安装!**


#### 访问Web

- **web**: http://your-server-ip:6366
- **账号**: admin_user
- **密码**: admin_user

> ⚠️ 首次登录后请立即修改默认密码！