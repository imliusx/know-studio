# Database Guidelines

## Scenario: MyBatis-Plus Persistence

### 1. Scope / Trigger

Use this contract whenever backend code reads or writes PostgreSQL. The project uses MyBatis-Plus for standard CRUD and Mapper XML for PostgreSQL-specific or state-machine SQL.

### 2. Signatures

- Entity: `@TableName("table_name")` plus `@TableId` for Snowflake IDs.
- Mapper: `interface XxxMapper extends BaseMapper<XxxEntity>` and explicit `@Mapper`.
- Domain boundary: infrastructure repositories implement domain repository interfaces and convert between entities and domain records.
- Complex SQL: place methods in a dedicated mapper and statements under `src/main/resources/mapper/**/*.xml`.
- Migrations: `bootstrap/src/main/resources/db/migration/V<version>__<description>.sql`.
- Final ownership schema: content and evaluation tables contain `knowledge_base_id`; sessions contain `user_id`; no legacy ownership column remains in business tables.

### 3. Contracts

- Knowledge content queries must explicitly include `knowledge_base_id`.
- User-owned conversation queries must explicitly include `user_id` and `session_id`.
- Team access is resolved through explicit Team membership and KnowledgeBase grants; do not use a hidden tenant interceptor.
- A Team grant always gives members READ access. A `MANAGE` grant is exercisable only by `TEAM_ADMIN`; ordinary `MEMBER` users must be downgraded to READ.
- Database columns use `snake_case`; Java properties use `camelCase` with underscore mapping enabled.
- IDs are application-generated Snowflake `BIGINT` values. Do not use database sequences.
- Standard CRUD uses `BaseMapper` and `LambdaQueryWrapper`/`LambdaUpdateWrapper`.
- pgvector, JSONB concatenation, atomic status claims, `ON CONFLICT`, and batch inserts use Mapper XML.
- Required development environment: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, all with local defaults in `application-dev.yml`.

### 4. Validation & Error Matrix

- Missing `knowledge_base_id` on content or `user_id` on conversation data -> architecture/security defect; reject in review.
- Duplicate unique key -> translate `DuplicateKeyException` to `ErrorCode.CONFLICT` at the service boundary.
- Atomic status update affects zero rows -> treat as already claimed or invalid state; do not process twice.
- Flyway validation failure -> application startup must fail; never enable automatic repair in application code.
- A clean database and an existing deployed database must both migrate to the same latest schema; verify legacy tables and columns are absent after ownership cleanup.
- Entity/domain enum value mismatch -> fail fast with `Enum.valueOf`; do not silently default.

### 5. Good/Base/Bad Cases

- Good: `selectOne(lambdaQuery().eq(Entity::getKnowledgeBaseId, knowledgeBaseId).eq(Entity::getId, id))`.
- Base: `selectById(id)` only for globally scoped tables such as `users`.
- Bad: query `documents` by `id` without `knowledge_base_id`.
- Good: XML `UPDATE ... WHERE status IN ('PENDING', 'FAILED')` for an ingestion claim.
- Bad: read status and then update it in two independent statements.

### 6. Tests Required

- Repository integration: Flyway applies on PostgreSQL and Mapper XML statements resolve.
- Service unit tests: duplicate, missing membership, and role hierarchy behavior.
- Isolation test: a user without a KnowledgeBase grant cannot retrieve, preview, download, cite, or mutate its content.
- State-machine test: only one consumer can claim a pending document.
- Migration check: `mvn test` plus application startup against `pgvector/pgvector:pg16`.
- Destructive cleanup migration check: migrate one populated pre-cleanup database and one empty database, then assert data counts plus zero legacy tables/columns.

### 7. Wrong vs Correct

#### Wrong

```java
jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", mapper, documentId);
```

#### Correct

```java
documentMapper.selectOne(Wrappers.<DocumentEntity>lambdaQuery()
        .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
        .eq(DocumentEntity::getId, documentId));
```

For PostgreSQL-specific statements:

```xml
<update id="claimDocumentForProcessing">
    UPDATE documents
    SET status = 'PROCESSING'
    WHERE knowledge_base_id = #{knowledgeBaseId}
      AND id = #{documentId}
      AND status IN ('PENDING', 'FAILED')
</update>
```

## Design Decisions

### MyBatis-Plus With Explicit Access Filtering

The project does not enable the MyBatis-Plus tenant interceptor. The product is
a single-company system: Team membership controls access to KnowledgeBase
resources, while conversations belong directly to users. Access filters remain
visible in repository methods so async ingestion, retrieval, and administration
cannot inherit or bypass request-local state.
