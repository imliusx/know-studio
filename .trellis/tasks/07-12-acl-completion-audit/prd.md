# ACL completion audit gaps

## Goal

Close Team Admin navigation, secure citation download, persisted citation metadata, and final visual verification gaps found after completion audit.

## Requirements

- Team Admin 或拥有可管理知识库的用户能看到对应管理入口；普通 MEMBER 不显示管理入口。
- SSE 与持久化消息中的引用必须包含 knowledgeBaseId、documentId、chunkId、chunkIndex、fileName、score 和 snippet。
- 提供按 knowledgeBaseId + documentId 授权的原文下载接口，失权后返回 403。
- Chat 引用可触发受保护的原文下载，不通过公开对象 URL 绕过鉴权。
- 所有 Snowflake 实体 ID 必须以字符串跨越浏览器 API/SSE 边界，禁止 JavaScript `number` 精度丢失导致合法会话返回不存在。
- 保留全部现有正式导航和功能，仅调整管理入口可见性。
- 普通聊天页 Logo 区域保持静态品牌展示，不显示或切换管理员知识库列表。
- 管理页侧栏同样保持静态品牌展示；知识库选择必须位于具体管理页面的业务操作区。
- 短事实问答必须围绕当前问题：单一意图查询保留原问题，生成证据不得串联成整章噪声，明确规则问题可直接抽取原文。
- 完成桌面与移动端浏览器验收；若运行环境仍无浏览器实例，保留明确的未完成证据，不得宣称全部完成。

## Acceptance Criteria

- [x] 普通 MEMBER 的侧栏不显示知识库、评测、团队授权等管理入口。
- [x] Team Admin 能进入团队成员管理；具备 MANAGE 的 Team Admin 能进入文档和评测管理。
- [x] 新旧会话引用均能被前端解析并显示完整摘要。
- [x] 授权用户可下载原文，失权用户和无权用户收到 403。
- [x] 后端测试、前端 lint/typecheck/build、E2E 和 diff check 通过。
- [x] 新建会话、历史引用和 SSE 使用完整字符串 ID，实际对话不再返回“会话不存在”。
- [x] 聊天侧栏恢复静态 KnowStudio Logo，不暴露知识库管理选择器。
- [x] 管理侧栏恢复静态 Logo，评测页面提供独立的可管理知识库选择器。
- [x] “Java 类名如何命名？”真实问答直接返回 UpperCamelCase、例外和正反例，不再概括整本手册或混入方法/变量规则。
- [ ] 桌面与移动端截图和交互验收完成。

## Notes

- 2026-07-12 final visual verification retry: backend `/actuator/health` returned `UP` and frontend `http://127.0.0.1:5174` returned HTTP 200, but the approved in-app browser runtime returned `No browser is available`; runtime discovery returned an empty browser list (`[]`). Desktop/mobile screenshot acceptance remains blocked and incomplete.
- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
