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

## Docker 部署

```bash
# 1. 创建 .env 文件 (参考 .env.example)
cp .env.example .env
# 编辑 .env 填入真实的 API Key 和 OSS 密钥

# 2. 构建后端
cd backend && mvn clean package -DskipTests && cd ..

# 3. 构建前端
cd frontend && npm run build && cd ..

# 4. 启动所有服务
docker-compose up -d --build
```

访问: http://localhost (Nginx代理)

## 默认管理员账号
- 用户名: admin
- 密码: admin123
