# ARAG 前端适配与端到端联调

## Goal

将现有 `know-studio-ui` 从旧版后端契约迁移到当前 ARAG 多模块后端，交付可实际演示的完整用户链路：认证、工作空间、文档入库、Agentic 对话、引用/思考/工具事件展示，以及评测运行与结果查看。最终应能在本地或 Docker Compose 环境中完成真实端到端验收，而不是继续依赖 mock 数据或旧接口。

## User Value

- 用户可以登录后创建或选择工作空间，并管理该空间中的文档。
- 用户可以看到文档上传、分片续传、入库状态和失败反馈。
- 用户可以进行支持知识检索、工具调用和深度思考的流式对话。
- 用户可以看到引用来源、思考过程、工具调用过程及可控错误状态。
- 项目具备完整可演示路径，为后续真实模型接入、部署和简历展示提供界面入口。

## Confirmed Facts

- 前端技术栈为 React 19、TypeScript、Vite 8、TanStack Router/Query、Zustand、Tailwind CSS v4 和 shadcn 风格组件。
- 前端已有登录注册、工作空间/小组、文档管理、分片上传和 Chat UI，可复用现有页面与交互，不需要重建新的视觉框架。
- 现有普通端、管理端及任务、问答、用户、意图、数据通道、设置等页面和入口均属于正式产品功能，不是待清理的遗留页面。
- 前端 API 仍面向旧契约，例如 `/assistant/*`、`/groups/*`、`/documents/*`，旧 SSE 事件为 `start/delta/done/error`。
- ARAG 后端采用 workspace 路径隔离：`/api/workspaces/{workspaceId}/...`。
- ARAG Chat SSE 事件包含 `token`、`thinking`、`tool_call`、`tool_result`、`citation`、`done`、`error`。
- ARAG 后端当前已有注册、登录、登出、当前用户、创建工作空间、添加成员、创建会话、读取会话上下文、Agent 流式对话、分片上传、文档详情、检索及评测创建/运行接口。
- ARAG 后端当前缺少支撑完整前端工作流的部分查询/管理接口，例如工作空间列表、会话列表与管理、文档列表/删除/重试，以及评测数据集和运行记录查询。
- 当前工作树已有三处未提交前端改动：隐藏 Chat 页重复的账户/设置入口。这些改动必须保留，不能在本任务中被覆盖或回退。
- 开发环境前端端口为 `5174`，`/api` 代理到 `VITE_DEV_PROXY_TARGET`，并已允许 `knows.liusx.dev`。

## Requirements

### 1. API 与认证适配

- 统一前端 ARAG API 类型、错误解析和鉴权头处理。
- 登录、注册、登出和当前用户信息使用新后端真实契约。
- 删除旧后端不存在的 refresh/reset/account API 假设，或由明确的新后端契约替代。
- 401、403、429、业务错误和网络错误提供一致的用户反馈。

### 2. 工作空间

- 登录后可加载用户可访问的工作空间，并选择当前工作空间。
- 当前工作空间作为全局应用上下文，由顶部或侧栏切换器统一管理，并在本地持久化最后一次有效选择。
- 可创建工作空间；成员管理按后端实际支持范围接入。
- 所有文档、会话、检索和评测请求显式携带当前 workspaceId。
- 切换工作空间时清理或隔离相关 Query 缓存和页面状态，避免跨空间数据串用。
- 持久化的工作空间失效或用户已无权访问时，自动回退到第一个可用工作空间；无工作空间时进入明确的创建引导状态。
- 权限矩阵：OWNER 拥有工作空间设置、成员、文档、Chat、评测的全部权限；ADMIN 可管理成员、文档、Chat 和评测，但不能删除工作空间或转移所有权；MEMBER 仅可使用 Chat、查看 READY 文档和引用。
- 系统 ADMIN 沿用后端既有规则，在工作空间权限判断中视为 OWNER。

### 3. 文档与入库

- 适配 ARAG 分片上传初始化、分片 PUT、进度查询和完成接口。
- 支持上传进度、断点续传、秒传结果和失败重试反馈。
- 文档列表展示真实状态，至少覆盖 PENDING、PROCESSING、READY、FAILED。
- 可查看文档详情及失败原因；列表、删除和重试能力取决于本任务确认的后端补齐范围。
- 移除影响正式演示的 mock 文档和硬编码知识库映射。

### 4. Agentic Chat

- 创建会话并加载已有上下文。
- 请求包含 sessionId、workspaceId、toolMode 和 deepThinking。
- 正确解析流式 SSE 分帧、命名事件、多行 data、AbortSignal 和异常响应。
- `token` 增量渲染回答；`thinking` 展示思考过程；`tool_call/tool_result` 展示工具状态；`citation` 形成可查看引用；`done/error` 正确收口。
- 支持用户主动停止生成、断线后的可理解状态以及 429 限流反馈。
- 不因事件顺序、重复 done 或流末尾残片导致重复消息或 UI 卡死。

### 5. Evaluation

- 可创建评测数据集、添加标注样本并触发消融运行。
- 展示 VECTOR_ONLY、HYBRID、HYBRID_RERANK 的 Recall@K 和延迟对比。
- 数据集列表、样本列表和历史运行展示取决于本任务确认的后端补齐范围。

### 6. End-to-End Validation

- 使用真实 PostgreSQL、Redis、RabbitMQ、MinIO、Elasticsearch 和至少一个可用模型供应商运行端到端链路。
- 本任务以本地 Docker Compose 环境为验收边界，不包含部署到 `23.80.83.80`。
- 覆盖认证、工作空间隔离、文档上传入库、检索对话、深度思考/工具事件和评测运行。
- 前端通过 lint、typecheck、build；后端通过相关模块测试和全模块构建。
- 使用桌面和移动视口验证主要页面无溢出、遮挡或不可操作状态。

### 7. Minimal Backend Completion

- 允许补齐前端完整工作流必需的后端接口，但只限于现有领域能力的查询和管理闭环。
- identity：用户可访问工作空间列表及必要的成员查询。
- conversation：会话列表、详情/上下文、重命名和删除。
- knowledge：文档列表、删除、失败任务重试及前端状态轮询所需信息。
- evaluation：数据集、样本和运行记录查询。
- 新接口继续遵守 workspace 隔离、统一响应、鉴权、错误码和模块边界规范。
- 不借联调任务新增与现有 PRD 无关的业务模块或管理功能。

## Acceptance Criteria

- [ ] 新用户可注册、登录并进入受保护页面，刷新页面后鉴权行为符合后端实际 token 机制。
- [ ] 用户可创建/选择工作空间，所有业务请求均使用正确 workspaceId，跨工作空间不可读取数据。
- [ ] OWNER、ADMIN、MEMBER 的前端操作可见性与后端授权结果一致，越权请求返回 403。
- [ ] 文件可通过 ARAG 分片上传接口完成上传并最终看到 READY 或明确的 FAILED 原因。
- [ ] 用户可创建会话并收到真实 SSE 流；token、thinking、tool_call、tool_result、citation、done、error 均有正确 UI 状态。
- [ ] 用户可切换工具模式和深度思考模式，后端收到对应字段。
- [ ] 用户可创建评测数据集、添加样本、运行消融并看到三种检索模式的指标。
- [ ] 关键页面不再依赖旧 `/assistant`、`/groups`、根级 `/documents` API 或正式路径中的 mock 数据。
- [ ] 401、403、429 和服务异常均不会造成空白页、无限 loading 或重复消息。
- [ ] `pnpm lint`、`pnpm typecheck`、`pnpm build` 通过；相关后端测试和 Maven 构建通过。
- [ ] 桌面和移动端的登录、工作空间、文档、Chat、评测主流程完成视觉和交互验收。

## Constraints

- 复用现有前端设计系统和页面结构，避免无关的全站视觉重构。
- 保留现有信息架构、路由和功能入口；不得以 ARAG 联调为由删除、隐藏或合并现有正式功能。
- 不回退或覆盖用户当前未提交的三处 Chat/Profile 导航改动。
- 不保留双套长期 API 兼容层；迁移完成后以 ARAG 契约为准。
- 不在前端持久化模型供应商密钥或其他服务端机密。
- 本任务只迁移认证、工作空间、文档、Agentic Chat、评测五条 ARAG 核心链路。
- 用户、问答、意图、数据通道、任务、设置等其他正式功能保持现有入口和行为，不在本任务中强行改接尚不存在的 ARAG 后端领域。
- 后续按业务领域补齐 ARAG 后端后，再为这些功能分别创建迁移任务。

## Out of Scope

- K8s、微服务拆分或新的基础设施选型。
- 模型训练和微调。
- 全站品牌与视觉语言重做。
- 大规模性能压测和生产发布自动化；本任务只保证可重复的端到端验收基础。
- 服务器部署、生产反向代理、HTTPS 和线上数据迁移；这些内容单独创建部署任务。

## Open Questions

- None. Core scope and local acceptance boundary are confirmed.
