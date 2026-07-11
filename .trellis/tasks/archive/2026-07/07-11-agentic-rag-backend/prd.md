# Agentic RAG 平台后端（全新构建）

## Goal

从 0 构建一个全新的 Agentic RAG 平台后端（Java + Spring AI），对标 nageoffer/ragent，覆盖文档入库、多路检索、意图识别、Agent 工具调用（含 MCP）、深度思考、会话记忆，并具备模型路由容错、限流、可观测等生产级工程支柱。**完全新设计，不复用 know-studio 任何代码**（know-studio 仅作对照参考）。

定位：生产级工程深度 · 求职向——生产支柱做到可面试深挖，但按中小规模落地，不过度工程。

## Confirmed Facts

来自前期讨论：
- 技术栈：Java 21 + Spring Boot 3.x + Spring AI（Spring AI 作模型调用底座，检索/意图/Agent 核心编排自研）
- 产品形态：Agentic RAG 平台（RAG + 意图识别 + 多工具 + MCP + 深度思考）
- 资产处理：完全推倒重来，不复用 know-studio 代码
- 技术选型基线：多供应商模型路由+三态熔断、BGE-M3 embedding、bge-reranker 重排、pgvector 向量、ES8+IK 关键词、MinIO、PostgreSQL、Redis+Redisson、RabbitMQ（入库异步/死信重试）、Sa-Token、MCP client、Micrometer+OpenTelemetry+Langfuse；Maven 多模块（结构原创设计，见 D11）

来自仓库证据：
- `docs/resume-experience.md`：know-studio 是"个人主导实战项目（非企业生产、无线上流量）"，配套简历经历文档
- `docs/interview.md`：配套面试讲解话术（STAR/追问应对）
- → 项目具备明确的**求职/简历属性**；对标的 ragent 定位"后端转 AI 面试第一站"
- `.trellis/spec/backend/*`：目前为空模板（待填）

## Decisions（brainstorm 已定）

- **D1 架构重量**：生产级工程深度 · 求职向。规模按单机/双机 + 自建评测集落地；模型路由/熔断/限流/可观测/Agentic 全做到可深挖；不上 K8s / Kafka / 真 SaaS 多租户。下游收敛：向量库用 pgvector、消息队列用 RabbitMQ（入库异步解耦/削峰/死信重试；不上 Kafka）、部署走 Docker Compose。
- **D2 MVP 范围**：广覆盖 · 每块最小完整。ragent 核心亮点全纳入，每块做"可深挖的最小完整实现 + 一个真亮点"，不追功能丰富度。
- **D3 规模/部署（D1 推论，可调）**：单知识库万级文档、百万级向量 chunk、日活百级、峰值几十 QPS；Docker Compose（单机 all-in-one + 双机拆分示例）。
- **D4 多租户/权限**：多用户 + 知识库(工作空间)分组 + RBAC；`tenantId/groupId` 全程过滤的逻辑隔离；Sa-Token 鉴权（系统角色 + 组内角色）；不做物理隔离。
- **D5 意图/MCP**：意图识别浅层分类（知识检索 / 工具调用 / 闲聊 / 低置信度澄清）+ 主动澄清；MCP 接 1-2 个真实工具（Web 搜索 + 自建 mock 业务工具），覆盖"外部 API 类 + 内部业务类"；重整链路可深挖。
- **D6 深度思考**：用户开关触发；开启走 reasoning 模型 + 多步 agentic loop（问题分解 → 多轮检索 → 综合）+ thinking 流式；关闭走快答。
- **D7 rerank + 评测**：均纳入 MVP。rerank（bge-reranker）多路召回后精排；自建评测集（50-200 条标注）跑消融对比（仅向量 / 混合 / +rerank），作为简历量化数字来源。
- **D8 模型来源**：混合。Embedding（BGE-M3）/ Rerank 走本地 Ollama；Chat + Reasoning 走云 API 为主 + 本地兜底（承载多供应商路由+熔断降级亮点）。
- **D9 前端**：本 task 只做后端；交付完整 REST API + OpenAPI/knife4j + SSE；前端后置（另开 task）。
- **D10 实现顺序**：自底向上 5 阶段（① 地基 → ② 入库 → ③ 检索+rerank+证据 → ④ Agentic → ⑤ 生产支柱）；细化留 implement.md。
- **D11 模块化范式**：**模块化单体（业务垂直切片）**，不照搬 ragent 的水平技术分层。
  - 业务模块（每模块内部 `api / domain / infra` 自含）：`identity`（用户/租户/权限）、`knowledge`（知识库/文档/入库）、`retrieval`（多路检索/RRF/rerank/证据分级）、`agent`（意图/编排/MCP/深思）、`conversation`（会话/记忆）、`evaluation`（评测）
  - 横切平台模块：`platform-core`（响应/异常/Trace/限流/SSE/上下文透传/ID）、`platform-ai`（模型供应商适配/路由/熔断/embedding/rerank client）
  - 启动：`bootstrap`（配置聚合/启动/组装）
  - 依赖规则：业务模块 → platform-*（单向）；业务模块间只经暴露的 API / 领域事件；用**约定 + ArchUnit 测试**校验边界（不引入 Spring Modulith）

## Requirements

### 功能需求

- **identity**：注册/登录（Sa-Token）、用户管理、知识库(工作空间)创建与成员管理、系统角色 + 组内角色 RBAC、`tenantId/groupId` 逻辑隔离贯穿全链路
- **knowledge**：知识库 CRUD、文档上传（分片 / 断点续传 / 秒传 hash 判定）、MinIO 原文存储、**节点编排入库 Pipeline**（解析 → 清洗 → 结构化切片 → pgvector 向量化 + ES 索引双写）、入库状态机 + 逐节点日志 + 启动回收卡死任务
- **retrieval**：多路召回（pgvector 语义 + ES BM25 关键词）、查询改写/规划、**RRF 融合**、聚簇 + 邻居扩窗、**bge-reranker 精排**、**证据分级 + 拒答**
- **agent**：浅层意图路由（知识/工具/闲聊/澄清）、Agent 编排、**MCP 工具接入**（Web 搜索 + mock 业务工具）、工具防重复调用 + 防越权（context 透传 groupId）、**深度思考模式**（开关 → reasoning + 多步 loop + thinking 流式）
- **conversation**：多轮会话、消息持久化、短期记忆重建、摘要压缩、**SSE 流式输出**
- **evaluation**：自建评测集管理、消融对比（仅向量 / 混合 / +rerank）、Recall@K 等指标输出

### 非功能需求（生产支柱）

- **platform-ai**：多供应商模型路由 + **三态熔断**（CLOSED/OPEN/HALF_OPEN）+ 优先级降级链 + 首包探测；embedding/rerank 本地、chat/reasoning 云
- **platform-core**：统一响应 / 三级异常体系 + 全局拦截、**全链路 Trace**（AOP + OpenTelemetry，每步耗时/输入输出）、**限流**（Redis+Redisson，全局 + 用户级）、SSE 线程安全封装、用户/Trace 上下文跨线程透传、分布式 ID
- **可观测**：Micrometer + Prometheus + Grafana；OTel 链路；Langfuse LLM 观测（调用链/成本）
- **部署**：Docker Compose（单机 all-in-one + 双机拆分示例）；依赖 PostgreSQL(pgvector) / ES8+IK / MinIO / Redis / Ollama
- **架构**：模块化单体（见 D11），ArchUnit 校验模块依赖边界

## Acceptance Criteria

- [ ] 多模块工程编译通过；ArchUnit 测试通过（业务模块→platform 单向，业务模块间不直接依赖内部实现）
- [ ] 用户可注册/登录；非本组用户访问他组知识库被拒（隔离测试通过）
- [ ] 文档上传 → 入库 → READY 全链路跑通；支持分片/断点续传/秒传；入库失败可恢复、逐节点日志可查
- [ ] 检索结果经 RRF + rerank 排序并带证据分级；无有效证据时拒答（不编造）
- [ ] 意图路由能区分 知识/工具/闲聊/澄清；MCP 工具可被 Agent 自主调用并返回结果
- [ ] 深度思考开关生效：开启走 reasoning + 多步 loop、thinking 流式；关闭走快答
- [ ] 多轮会话记忆生效；长会话触发摘要压缩，单轮输入受控
- [ ] 某模型供应商故障时自动熔断并降级到备用，问答不中断（容错测试通过）
- [ ] 限流生效（超阈值排队/拒绝）；全链路 Trace 可见每步耗时与输入输出
- [ ] 评测集可跑消融对比并输出 Recall@K 等指标（仅向量 vs 混合 vs +rerank）
- [ ] 提供 OpenAPI/knife4j 文档；SSE 流式接口可用

## Out of Scope

- 复用 know-studio 任何代码
- 前端（后置，另开 task）；本 task 只交付后端 API
- K8s / Kafka / 真 SaaS 物理多租户隔离（分库/schema per tenant）
- 模型微调 / 训练（仅调用 + 编排）

## Open Questions

（brainstorm 主干决策 D1–D11 已定，无 blocking 项；实现级细节留 design.md / implement.md 展开）
