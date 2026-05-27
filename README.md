# 运维数字员工系统

基于 Spring Boot 3 + Vue 3 + AnythingLLM RAG 私有知识库的智能运维助手系统，支持知识库管理、大模型流式对话、工单闭环处理。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.1.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 前端框架 | Vue 3 (Composition API) | 3.4 |
| UI 组件库 | Element Plus | 2.7 |
| 构建工具 | Vite | 5.2 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.2 |
| 接口文档 | Knife4j (Swagger) | 4.4 |
| 本地大模型 | Ollama + qwen2.5:3b | - |
| RAG 知识库 | AnythingLLM | latest |
| 容器化 | Docker Compose | 3.8 |

## 功能模块

- **首页仪表盘** — 用户数、知识库条目、待处理工单、当日处理量统计
- **账号管理** — 运维人员账号的增删改查，角色与状态管理
- **知识库管理** — FAQ 问答对的 CRUD，支持逐条或全量同步到向量知识库
- **工单管理** — 报障工单的创建、受理、处理、回访全流程
- **智能问答** — 基于 RAG 的流式对话，高置信度 FAQ 直接返回，低置信度调用 Ollama 推理，SSE 流式输出 + 打字机动画

## RAG 问答流程

```
用户提问 → AnythingLLM 向量检索 → 相似度判断
  ├─ score > 0.7 → 匹配 FAQ 正则提取答案 → 直接返回
  └─ score ≤ 0.7 → 拼接知识库上下文 → Ollama 流式推理 → SSE 逐 token 推送
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker 与 Docker Compose（用于中间件）

### 1. 启动中间件

```bash
# 启动 MySQL + Redis + AnythingLLM
docker-compose up -d
```

### 2. 安装并启动 Ollama

```bash
# 下载安装 https://ollama.com，然后拉取模型
ollama pull qwen2.5:3b
```

### 3. 配置 AnythingLLM

1. 访问 http://localhost:3001 完成初始化设置
2. 进入 Settings → Api Keys → 复制 API Key
3. 创建工作区，浏览器地址栏 URL 最后一段即为 workspace slug
4. 修改 `backend/src/main/resources/application.yml` 中 `anything-llm` 配置

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
# 接口文档：http://localhost:8080/api/doc.html
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
# 访问地址：http://localhost:5173
# 默认账号：admin / 123456
```

## 项目结构

```
ops-digital-staff/
├── backend/
│   └── src/main/
│       ├── java/com/itheima/ops/digital/staff/
│       │   ├── common/          # 通用返回体、异常处理
│       │   ├── config/          # 配置类（MyBatis-Plus、CORS、Tomcat）
│       │   ├── controller/      # 控制器
│       │   ├── dto/             # 数据传输对象
│       │   ├── entity/          # 实体类
│       │   ├── integration/     # 外部系统集成（AnythingLLM Client）
│       │   ├── mapper/          # MyBatis Mapper 接口
│       │   └── service/         # 业务服务
│       └── resources/
│           ├── application.yml  # 应用配置
│           └── sql/init.sql     # 数据库初始化脚本
├── frontend/
│   └── src/
│       ├── api/                 # 接口请求封装
│       ├── layout/              # 布局组件
│       ├── router/              # 路由配置
│       ├── store/               # Pinia 状态管理
│       ├── utils/               # 工具函数（request 拦截器）
│       └── views/               # 页面组件
│           ├── Dashboard.vue    # 首页仪表盘
│           ├── SysUser.vue      # 账号管理
│           ├── OpsFaq.vue       # 知识库管理
│           ├── OpsTicket.vue    # 工单管理
│           └── Chat.vue         # 智能问答（SSE 流式 + 打字机动画）
├── docker-compose.yml           # 完整中间件编排
├── docker-compose-light.yml     # 轻量编排（仅 MySQL + Redis）
└── 部署文档.md                   # 详细部署说明
```

## 部署到服务器

参考 [部署文档.md](部署文档.md)，步骤概要：

1. 服务器安装 Docker Compose 启动中间件
2. 安装 Ollama 并拉取 qwen2.5:3b 模型
3. 配置 AnythingLLM 工作区并获取 API Key
4. 修改 `application.yml` 中数据库、Redis、AnythingLLM 连接信息
5. `mvn package` 打包后端，`java -jar` 启动
6. `npm run build` 构建前端，Nginx 代理静态文件并转发 `/api` 请求

## 许可证

MIT License
