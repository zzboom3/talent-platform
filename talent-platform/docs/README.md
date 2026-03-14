# `talent-platform` 文档总览

这套文档面向三类读者：

- 平台用户：想知道这个平台能做什么、如何一步步使用。
- 项目开发者：想快速跑起项目、定位代码入口、理解前后端协作方式。
- 技术学习者：想从真实代码出发，系统理解这个项目的架构、数据模型、权限、接口和业务流。

## 阅读顺序

如果你第一次接触这个项目，推荐按下面顺序阅读：

1. [`01-project-overview.md`](./01-project-overview.md)
2. [`02-user-guide.md`](./02-user-guide.md)
3. [`03-frontend-guide.md`](./03-frontend-guide.md)
4. [`04-backend-architecture.md`](./04-backend-architecture.md)
5. [`05-domain-models.md`](./05-domain-models.md)
6. [`06-api-and-business-flows.md`](./06-api-and-business-flows.md)
7. [`07-developer-setup-and-deploy.md`](./07-developer-setup-and-deploy.md)
8. [`08-known-gaps-and-evolution.md`](./08-known-gaps-and-evolution.md)

## 文档地图

### 1. 先理解项目是什么

- [`01-project-overview.md`](./01-project-overview.md)
  - 项目背景、目标、角色、能力边界
  - 技术栈总览
  - 系统架构、部署结构、核心截图入口

### 2. 再理解平台怎么用

- [`02-user-guide.md`](./02-user-guide.md)
  - 访客、人才、企业、管理员四类角色的使用流程
  - 注册登录、人才档案、企业审核、岗位投递、课程学习、证书、AI 功能、后台入口

### 3. 然后理解代码怎么组织

- [`03-frontend-guide.md`](./03-frontend-guide.md)
  - 前端目录结构、路由、状态管理、API 封装、页面组织
- [`04-backend-architecture.md`](./04-backend-architecture.md)
  - 后端包结构、鉴权、配置、请求链路、外部依赖

### 4. 深入业务和技术细节

- [`05-domain-models.md`](./05-domain-models.md)
  - 实体关系、字段语义、状态枚举、业务生命周期
- [`06-api-and-business-flows.md`](./06-api-and-business-flows.md)
  - 控制器与接口清单、关键业务流、前后端配合方式

### 5. 最后看运行和演进

- [`07-developer-setup-and-deploy.md`](./07-developer-setup-and-deploy.md)
  - 本地开发、环境变量、Docker 部署、排障
- [`08-known-gaps-and-evolution.md`](./08-known-gaps-and-evolution.md)
  - 代码现状、实现缺口、技术债、风险与后续演进建议

## 适用范围

这套文档只以当前仓库中的 `talent-platform/` 子项目为权威来源，不以外层 `project-demo` 目录中的其他同名或历史副本为准。

文档采用“现状优先”的写法：

- 先描述代码里已经真实实现的能力。
- 再单独标注“建议”“注意”“已知限制”“实现偏差”。
- 不把 README、PPT 材料中的展示性表述直接当作既成事实。

## 项目速览

`talent-platform` 是一个面向软件产业人才服务场景的全栈项目，核心能力包括：

- 人才档案管理与人才展示
- 企业入驻、审核、发岗与招聘
- 岗位搜索、投递与申请状态流转
- 课程学习、学习进度、证书发放
- AI 岗位匹配、AI 人才匹配、AI 课程推荐、AI 职业评估
- 资讯、公告、政策内容展示与后台审核
- 区块链模拟存证与后台链状态查看
- 数据大屏和后台监控统计

## 术语表

- `Talent`：人才用户，对应求职者角色。
- `Enterprise`：企业用户，对应招聘方角色。
- `Admin`：管理员角色，负责审核、运营与监控。
- `TalentProfile`：人才档案，承载简历与展示信息。
- `Company`：企业资料，承载企业主体信息与审核状态。
- `Job`：标准岗位。
- `TalentRecruitment`：另一类“人才引进/招募需求”实体，和标准岗位并存。
- `CourseEnrollment`：用户课程报名与学习进度记录。
- `LearningCertificate`：课程完成后的学习证书。
- `BlockchainBlock`：模拟区块链中的区块实体。
- `Result<T>`：后端常用统一返回包装体，字段为 `code`、`message`、`data`。

## 角色导航

### 我是平台普通使用者

优先阅读：

1. [`01-project-overview.md`](./01-project-overview.md)
2. [`02-user-guide.md`](./02-user-guide.md)

### 我是前后端开发者

优先阅读：

1. [`03-frontend-guide.md`](./03-frontend-guide.md)
2. [`04-backend-architecture.md`](./04-backend-architecture.md)
3. [`06-api-and-business-flows.md`](./06-api-and-business-flows.md)
4. [`07-developer-setup-and-deploy.md`](./07-developer-setup-and-deploy.md)

### 我是技术学习者或答辩讲解者

优先阅读：

1. [`01-project-overview.md`](./01-project-overview.md)
2. [`04-backend-architecture.md`](./04-backend-architecture.md)
3. [`05-domain-models.md`](./05-domain-models.md)
4. [`06-api-and-business-flows.md`](./06-api-and-business-flows.md)
5. [`08-known-gaps-and-evolution.md`](./08-known-gaps-and-evolution.md)

## 快速入口

- 项目根说明：[`../README.md`](../README.md)
- 前端工程：[`../frontend`](../frontend)
- 后端工程：[`../backend`](../backend)
- 环境变量模板：[`../.env.example`](../.env.example)
- 容器编排：[`../docker-compose.yml`](../docker-compose.yml)
- Nginx 配置：[`../nginx.conf`](../nginx.conf)
- 一键部署脚本：[`../deploy.sh`](../deploy.sh)

## 说明

文档中出现“存在风险”“建议优化”“实现偏差”并不代表项目不可用，而是为了帮助读者准确理解：

- 哪些能力已经完成
- 哪些能力是展示层面提到但实现不完整
- 哪些地方适合作为后续毕业设计优化点或二次开发入口
