# Team + KnowledgeBase ACL 架构纠偏

## Goal

移除此前未经产品确认引入的 Workspace 顶层领域，将系统调整为单公司内部知识问答产品：管理员维护知识库，普通用户使用问答；公司内部通过 Team 与 KnowledgeBase ACL 支持公共知识、团队私有知识和跨团队共享。

## User Value

- 普通用户登录后直接问答，不需要理解或切换 Workspace。
- 管理员可维护知识库、文档、入库、意图路由和评测。
- 用户可同时属于多个团队，并自动获得对应知识库读取权限。
- 同一个知识库可以授权给多个团队，不需要复制文档和向量数据。
- 检索层强制执行知识库权限过滤，防止跨团队引用泄露。

## Confirmed Facts

- 产品为单公司内部系统，不是 SaaS 多租户产品。
- 系统级角色已经存在 `ADMIN` 和 `USER`。
- 普通用户只使用问答和自己的会话，不维护知识库。
- 管理员负责知识库、文档、检索配置和评测。
- 用户可属于多个 Team；Team 支持 `TEAM_ADMIN` 和 `MEMBER`。
- 知识库可见性包括公司公开、团队授权和私有。
- Chat 默认不要求用户选择 Team 或知识库，由后端根据用户权限和意图路由选择。
- 当前 Workspace 是重构过程中额外引入的抽象，并非原产品需求。
- 当前数据库 V1-V5、六个后端模块和前端核心链路均依赖 `workspace_id`。
- 当前本地已有联调数据，迁移必须可执行，不能通过修改已应用 Flyway 脚本完成。
- `ragent` 采用全局知识库、用户会话隔离和意图到知识库路由，但其知识库权限校验不足；本项目需要补齐 Team ACL。

## Requirements

### 1. Identity and Teams

- 删除 Workspace、WorkspaceMember 和 `OWNER/ADMIN/MEMBER` 空间角色。
- 保留系统级 `ADMIN/USER`。
- 新增 Team、TeamMember，成员角色为 `TEAM_ADMIN/MEMBER`。
- 系统 ADMIN 可管理全部 Team、用户和知识库。
- Team Admin 可管理本团队成员以及本团队拥有或具备 MANAGE 权限的知识库。
- 普通用户只能读取自己通过 Team、公司公开或显式授权获得的知识库。

### 2. Knowledge Bases

- KnowledgeBase 成为正式聚合根，包含名称、描述、可见性、拥有团队、创建人和状态。
- 可见性：`COMPANY` 全公司登录用户可读；`TEAM` 按 ACL 可读；`PRIVATE` 仅创建人、系统管理员和显式授权主体可读。
- 一个知识库可授权多个 Team，权限至少包括 `READ/MANAGE`。
- 文档、Chunk、Embedding、上传会话和入库任务改为归属 `knowledge_base_id`。
- 文档 hash 去重范围改为知识库，而不是 Workspace。

### 3. Chat, Conversation and Retrieval

- Conversation 和 Message 只按当前 `user_id` 隔离，不再归属 Workspace。
- Chat 请求不要求 workspaceId；Team/knowledgeBaseId 仅可作为可选的用户显式范围过滤。
- 后端先计算当前用户可读知识库集合，再进行意图路由和检索。
- 意图命中的知识库必须与授权集合取交集；无可访问知识库时返回明确业务错误。
- pgvector、Elasticsearch、引用详情和文档读取均再次按 knowledgeBaseId 授权，不能只依赖前端或意图路由。
- 会话可记录每条回答实际使用的知识库和引用，但用户失权后不能重新打开受限文档。

### 4. Evaluation and Administration

- 评测数据集、样本和运行记录归属知识库，或在明确的全公司范围运行。
- 只有系统 ADMIN 或对目标知识库具有 MANAGE 权限的 Team Admin 可管理和运行评测。
- 管理端保留知识库、文档、团队、用户、意图、评测和链路入口。
- 普通端不展示 Workspace 切换器、Team 管理或知识库管理操作。

### 5. Migration and Compatibility

- 使用新的 Flyway migration，不修改 V1-V5。
- 为保留现有数据，每个旧 Workspace 迁移为同名 Team 和一个同名默认 KnowledgeBase。
- 旧 WorkspaceMember 映射为 TeamMember：OWNER/ADMIN → TEAM_ADMIN，MEMBER → MEMBER。
- 旧 Workspace 下的文档、Chunk、Embedding、上传和入库数据迁移到对应默认 KnowledgeBase。
- 旧 Session 保留用户会话和消息，但移除 Workspace 所有权。
- 旧 Evaluation 数据迁移到对应默认 KnowledgeBase。
- 新 API 生效后删除前端 Workspace store、切换器和创建引导；不长期保留双套业务接口。

## Acceptance Criteria

- [ ] 前端不再显示或要求创建、选择 Workspace。
- [ ] 系统 ADMIN 可创建 Team、添加成员、创建知识库并授权给多个 Team。
- [ ] Team Admin 只能管理具备 MANAGE 权限的知识库和本团队成员。
- [ ] USER 可以直接进行问答并管理自己的会话。
- [ ] USER 可检索公司公开知识库和所属 Team 获权知识库，不能读取其他 Team 私有知识库。
- [ ] 多 Team 用户可同时检索所有已授权知识库，无需切换上下文。
- [ ] Chat、检索、引用详情、文档下载均执行后端知识库权限校验。
- [ ] 文档、Chunk、Embedding、上传和入库任务全部按 knowledgeBaseId 隔离。
- [ ] 会话与消息按 userId 隔离，不再包含强制 workspaceId。
- [ ] Evaluation 按 knowledgeBaseId 隔离并要求 MANAGE 权限。
- [ ] V6+ migration 可从现有 V1-V5 数据库无损升级，旧联调数据仍可读取。
- [ ] 后端全量测试和前端 lint/typecheck/build 通过。
- [ ] 覆盖公司公开、单团队、跨团队共享、多团队用户、失权和越权场景。

## Constraints

- 单公司边界内实现，不新增 Company/tenant 切换 UI。
- 不把 Team 重新包装为 Workspace。
- 不允许仅在 Controller 或前端做权限判断；授权集合必须进入检索和存储查询。
- 保留现有正式前端功能入口，不以架构纠偏为由删除无关功能。
- 不重写或 squash 现有提交，使用增量提交保留可回滚基线。

## Out of Scope

- 多公司 SaaS tenant 隔离。
- 外部客户门户和跨公司共享。
- LDAP/企业微信/钉钉组织同步。
- 复杂审批流和临时授权到期机制。
- 生产数据迁移窗口、双写和零停机发布。

## Open Questions

- None. Product model and MVP permission behavior are confirmed.
