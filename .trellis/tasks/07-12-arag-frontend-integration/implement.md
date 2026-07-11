# ARAG 前端适配与端到端联调 · 执行计划

## Stage 0 · Contract Inventory and Test Baseline

- [x] 核对 Controller/OpenAPI 契约，建立前端旧接口到 ARAG 接口的迁移表：`research/api-contract-matrix.md`。
- [x] 记录当前三处未提交前端改动并确保后续不回退。
- [x] 运行前端 lint/typecheck/build 和后端测试，记录基线结果。
- [x] 创建 frontend Trellis 指引，明确 API、Query key、workspace、权限与 SSE 约定。

基线记录（2026-07-12）：

- `pnpm lint`：失败，20 errors / 3 warnings；集中于 React purity、effect setState 和 Fast Refresh 既有规则。
- `pnpm typecheck`：通过。
- `pnpm build`：通过，存在 >500 kB chunk 警告。
- `mvn -q clean validate test`：通过；Java 26 运行环境有 ArchUnit class-version fallback 警告，项目目标仍为 Java 21。

验证：

```bash
cd know-studio-ui && pnpm lint && pnpm typecheck && pnpm build
mvn -q clean validate test
```

回滚点：仅文档/规范，不修改运行行为。

## Stage 1 · Minimal Backend API Completion

- [x] identity：工作空间列表、成员列表、角色字段与权限测试。
- [x] knowledge：文档列表、软删除、失败入库重试与 workspace 隔离。
- [x] conversation：会话列表、重命名、软删除与 user/workspace 隔离测试。
- [x] evaluation：数据集、样本、运行记录查询与 ADMIN 权限入口。
- [x] OpenAPI 通过 Springdoc 自动覆盖新增 Controller 契约。
- [x] 新增 V5 migration，将文档 hash 唯一约束改为排除 `DELETED` 的部分唯一索引。

验证记录（2026-07-12）：

- `module-identity`、`module-knowledge`、`module-conversation`、`module-evaluation` 定向 `validate test` 均通过。
- `mvn -q clean validate test` 全量通过。
- 文档删除将 document 标记为 `DELETED`、chunk 标记 deleted，并移除 pgvector/ES 索引；MinIO 原文保留以支持恢复和后续清理策略。
- 上传初始化、分片、进度和完成接口统一要求 workspace ADMIN 以上权限；MEMBER 仅可读取 READY 文档和引用。

验证：模块单测、Controller 集成测试、ArchUnit、`mvn -q clean validate test`。

回滚点：`feat: complete frontend integration APIs`

## Stage 2 · Authentication and Workspace Context

- [x] 将前端认证类型迁移到 `AuthSession` / `CurrentIdentity`。
- [x] 核心认证恢复改为 `/auth/me` 校验，不再调用 refresh endpoint；非核心 reset/account API 暂保留原页面边界。
- [x] 建立全局 workspace store 与 localStorage 持久化、失效回退逻辑。
- [x] 适配 workspace 列表、创建、切换和无 workspace 引导。
- [x] workspace 查询建立全局 key；文档/会话/评测的 workspace-scoped key 在对应迁移阶段完成。
- [x] workspace role 已进入全局状态；具体页面按钮权限在文档、Chat、评测阶段接入。

验证记录（2026-07-12）：

- Stage 2 涉及文件定向 ESLint 通过。
- `pnpm typecheck` 通过。
- `pnpm build` 通过，保留既有 >500 kB chunk 警告。
- 注册直接消费后端返回的 AuthSession 并进入已登录状态；持久 token 通过 `/auth/me` 重新校验。

验证：登录刷新、token 失效、workspace 失效回退、跨空间缓存隔离、403 UI。

回滚点：`feat: integrate authentication and workspaces`

## Stage 3 · Document and Ingestion Integration

- [x] API 改为 `/api/workspaces/{workspaceId}/documents...`。
- [x] 上传初始化字段、PUT multipart 分片和 hash header 对齐后端。
- [x] API 层支持秒传、恢复上传、上传进度和完成操作。
- [x] 文档列表/详情/删除/重试 API 接入真实接口。
- [x] 上传完成后在 PENDING/PROCESSING 状态自动轮询，区分上传完成与 READY。
- [x] 移除核心文档页面 mock 分支及硬编码 workspace 名称/模型映射。
- [x] MEMBER 只读；ADMIN/OWNER 可上传、重试和删除。

验证记录（2026-07-12）：

- 文档 API、分片上传、状态查询和完成接口全部改为 workspace-scoped ARAG 契约。
- `pnpm build` 通过，保留既有大 chunk 警告。
- `mvn -q -pl module-knowledge -am validate test` 通过。

验证：小文件、跨多分片文件、秒传、断点恢复、FAILED 重试、越权操作。

回滚点：`feat: integrate document ingestion workflow`

## Stage 4 · Agentic Chat Integration

- [x] 会话创建、列表、上下文、重命名、删除改用 workspace 契约。
- [x] Chat 请求传递 toolMode 和 deepThinking。
- [x] 重写 SSE 类型与 parser，支持七种 ARAG 事件。
- [x] 将 token/thinking/tool/citation 映射到现有 Chat UI 组件。
- [x] 实现 AbortController、停止生成、切换 workspace/session 中止。
- [x] 处理重复 done、流末残片、401/403/429 和中途 error。
- [x] 保留当前 Header/Profile/Chat 导航调整。

验证记录（2026-07-12）：

- 会话 API、Dashboard 会话统计和 Query key 均改为 workspace-scoped 契约。
- SSE API 边界支持 CRLF、多行 data、任意分片、末尾残帧和七种事件的类型化投影。
- Chat UI 已展示深度思考、工具调用状态和引用；流错误保留已生成内容，重复终止事件不会重复收口。
- 停止生成、切换会话、切换工作空间和组件卸载都会中止活动流；切换工作空间同时清理瞬态会话状态，避免跨空间串用。
- `pnpm typecheck` 与 `pnpm build` 通过；API/Dashboard 定向 ESLint 通过。Chat 文件仍有 Stage 0 已记录的 React purity/effect 基线问题，本阶段未新增规则抑制或新的 lint 类别。

验证：知识回答、引用、澄清、工具调用、深度思考、停止生成、限流和模型失败降级。

回滚点：`feat: integrate agentic chat streaming`

## Stage 5 · Evaluation UI

- [ ] 接入数据集列表/创建。
- [ ] 接入样本列表/添加。
- [ ] 支持 topK 和消融运行。
- [ ] 展示三种模式 Recall@K 与 latency。
- [ ] 处理运行中、空数据、429 冷却和失败状态。
- [ ] MEMBER 不显示管理入口且直接请求返回 403。

验证：真实样本创建、三模式运行、历史记录刷新、权限和限流。

回滚点：`feat: add retrieval evaluation workspace`

## Stage 6 · Local End-to-End Acceptance

- [ ] 启动全部 Compose 依赖、后端和 Vite 前端。
- [ ] 创建 OWNER、ADMIN、MEMBER 测试用户并验证权限矩阵。
- [ ] 跑通注册/登录 → workspace → 上传入库 → Chat 引用 → Evaluation。
- [ ] 验证跨 workspace 隔离、401/403/429、SSE 断连与恢复。
- [ ] 使用桌面和移动视口做 Playwright 交互及截图检查。
- [ ] 运行前端 lint/typecheck/build、后端全测试和 `git diff --check`。
- [ ] 更新 README：本地启动、环境变量、核心演示流程和已知可选依赖。

最终验证：

```bash
docker compose config
mvn -q clean validate test
cd know-studio-ui && pnpm lint && pnpm typecheck && pnpm build
git diff --check
```

回滚点：E2E 修复与文档独立提交。

## Start Gate

- [ ] 用户 review `prd.md`、`design.md`、`implement.md`。
- [ ] 明确现有三处前端未提交改动的提交归属，但不回退。
- [ ] 确认可用于本地验收的 Chat provider 配置；无真实 reasoning/MCP 时接受明确降级。
- [ ] 任务激活后才开始修改代码。
