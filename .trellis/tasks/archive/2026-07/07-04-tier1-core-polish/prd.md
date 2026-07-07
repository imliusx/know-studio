# PRD：第一梯队核心补强（父任务）

## 背景

项目核心 RAG 链路（入库、混合检索、证据问答、助手）已可用，但存在三类影响演示与使用体验的缺口：

1. QA 问答接口 `/api/qa/ask` 为同步阻塞，用户需等待完整回答；assistant 模块已有 SSE 流式基建未被复用。
2. 文档入库是多阶段流水线，但各阶段状态对用户不可见，失败无反馈无重试；管理后台 `data-channels/pipelines`、`data-channels/tasks` 两页仅为 "Coming Soon" 占位。
3. 前端存在 mock 数据兜底（dashboard、documents 页）与 shadcn-admin 模板遗留页面（tasks/apps/chats/help-center 等），演示时会暴露假数据。

## 任务地图

| 子任务 | 目录 | 交付物 | 类型 |
|---|---|---|---|
| QA 问答流式输出 | 07-04-qa-streaming | `/api/qa/ask` 流式版本 + 前端 QA 页流式渲染 | 复杂（prd+design+implement） |
| 入库任务状态可视化 | 07-04-ingestion-status-ui | 入库阶段状态落库 + 查询/重试 API + pipelines/tasks 真实页面 | 复杂（prd+design+implement） |
| 清理 mock 与模板遗留 | 07-04-mock-cleanup | 删除/接真清单落地，无假数据残留 | 轻量（PRD-only） |

## 执行顺序

mock-cleanup 与其余两项无代码耦合，可任意顺序；建议顺序：qa-streaming → ingestion-status-ui → mock-cleanup（前两者可能新增页面/导航，清理放最后避免返工）。子任务间无阻塞依赖。

## 跨子任务验收标准

- [ ] 三个子任务各自验收标准全部通过（见各子 prd.md）。
- [ ] `cd know-studio-ui && pnpm lint && pnpm build` 通过。
- [ ] `mvn -q -DskipTests package` 通过。
- [ ] 前端不再存在任何渲染 mock 业务数据的路径（输入框 placeholder 提示文案除外）。
- [ ] 管理后台导航中不再有点开为 "Coming Soon" 的 pipelines/tasks 页（intent、keywords、traces 等本梯队未覆盖的占位页在导航中处理方式见 mock-cleanup PRD）。

## 非目标

- Rerank、检索评测、更多文档格式解析（第二梯队）。
- intent 意图管理、语音输入（第三梯队）。
