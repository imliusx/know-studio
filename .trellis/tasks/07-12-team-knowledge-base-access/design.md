# Team + KnowledgeBase ACL 架构纠偏 · 技术设计

## 1. Domain Boundaries

### Identity

- `User`: 系统身份与 `ADMIN/USER` 角色。
- `Team`: 单公司内部组织单元，允许用户加入多个 Team。
- `TeamMember`: `TEAM_ADMIN/MEMBER`。

### Knowledge

- `KnowledgeBase`: 文档和检索内容边界。
- `KnowledgeBaseTeamGrant`: Team 对知识库的 `READ/MANAGE` 权限。
- `Document`、`Chunk`、`Embedding`、`UploadSession`、`IngestionJob`: 全部归属 KnowledgeBase。

### Conversation

- Session、Message、Memory 归属 User。
- 回答结果记录实际知识库和引用，不将 Team 作为会话所有权。

### Evaluation

- Dataset、Sample、Run 归属 KnowledgeBase。
- 管理权限复用 KnowledgeBase MANAGE 判定。

## 2. Database Migration

使用 V6+ 增量 migration，保持 V1-V5 不变。V6 先新增目标表、兼容列并回填，
业务代码完成切换后再通过后续 migration 删除 Workspace 表和旧列，确保每个阶段
都能启动和回滚应用代码。

### New Tables

```text
teams(id, name, description, parent_id, status, created_by, created_at, updated_at)
team_members(id, team_id, user_id, team_role, created_at)
knowledge_bases(id, name, description, visibility, owner_team_id, created_by, status, created_at, updated_at)
knowledge_base_team_grants(id, knowledge_base_id, team_id, permission, created_at)
```

### Existing Data Mapping

1. V6：`workspaces` → 同 ID `teams`。
2. V6：`workspace_members` → `team_members`，OWNER/ADMIN 映射 TEAM_ADMIN。
3. V6：每个旧 Workspace 创建同 ID默认 KnowledgeBase，并为对应 Team 写入 MANAGE grant。
4. V6：Knowledge 与 Evaluation 表新增 `knowledge_base_id` 并按旧 Workspace 回填，
   暂时保留 workspace_id 供旧代码运行。
5. 业务代码切换：所有新写入只依赖 knowledgeBaseId；Conversation 只依赖 userId。
6. 最终 migration：将 knowledge_base_id 改为 NOT NULL，建立最终唯一约束和索引，
   删除 workspace_id、Workspace 表及旧索引。

迁移在事务内执行；新增约束前先进行数量、空值和孤儿记录检查。

## 3. Authorization API

`IdentityApi` 不再暴露 Workspace role，改为：

```text
requireSystemAdmin()
requireTeamRole(teamId, TEAM_ADMIN)
readableKnowledgeBaseIds(userId)
requireKnowledgeBaseReadable(knowledgeBaseId)
requireKnowledgeBaseManageable(knowledgeBaseId)
```

授权规则：

1. System ADMIN 对全部知识库可读可管。
2. COMPANY 可见性对全部登录用户可读。
3. 创建人对 PRIVATE 知识库可读可管。
4. Team grant READ/MANAGE 通过当前用户的 TeamMember 关系生效。
5. ownerTeam 隐含 MANAGE；数据库同时写显式 grant，查询保持统一。

## 4. Retrieval Security

`RetrievalQuery` 使用 `Set<Long> knowledgeBaseIds` 或由服务端权限上下文解析，禁止客户端直接扩大范围。

```text
current user
→ readableKnowledgeBaseIds
→ optional requested scope intersection
→ intent-selected KB intersection
→ vector/keyword retrieval with KB filter
→ citation authorization check
```

pgvector SQL 和 Elasticsearch query 都必须包含 knowledgeBaseId 集合过滤。没有可读知识库时返回业务错误，不退化为全库检索。

## 5. API Shape

### Administration

```text
GET/POST        /api/teams
GET/POST/DELETE /api/teams/{teamId}/members
GET/POST        /api/knowledge-bases
GET/PATCH/DELETE /api/knowledge-bases/{knowledgeBaseId}
GET/PUT         /api/knowledge-bases/{knowledgeBaseId}/teams
/api/knowledge-bases/{knowledgeBaseId}/documents/**
/api/knowledge-bases/{knowledgeBaseId}/evaluations/**
```

### User Chat

```text
GET/POST/PATCH/DELETE /api/conversations/**
POST                  /api/agent/chat/stream
```

Chat 请求保留 `toolMode/deepThinking`；可选 `teamId/knowledgeBaseIds` 只能缩小服务端计算的授权集合。

## 6. Frontend State

- 删除 workspace store、持久化 currentWorkspaceId 和全局 Workspace 初始化门禁。
- 顶部 TeamSwitcher 不再承担业务上下文选择；普通端直接进入 Chat。
- 管理端新增 Team 与 KnowledgeBase ACL 管理入口。
- 文档和评测页面先选择 KnowledgeBase，而不是 Workspace。
- Query key 使用 `knowledgeBaseId`；会话 key 只包含当前用户身份边界。
- 切换知识库时中止上传和页面请求；Chat 不因 Team 变化重置会话。

## 7. Compatibility and Rollback

- 先提交数据库与领域模型，再迁移读取路径，最后移除 Workspace API/UI。
- 每阶段保持 Maven 和前端构建可通过。
- 不长期保留 Workspace 与 KnowledgeBase 双写。
- 回滚应用版本时 V1-V5 代码无法理解 V6 schema，因此 V6 上线前必须保留数据库备份；本任务本地验收以向前迁移为主。
