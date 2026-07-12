# ARAG 前端适配与端到端联调 · 技术设计

## 1. Scope and Boundaries

本任务保持现有前端路由、导航和正式功能完整，仅迁移以下五条核心链路：

1. 认证
2. 工作空间
3. 文档与入库
4. Agentic Chat
5. Evaluation

后端改动限定为上述链路缺失的查询和管理接口。用户、问答、意图、数据通道、任务和设置不在本任务中迁移，也不得删除或隐藏。

## 2. Frontend Architecture

### 2.1 API Layer

保留 `src/api` 作为 HTTP 边界，但按 ARAG 领域重组契约：

- `auth.ts`：register/login/logout/me
- `workspaces.ts`：列表、创建、成员查询与管理
- `documents.ts`：列表、详情、上传、删除、重试
- `conversations.ts`：会话 CRUD、上下文
- `agent.ts`：SSE Chat
- `evaluations.ts`：数据集、样本、运行和历史查询
- `http.ts`：统一 ApiResponse、Bearer token、401/403/429 与业务错误

不保留面向旧 `/assistant`、`/groups` 和根级 `/documents` 的长期双写兼容层。调用方迁移完成后删除无引用旧类型和函数。

### 2.2 Global Workspace Context

新增单一 workspace store/provider，状态至少包含：

- `workspaces`
- `currentWorkspaceId`
- 当前用户在工作空间中的 `role`
- `setCurrentWorkspace`
- 恢复最后选择与失效回退逻辑

`currentWorkspaceId` 持久化到 localStorage。TanStack Query key 必须包含 workspaceId，例如：

```text
['documents', workspaceId, filters]
['sessions', workspaceId]
['evaluation-datasets', workspaceId]
```

切换工作空间时取消旧空间进行中的请求和 SSE，重置页面选择态；缓存通过 workspaceId 天然隔离，不使用全局无空间 key。

### 2.3 Permissions

后端返回工作空间成员角色，前端只将角色用于隐藏或禁用操作，安全边界仍由后端校验。

| Capability | OWNER | ADMIN | MEMBER |
|---|---:|---:|---:|
| Chat / view citations | yes | yes | yes |
| View READY documents | yes | yes | yes |
| Upload/delete/retry documents | yes | yes | no |
| Run/manage evaluations | yes | yes | no |
| Manage members | yes | yes | no |
| Delete workspace / transfer ownership | yes | no | no |

系统 ADMIN 在后端权限解析中等价于 workspace OWNER。

## 3. Backend API Completion

### 3.1 Identity

新增当前用户可访问工作空间列表，返回 workspace 基本信息和当前角色；新增成员列表。复用 `IdentityApi` / `IdentityService` 和 MyBatis-Plus repository，不允许 Controller 直接访问 Mapper。

建议契约：

```text
GET /api/workspaces
GET /api/workspaces/{workspaceId}/members
```

### 3.2 Knowledge

新增 workspace 隔离的文档列表、删除和失败入库重试。文档列表支持状态和文件名过滤，返回上传及入库状态所需字段。

```text
GET    /api/workspaces/{workspaceId}/documents
DELETE /api/workspaces/{workspaceId}/documents/{documentId}
POST   /api/workspaces/{workspaceId}/documents/{documentId}/retry-ingestion
```

删除采用现有领域状态/软删除约定，并同步处理 pgvector、ES 和对象存储的既有一致性策略；若当前领域尚无完整删除能力，则在实施时以可恢复的软删除为首选。

### 3.3 Conversation

补齐会话列表、详情、重命名和删除。所有操作同时校验 workspaceId、sessionId 和当前用户，不能只按 sessionId 查询。

```text
GET    /api/workspaces/{workspaceId}/sessions
PATCH  /api/workspaces/{workspaceId}/sessions/{sessionId}
DELETE /api/workspaces/{workspaceId}/sessions/{sessionId}
```

### 3.4 Evaluation

补齐数据集、样本和运行记录查询：

```text
GET /api/workspaces/{workspaceId}/evaluations/datasets
GET /api/workspaces/{workspaceId}/evaluations/datasets/{datasetId}/samples
GET /api/workspaces/{workspaceId}/evaluations/datasets/{datasetId}/runs
```

创建、添加样本和运行消融沿用现有接口。管理和运行操作要求 ADMIN 以上角色。

## 4. Authentication Contract

前端以新后端 `AuthSession` 和 `CurrentIdentity` 为唯一真实类型。登录后保存 Sa-Token access token，并通过 `Authorization: Bearer` 发送；不调用当前后端不存在的 refresh/reset-password/account 接口。

应用启动时若存在 token，调用 `/api/auth/me` 验证。失败则清理本地认证状态并跳转登录页。SSE 使用同一 access token。Token 存储沿用现有前端机制，本任务不引入 refresh token 体系。

## 5. Agent SSE State Machine

SSE parser 解析 `event:` 和多行 `data:`，事件 payload 根据事件名进行类型收窄：

- `token`：追加回答文本
- `thinking`：追加思考步骤
- `tool_call`：创建进行中的工具调用项
- `tool_result`：完成对应工具调用并展示结果摘要
- `citation`：按 chunkId 去重并挂到当前 assistant message
- `done`：只执行一次收口、持久化 UI 状态并刷新会话列表
- `error`：结束 loading，保留已收到内容并展示可重试错误

每次发送创建 AbortController。切换工作空间、切换会话、用户停止或组件卸载时中止流。Reducer/state machine 必须容忍重复 done、error 后断流和无尾部空行。

## 6. Document Upload Flow

前端按后端新契约执行：

```text
hash file
→ POST /documents/uploads
→ 若秒传完成，刷新文档列表
→ PUT /documents/uploads/{sessionId}/chunks/{index}
→ GET progress（恢复时）
→ POST complete
→ 轮询文档状态直到 READY/FAILED
```

分片请求使用 multipart 字段 `file`，hash 放入 `X-Chunk-SHA256`。上传状态与入库状态分离展示，避免“上传 100%”被误认为“入库完成”。

## 7. Evaluation UI

在现有路由体系内增加或复用评测页面，不重构全站导航。页面包含：

- 数据集列表与创建
- 样本列表与添加
- topK 输入
- 运行消融
- 三种检索模式的 Recall@K、latency 对比表/图
- 429 冷却提示和运行中禁用状态

## 8. Compatibility and Existing Work

- 保留当前未提交的 Header/Profile/Chat 导航调整，不回退。
- 保留所有非核心正式功能路由与页面。
- 旧 API 模块只有在确认无核心调用方后才删除；非核心功能仍依赖旧契约时必须保持其代码可编译。
- 后端迁移使用新的 Flyway 版本，不修改已应用的 V1-V4。

## 9. Local Validation Topology

使用根目录 Docker Compose 启动 PostgreSQL、Elasticsearch、MinIO、Redis、RabbitMQ、Ollama；后端 `dev` profile 运行在 8080，前端 Vite 运行在 5174 并代理 `/api`。

验收至少配置一个可生成回答的 Chat provider。未配置的可选 MCP、Langfuse 或 reasoning provider必须表现为明确降级，不阻塞基础 Chat 验收。

## 10. Rollback

- 后端接口补齐、前端 API/store 迁移、页面接入和 E2E 修复分别提交，便于按阶段回退。
- 数据库只新增向后兼容迁移；回滚应用时旧代码可忽略新增结构。
- 前端切换 workspace 和 SSE 的状态改造独立于视觉组件，出现问题可回退 API 调用层而不重做页面。
