# 在 IDEA 中本地启动 know-studio

> 目标：用 Docker 起依赖中间件 + 在 IDEA 里跑后端 + （可选）前端。
>
> 后端默认 profile = `dev`（`application.yml` 里 `spring.profiles.active: dev`），所以 IDEA 直接运行主类即可，无需额外指定 profile。

---

## 0. 前置检查

| 依赖 | 要求 | 检查命令 |
|---|---|---|
| JDK | **21**（pom 要求） | `java -version` |
| Maven | 3.9+（IDEA 自带也可） | `mvn -version` |
| Docker | 已启动 | `docker info` |
| Docker Compose | v2 | `docker compose version` |

> IDEA 建议 2023.2+（对 Spring Boot 3.5 / Java 21 支持较好）。

---

## 第一步：用 Docker 起依赖中间件

后端依赖 4 个外部服务（地址见 `application-dev.yml`）：

| 服务 | 端口 | 用途 |
|---|---|---|
| PostgreSQL + pgvector | **5433** | 业务库 + 向量存储 |
| Elasticsearch | 9200 | 关键词检索 |
| MinIO | 9000 / 9001 | 文档原文存储 |
| Ollama | 11434 | 本地 embedding 模型 |

**只起这 4 个依赖，不要起 backend/frontend**（后端你要在 IDEA 里跑，避免端口/重复启动冲突）。

```bash
# 在项目根目录执行
docker compose up -d postgres elasticsearch minio ollama ollama-model-init
```

> `ollama-model-init` 会自动拉取 embedding 模型 `qllama/bge-small-zh-v1.5`（首次较慢，看日志等到 "Pulling model ... success"）。

验证：
```bash
docker compose ps          # 4 个服务都应是 healthy / running
docker compose logs -f ollama-model-init   # 看模型拉取进度
```

四个服务都 healthy 后再继续。

> ⚠️ 如果不打算用 docker-compose 而是本机各装一套，注意端口要对上：**PG 用 5433（不是默认 5432）**，Ollama 用 11434。

---

## 第二步：（可选但建议）确认 DashScope API Key

`application-dev.yml` 里 chat 模型用的是阿里 DashScope（`glm-5`），key 已写死在配置里：
```yaml
spring.ai.dashscope.api-key: ${DASHSCOPE_API_KEY:}
```
如果这个 key 已失效，启动不报错，但**一对话就会 401**。替换成你自己的 key（[阿里云百炼控制台](https://bailian.console.aliyun.com/)获取）后，用环境变量覆盖最干净：
```bash
# 在 IDEA 的运行配置 Environment variables 里加（见第三步）
DASHSCOPE_API_KEY=sk-你自己的key
```

---

## 第三步：IDEA 配置

1. **导入项目**：`File → Open` 选项目根目录，让 IDEA 识别为 Maven 项目，等它下载依赖（首次较久）。
2. **设 JDK 21**：`File → Project Structure → Project SDK` 选 21。没有就 `Add SDK → Download JDK → 21`。
3. **Maven**：右侧 Maven 面板确认能识别；可在 `Settings → Build → Compiler → Java Compiler` 确认 Target bytecode version = 21。

---

## 第四步：运行后端

1. 打开 `src/main/java/com/dong/ddrag/DDRagApplication.java`
2. 点类名旁的绿色三角 ▶️ 运行（或右键 → Run）

首次运行会自动执行 **Flyway 建表**（`src/main/resources/db/migration`）+ 创建 **dev 管理员账号**：
```
账号: admin
密码: Admin@123456
```

启动成功标志（控制台）：
```
Tomcat started on port 8080
Started DDRagApplication in xx seconds
```

验证健康检查：
```bash
curl http://localhost:8080/actuator/health
# 期望 {"status":"UP"}
```

---

## 第五步：（可选）启动前端

另开终端：
```bash
cd frontend
npm install
npm run dev
```
访问 http://localhost:5173 ，用 `admin / Admin@123456` 登录。

> 前端 dev server 默认通过 Vite 代理把 `/api` 转发到后端 8080，所以**先后端、后前端**的顺序不影响。

---

## 常见问题排查

### 1. 启动报数据库连接失败
- 确认 postgres 容器 healthy 且端口 5433：`docker compose ps`
- 确认没被本机其他 PG 占用 5433

### 2. 一上传文档 / 一问答就报 Ollama 错误
- 确认模型已拉取：`docker compose exec ollama ollama list`，应能看到 `qllama/bge-small-zh-v1.5`
- 没有就手动拉：`docker compose exec ollama ollama pull qllama/bge-small-zh-v1.5`

### 3. 对话报 401 / Invalid API key
- DashScope key 失效，按第二步替换。

### 4. Elasticsearch 连不上 / 索引报错
- 确认 es 容器 healthy：`curl http://localhost:9200`
- 索引名 `know_studio_document_chunks`（见 `application-dev.yml`），首次写入会自动建。

### 5. 端口 8080 被占用
- 改 `application-dev.yml` 的 `server.port`，或在 IDEA 运行配置 VM options 加 `-Dserver.port=8081`。

### 6. Flyway 报迁移失败
- 通常是之前残留的脏数据。开发环境可直接清掉 pg volume 重来：
  ```bash
  docker compose down -v   # ⚠️ 会清空所有数据
  docker compose up -d postgres elasticsearch minio ollama ollama-model-init
  ```

---

## 各服务管理控制台（调试用）

| 服务 | 地址 | 账号 |
|---|---|---|
| 后端 Swagger / Knife4j | http://localhost:8080/doc.html （若启用） | - |
| MinIO 控制台 | http://localhost:9001 | minioadmin / minioadmin |
| Elasticvue（ES 可视化） | 见 docker-compose 的 8088（需起该服务） | - |
| Ollama | http://localhost:11434 | - |
