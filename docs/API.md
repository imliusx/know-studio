# know-studio 接口清单

> 从后端 13 个 Controller、约 41 个端点提取。作为前端对接的唯一依据。
>
> 生成依据：`src/main/java/com/dong/ddrag/**/controller/*.java` + 对应 `model/dto`、`model/vo`。

---

## 0. 通用约定

### 0.1 统一返回结构 `ApiResponse<T>`

绝大多数接口返回该包装。`record ApiResponse<T>(boolean success, T data, String message)`。

```jsonc
{
  "success": true,        // 业务是否成功
  "data": { },            // 成功时的业务数据；无数据时为 null
  "message": null         // 失败时的提示信息；成功时为 null
}
```

> ⚠️ **例外**：以下接口**直接返回业务对象**，不包 `ApiResponse`（前端解析时需区分）：
> - `GET /api/groups/my` → `GroupQueryResult`
> - `GET /api/groups/{groupId}/members` → `GroupMemberVO[]`
> - `GET /api/documents` → `DocumentListItemVO[]`
> - `GET /api/documents/{documentId}/preview` → `DocumentPreviewVO`
> - `GET /api/assistant/sessions` → `AssistantSessionListItemVO[]`
> - `GET /api/assistant/sessions/{sessionId}` → `AssistantSessionDetailVO`
> - `GET /api/assistant/sessions/{sessionId}/context` → `AssistantConversationContextVO`
> - `POST /api/qa/ask` → `AskQuestionResponse`
> - `POST /api/assistant/chat/stream` → SSE 事件流（`text/event-stream`）

### 0.2 鉴权方式

- **Cookie 鉴权**：所有请求需 `withCredentials: true`。
- **Access Token**：登录/刷新成功后，`accessToken` 在响应体 `data` 中返回，由前端自行存储（如内存/localStorage）并按后端要求携带（参考现有 `frontend/src/api/http.ts`）。
- **Refresh Token**：由后端通过 **HttpOnly Cookie** 写入/清除（`refresh` 写、`logout` 清），前端不直接读取，调用 `refresh`/`logout` 时后端自动从 Cookie 取。
- **权限模型**：系统角色 `ADMIN / USER`；组内角色 `OWNER / MEMBER`。标注 `🔑 ADMIN` 的接口要求系统管理员；标注 `🔑 组权限` 的接口要求当前用户是该组成员。

### 0.3 通用错误

- 参数校验失败：HTTP 400，`success=false`，`message` 含校验提示。
- 未登录/鉴权失败：HTTP 401。
- 权限不足：HTTP 403。
- 其余业务异常：HTTP 200 但 `success=false`，或对应 HTTP 状态码，`message` 为原因。

---

## 1. 认证 Auth — `/api/auth`

### 1.1 登录
`POST /api/auth/login`

- Body：`LoginRequest`
  | 字段 | 类型 | 必填 | 说明 |
  |---|---|---|---|
  | loginId | String | 是 (@NotBlank) | 用户名或邮箱 |
  | password | String | 是 (@NotBlank) | 密码 |
- 返回：`ApiResponse<AuthTokensResponse>`
  - 后端会同时写 Refresh Token Cookie。
  - `AuthTokensResponse`
    | 字段 | 类型 |
    |---|---|
    | accessToken | String |
    | currentUser | CurrentUserProfileResponse |

### 1.2 注册
`POST /api/auth/register`

- Body：`RegisterRequest`
  | 字段 | 类型 | 必填 | 约束 |
  |---|---|---|---|
  | username | String | 是 | max 64 |
  | email | String | 是 | 合法邮箱，max 128 |
  | displayName | String | 是 | max 128 |
  | password | String | 是 | max 256 |
- 返回：`ApiResponse<Void>`

### 1.3 刷新 Token
`POST /api/auth/refresh`

- Body：无（Refresh Token 从 Cookie 读取）
- 返回：`ApiResponse<AuthTokensResponse>`（结构同登录）

### 1.4 登出
`POST /api/auth/logout`

- Body：无
- 后端：撤销 Refresh Token 并清除 Cookie。
- 返回：`ApiResponse<Void>`

### 1.5 当前用户信息
`GET /api/auth/me`

- 返回：`ApiResponse<CurrentUserProfileResponse>`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | userId | Long | |
  | userCode | String | |
  | displayName | String | |
  | systemRole | enum | `ADMIN` / `USER` |
  | mustChangePassword | boolean | 首次登录强制改密标志 |

---

## 2. 账号 Account — `/api/account`

### 2.1 修改密码
`POST /api/account/change-password` · 🔑 已登录

- Body：`ChangePasswordRequest`
  | 字段 | 类型 | 必填 |
  |---|---|---|
  | currentPassword | String | 是 (@NotBlank) |
  | newPassword | String | 是 (@NotBlank) |
- 返回：`ApiResponse<Void>`

---

## 3. 用户管理（后台）Admin Users — `/api/admin/users` · 🔑 ADMIN

### 3.1 用户列表
`GET /api/admin/users`

- 返回：`ApiResponse<List<AdminUserItemResponse>>`
  | 字段 | 类型 |
  |---|---|
  | userId | Long |
  | userCode | String |
  | username | String |
  | email | String |
  | displayName | String |
  | systemRole | enum (`ADMIN`/`USER`) |
  | status | enum (`UserStatus`) |
  | mustChangePassword | boolean |
  | lastLoginAt | LocalDateTime |

### 3.2 用户详情
`GET /api/admin/users/{userId}`

- Path：`userId: Long`
- 返回：`ApiResponse<AdminUserItemResponse>`

### 3.3 修改用户状态
`PATCH /api/admin/users/{userId}/status`

- Path：`userId: Long`
- Body：`UpdateUserStatusRequest`
  | 字段 | 类型 | 必填 |
  |---|---|---|
  | status | enum (`UserStatus`) | 是 (@NotNull) |
- 返回：`ApiResponse<Void>`

> 备注：存在 `ResetUserPasswordRequest` DTO，但当前未在任何 Controller 暴露端点。

---

## 4. 协作小组 Group — `/api/groups`

### 4.1 我可见的组
`GET /api/groups/my`

- 返回（**裸对象，非 ApiResponse**）：`GroupQueryResult`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | ownedGroups | VisibleGroup[] | 我作为 OWNER 的组 |
  | joinedGroups | VisibleGroup[] | 我作为 MEMBER 加入的组 |
  | pendingInvitations | PendingInvitation[] | 待处理的邀请 |

  `VisibleGroup`：`groupId: Long`、`groupCode: String`、`groupName: String`

  `PendingInvitation`：`invitationId`、`groupId`、`groupName`、`inviterUserId`、`inviterDisplayName`、`status: String`

### 4.2 创建组
`POST /api/groups`

- Body：`CreateGroupRequest`
  | 字段 | 类型 | 必填 | 约束 |
  |---|---|---|---|
  | name | String | 是 | max 128 |
  | description | String | 否 | max 512 |
- 返回：`ApiResponse<Long>`（新 groupId）

### 4.3 组成员列表
`GET /api/groups/{groupId}/members` · 🔑 组权限

- Path：`groupId: Long`
- 返回（**裸数组**）：`GroupMemberVO[]`
  | 字段 | 类型 |
  |---|---|
  | userId | Long |
  | userCode | String |
  | displayName | String |
  | role | String (`OWNER`/`MEMBER`) |

### 4.4 移除成员
`DELETE /api/groups/{groupId}/members/{userId}` · 🔑 OWNER

- Path：`groupId: Long`、`userId: Long`
- 返回：`ApiResponse<Void>`

### 4.5 退出组
`POST /api/groups/{groupId}/leave`

- Path：`groupId: Long`
- 返回：`ApiResponse<Void>`

### 4.6 创建邀请
`POST /api/groups/{groupId}/invitations` · 🔑 OWNER

- Path：`groupId: Long`
- Body：`CreateInvitationRequest`
  | 字段 | 类型 | 必填 |
  |---|---|---|
  | inviteeUserId | Long | 是 (@NotNull, @Positive) |
- 返回：`ApiResponse<Long>`（invitationId）

---

## 5. 小组邀请决策 Invitation — `/api/invitations`

### 5.1 接受邀请
`POST /api/invitations/{invitationId}/accept`

- 返回：`ApiResponse<Void>`

### 5.2 拒绝邀请
`POST /api/invitations/{invitationId}/reject`

- 返回：`ApiResponse<Void>`

### 5.3 取消邀请
`POST /api/invitations/{invitationId}/cancel`

- 返回：`ApiResponse<Void>`

---

## 6. 加入申请 Join Request — `/api/groups`（前缀与组接口共用）

### 6.1 提交加入申请
`POST /api/groups/join-requests`

- Body：`CreateJoinRequestRequest`
  | 字段 | 类型 | 必填 | 约束 |
  |---|---|---|---|
  | groupCode | String | 是 | max 80 |
- 返回：`ApiResponse<Long>`（requestId）

### 6.2 我的加入申请
`GET /api/groups/join-requests/my`

- 返回：`ApiResponse<List<MyJoinRequestVO>>`
  | 字段 | 类型 |
  |---|---|
  | requestId | Long |
  | groupId | Long |
  | groupCode | String |
  | groupName | String |
  | status | String |
  | createdAt | LocalDateTime |
  | decidedAt | LocalDateTime |

### 6.3 组内收到的加入申请（OWNER 视角）
`GET /api/groups/{groupId}/join-requests` · 🔑 OWNER

- 返回：`ApiResponse<List<OwnerJoinRequestVO>>`
  | 字段 | 类型 |
  |---|---|
  | requestId | Long |
  | groupId | Long |
  | applicantUserId | Long |
  | applicantUserCode | String |
  | applicantDisplayName | String |
  | status | String |
  | createdAt | LocalDateTime |

### 6.4 批准加入申请
`POST /api/groups/{groupId}/join-requests/{requestId}/approve` · 🔑 OWNER

- 返回：`ApiResponse<Void>`

### 6.5 拒绝加入申请
`POST /api/groups/{groupId}/join-requests/{requestId}/reject` · 🔑 OWNER

- 返回：`ApiResponse<Void>`

---

## 7. 文档 Document — `/api/documents`

> 支持两种上传方式：① 分片上传（init → chunks → complete，支持断点续传/秒传）；② 整文件直传。

### 7.1 初始化分片上传
`POST /api/documents/upload/init` · `Content-Type: application/json` · 🔑 组权限

- Body：`UploadInitRequest`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | groupId | Long | 目标组 |
  | fileName | String | |
  | fileSize | Long | |
  | contentType | String | |
  | fileHash | String | 整文件哈希，用于秒传判断 |
  | chunkSize | Long | 单片大小 |
  | chunkCount | Integer | 总片数 |
- 返回：`ApiResponse<UploadInitResponse>`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | instantUpload | boolean | true=秒传命中，无需再传分片 |
  | documentId | Long | 秒传时返回 |
  | uploadId | String | 非秒传时返回的上传会话 ID |
  | uploadedChunks | Integer[] | 已上传分片序号（断点续传） |
  | chunkSize | Long | |
  | chunkCount | Integer | |

### 7.2 上传单个分片
`POST /api/documents/upload/chunks` · `Content-Type: multipart/form-data` · 🔑 组权限

- Form：`UploadChunkRequest`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | uploadId | String | 上传会话 ID |
  | chunkIndex | Integer | 分片序号 |
  | chunkHash | String | 分片哈希 |
  | chunk | MultipartFile | 分片二进制 |
- 返回：`ApiResponse<UploadStatusResponse>`
  | 字段 | 类型 |
  |---|---|
  | status | String |
  | uploadedChunks | Integer[] |
  | uploadedChunkCount | Integer |
  | chunkCount | Integer |

### 7.3 查询上传状态（断点续传）
`GET /api/documents/upload/{uploadId}`

- Path：`uploadId: String`
- 返回：`ApiResponse<UploadStatusResponse>`

### 7.4 完成上传（触发 ETL）
`POST /api/documents/upload/{uploadId}/complete`

- Path：`uploadId: String`
- 返回：`ApiResponse<Long>`（documentId；ETTL 异步执行，状态需轮询/刷新）

### 7.5 整文件直传（兼容旧链路）
`POST /api/documents/upload` · `Content-Type: multipart/form-data`

- Form：`UploadDocumentRequest`
  | 字段 | 类型 |
  |---|---|
  | groupId | Long |
  | file | MultipartFile |
- 返回：`ApiResponse<Long>`（documentId）

### 7.6 文档列表
`GET /api/documents` · 🔑 组权限

- Query（`DocumentQuery`，均可选）：
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | groupId | Long | |
  | groupRelation | String | |
  | fileName | String | |
  | uploaderUserId | Long | |
  | status | String | |
  | uploadedFrom | LocalDateTime (ISO) | |
  | uploadedTo | LocalDateTime (ISO) | |
- 返回（**裸数组**）：`DocumentListItemVO[]`
  | 字段 | 类型 |
  |---|---|
  | documentId | Long |
  | groupId | Long |
  | fileName | String |
  | fileExt | String |
  | contentType | String |
  | fileSize | Long |
  | status | String |
  | failureReason | String |
  | uploadedAt | LocalDateTime |
  | uploaderUserId | Long |
  | uploaderUserCode | String |
  | uploaderDisplayName | String |
  | previewText | String |

### 7.7 删除文档（软删）
`DELETE /api/documents/{documentId}?groupId={groupId}` · 🔑 组权限

- Path：`documentId: Long`；Query：`groupId: Long`
- 返回：`ApiResponse<Void>`

### 7.8 重试入库（针对 FAILED 文档）
`POST /api/documents/{documentId}/retry-ingestion?groupId={groupId}` · 🔑 组权限

- Path：`documentId: Long`；Query：`groupId: Long`
- 返回：`ApiResponse<Void>`

### 7.9 文档预览
`GET /api/documents/{documentId}/preview?groupId={groupId}` · 🔑 组权限

- Path：`documentId: Long`；Query：`groupId: Long`
- 返回（**裸对象**）：`DocumentPreviewVO`
  | 字段 | 类型 |
  |---|---|
  | documentId | Long |
  | fileName | String |
  | previewText | String |

---

## 8. 问答 QA — `/api/qa`

### 8.1 提问
`POST /api/qa/ask` · 🔑 组权限

- Body：`AskQuestionRequest`
  | 字段 | 类型 | 必填 | 约束 |
  |---|---|---|---|
  | groupId | Long | 是 (@NotNull, @Positive) | |
  | question | String | 是 (@NotBlank) | max 2000 |
- 返回（**裸对象**）：`AskQuestionResponse`（`@JsonInclude(NON_NULL)`，空值字段不输出）
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | answered | boolean | 是否给出了答案（false=证据不足拒答） |
  | answer | String | 答案正文 |
  | reasonCode | String | 拒答时的原因码 |
  | reasonMessage | String | 拒答时的原因说明 |
  | citations | Citation[] | 引用证据列表 |

  `Citation`
  | 字段 | 类型 |
  |---|---|
  | documentId | Long |
  | chunkId | Long |
  | chunkIndex | Integer |
  | fileName | String |
  | score | double |
  | snippet | String |

---

## 9. AI 助手 Assistant — `/api/assistant`

### 9.1 普通对话（一次性返回）
`POST /api/assistant/chat`

- Body：`AssistantChatRequest`
  | 字段 | 类型 | 必填 | 说明 |
  |---|---|---|---|
  | sessionId | Long | 是 (@NotNull, @Positive) | 会话 ID |
  | message | String | 是 (@NotBlank, max 4000) | 用户消息 |
  | toolMode | enum | 是 (@NotNull) | `CHAT` 直接对话 / `KB_SEARCH` 走知识库检索工具 |
  | groupId | Long | 否 (@Positive) | 知识库检索时的目标组 |
- 返回：`ApiResponse<AssistantChatResponse>`
  | 字段 | 类型 |
  |---|---|
  | sessionId | Long |
  | messageId | Long |
  | reply | String |
  | toolMode | enum (`CHAT`/`KB_SEARCH`) |
  | groupId | Long |
  | citations | AskQuestionResponse.Citation[] |

### 9.2 流式对话（SSE）⭐ 前端重点
`POST /api/assistant/chat/stream` · `Content-Type: application/json` → 响应 `text/event-stream`

- Body：同 9.1 `AssistantChatRequest`
- 响应：`SseEmitter` 事件流，每条事件的 `event` 名即 SSE event name，`data` 为 `AssistantChatStreamEvent`。
- **事件类型**（4 种）：

  | event 名 | 触发时机 | 关键 data 字段 |
  |---|---|---|
  | `start` | 流开始 | `sessionId`、`toolMode`、`groupId` |
  | `delta` | 增量文本 | `delta`（增量片段） |
  | `done` | 流结束（成功） | `messageId`、`reply`（完整回复）、`citations` |
  | `error` | 出错 | `error`（错误信息） |

  `AssistantChatStreamEvent` 完整字段：`event`、`sessionId`、`toolMode`、`groupId`、`delta`、`messageId`、`reply`、`citations`、`error`（不同事件下仅相关字段非空）。

  > 前端实现建议：用 `fetch` + `ReadableStream` 解析（需带 cookie），或 `EventSource`（注意 `EventSource` 仅支持 GET，此处为 POST，故优先用 fetch 流式读取）。客户端断开后后端会停止写事件。

### 9.3 会话管理 — `/api/assistant/sessions`

| 方法 | 路径 | 说明 | 返回 |
|---|---|---|---|
| POST | `/api/assistant/sessions` | 新建会话（Body 可选：`CreateAssistantSessionRequest{ initialMessage?: String }`） | `ApiResponse<AssistantSessionDetailVO>` |
| GET | `/api/assistant/sessions` | 我的会话列表 | **裸数组** `AssistantSessionListItemVO[]` |
| GET | `/api/assistant/sessions/{sessionId}` | 会话详情 | **裸对象** `AssistantSessionDetailVO` |
| PATCH | `/api/assistant/sessions/{sessionId}` | 重命名（Body：`UpdateAssistantSessionRequest{ title: String @NotBlank max 255 }`） | `ApiResponse<AssistantSessionDetailVO>` |
| DELETE | `/api/assistant/sessions/{sessionId}` | 删除会话 | `ApiResponse<Void>` |

`AssistantSessionListItemVO`：`sessionId`、`title`、`lastMessageAt`

`AssistantSessionDetailVO`：`sessionId`、`title`、`status`、`lastMessageAt`、`createdAt`

### 9.4 会话上下文（恢复对话）
`GET /api/assistant/sessions/{sessionId}/context?recentLimit={n}`

- Path：`sessionId: Long`；Query：`recentLimit: int`（默认 12）
- 返回（**裸对象**）：`AssistantConversationContextVO`
  | 字段 | 类型 | 说明 |
  |---|---|---|
  | summaryText | String | 会话摘要（长对话压缩） |
  | recentMessages | AssistantMessageVO[] | 最近 N 条消息 |

  `AssistantMessageVO`：`messageId`、`sessionId`、`role`、`toolMode`、`groupId`、`content`、`structuredPayload`、`createdAt`

---

## 附录 A：枚举值速查

| 枚举 | 取值 |
|---|---|
| `SystemRole` | `ADMIN`、`USER` |
| `UserStatus` | （由后端 `UserStatus` 定义，常用：启用/禁用，前端以字符串透传） |
| 组内角色 `role` | `OWNER`、`MEMBER` |
| `AssistantToolMode` | `CHAT`、`KB_SEARCH` |
| SSE 事件名 | `start`、`delta`、`done`、`error` |

## 附录 B：前端对接备忘

1. 请求 baseURL：`/api`（dev 走 Vite 代理），统一 `withCredentials: true`。
2. 注意区分 0.1 节列出的「裸返回」接口，封装 `http` 时不要强行取 `data.data`。
3. 复杂点集中在三处：① 文档分片上传（7.1–7.4 的 init/chunk/complete 协议 + 进度 + 断点续传 + 秒传）；② 助手 SSE 流式（9.2，POST + fetch 流读取 + 4 种事件）；③ QA/助手的引用 `citations` 展示。
4. 时间字段均为 ISO `LocalDateTime` 字符串。
