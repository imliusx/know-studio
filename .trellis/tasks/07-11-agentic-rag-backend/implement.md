# Agentic RAG 平台后端 · 执行计划（implement.md）

> 配套 `prd.md` + `design.md`。把 D10 五阶段拆成有序 checklist + 验证命令 + 回滚点。
> **每阶段收口标准**：全模块编译通过 + ArchUnit 通过 + 该阶段功能自测通过，才进入下一阶段；每阶段独立 git commit 作为回滚点。

## 通用验证命令

```bash
# 起依赖
docker compose up -d postgres elasticsearch minio redis rabbitmq ollama
# 全模块编译
mvn -q clean compile
# 模块边界校验
mvn -q test -pl bootstrap -Dtest='ArchUnit*'
# 启动
mvn -q -pl bootstrap spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 阶段 0 · 项目初始化（地基前置）

- [x] Maven 多模块骨架：父 pom（packaging=pom）+ 9 子模块 pom
- [x] 父 pom：Spring Boot 3.x parent、Java 21、dependencyManagement（Spring AI BOM / Sa-Token / MCP SDK / Redisson / AMQP）
- [x] 各模块空壳 + 包结构 `know.studio.arag.*`
- [x] `docker-compose.yml`：postgres(pgvector) / es8+ik / minio / redis / rabbitmq / ollama
- [x] bootstrap 空 Spring Boot 可启动
- **验证**：`mvn clean compile` 通过；`docker compose up -d` 依赖健康；应用起得来
- **回滚点**：骨架独立提交

## 阶段 1 · 地基（platform-core / platform-ai / identity）

- [x] platform-core：ApiResponse / 三级异常 + 全局拦截 / Snowflake / UserContext + TtlExecutors
- [x] platform-core：SseEmitterSender / @RateLimit(Redisson) / **mq 封装**(发送确认 + 消费基类 + 死信) / @RagTraceNode(AOP+OTel)
- [x] platform-ai：`AiProvider` SPI + DashScope / DeepSeek / Ollama provider
- [x] platform-ai：ChatModelRouter + **三态熔断** + 降级链 + 首包探测 + embedding/rerank client
- [x] module-identity：users/workspaces/workspace_members 表(Flyway) + Sa-Token 鉴权 + RBAC + `IdentityApi`
- [x] ArchUnit 规则落地（依赖方向约束）
- **验证**：登录/鉴权/越权自测；模型路由熔断单测（模拟 provider 故障→降级）；ArchUnit 通过
- **回滚点**：阶段提交

## 阶段 2 · 入库（knowledge + RabbitMQ）

- [x] documents / document_chunks / ingestion_jobs / upload_* 表(Flyway)
- [x] 文档上传（分片 / 断点续传 / 秒传 hash）+ MinIO 存储
- [x] RabbitMQ：ingestion exchange/queue/dlx/dlq 声明 + producer(publisher-confirm)
- [ ] IngestionPipeline 节点编排：Parse(pdf/docx/txt/md) → Clean → Chunk(结构感知) → Embed → pgvector 写 + ES 索引写
- [ ] 消费者：手动 ACK + 幂等(状态机去重) + 死信退避重试 + 启动回收僵尸任务
- **当前进度**：消费者手动 ACK、三级 TTL 退避、最终 DLQ 和事务提交后投递已完成；待 Pipeline 接入状态机幂等与僵尸任务回收。
- **验证**：传 pdf → 消费入库 → document READY，chunk 落库、pgvector/ES 有数据；模拟失败进 dlq
- **回滚点**：阶段提交

## 阶段 3 · 检索（retrieval + rerank + 证据）

- [ ] VectorSearchPort(pgvector) + KeywordSearchPort(ES BM25) adapter
- [ ] QueryPlanner(LLM 查询规划) + 双通道并行召回
- [ ] RrfFusion + 聚簇 + 邻居扩窗
- [ ] RerankPort(bge-reranker) 精排
- [ ] EvidenceGrader 证据分级 + `RetrievalApi` + Controller
- **验证**：检索 API 返回 EvidenceBundle（RRF+rerank 排序 + 分级）；无证据拒答
- **回滚点**：阶段提交

## 阶段 4 · Agentic（agent + conversation）

- [ ] sessions / messages / session_memory 表 + `ConversationApi` + 记忆整体重建 + 摘要压缩
- [ ] IntentClassifier(LLM 浅层意图) + 路由(知识/工具/闲聊/澄清)
- [ ] Agent 编排 + KB 检索工具(复用 retrieval) + ResultHolder 防重复守卫
- [ ] MCP client 接入（Web 搜索 + mock 业务工具）
- [ ] 深度思考（开关 + reasoning 模型 + 多步 loop + thinking 流式）
- [ ] SSE 流式 `streamChat` + `AgentApi`
- **验证**：多轮对话 / 意图路由 / 工具调用 / 深度思考开关 / SSE 流式；长会话触发摘要压缩
- **回滚点**：阶段提交

## 阶段 5 · 生产支柱补全（evaluation + 可观测）

- [ ] 限流接入各入口（检索/chat）
- [ ] Trace 埋点覆盖全链路 + OTel 导出
- [ ] Prometheus / Grafana + Langfuse 接入
- [ ] evaluation：数据集/样本表 + 消融对比(仅向量 / 混合 / +rerank) + Recall@K 输出
- [ ] OpenAPI / knife4j 文档
- **验证**：跑评测集出消融对比数字；Trace/指标可见；限流生效；容错测试（模型熔断降级不中断）
- **回滚点**：阶段提交

---

## 风险点 / 回滚

- 每阶段独立 git commit，可回退到上一阶段
- **高风险项**：
  - platform-ai 熔断降级 + 流式首包探测（涉及 SSE，充分单测）
  - 入库消费幂等（防重复入库，靠 document 状态机 + 死信）
  - ArchUnit 边界（阶段 1 就引入，避免后期模块乱依赖大改）
  - Spring AI / Spring AI Alibaba / MCP SDK 版本兼容（阶段 0 先小验证）

## `task.py start` 前检查

- [x] prd / design / implement 三件套用户 review 通过
- [x] 依赖版本确认（Spring Boot、Spring AI、Sa-Token、MCP SDK 取最新 GA）
- [x] docker-compose 依赖可一键起、健康检查通过
