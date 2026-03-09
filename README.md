# 软件产业人才公共服务平台

> 太原理工大学毕业实习项目 · XX市软件产业人才公共服务平台 MVP

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + Spring Data JPA |
| 数据库 | MySQL 8.0（Docker） |

## 功能模块

- **门户首页**：新闻公告、数据概览
- **人才库**：人才档案 CRUD、技能展示
- **岗位中心**：岗位发布、申请管理
- **智能匹配**：按技能关键词匹配岗位
- **终生学习**：课程列表、在线报名
- **政策法规**：政策/资讯/公告管理
- **后台管理**：用户管理、数据统计

## 快速启动

### 1. 启动 MySQL（Docker）

```bash
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -v ~/docker/mysql/data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_CHARACTER_SET_SERVER=utf8mb4 \
  --restart=always \
  mysql:8.0

docker exec mysql8 mysql -uroot -proot -e \
  "CREATE DATABASE IF NOT EXISTS talent_platform CHARACTER SET utf8mb4;"
```

### 2. 启动后端

```bash
cd backend
mvn package -DskipTests
java -jar target/platform-0.0.1-SNAPSHOT.jar
# 访问 http://localhost:8080
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

## 默认账号

首次运行后，调用注册接口或通过前端注册页面创建管理员账号（role 选 ADMIN）。

## 项目结构

```
project-demo/
├── backend/          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/talent/platform/
│       ├── controller/
│       ├── entity/
│       ├── repository/
│       ├── service/
│       ├── security/
│       └── config/
└── frontend/         # Vue 3 前端
    ├── package.json
    └── src/
        ├── views/
        ├── api/
        ├── stores/
        └── router/
```
