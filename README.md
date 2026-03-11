# 京州市软件产业人才公共服务平台

## 项目概述

基于 Spring Boot + Vue 3 的全栈人才服务平台，集成 AI 大模型智能匹配、模拟区块链存证、阿里云 OSS 文件存储、ECharts 数据大屏等核心功能。

## 技术架构

**后端**: Spring Boot 3.2 + Spring Security + JWT + Spring Data JPA + Spring AOP + WebClient
**前端**: Vue 3 + Vite + Element Plus + Pinia + Vue Router + ECharts
**数据库**: MySQL 8.0
**AI**: DeepSeek API (OpenAI兼容接口)
**存储**: 阿里云 OSS
**部署**: Docker Compose + Nginx

## 功能模块

### 核心业务
- 人才档案管理 (CRUD + 头像上传)
- 企业信息管理 + 审核流程 (审核通过后才能发岗)
- 岗位管理 (发布/搜索/申请/状态流转)
- 新闻资讯管理

### AI 智能模块
- 人才-岗位智能匹配 (DeepSeek 大模型评分+分析)
- AI 课程推荐 (基于个人技能和已完成课程)
- AI 职业能力评估 (六维雷达图 + 综合评分 + 报告 + 建议)
- 评估历史记录 + PDF 导出

### 终生学习平台
- 视频课程播放
- 学习进度跟踪 + 学习时长统计
- 课程完成后颁发证书
- 证书区块链上链存证

### 模拟区块链
- SHA-256 哈希链 (Java实现)
- 创世区块自动生成
- 学习记录不可篡改存证
- 链完整性验证
- 区块链浏览器 (可视化)

### 数据大屏
- 全屏科技风数据大屏
- 核心指标实时展示
- 人才城市分布图
- 技能热度排行
- 月度趋势分析

### 后台管理
- 用户管理 / 资讯管理 / 课程管理
- 企业管理 (审核 + 公开/隐藏)
- 人才管理 (公开/隐藏 + 精选展示)
- 系统监控 (API调用统计/响应时间/错误率)
- 区块链管理

### 文件上传
- 阿里云 OSS 对象存储
- 支持头像/课程封面/视频/证书等

## 本地开发

### 环境要求
- JDK 17+, Maven 3.6+, Node.js 18+, MySQL 8.0 (或Docker)

### 启动 MySQL
```bash
docker run -d --name mysql8 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=talent_platform mysql:8.0
```

### 配置环境变量
```bash
export DEEPSEEK_API_KEY=your_key
export OSS_ACCESS_KEY_ID=your_id
export OSS_ACCESS_KEY_SECRET=your_secret
```

### 启动后端
```bash
cd backend && mvn spring-boot:run
```

### 启动前端
```bash
cd frontend && npm install && npm run dev
```

## Docker 一键部署

```bash
# 1. 创建 .env 文件
cp .env.example .env
vim .env  # 填入 DeepSeek API Key 和 OSS 密钥

# 2. 一键部署
chmod +x deploy.sh && ./deploy.sh
```

访问: http://你的服务器IP（Nginx 代理）

## 云服务器部署指南

### 第一步：购买服务器

在阿里云购买**轻量应用服务器**（推荐配置如下）：
- CPU / 内存：2核 2G（最低配置，够朋友体验）
- 系统盘：40G SSD
- 镜像：**Ubuntu 22.04**
- 带宽：3-5Mbps
- 预估费用：约 60-100 元/月（学生优惠约 24-35 元/月）

购买地址：https://www.aliyun.com/product/swas

> 购买后记下服务器的**公网 IP 地址**。

### 第二步：注册域名（可选）

在阿里云万网注册域名：https://wanwang.aliyun.com/domain
- `.com` 约 55 元/年，`.cn` 约 29 元/年
- 注册后进入域名控制台 → 解析设置 → 添加记录：
  - 记录类型：**A**
  - 主机记录：**@**（或 `www`）
  - 记录值：填入服务器公网 IP

> 注意：`.cn` 域名需完成实名认证后才能解析生效。

### 第三步：服务器环境安装

SSH 登录服务器后执行以下命令：

```bash
# 更新系统
apt update && apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker

# 安装 Docker Compose
apt install -y docker-compose

# 安装 Git、Node.js 18、Maven
apt install -y git maven
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt install -y nodejs

# 验证安装
docker --version && docker-compose --version && node -v && mvn -v
```

### 第四步：部署项目

```bash
# 克隆项目
git clone https://github.com/zzboom3/talent-platform.git
cd talent-platform

# 配置环境变量
cp .env.example .env
vim .env  # 填入 DEEPSEEK_API_KEY、OSS 相关密钥

# 一键部署
chmod +x deploy.sh && ./deploy.sh
```

部署完成后通过 `http://你的公网IP` 或 `http://你的域名` 访问。

### 常用运维命令

```bash
# 查看服务状态
docker-compose ps

# 查看后端日志
docker-compose logs -f backend

# 重启所有服务
docker-compose restart

# 停止所有服务
docker-compose down

# 更新代码后重新部署
git pull && ./deploy.sh
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 演示企业（京州智科技术） | jingzhou_tech | demo123 |
| 演示企业（星云数据科技） | xingyun_data | demo123 |
| 演示企业（蓝月网络科技） | lanyue_net | demo123 |

## 版本标签

- `v1.0-pre-deploy`：上线前开发完成快照，可回退到此状态继续开发
  ```bash
  git checkout v1.0-pre-deploy  # 回到上线前的开发状态
  ```
