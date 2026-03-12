# 当前服务器部署手册

这份手册适用于当前项目的推荐上线方式：

- 本地开发和构建
- 上传构建产物到服务器
- 服务器只负责启动 Docker 容器，不再现场构建前端和后端

这样最适合“本地经常改代码，服务器反复更新”的场景，也能避开服务器 Node/Vite 构建慢、Docker Hub 拉取不稳定等问题。

## 第一步：确认本地部署相关文件

当前项目使用这些文件完成部署：

- `docker-compose.yml`
- `backend/Dockerfile`
- `nginx.conf`
- `.env`
- `package-release.sh`
- `server-deploy.sh`

其中：

- `package-release.sh` 负责在本地构建前端和后端，并生成发布包
- `server-deploy.sh` 负责在服务器上用最新发布包启动容器

## 第二步：首次准备服务器

以下步骤在新服务器上只需要做一次。

### 2.1 修复 DNS

如果服务器存在 Docker 拉镜像慢、域名无法解析的问题，先执行：

```bash
sudo rm -f /etc/resolv.conf
echo "nameserver 223.5.5.5" | sudo tee /etc/resolv.conf
echo "nameserver 223.6.6.6" | sudo tee -a /etc/resolv.conf
```

验证：

```bash
ping -c 2 registry-1.docker.io
```

只要能解析出 IP 就可以，`ping` 丢包不一定代表有问题。

### 2.2 安装 Docker 和 Docker Compose

如果系统已经装好可跳过。

Ubuntu / Debian 可执行：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker
```

验证：

```bash
docker --version
docker-compose --version
```

### 2.3 配置 Docker 镜像加速

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": ["https://pccrcjyp.mirror.aliyuncs.com"]
}
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 2.4 放行服务器端口

阿里云安全组至少需要：

- `22`：SSH
- `80`：HTTP
- `443`：可选，后续做 HTTPS 时使用

## 第三步：本地准备发布包

以后每次你在本地改完代码，都走这一套流程。

### 3.1 确认本地环境变量

本地项目根目录需要存在 `.env`。

### 3.2 在本地执行构建和打包

在项目根目录执行：

```bash
chmod +x package-release.sh
./package-release.sh
```

脚本会自动完成：

1. 前端 `npm run build`
2. 后端 `mvn clean package -DskipTests -q`
3. 生成发布包 `release/deploy-built.tar.gz`

## 第四步：把发布包上传到服务器

在你的本地电脑执行：

```bash
scp ./release/deploy-built.tar.gz root@你的服务器IP:~/
```

如果你还想同步单独修改过的 `.env`，可以额外上传：

```bash
scp ./.env root@你的服务器IP:~/
```

## 第五步：服务器首次解压项目

如果服务器上还没有项目目录，执行：

```bash
cd ~
mkdir -p talent-platform
cd talent-platform
tar -xzvf ../deploy-built.tar.gz
```

如果你单独上传了 `.env`，再执行：

```bash
mv ~/.env ~/talent-platform/.env
```

## 第六步：服务器后续更新项目

以后每次重新部署，不需要重新 `git clone`，只需要覆盖解压。

在服务器执行：

```bash
cd ~
rm -rf talent-platform
mkdir -p talent-platform
cd talent-platform
tar -xzvf ../deploy-built.tar.gz
```

如果 `.env` 没打进包里，就再放一次：

```bash
cp ~/.env ~/talent-platform/.env
```

## 第七步：在服务器上启动项目

在服务器项目目录执行：

```bash
cd ~/talent-platform
chmod +x server-deploy.sh
./server-deploy.sh
```

脚本会执行：

1. `docker-compose down --remove-orphans`
2. `docker-compose up -d --build`
3. `docker-compose ps`

## 第八步：检查服务状态

```bash
cd ~/talent-platform
docker-compose ps
```

理想状态应看到：

- `talent-mysql`
- `talent-backend`
- `talent-nginx`

都处于 `Up` 状态。

查看后端日志：

```bash
docker-compose logs -f backend
```

查看 MySQL 日志：

```bash
docker-compose logs -f mysql
```

## 第九步：访问项目

浏览器打开：

```text
http://你的服务器公网IP
```

默认管理员：

- 用户名：`admin`
- 密码：`admin123`

## 第十步：以后每次更新的最短流程

每次本地改完代码后，只需要做下面 4 步：

### 本地

```bash
cd /Users/xutengfei/毕业实习/project-demo
./package-release.sh
scp ./release/deploy-built.tar.gz root@你的服务器IP:~/
```

### 服务器

```bash
cd ~
rm -rf talent-platform
mkdir -p talent-platform
cd talent-platform
tar -xzvf ../deploy-built.tar.gz
./server-deploy.sh
```

## 常见问题

### 1. Docker 拉镜像失败

先检查：

```bash
cat /etc/resolv.conf
cat /etc/docker/daemon.json
```

确认 DNS 和镜像加速器仍然存在。

### 2. 后端提示 `UnknownHostException: mysql`

一般是 MySQL 容器没起来，先看：

```bash
docker-compose ps
docker-compose logs mysql
```

### 3. 服务器上不要再跑前端构建

不要在服务器上执行：

```bash
npm run build
```

这个项目的前端构建在小机器上容易慢、卡、占内存。推荐一直使用“本地构建 + 上传发布包”的方式。

### 4. 如果要保留数据库数据

下面命令会保留 MySQL 数据卷：

```bash
docker-compose down
```

下面命令会删掉 MySQL 数据卷并重建空库：

```bash
docker-compose down -v
```

只有你明确要重置数据库时，才使用 `-v`。
