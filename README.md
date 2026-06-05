# 运维数字员工系统

基于 Spring Boot 3 + Vue 3 + AnythingLLM RAG 私有知识库的智能运维助手系统，支持知识库管理、大模型流式对话、工单闭环处理。

## 技术栈

| 层级       | 技术                    | 版本   |
| ---------- | ----------------------- | ------ |
| 后端框架   | Spring Boot             | 3.1.5  |
| ORM        | MyBatis-Plus            | 3.5.7  |
| 前端框架   | Vue 3 (Composition API) | 3.4    |
| UI 组件库  | Element Plus            | 2.7    |
| 构建工具   | Vite                    | 5.2    |
| 数据库     | MySQL                   | 8.0    |
| 缓存       | Redis                   | 7.2    |
| 接口文档   | Knife4j (Swagger)       | 4.4    |
| 本地大模型 | Ollama + qwen2.5:3b     | -      |
| 嵌入模型   | bge-m3                  | -      |
| RAG 知识库 | AnythingLLM (可选)      | latest |
| 容器化     | Docker Compose          | 3.8    |

## 功能模块

- **首页仪表盘** — 用户数、知识库条目、待处理工单、当日处理量统计
- **账号管理** — 运维人员账号的增删改查，角色与状态管理
- **知识库管理** — FAQ 问答对的 CRUD，支持逐条或全量同步到向量知识库
- **工单管理** — 报障工单的创建、受理、处理、回访全流程
- **智能问答** — 基于 RAG 的流式对话，高置信度 FAQ 直接返回，低置信度调用 Ollama 推理，SSE 流式输出 + 打字机动画

## RAG 问答流程

```
用户提问
  │
  ▼
① 本地嵌入搜索 (bge-m3 余弦相似度)
  ├─ score ≥ 0.75 → 即时返回知识库答案（毫秒级，免调用大模型）
  └─ score < 0.75
       │
       ▼
  ② AnythingLLM 向量检索（如果已配置，可选）
       │
       ▼
  ③ Ollama 流式推理 + 相关知识库 FAQ 上下文 → SSE 逐 token 推送
```

> **不依赖 AnythingLLM 也能工作**：系统内置基于 bge-m3 嵌入的本地语义搜索，即使 AnythingLLM 未配置或不可用，知识库问答仍然正常运作。

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
ollama pull qwen2.5:3b      # 对话模型
ollama pull bge-m3:latest    # 嵌入模型（用于本地RAG语义搜索）
```

### 3. 配置（可选）

**Ollama 配置**（`application.yml` 中的 `ollama` 段）：

```yaml
ollama:
  base-url: http://localhost:11434         # Ollama 服务地址
  chat-model: qwen2.5:3b                   # 对话模型
  embedding-model: bge-m3:latest            # 嵌入模型
  timeout: 300000                           # 请求超时(ms)
  num-predict: 2048                         # 最大生成 token 数
  embedding-similarity-threshold: 0.75      # 嵌入匹配阈值
```

**AnythingLLM 配置**（可选，用于增强 RAG）：

1. 访问 http://localhost:3001 完成初始化设置
2. 进入 Settings → Api Keys → 复制 API Key
3. 创建工作区，浏览器地址栏 URL 最后一段即为 workspace slug
4. 修改 `backend/src/main/resources/application.yml` 中 `anything-llm` 配置

> 不配置 AnythingLLM 也能正常使用，系统自带本地嵌入语义搜索。

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
│       │   ├── config/          # 配置类（CORS、线程池、分页、Tomcat）
│       │   ├── controller/      # 控制器
│       │   ├── entity/          # 实体类
│       │   ├── integration/     # 外部集成
│       │   │   ├── AnythingLLMClient.java      # AnythingLLM RAG 客户端
│       │   │   ├── LocalEmbeddingSearch.java    # 本地 bge-m3 嵌入语义搜索
│       │   │   └── OllamaProperties.java        # Ollama 可配置属性
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
├── docs/                         # 项目文档
│   ├── 软件测试文档.md            # 测试用例与结果
│   ├── 软件设计文档.md            # 架构与接口设计
│   └── 课程报告设计书.md          # 课程项目报告
└── 部署文档.md                   # 详细部署说明
```

## 核心特性

- **本地嵌入 RAG**：基于 bge-m3 的语义搜索，不依赖外部向量数据库，启动自动加载
- **分层回退策略**：本地嵌入 → AnythingLLM → Ollama 直连，任何一层故障自动降级
- **SSE 流式输出**：token 级打字机动画，支持 AbortController 中断
- **输入安全校验**：用户名唯一性、密码 WRITE_ONLY 序列化、满意度 1-5 范围约束
- **异常不泄露**：全局异常处理器分层响应，生产环境不暴露内部错误
- **优雅线程管理**：SSE 异步处理使用托管线程池，支持优雅关闭
- **FAQ 启用/停用**：知识库条目可一键停用，即时从 AI 知识库移除
- **配置外置**：Ollama 地址、模型名、超时、阈值等均可通过 yml 配置

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
