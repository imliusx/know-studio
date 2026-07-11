# Agentic RAG 平台后端 · 架构设计（design.md）

> 配套 `prd.md`。记录技术架构：模块结构、领域模型与契约、核心链路、数据模型、生产支柱设计、部署与关键权衡。
> 决策依据见 prd 的 D1–D11。

## 命名约定

| 项 | 值 |
|---|---|
| groupId | `know.studio` |
| 父项目 artifactId | `arag`（packaging=pom，聚合 + BOM） |
| 根包 | `know.studio.arag` |
| 模块 artifactId | `platform-core` / `platform-ai` / `module-identity` / `module-knowledge` / `module-retrieval` / `module-agent` / `module-conversation` / `module-evaluation` / `bootstrap` |
| 模块包 | `know.studio.arag.<模块>`（如 `know.studio.arag.retrieval`、`know.studio.arag.platform.core`） |

> 注：`know.studio` 非严格反向域名，自用/求职无碍；将来若发布 Maven 中央仓库需改为拥有的域名反写或 `io.github.<user>`。

---

## 1. 模块结构与依赖

### 1.1 顶层模块树

```
arag/                                 ← 父 pom (packaging=pom, 聚合 + dependencyManagement/BOM)
├── pom.xml
│
├── platform-core/                    ← 横切基础设施 (零业务)
├── platform-ai/                      ← AI 供应商适配 / 路由 / 熔断 (零业务)
│
├── module-identity/                  ← 业务: 用户 / 租户 / 权限
├── module-knowledge/                 ← 业务: 知识库 / 文档 / 入库 Pipeline
├── module-retrieval/                 ← 业务: 多路检索 / RRF / rerank / 证据分级
├── module-agent/                     ← 业务: 意图 / 编排 / MCP / 深度思考
├── module-conversation/              ← 业务: 会话 / 记忆
├── module-evaluation/                ← 业务: 评测集 / 消融对比
│
├── bootstrap/                        ← 启动 + 配置聚合 + 组装 (唯一可运行 jar)
│
├── docs/  docker/  docker-compose.yml  README.md
```

### 1.2 业务模块内部结构（垂直切片 + 六边形分层）

每个 `module-*` 同构。以 `module-retrieval` 为例：

```
module-retrieval/
├── pom.xml                          ← 依赖 platform-*、及其他模块的 :api 包
└── src/main/java/know/studio/arag/retrieval/
    ├── api/                         ← ✅ 模块对外唯一入口 (其他模块只能 import 这里)
    │   ├── RetrievalApi.java              retrieve(query, groupId) → EvidenceBundle
    │   ├── dto/  (RetrievalQuery, EvidenceBundle…)
    │   └── event/ (RetrievalCompletedEvent…)
    ├── rest/                        ← HTTP 入站适配器
    │   └── RetrievalController.java       POST /api/retrieval/search
    ├── domain/                      ← ⭐ 领域核心 (纯业务, 不依赖 Spring/DB/HTTP)
    │   ├── model/    (RetrievalCandidate, RetrievalCluster, EvidenceLevel…)
    │   ├── service/  (HybridRetrievalService, RrfFusion, EvidenceGrader…)
    │   └── port/     (VectorSearchPort, KeywordSearchPort, RerankPort)  ← 出站端口
    └── infra/                       ← 技术实现 (出站适配器)
        ├── adapter/  (PgVectorSearchAdapter, EsKeywordSearchAdapter, RerankAdapter)
        └── config/   (RetrievalModuleConfig)
```

**依赖方向（六边形）**：`rest → api/domain`；`domain` 只定义 `port` 接口、不碰技术；`infra` 实现 `port` 并调 `platform-*`。→ `domain` 可脱离 Spring 单测。其余 5 个业务模块结构同构。

### 1.3 平台横切模块

```
platform-core/…/platform/core/
├── response/   ApiResponse<T> 统一返回
├── exception/  三级异常体系 + @RestControllerAdvice 全局拦截
├── trace/      @RagTraceNode + AOP + TraceContext (OTel)
├── ratelimit/  @RateLimit + Redisson 限流 (全局 + 用户级)
├── mq/         RabbitMQ 封装 (发送确认 / 死信重试 / 幂等消费基类)
├── sse/        SseEmitterSender (线程安全流式)
├── context/    UserContext / TtlExecutors 跨线程透传
└── id/         Snowflake 分布式 ID

platform-ai/…/platform/ai/
├── chat/          ChatModelRouter (多供应商路由入口)
├── routing/       CircuitBreaker (三态熔断) + FailoverChain (降级链) + ProbeBuffering (首包探测)
├── embedding/     EmbeddingClient (→ 本地 Ollama BGE-M3)
├── rerank/        RerankClient (→ 本地 bge-reranker)
├── provider/      DashScopeProvider / DeepSeekProvider / OllamaProvider (统一 SPI)
└── observability/ Langfuse 接入
```

### 1.4 bootstrap（唯一可运行模块）

```
bootstrap/
├── pom.xml                          ← 依赖所有 module-* 和 platform-*
└── src/
    ├── main/java/know/studio/arag/
    │   ├── ARagApplication.java           @SpringBootApplication
    │   └── config/                        全局配置 (Sa-Token/MyBatis/OpenAPI/CORS)
    ├── main/resources/
    │   ├── application.yml / application-dev.yml
    │   ├── db/migration/                   Flyway
    │   └── prompts/                        Prompt 模板
    └── test/java/…/archunit/               ArchUnit 模块边界测试
```

### 1.5 依赖规则（ArchUnit 强制）

```
bootstrap ──▶ module-* (全部)
module-*  ──▶ platform-core, platform-ai
module-A  ──▶ module-B.api        ✅ 只能碰 :api 包
module-A  ──✗ module-B.domain/infra   ❌ ArchUnit 拦截
platform-* ──✗ module-*            ❌ 平台层不许反向依赖业务
```

---

## 2. 领域模型与模块接口契约

### 2.1 各模块对外 API（门面，其他模块只依赖这些 `:api` 接口）

| 模块 | 门面 | 关键方法 |
|---|---|---|
| identity | `IdentityApi` | `currentUser()` · `requireGroupReadable(groupId)` · `requireRole(...)` |
| knowledge | `KnowledgeApi` | `createKb` · `listKb` · `getDocument` |
| knowledge | `IngestionApi` | `submit(documentId)` · `status(documentId)` |
| retrieval | `RetrievalApi` | `retrieve(RetrievalQuery) → EvidenceBundle` |
| agent | `AgentApi` | `chat(ChatRequest)` · `streamChat(ChatRequest, sink)` |
| conversation | `ConversationApi` | `appendMessage` · `loadContext(sessionId)` · `summarizeIfNeeded` |
| evaluation | `EvaluationApi` | `runAblation(datasetId) → EvalReport` |

### 2.2 核心 DTO（`:api` 包，跨模块传递）

- `RetrievalQuery`(question, groupId, topK)
- `EvidenceBundle`(List\<Evidence\>, EvidenceLevel, guidance)
- `Evidence`(documentId, chunkId, fileName, text, score, source)
- `ChatRequest`(sessionId, groupId, message, toolMode, **deepThinking**)
- `ChatStreamEvent`(type, payload) — type: `token / thinking / tool_call / tool_result / citation / done / error`
- `IntentResult`(intent, confidence) — intent: `KNOWLEDGE / TOOL / CHAT / CLARIFY`

### 2.3 领域事件（模块间异步解耦，Spring `ApplicationEvent`）

- `DocumentUploadedEvent`（knowledge 内部 → 触发异步入库）
- `IngestionCompletedEvent`（knowledge → 索引就绪/通知）
- `MessagePersistedEvent`（conversation → 触发记忆维护）

> **同步 vs 异步约定**：跨模块「查询/命令」走 `:api` 接口调用（如 agent 调 `RetrievalApi.retrieve`）；「事件通知/解耦」走领域事件（如上传后异步入库）。这也是模块低耦合的关键。

## 3. 核心链路时序

### 3.1 文档入库（knowledge）

```
Controller.upload → 存 MinIO → document(PENDING)
  → 发 RabbitMQ (ingestion.exchange → ingestion.queue, publisher-confirm)
  └─(消费者: 手动 ACK + 幂等去重)→ IngestionPipeline (节点编排)
        Parse → Clean → Chunk → Embed(platform-ai) → PgVectorWrite
                                                   → EsIndexWrite
        每节点: 状态更新 + 逐节点日志
        成功 → document(READY) + ACK + IngestionCompletedEvent
        失败 → TTL+DLX 指数退避重试; 耗尽 → ingestion.dlq(死信) + document(FAILED)
        (启动回收僵尸 PROCESSING 任务 → 重投 / FAILED)
```
> 队列拓扑：`ingestion.exchange`(direct) → `ingestion.queue`；死信 `ingestion.dlx → ingestion.dlq`；**幂等靠 document 状态机去重**。

### 3.2 检索问答（retrieval，每次必检索）

```
RetrievalApi.retrieve
 ① QueryPlanner(LLM) → 多 query (DIRECT / REWRITE / DECOMPOSE)
 ② 双通道并行(线程池): VectorSearchPort(pgvector) ∥ KeywordSearchPort(ES BM25), 各 topK=50
 ③ RrfFusion  score += 1/(60+rank)  合并两路
 ④ 聚簇(同文档相邻 chunk) + 邻居扩窗(±1)
 ⑤ RerankPort(bge-reranker) 精排 → topN
 ⑥ EvidenceGrader → SUFFICIENT / PARTIAL / WEAK / NONE
 → EvidenceBundle (证据 + 分级 + 指导语)
```

### 3.3 Agentic 对话 + 深度思考（agent，模型自主决定是否检索）

```
AgentApi.streamChat
 ① IntentClassifier(LLM) → IntentResult
      CLARIFY(低置信) → 回澄清问题, 结束
 ② 按 intent 路由:
      KNOWLEDGE → RetrievalApi.retrieve → 证据 → 生成(SSE token 流)
      TOOL      → MCP 工具调用(ResultHolder 防重复) → 观察 → 生成
      CHAT      → 直接对话
 ③ 深度思考开关 ON → reasoning 模型 + 多步 loop:
      分解子问题 → 逐个 retrieve → 综合;  thinking 片段 SSE 流式(type=thinking)
 ④ 全程 ConversationApi.appendMessage; platform-core @RagTraceNode 埋点
```

### 3.4 会话记忆（conversation）

```
loadContext(sessionId):   [整体重建, 非增量打补丁]
    compact summary  +  session memory  +  最近 N 条  +  当前问题
appendMessage → MessagePersistedEvent
    └─ 记忆维护: 消息数>20 或 token>8000 → 摘要压缩
                 运行时上下文>阈值 → 运行时压缩
```

## 4. 数据模型

> 隔离约定：`workspace` 即知识库空间 = 隔离单位，**`groupId ≡ workspace_id`**（对标 know-studio 的 group，单层、简单）；所有业务表带 `workspace_id`，检索/SQL 全程过滤。主键 Snowflake。

### 4.1 identity
- `users`(id, user_code, username, email, display_name, password_hash, system_role, status, must_change_password, last_login_at, created_at, updated_at)
- `workspaces`(id, name, description, owner_id, status, created_at, updated_at) — 知识库空间 = groupId
- `workspace_members`(id, workspace_id, user_id, group_role, joined_at) — 成员 + 组内角色
- 会话/token 由 **Sa-Token 管理**（存 Redis），不建 refresh_token 表

### 4.2 knowledge
- `documents`(id, workspace_id, file_name, object_key, file_type, file_size, hash, status[PENDING/PROCESSING/READY/FAILED], preview_text, failure_reason, chunk_count, created_at, processed_at)
- `document_chunks`(id, workspace_id, document_id, chunk_index, chunk_text, char_start, char_end, section_path, status, deleted, created_at)
- `ingestion_jobs`(id, workspace_id, document_id, type, status, node_logs jsonb, started_at, finished_at, error) — 逐节点日志
- `upload_sessions` + `upload_chunks` — 分片 / 断点续传 / 秒传(hash)

### 4.3 向量与关键词索引
- **pgvector** `chunk_embeddings`(chunk_id, workspace_id, document_id, embedding **vector(1024)**, metadata jsonb) — BGE-M3 1024 维，**HNSW + cosine**
- **ES** `arag_document_chunks`(workspace_id, document_id, chunk_id, chunk_index, file_name, chunk_text, status, deleted) — **IK 分词**(index=ik_max_word / search=ik_smart)

### 4.4 conversation
- `sessions`(id, workspace_id, user_id, title, tool_mode, deep_thinking, status, created_at, updated_at)
- `messages`(id, session_id, role[USER/ASSISTANT/TOOL], content, tokens, metadata jsonb[citations/thinking], created_at)
- `session_memory`(id, session_id, compact_summary, session_summary, updated_at)

### 4.5 evaluation
- `eval_datasets`(id, name, description, created_at)
- `eval_samples`(id, dataset_id, question, relevant_chunk_ids jsonb, expected_answer)
- `eval_runs`(id, dataset_id, config[vector_only / hybrid / hybrid_rerank], recall_at_k, hallucination_rate, extra jsonb, created_at)

### 4.6 agent
- 浅层意图用 LLM 分类，**无意图树表**；MCP 工具用配置 + `DefaultMCPToolRegistry` Bean 注册（需动态再加 `mcp_tools` 表，MVP 不建）

## 5. platform-ai：模型路由 / 熔断 / 降级

### 5.1 供应商 SPI
`AiProvider`（能力标签：chat / reasoning / embedding / rerank）→ `DashScopeProvider`（chat/reasoning 云）、`DeepSeekProvider`（reasoning 云）、`OllamaProvider`（embedding/rerank/chat 本地兜底）。每个声明支持能力 + 优先级。**新增供应商 = 加一个实现类**（对扩展开放）。

### 5.2 路由 + 降级链
```
ChatModelRouter.chat(req):
  chain = 按能力+优先级 [DashScope → DeepSeek → Ollama]
  for p in chain:
    if breaker(p).allow():
      try:
        stream = p.chat(req)
        ProbeBuffering: 缓冲首 token 确认流正常 → 输出;  breaker(p).onSuccess(); return
      catch e:
        breaker(p).onFailure();  continue   # 无感切下一个
  → 全失败: 抛可控降级错误(而非裸异常)
```

### 5.3 三态熔断状态机（每 provider 独立）
```
CLOSED ──失败数≥阈值──▶ OPEN ──冷却期到──▶ HALF_OPEN
   ▲                                      │探测成功    │探测失败
   └──────────────────────────────────────┘            ▼
                                                       OPEN
```
- 状态存储：单机 `ConcurrentHashMap`；多实例切 **Redis 共享**（对应 D3 双机）
- 定时健康探测 + `ProbeBuffering` 首包探测 → 切换 provider 时用户无感

### 5.4 本地 / 云分工（D8）
- embedding(BGE-M3) · rerank(bge-reranker) → **本地 Ollama**（离线/免费/稳定）
- chat · reasoning → **云 API 为主**（DashScope/DeepSeek）+ 本地 chat 兜底

## 6. platform-core：Trace / 限流 / 上下文透传

### 6.1 全链路 Trace
- `@RagTraceNode` + `@Aspect`：每标注方法记 traceId/step/耗时/入参出参 → OTel span + 结构化日志
- traceId 存 `UserContext` + MDC；埋点覆盖 检索 / 意图 / 生成 / 工具 各步
- 一次问答 = 一棵 trace 树，出问题精确定位到节点（面试亮点）

### 6.2 限流
- `@RateLimit(key, permits, window)` + Redisson：全局并发 + 用户级
- 超限 → 排队(SSE 推排队状态) 或 拒绝(429 + 可控响应)

### 6.3 上下文跨线程透传
- `UserContext`(userId, workspaceId, traceId) ThreadLocal
- 异步线程池用 `TtlExecutors` 包装 → 异步不丢用户/Trace 上下文
- 专用线程池（入库 / 多路检索 / 流式生成）各配队列与拒绝策略

### 6.4 消息队列（RabbitMQ · Spring AMQP）
- 发送：publisher-confirm 确认投递；消费：手动 ACK + 幂等去重基类
- 拓扑约定：业务 `exchange → queue`；`*.dlx → *.dlq` 死信；TTL + DLX 指数退避重试
- 首个场景：入库（见 3.1）；预留 通知 / 批处理 等异步链路

### 6.5 其他
- `SseEmitterSender` 线程安全 SSE（AtomicBoolean 防断开后写）
- `ApiResponse<T>` 统一返回 + 三级异常体系 + `@RestControllerAdvice`
- Snowflake 分布式 ID

## 7. 容错 · 可观测 · 部署拓扑

### 7.1 容错（多层防御）
| 层 | 机制 |
|---|---|
| 模型 | 三态熔断 + 降级链（platform-ai） |
| 入库 | spring-retry 重试 + 兜底 FAILED + 启动回收僵尸任务 |
| 检索 | 单路失败降级（ES 挂 → 仅向量路继续） |
| 流量 | 全局/用户级限流 |
| 回答 | 证据分级 + 无证据拒答（防幻觉） |

### 7.2 可观测
- 指标：Micrometer → Prometheus → Grafana
- 链路：OpenTelemetry → Jaeger/Tempo（+ 自研 RAG Trace）
- LLM：Langfuse（调用链 / token 成本 / 评测）
- 日志：Logback 结构化 JSON → Loki

### 7.3 部署拓扑
```
Docker Compose:
  postgres(pgvector) · elasticsearch(IK) · minio · redis · rabbitmq · ollama · backend  [前端后置]
单机 all-in-one   |   双机拆分: [DB/模型机] + [应用机]
```

## 8. 关键技术权衡

| 决策 | 选择 | 理由 |
|---|---|---|
| 模块化单体 vs 微服务 | **模块化单体** | 运维简单、事务一致、够用；垂直切片 + ArchUnit 预留拆分路径 |
| 逻辑 vs 物理隔离 | **逻辑隔离**（workspace_id 过滤） | 成本低、够中小规模；不做 schema-per-tenant |
| 自研编排 vs 框架高层 Agent | **自研核心编排** + Spring AI 作模型底座 | 可控 / 可观测 / 可测；不被框架黑盒绑架 |
| RRF vs 加权融合 | **RRF** `1/(60+rank)` | 跨打分尺度鲁棒、无需调权重 |
| 规则 vs LLM 判证据 | **规则分级** | 可解释、可审计、零额外成本 |
| 熔断状态存储 | 内存起步 → **Redis**（多实例） | 单机够快，双机共享一致 |
| 消息队列 | **RabbitMQ**（不上 Kafka） | 入库解耦/削峰 + 死信重试；比 Kafka 轻、面试可深挖；轻量通知仍用 Spring Event |
| 数据访问 | **MyBatis-Plus + Mapper XML** | 标准 CRUD 使用 MP 降低模板代码；pgvector/JSONB/状态机/批量写入保留显式 SQL；不启用全局租户插件，所有业务查询显式携带 `workspace_id` |
