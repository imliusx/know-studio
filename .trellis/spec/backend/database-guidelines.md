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

### 3. Contracts

- Every business query must explicitly include `workspace_id` when the table is workspace-scoped.
- Database columns use `snake_case`; Java properties use `camelCase` with underscore mapping enabled.
- IDs are application-generated Snowflake `BIGINT` values. Do not use database sequences.
- Standard CRUD uses `BaseMapper` and `LambdaQueryWrapper`/`LambdaUpdateWrapper`.
- pgvector, JSONB concatenation, atomic status claims, `ON CONFLICT`, and batch inserts use Mapper XML.
- Required development environment: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, all with local defaults in `application-dev.yml`.

### 4. Validation & Error Matrix

- Missing `workspace_id` filter on a scoped query -> architecture/security defect; reject in review.
- Duplicate unique key -> translate `DuplicateKeyException` to `ErrorCode.CONFLICT` at the service boundary.
- Atomic status update affects zero rows -> treat as already claimed or invalid state; do not process twice.
- Flyway validation failure -> application startup must fail; never enable automatic repair in application code.
- Entity/domain enum value mismatch -> fail fast with `Enum.valueOf`; do not silently default.

### 5. Good/Base/Bad Cases

- Good: `selectOne(lambdaQuery().eq(Entity::getWorkspaceId, workspaceId).eq(Entity::getId, id))`.
- Base: `selectById(id)` only for globally scoped tables such as `users`.
- Bad: query `documents` by `id` without `workspace_id`.
- Good: XML `UPDATE ... WHERE status IN ('PENDING', 'FAILED')` for an ingestion claim.
- Bad: read status and then update it in two independent statements.

### 6. Tests Required

- Repository integration: Flyway applies on PostgreSQL and Mapper XML statements resolve.
- Service unit tests: duplicate, missing membership, and role hierarchy behavior.
- Isolation test: a user outside workspace A cannot read or mutate workspace A data.
- State-machine test: only one consumer can claim a pending document.
- Migration check: `mvn test` plus application startup against `pgvector/pgvector:pg16`.

### 7. Wrong vs Correct

#### Wrong

```java
jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", mapper, documentId);
```

#### Correct

```java
documentMapper.selectOne(Wrappers.<DocumentEntity>lambdaQuery()
        .eq(DocumentEntity::getWorkspaceId, workspaceId)
        .eq(DocumentEntity::getId, documentId));
```

For PostgreSQL-specific statements:

```xml
<update id="claimDocumentForProcessing">
    UPDATE documents
    SET status = 'PROCESSING'
    WHERE workspace_id = #{workspaceId}
      AND id = #{documentId}
      AND status IN ('PENDING', 'FAILED')
</update>
```

## Design Decisions

### MyBatis-Plus With Explicit Tenant Filtering

The project does not enable the MyBatis-Plus tenant interceptor. Workspace isolation remains visible in every query so asynchronous jobs, system administration, and cross-module APIs cannot accidentally inherit or bypass hidden tenant state.
