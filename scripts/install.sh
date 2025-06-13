#!/bin/bash

ARCH=$(uname -m)
if [[ "$ARCH" != "x86_64" ]]; then
  echo "❌ 不支持的架构: $ARCH，仅支持 x86_64。"
  exit 1
fi

# 固定gost下载地址
DOWNLOAD_URL="https://raw.githubusercontent.com/bqlpfy/forward-panel/refs/heads/main/gost"

while getopts "a:p:s:" opt; do
  case $opt in
    a) SERVER_ADDR="$OPTARG" ;;
    p) PORT="$OPTARG" ;;
    s) SECRET="$OPTARG" ;;
    *) echo "无效参数"; exit 1 ;;
  esac
done

if [[ -z "$SERVER_ADDR" || -z "$PORT" || -z "$SECRET" ]]; then
  echo "用法: $0 -a 服务器地址 -p 端口 -s 密钥"
  exit 1
fi


INSTALL_DIR="/etc/gost"
mkdir -p "$INSTALL_DIR"

# 检查gost服务是否存在并运行
if systemctl list-units --full -all | grep -Fq "gost.service"; then
  echo "检测到已存在的gost服务"
  if systemctl is-active --quiet gost; then
    echo "停止运行中的gost服务..."
    systemctl stop gost
  fi
  if systemctl is-enabled --quiet gost; then
    echo "禁用gost服务自启动..."
    systemctl disable gost
  fi
fi

# 删除旧文件
for FILE in gost config.json gost.json; do
  if [[ -f "$INSTALL_DIR/$FILE" ]]; then
    echo "删除已有文件: $INSTALL_DIR/$FILE"
    rm -f "$INSTALL_DIR/$FILE"
  fi
done


# 下载gost可执行文件
curl -L "$DOWNLOAD_URL" -o "$INSTALL_DIR/gost"
chmod +x "$INSTALL_DIR/gost"


cat > "$INSTALL_DIR/config.json" <<EOF
{
  "addr": "$SERVER_ADDR",
  "secret": "$SECRET"
}
EOF


cat > "$INSTALL_DIR/gost.json" <<EOF
{
  "services": [
    {
      "name": "web_api",
      "addr": ":$PORT",
      "handler": {
        "type": "api",
        "auth": {
          "username": "$SECRET",
          "password": "$SECRET"
        },
        "metadata": {
          "pathPrefix": "/api"
        }
      }
    }
  ]
}
EOF

cat > /etc/systemd/system/gost.service <<EOF
[Unit]
Description=Gost Proxy Service
After=network.target

[Service]
WorkingDirectory=$INSTALL_DIR
ExecStart=$INSTALL_DIR/gost
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF


systemctl daemon-reexec
systemctl daemon-reload
systemctl enable gost
systemctl start gost

# 检查服务状态
if systemctl is-active --quiet gost; then
  echo "✅ 安装完成，gost服务已启动并设置为开机启动。"
  echo "配置文件位于: $INSTALL_DIR"
  echo "服务状态: $(systemctl is-active gost)"
else
  echo "❌ gost服务启动失败，请检查日志:"
  echo "journalctl -u gost -f"
fi
