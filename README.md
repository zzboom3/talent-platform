# XX市软件产业人才公共服务平台

太原理工大学毕业实习项目

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + Spring Data JPA |
| 数据库 | MySQL 8.0（Docker） |

## 项目结构

```
talent-platform/
├── backend/     # Spring Boot 后端
├── frontend/    # Vue 3 前端
└── README.md
```

## 快速启动

### 1. 启动数据库（Docker）

```bash
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -v ~/docker/mysql/data:/var/lib/mysql \
  -v ~/docker/mysql/conf:/etc/mysql/conf.d \
  -v ~/docker/mysql/log:/var/log/mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_CHARACTER_SET_SERVER=utf8mb4 \
  -e MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci \
  --restart=always \
  mysql:8.0
```

创建数据库：

```sql
CREATE DATABASE talent_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

## 默认账户

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

## 模块说明

- **门户首页**：新闻公告、政策法规展示
- **人才库**：人才信息 CRUD、档案管理
- **岗位管理**：企业发布岗位、人才申请
- **智能匹配**：基于技能关键词的人才-岗位匹配
- **终生学习**：课程列表、报名管理
- **政策宣传**：政策文章发布与展示
- **后台管理**：用户管理、数据统计
