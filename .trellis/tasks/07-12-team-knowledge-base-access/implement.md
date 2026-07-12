# Team + KnowledgeBase ACL 架构纠偏 · 执行计划

## Stage 0 · Contract and Migration Baseline

- [x] 建立 Workspace 引用与目标归属矩阵。
- [x] 固化 Team、KnowledgeBase、ACL、Conversation 和 Evaluation 契约。
- [x] 补充 backend/frontend spec：Team + KnowledgeBase ACL 与检索授权约定。
- [x] 记录当前本地 V1-V5 数据数量，作为迁移验收基线。

验证：`mvn -q clean validate test`、`pnpm lint/typecheck/build`。

回滚点：仅任务文档和规范。

## Stage 1 · Identity and Schema Migration

- [x] 新增 V6 migration：Team、TeamMember、KnowledgeBase、Team Grant。
- [x] 将旧 Workspace 数据迁移为 Team + 默认 KnowledgeBase。
- [x] 将 WorkspaceMember 角色迁移为 TeamMember。
- [x] 为 knowledge/evaluation 增加 KnowledgeBase 兼容列并回填。
- [ ] 将 conversation 从 Workspace 所有权迁移为 User 所有权。
- [ ] 删除旧 Workspace 表和约束。
- [x] 使用 MyBatis-Plus 实现 Team/KnowledgeBase ACL repository 和访问服务。
- [ ] 替换 IdentityApi 的 Workspace 权限接口。

验证：Flyway 从空库和现有 V5 库升级、权限单测、MyBatis/ArchUnit、全量 Maven。

回滚点：`feat: replace workspaces with team knowledge access`

## Stage 2 · Knowledge and Ingestion Ownership

- [x] 文档、Chunk、Embedding、上传和入库领域对象改为 knowledgeBaseId。
- [x] API 路径迁移到 `/api/knowledge-bases/{knowledgeBaseId}/documents`。
- [x] 管理操作使用 `requireManageable`。
- [x] 文档读取使用 `requireReadable`，后续引用入口在 Stage 3 接入同一边界。
- [x] MQ、恢复任务、MinIO object key、pgvector 与 ES metadata 同步迁移。
- [x] 文档去重约束改为 knowledgeBaseId + contentHash。

验证：上传、秒传、分片恢复、入库、删除、重试、跨 Team 越权。

回滚点：`refactor: scope knowledge ingestion by knowledge base`

## Stage 3 · Retrieval, Agent and Conversation

- [x] Retrieval 入口计算当前用户可读 knowledgeBaseIds。
- [x] 显式范围和意图命中结果只允许缩小授权集合。
- [x] vector、keyword、rerank 和 citation 全链路携带知识库过滤。
- [x] Session/Message/Memory 查询仅按 userId + sessionId 隔离。
- [x] Chat API 移除强制 workspaceId 路径和请求字段。
- [x] 保存回答实际知识库和引用，失权后阻止文档访问。
- [x] TOOL 无匹配时继续在授权知识库内执行知识检索。

验证：公开库、单 Team、跨 Team、多 Team、无权限、会话隔离和 SSE 七事件。

回滚点：`refactor: authorize agent retrieval by knowledge base`

## Stage 4 · Evaluation and Admin APIs

- [ ] Evaluation 数据改为 knowledgeBaseId 归属。
- [ ] System ADMIN 和 KnowledgeBase MANAGE 才能运行评测。
- [ ] 完成 Team CRUD、成员管理、知识库 CRUD 和 Team grant API。
- [ ] OpenAPI、统一错误码和 401/403/429 行为更新。

验证：Team Admin 权限矩阵、跨 Team 评测隔离、API 集成测试。

回滚点：`feat: add team knowledge administration`

## Stage 5 · Frontend Workspace Removal

- [ ] 删除 Workspace store、创建引导、切换器和 Workspace API。
- [ ] 普通用户登录后直接进入 Chat，并只加载自己的会话。
- [ ] 文档和评测 Query key 改为 knowledgeBaseId。
- [ ] 管理端接入 Team、成员、知识库和授权管理。
- [ ] 文档、Chat、Evaluation 适配新 API。
- [ ] 保留其他正式功能入口和现有登录注册视觉。

验证：前端 lint/typecheck/build，ADMIN/USER 路由与操作可见性。

回滚点：`refactor: remove workspace frontend context`

## Stage 6 · Migration and End-to-End Acceptance

- [ ] 从现有 V5 数据库执行 V6+ 升级并核对数据数量。
- [ ] 验证管理员创建 Team、知识库、文档和跨 Team 授权。
- [ ] 验证普通用户无需切换上下文直接问答。
- [ ] 验证多 Team 用户、公司公开库、Team 私有库和失权场景。
- [ ] 验证 Chat 引用、深度思考、评测和会话隔离。
- [ ] 完成桌面/移动端交互与截图验收。
- [ ] 更新 README 和本地演示流程。
- [ ] 全量 Maven、前端质量门禁、Compose 配置和 diff check 通过。

回滚点：E2E 修复与文档独立提交。

## Start Gate

- [x] 用户已确认 Team + KnowledgeBase ACL 架构并批准实施。
- [x] 任务已激活后开始代码修改。

## Validation Record

### Foundation batch (2026-07-12)

- V6 successfully upgraded the local V5 database and preserved all baseline counts.
- V1-V6 successfully migrated a clean PostgreSQL database and the application started on port 18080.
- Backfill counts: teams 6, team members 10, knowledge bases 6, grants 6,
  documents 2, chunks 2, evaluation datasets 3, samples 2, runs 9.
- `mvn -q -pl module-identity -am clean validate test` passed.
- `mvn -q -pl module-knowledge -am clean validate test` passed.
- `mvn -q clean validate test` passed; Java 26 ArchUnit fallback warnings remain baseline noise.

### Knowledge ownership batch (2026-07-12)

- V7 activated non-null `knowledge_base_id` ownership while retaining nullable legacy columns until final cleanup.
- V8 temporarily deferred the Evaluation `knowledge_base_id` non-null constraint because Evaluation remains on
  Workspace ownership until Stage 4; Stage 4 will backfill new writes and restore the constraint.
- Existing E2E owner listed two granted KnowledgeBases and read the migrated READY document through the new API.
- A newly registered user without Team grants received HTTP 403 when reading the same KnowledgeBase documents.
- `mvn -q -pl module-knowledge -am validate test` passed.

### Retrieval and conversation batch (2026-07-12)

- V9 made Conversation sessions user-owned while preserving migrated rows; new sessions write `workspace_id=NULL`.
- Vector SQL, Elasticsearch keyword search, neighbor expansion, rerank evidence and citations use the same
  server-authorized KnowledgeBase ID set. Requested scopes are intersected and cannot expand permissions.
- The startup Elasticsearch migration backfilled `knowledgeBaseId` on two legacy indexed chunks.
- A second user received HTTP 404 for another user's conversation; an unauthorized KnowledgeBase-only scope
  received HTTP 403 before query planning.
- Authorized vector retrieval returned successfully and Agent SSE emitted `token` then `done` through the new APIs.
