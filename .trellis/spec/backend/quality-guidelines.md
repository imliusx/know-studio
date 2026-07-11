# Backend Quality Guidelines

## Scenario: Java Code Quality Gate

### 1. Scope / Trigger

Applies to all Java source and test code. `mvn validate` is the minimum local and CI quality gate.

### 2. Signatures

- Persistence entity: `@Getter @Setter @NoArgsConstructor @TableName`.
- Constructor injection: final fields plus `@RequiredArgsConstructor` when no constructor logic is required.
- Logger: `@Slf4j`; use parameterized messages.
- Domain DTOs and immutable values: Java `record`, not Lombok data classes.

### 3. Contracts

- Lombok is compile-only and excluded from the executable Spring Boot JAR.
- The parent Maven compiler configuration must register Lombok under
  `annotationProcessorPaths`; inherited `provided` dependencies alone are not a
  reproducible annotation-processing contract on newer JDKs.
- Do not use `@Data` on entities, services, controllers, or domain models.
- Do not use `System.out`, `System.err`, or `printStackTrace`.
- Custom exceptions declare `serialVersionUID`.
- Checkstyle configuration lives at `config/checkstyle/checkstyle.xml`.

### 4. Validation & Error Matrix

- Checkstyle violation -> Maven `validate` fails.
- Lombok annotation processing failure -> compilation fails; do not add handwritten duplicate accessors.
- Incremental build passes but `mvn clean` loses generated loggers/constructors ->
  verify the parent compiler annotation processor path before changing source.
- Console output or `@Data` -> Checkstyle failure.
- Missing runtime logging implementation -> application packaging/startup failure.

### 5. Good/Base/Bad Cases

- Good: `@RequiredArgsConstructor` on a service with final dependencies.
- Base: explicit constructor when it performs callback registration or validation.
- Bad: field injection with `@Autowired`.
- Good: `log.warn("failed id={}", id, exception)`.
- Bad: `System.out.println(...)` or string-concatenated logs.

### 6. Tests Required

- `mvn validate` for static rules.
- `mvn clean test` for annotation processing and behavior; an incremental-only
  pass is insufficient.
- Inspect the executable JAR: Logback and encoder present; Lombok absent.
- Start the application and verify the configured console pattern parses.

### 7. Wrong vs Correct

#### Wrong

```java
@Data
public class UserEntity {
    private String passwordHash;
}
```

#### Correct

```java
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {
    private String passwordHash;
}
```
