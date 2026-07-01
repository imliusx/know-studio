# know-studio 源码链路走读指南

> 目的：照着这份指南，**按真实调用顺序**把代码从入口一路跟到底。看完你能讲清"一个请求进来，代码是怎么一步步跑完的"。
>
> 所有路径相对 `src/main/java/com/dong/ddrag/`。

---

## 阅读方法（先看这段）

1. **每个链路我给了「调用栈 + 每个文件看什么」**。建议在 IDE 里打开第一个文件，看完跳到下一个，像爬藤蔓。
2. **重点代码我标了 ⭐**，这些是项目的灵魂，要逐行读；没标的是胶水/编排代码，扫一眼知道它干嘛即可。
3. 三条主链路建议**按 1→2→3 顺序读**，因为它们共享底层组件：
   - **链路 A：文档入库**（数据怎么进来的）—— 决定了能检索什么
   - **链路 B：QA 问答**（数据怎么被用）—— RAG 主战场
   - **链路 C：Assistant 助手**（多轮 + 工具调用）—— B 的进阶版
4. 配套入门读物：先读 `docs/rag-from-scratch.md` 建立概念，再回来跟代码。

---

## 链路 A：文档入库（上传 → 可检索）

> 目标：理解一份文档上传后，经历了什么变成"可被 RAG 检索"的状态。

### A.0 入口：上传请求怎么进来的

**`document/controller/DocumentController.java`**
- 看两个上传入口：
  - `POST /api/documents/upload`（整文件直传，简单）
  - `POST /api/documents/upload/init` → `/upload/chunks` → `/upload/{uploadId}/complete`（分片上传，复杂）
- ⭐ 看分片那套：秒传判断（init 时算 hash）、断点续传（complete 时合并）、上传完成后触发 ETL。
- **关键转折点**：上传完成 → 状态置 PROCESSING → 异步调用入库处理器。

**`document/service/DocumentUploadService.java`** & **`DocumentService.java`**
- 看 `uploadDocument` / `completeUpload` 怎么落库 document 记录、怎么调起 ETL。

### A.1 入库主流程（⭐ 核心）

**`ingestion/service/EtlDocumentIngestionProcessor.java`** ⭐⭐⭐
- 这是入库的"总指挥"，`process(documentId, groupId)` 方法按顺序做完全部步骤。**在这里你看到完整的 ETL 管道**：

```java
process(documentId, groupId) {
    // 1. 读取原文（从 MinIO）
    StoredObjectDocumentReader reader = ...;
    // 2. 解析成文本
    List<Document> rawDocs = reader.read(...);
    // 3. 文本清洗
    List<Document> cleaned = textCleanupTransformer.apply(rawDocs);
    // 4. 切片 ⭐
    List<Document> chunkDocs = chunkTransformer.apply(cleaned);
    // 5. chunk 落库 PostgreSQL
    List<DocumentChunkEntity> chunks = chunkService.saveChunkDocuments(...);
    // 6. 向量写入 pgvector ⭐
    vectorService.ingestChunks(chunks);
    // 7. ES 关键词索引 ⭐（在别处或本类，见下）
    // 8. 状态置 READY
}
```
- **对着这个方法，逐行跳进每个依赖看。**

### A.2 每一步对应的文件

| 步骤 | 文件 | 看什么 |
|---|---|---|
| 读原文 | `ingestion/reader/StoredObjectDocumentReader.java` | 从 MinIO 把文件读出来 |
| 解析 | `ingestion/parser/factory/DocumentParserFactory.java` + 各种 Parser | 按文件类型（pdf/docx/…）选解析器 |
| 清洗 | `ingestion/transformer/TextCleanupTransformer.java` | 去乱码、规范化文本 |
| **切片** ⭐ | `ingestion/transformer/StructureAwareChunkTransformer.java` | **看它怎么切**：目标 500 token、重叠 80、按结构边界切。对齐入门文档第 2 步 |
| 切片配置 | `ingestion/transformer/ChunkingProperties.java` | 切片参数（已确认：target 500/max 800/overlap 80） |
| chunk 落库 | `ingestion/chunk/ChunkService.java` + `mapper/DocumentChunkMapper` | 切完的片段写 `document_chunks` 表，分批写（规避 PG 65535 参数限制） |
| **向量写入** ⭐ | `ingestion/vector/VectorIngestionService.java` | ⭐ 看 `ingestChunks`：调 Ollama 算 embedding → 分批写 pgvector。对齐入门文档第 1 步 |
| **ES 索引** ⭐ | `retrieval/elasticsearch/ElasticsearchChunkIndexService.java` | ⭐ 看 `index` 方法：把 chunk 写进 ES 供关键词检索。对齐入门文档第 3 步 |
| 状态机 | `ingestion/service/DocumentIngestionProcessor.java`（接口）+ 失败恢复 | PROCESSING → READY / FAILED；启动时回收卡死的任务 |

### A.3 走完链路 A，你应该能回答
- 一份 PDF 上传后，最终在哪些存储里留下了什么？（MinIO 原文 / PG chunk 表 / pgvector 向量 / ES 倒排）
- 切片时重叠 80 token 是为了避免什么？
- 为什么要分批写库？

---

## 链路 B：QA 问答（RAG 主战场）⭐⭐⭐

> 目标：跟着一个提问，走完"检索→融合→分级→生成"全流程。**这是项目最精华的部分，建议花最多时间。**

### B.0 入口

**`qa/controller/QaController.java`**
- `POST /api/qa/ask` → `QaService.ask(...)`

**`qa/service/QaService.java`**
- 极薄一层：鉴权（校验 groupId 可读）→ 转给 `QaChatService`。看一眼即可。

### B.1 编排层

**`qa/service/QaChatService.java`** ⭐⭐
- `ask(groupId, question)` 方法是 QA 的总编排。看它三步走：
  1. `documentRetriever.retrieveEvidence(...)` → 拿证据 ⭐（跳进 B.2）
  2. 证据够不够？够才往下生成
  3. `qaChatClient.prompt(...).advisors(...)` → 调大模型生成答案 + 解析结构化输出
- 看 `createUserPrompt`：怎么把证据、证据分级指导语、用户问题拼成 prompt。**对齐入门文档第 6 步（拒答机制）。**

### B.2 检索层（⭐ 项目灵魂，逐行读）

**`qa/rag/ReadyChunkDocumentRetriever.java`**
- 入口：`retrieveEvidence(groupId, question)` → 内部调 `HybridChunkRetrievalService`。

**`qa/rag/HybridChunkRetrievalService.java`** ⭐⭐⭐⭐⭐
- **整个项目含金量最高的文件，没有之一。** `retrieve()` 方法是 RAG 的心脏。建议分成 5 段读，每段对着入门文档：

```
第①段：查询规划（对应入门第4.3节）
  queryPlanningService.plan(question) → 拿到多个 query

第②段：双通道召回（对应入门第3节）
  for 每个 query:
      mergeVectorHits(...)   ← pgvector 语义召回（topK=50）
      mergeKeywordHits(...)  ← ES BM25 关键词召回（topK=50）

第③段：RRF 融合（对应入门第4节，⭐最核心）
  RetrievalCandidate 内部类里：
  rankingScore += 1/(60 + rank)   ← 这就是 RRF 公式
  两路都命中的 → source = "BOTH"

第④段：聚簇 + 扩窗（对应入门第5节）
  buildClusters(...)  ← 同文档相邻 chunkIndex 合并
  expandedStartChunkIndex / expandedEndChunkIndex ← 前后扩 1 片

第⑤段：证据分级（对应入门第6节）
  evaluateEvidenceLevel(...) → SUFFICIENT/PARTIAL/WEAK/NONE
  buildEvidenceGuidance(...) → 每级对应一句指令
```

- ⭐ 一定要打开 `RetrievalCandidate` 和 `RetrievalCluster` 这两个内部类，看 `reciprocalRank(rank)`、`isContinuousWith`、`add` 方法——**算法逻辑全在这两个小类里**。

### B.3 查询规划子链路

**`qa/service/QueryPlanningService.java`** + `qa/model/QueryPlanStrategy.java`
- 看 `plan()`：怎么用大模型把一个问题改写成 DIRECT/REWRITE/DECOMPOSE。
- `QueryPlanResult`：改写出的多个 query 在这里。

### B.4 检索的底层（两个数据源）

**`retrieval/vectorstore/PgVectorRetrievalAdapter.java`**
- 看 `search(groupId, query, topK)`：怎么在 pgvector 里做**带 groupId 过滤**的语义近邻搜索。⭐ 注意 `groupId` 过滤——这就是"权限隔离"在检索层的体现。

**`retrieval/elasticsearch/ElasticsearchChunkIndexService.java`**
- 看 `search(...)`：BM25 关键词召回，同样带 groupId 过滤。

### B.5 答案组装

**`qa/support/QaAnswerParser.java`**：解析大模型的结构化输出。
**`qa/support/CitationAssembler.java`**：把命中的文档片段组装成 `citations`（引用清单）。
**`qa/model/AskQuestionResponse.java`** + 嵌套 `Citation`：最终返回结构。

### B.6 配置类（理解 Spring AI 怎么装配的）

**`qa/config/QaChatClientConfiguration.java`**：⭐ 看 `ChatClient` 和 `PromptTemplate` 的 Bean 怎么定义、`RetrievalAugmentationAdvisor` 怎么挂上去——这是 Spring AI 的核心用法。
**`qa/config/QueryPlanningConfiguration.java`**：查询规划的 Bean。

### B.7 走完链路 B，你应该能回答
- 一个问题进来，代码先后调了哪几个组件？
- RRF 的公式在哪个文件哪一行？（答：`HybridChunkRetrievalService` 的 `reciprocalRank`）
- 证据四级是怎么判断的？依据是什么字段？
- groupId 权限隔离是在哪一步生效的？

---

## 链路 C：Assistant 助手（多轮 + 工具调用）

> 目标：理解"把检索做成工具"和"会话记忆"怎么实现的。读这条前**先读完链路 B**，因为它复用了 B 的检索能力。

### C.0 入口

**`assistant/controller/AssistantChatController.java`**
- 两个端点：
  - `POST /api/assistant/chat`（一次性返回）
  - `POST /api/assistant/chat/stream` ⭐（SSE 流式，对齐 API.md 9.2）
- 看 `streamChat` 怎么建 SSE 通道、怎么把事件回调下沉。

### C.1 编排层

**`assistant/service/AssistantService.java`**
- ⭐ 看 `chat` 和 `streamChat`：怎么准备运行上下文（userId/sessionId/groupId/toolMode）→ 调起 Agent。
**`assistant/service/AssistantStreamEventEmitter.java`**：把 Agent 产生的事件转成 SSE 的 4 种事件（start/delta/done/error）。

### C.2 Agent 核心（⭐ 看重点）

**`assistant/agent/AssistantReactAgentFactory.java`** ⭐
- ⭐ 看 ReactAgent 怎么构造：挂载哪个 ChatModel、注册哪些工具、设 `recursionLimit`（防死循环）、挂哪些 Hook。
- 这是 Spring AI Alibaba 的核心用法。

**`assistant/agent/AssistantKnowledgeBaseTool.java`** ⭐⭐
- ⭐⭐ 看 `@Tool` 注解的 `search` 方法：**这就是"知识库检索被包装成工具"**。
  - 从 `ToolContext` 取 groupId（注意：groupId 不在 prompt 里，而是通过 context 透传——**这是防 prompt 注入的安全设计**）
  - 调 `ReadyChunkDocumentRetriever`（复用链路 B 的检索！）
  - ⭐ 看 `AssistantKnowledgeBaseToolResultHolder`：**防重复调用**——本轮查过就返回 DUPLICATE_TOOL_CALL。对齐入门文档第 9 步。
- `AssistantAgentFacade.java`：Agent 的门面/协调。

### C.3 会话记忆（⭐ 理解"上下文重建"）

**`assistant/memory/AssistantShortTermMemoryHook.java`** ⭐⭐
- ⭐⭐ 看 `beforeModel` 方法：**不是增量打补丁，而是整体重建**模型上下文（摘要→最近10条→当前问题）。对齐入门文档第 9 步。
- 注意它怎么避免"当前问题在历史里回声导致重复喂模型"。

**`assistant/memory/AssistantSessionSummaryService.java`** + `AssistantMemorySummarizer.java` + `AssistantShortTermMemoryMaintenanceService.java`
- 长对话怎么被压缩成摘要（compact summary）。

**`assistant/memory/AssistantMemoryPromptConfiguration.java`**：记忆相关的 prompt 模板。

### C.4 会话管理

**`assistant/service/AssistantSessionService.java`**：会话 CRUD。
**`assistant/service/AssistantConversationService.java`**：加载会话上下文（恢复对话用）。
**`assistant/controller/AssistantSessionController.java`** + `AssistantConversationController.java`：会话相关端点。

### C.5 走完链路 C，你应该能回答
- Assistant 和 QA 的检索方式有什么本质区别？（工具调用 vs 固定检索）
- groupId 怎么传给工具的？为什么不放 prompt 里？
- 怎么防止 Agent 反复调工具烧钱？
- 长对话怎么不被历史撑爆 token？

---

## 横切关注点（穿插在所有链路里）

> 这些不属于某条链路，但每条链路都会碰到，建议单独过一遍。

### 认证鉴权
- `auth/security/JwtAuthenticationFilter.java`：每个请求先过这个 Filter 验 token。
- `identity/service/CurrentUserService.java`：⭐ 从请求里取出当前用户，提供 `requireSystemAdmin` 等权限校验。**Controller 里的鉴权全靠它。**
- `auth/config/AuthConfiguration.java`：BCrypt 密码加密。
- 详见之前确认：**没用 Spring Security 框架，只借了 BCrypt，Filter 是手写的。**

### 权限模型（多租户隔离）
- `groupmembership/service/GroupMembershipService.java`：⭐ 看 `listVisibleGroups` 和内部 record `GroupQueryResult`。
- 核心思想：系统角色 ADMIN/USER + 组内角色 OWNER/MEMBER，**数据隔离靠每个查询带 groupId 过滤**。

### 统一返回 & 异常
- `common/api/ApiResponse.java`：统一返回结构（注意哪些接口是裸返回，见 API.md）。
- `common/exception/`：全局异常处理。

### 存储
- `storage/`：MinIO 封装（原文存储）。
- 数据库表结构：搜 `db/migration`（Flyway 迁移脚本）看表是怎么建的——**读 SQL 是理解数据模型最快的方式**。

---

## 建议的"通关"顺序与检验

### 第一遍（建立全局观，2~3 小时）
1. 读 `docs/rag-from-scratch.md` 建立概念
2. 只读三条链路的 **⭐ 核心文件**（不看横切）：
   - A: `EtlDocumentIngestionProcessor`
   - B: `HybridChunkRetrievalService`（最重要）
   - C: `AssistantKnowledgeBaseTool` + `AssistantShortTermMemoryHook`

### 第二遍（补细节，逐文件）
3. 按本文档每个链路的表格，把所有列出的文件过一遍
4. 看配置类（`qa/config/*`、`auth/config/*`）理解 Spring AI 怎么装配

### 第三遍（查数据流）
5. 打开 Flyway 迁移脚本，对照表结构看数据怎么流转
6. 在关键方法打断点 / 加日志，跑一个真实请求看调用栈

### 通关自测题（答得出就算懂了）
1. 用户问一个问题，`groupId` 这个参数从 Controller 一路传到 SQL，经过了哪些方法签名？
2. RRF 融合发生在哪个方法？为什么用排名不用分数？
3. 证据分级为 SUFFICIENT 需要满足什么条件？这段判断在哪个文件？
4. 一个 chunk 被切片后，在 PG / pgvector / ES 三处分别存了什么？
5. Assistant 模式下，模型决定"要不要查知识库"的机制叫什么？代码在哪？

---

## 文件速查索引（按模块）

```
qa/                       ← 链路 B 主战场
  controller/QaController.java
  service/QaService.java
  service/QaChatService.java          ⭐ 编排
  rag/ReadyChunkDocumentRetriever.java
  rag/HybridChunkRetrievalService.java ⭐⭐⭐ 检索心脏
  service/QueryPlanningService.java
  support/QaAnswerParser.java
  support/CitationAssembler.java
  config/QaChatClientConfiguration.java ⭐ Spring AI 装配

ingestion/                ← 链路 A
  service/EtlDocumentIngestionProcessor.java ⭐⭐ ETL 总指挥
  transformer/StructureAwareChunkTransformer.java ⭐ 切片
  transformer/TextCleanupTransformer.java
  vector/VectorIngestionService.java  ⭐ 向量写入
  chunk/ChunkService.java
  reader/StoredObjectDocumentReader.java

retrieval/                ← 检索底层（A/B 共用）
  vectorstore/PgVectorRetrievalAdapter.java
  elasticsearch/ElasticsearchChunkIndexService.java

assistant/                ← 链路 C
  controller/AssistantChatController.java ⭐ SSE 入口
  service/AssistantService.java
  agent/AssistantReactAgentFactory.java ⭐ Agent 构造
  agent/AssistantKnowledgeBaseTool.java ⭐⭐ 检索即工具
  memory/AssistantShortTermMemoryHook.java ⭐⭐ 上下文重建
  memory/AssistantSessionSummaryService.java

auth/ + identity/ + groupmembership/  ← 横切：鉴权/权限
common/                   ← 横切：统一返回/异常
storage/                  ← 横切：MinIO
```

---

> 💡 小贴士：跟代码时遇到看不懂的 Spring AI / Alibaba 注解（如 `@Tool`、`@HookPositions`、`RetrievalAugmentationAdvisor`），先别钻框架源码，**先理解它在业务里的作用**（本文档已说明），框架细节需要时再查官方文档。重点是理解**数据怎么流的、每一步为什么这么做**，而不是背 API。
