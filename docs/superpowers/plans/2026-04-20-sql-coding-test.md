# SQL 코딩 테스트 연습 플랫폼 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** SQL 코딩 테스트를 연습할 수 있는 로컬 웹 플랫폼 구축 (쿼리 입력 → 실행 → 채점)

**Architecture:** JdbcTemplate으로 사용자 SQL을 직접 실행하고 결과를 기대값과 비교. 문제는 YAML+SQL 파일로 관리. Thymeleaf+CodeMirror 웹 UI 제공.

**Tech Stack:** Spring Boot 4.0.5, Java 25, JdbcTemplate, Thymeleaf, CodeMirror, H2/MySQL/PostgreSQL, Docker Compose

**Spec:** `docs/superpowers/specs/2026-04-20-sql-coding-test-design.md`

---

## File Structure

### New Files

| File | Responsibility |
|---|---|
| `build.gradle` (modify) | 의존성 추가 (JDBC, Thymeleaf, H2, MySQL, PostgreSQL, CodeMirror) |
| `src/main/resources/application.yaml` (modify) | 프로파일별 DB 설정 |
| `docker-compose.yaml` | MySQL/PostgreSQL 컨테이너 정의 |
| `src/main/java/.../model/Problem.java` | 문제 도메인 모델 |
| `src/main/java/.../model/Expected.java` | SELECT/DML 기대 결과 모델 |
| `src/main/java/.../model/Validation.java` | DDL 검증 모델 |
| `src/main/java/.../model/Category.java` | 카테고리 모델 |
| `src/main/java/.../dto/SqlRequest.java` | SQL 실행 요청 DTO |
| `src/main/java/.../dto/SqlResult.java` | SQL 실행 결과 DTO |
| `src/main/java/.../dto/SubmitResult.java` | 채점 결과 DTO |
| `src/main/java/.../service/ProblemService.java` | YAML에서 문제 로드/조회 |
| `src/main/java/.../service/SchemaService.java` | 문제별 스키마 초기화 |
| `src/main/java/.../service/SqlExecuteService.java` | SQL 실행 + 안전 검사 |
| `src/main/java/.../service/SqlValidationService.java` | 결과 비교/채점 |
| `src/main/java/.../controller/ProblemController.java` | 문제 목록/상세 페이지 |
| `src/main/java/.../controller/SqlExecuteController.java` | SQL 실행/채점 API |
| `src/main/resources/problems/categories.yaml` | 카테고리 정의 |
| `src/main/resources/problems/basic/001-select-all.yaml` | 샘플 문제 1 |
| `src/main/resources/problems/basic/001-schema.sql` | 샘플 문제 1 스키마 |
| `src/main/resources/problems/basic/002-where-filter.yaml` | 샘플 문제 2 |
| `src/main/resources/problems/basic/002-schema.sql` | 샘플 문제 2 스키마 |
| `src/main/resources/templates/problems.html` | 문제 목록 페이지 |
| `src/main/resources/templates/problem-detail.html` | 문제 풀이 페이지 |
| `src/test/java/.../service/ProblemServiceTest.java` | ProblemService 테스트 |
| `src/test/java/.../service/SchemaServiceTest.java` | SchemaService 테스트 |
| `src/test/java/.../service/SqlExecuteServiceTest.java` | SqlExecuteService 테스트 |
| `src/test/java/.../service/SqlValidationServiceTest.java` | SqlValidationService 테스트 |
| `src/test/java/.../controller/ProblemControllerTest.java` | ProblemController 테스트 |
| `src/test/java/.../controller/SqlExecuteControllerTest.java` | SqlExecuteController 테스트 |

> Note: `...` = `lemuel/com/database`

---

## Chunk 1: 프로젝트 기반 설정

### Task 1: build.gradle 의존성 추가 및 application.yaml 설정

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yaml`
- Create: `docker-compose.yaml`

- [ ] **Step 1: build.gradle 수정**

> Note: `spring-boot-starter-webmvc-test` → `spring-boot-starter-test`로 변경 (스펙 비고 참조)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.5'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'lemuel.com'
version = '0.0.1-SNAPSHOT'
description = 'database'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.webjars.npm:codemirror:5.65.18'
    implementation 'org.webjars:webjars-locator-core'

    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'org.postgresql:postgresql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: application.yaml 수정**

```yaml
spring:
  application:
    name: database
  profiles:
    default: h2

---
spring:
  config:
    activate:
      on-profile: h2
  datasource:
    url: jdbc:h2:mem:sqltest;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true

---
spring:
  config:
    activate:
      on-profile: mysql
  datasource:
    url: jdbc:mysql://localhost:3306/sqltest
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

---
spring:
  config:
    activate:
      on-profile: postgres
  datasource:
    url: jdbc:postgresql://localhost:5432/sqltest
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
```

- [ ] **Step 3: docker-compose.yaml 생성**

> Note: 볼륨 미설정은 의도적임 — 로컬 연습 환경이므로 컨테이너 재시작 시 데이터 초기화가 바람직

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: sqltest
    # 볼륨 없음: 연습 환경이므로 재시작 시 데이터 초기화

  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: sqltest
```

- [ ] **Step 4: docker-compose 파일 검증**

Run: `docker compose config --quiet` (Docker 미설치 시 건너뛰기)
Expected: 에러 없이 종료

- [ ] **Step 5: 빌드 확인**

Run: `./gradlew classes`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add build.gradle src/main/resources/application.yaml docker-compose.yaml
git commit -m "chore: add JDBC, Thymeleaf, DB driver dependencies and profile config"
```

---

### Task 2: Model 및 DTO 클래스 생성

**Files:**
- Create: `src/main/java/lemuel/com/database/model/Problem.java`
- Create: `src/main/java/lemuel/com/database/model/Expected.java`
- Create: `src/main/java/lemuel/com/database/model/Validation.java`
- Create: `src/main/java/lemuel/com/database/model/Category.java`
- Create: `src/main/java/lemuel/com/database/dto/SqlRequest.java`
- Create: `src/main/java/lemuel/com/database/dto/SqlResult.java`
- Create: `src/main/java/lemuel/com/database/dto/SubmitResult.java`

- [ ] **Step 1: Model 클래스 생성**

`src/main/java/lemuel/com/database/model/Category.java`:
```java
package lemuel.com.database.model;

public record Category(
    String id,
    String name,
    int order
) {}
```

`src/main/java/lemuel/com/database/model/Expected.java`:
```java
package lemuel.com.database.model;

import java.util.List;

public record Expected(
    List<String> columns,
    List<List<Object>> rows,
    boolean orderMatters
) {}
```

`src/main/java/lemuel/com/database/model/Validation.java`:
```java
package lemuel.com.database.model;

import java.util.Map;

public record Validation(
    String type,
    Map<String, String> checkPerProfile,
    int expectedValue
) {}
```

`src/main/java/lemuel/com/database/model/Problem.java`:
```java
package lemuel.com.database.model;

public record Problem(
    String id,
    String title,
    String category,
    int difficulty,
    String description,
    String type,
    String schema,
    Expected expected,
    Validation validation,
    String hint
) {}
```

- [ ] **Step 2: DTO 클래스 생성**

`src/main/java/lemuel/com/database/dto/SqlRequest.java`:
```java
package lemuel.com.database.dto;

public record SqlRequest(
    String problemId,
    String sql
) {}
```

`src/main/java/lemuel/com/database/dto/SqlResult.java`:
```java
package lemuel.com.database.dto;

import java.util.List;

public record SqlResult(
    List<String> columns,
    List<List<Object>> rows,
    String message,
    long executionTime
) {}
```

`src/main/java/lemuel/com/database/dto/SubmitResult.java`:
```java
package lemuel.com.database.dto;

public record SubmitResult(
    boolean correct,
    SqlResult userResult,
    SqlResult expectedResult,
    String feedback
) {}
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew classes`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/lemuel/com/database/model/ src/main/java/lemuel/com/database/dto/
git commit -m "feat: add domain models and DTOs for problems and SQL execution"
```

---

### Task 3: 샘플 문제 데이터 생성

**Files:**
- Create: `src/main/resources/problems/categories.yaml`
- Create: `src/main/resources/problems/basic/001-select-all.yaml`
- Create: `src/main/resources/problems/basic/001-schema.sql`
- Create: `src/main/resources/problems/basic/002-where-filter.yaml`
- Create: `src/main/resources/problems/basic/002-schema.sql`

- [ ] **Step 1: categories.yaml**

```yaml
categories:
  - id: basic
    name: "기본 SELECT"
    order: 1
  - id: join
    name: "JOIN"
    order: 2
  - id: advanced
    name: "고급 쿼리 (서브쿼리, 윈도우함수, CTE)"
    order: 3
  - id: ddl
    name: "DDL/제약조건"
    order: 4
```

- [ ] **Step 2: basic/001 문제**

`src/main/resources/problems/basic/001-schema.sql`:
```sql
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary INT NOT NULL
);

INSERT INTO employees (id, name, department, salary) VALUES
(1, '김철수', '개발팀', 5000),
(2, '이영희', '기획팀', 4500),
(3, '박민수', '개발팀', 5500);
```

`src/main/resources/problems/basic/001-select-all.yaml`:
```yaml
id: "basic-001"
title: "모든 직원 조회"
category: "basic"
difficulty: 1
description: |
  employees 테이블에서 모든 직원의 이름(name)과 부서(department)를 조회하세요.
type: SELECT
schema: "001-schema.sql"
expected:
  columns: ["name", "department"]
  rows:
    - ["김철수", "개발팀"]
    - ["이영희", "기획팀"]
    - ["박민수", "개발팀"]
  orderMatters: false
hint: "SELECT 컬럼명1, 컬럼명2 FROM 테이블명"
```

- [ ] **Step 3: basic/002 문제**

`src/main/resources/problems/basic/002-schema.sql`:
```sql
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary INT NOT NULL
);

INSERT INTO employees (id, name, department, salary) VALUES
(1, '김철수', '개발팀', 5000),
(2, '이영희', '기획팀', 4500),
(3, '박민수', '개발팀', 5500),
(4, '정수진', '인사팀', 4000),
(5, '최동현', '개발팀', 6000);
```

`src/main/resources/problems/basic/002-where-filter.yaml`:
```yaml
id: "basic-002"
title: "조건으로 필터링"
category: "basic"
difficulty: 1
description: |
  employees 테이블에서 salary가 5000 이상인 직원의 이름(name)과 급여(salary)를 조회하세요.
type: SELECT
schema: "002-schema.sql"
expected:
  columns: ["name", "salary"]
  rows:
    - ["김철수", 5000]
    - ["박민수", 5500]
    - ["최동현", 6000]
  orderMatters: false
hint: "SELECT ... FROM ... WHERE 컬럼명 >= 값"
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/problems/
git commit -m "feat: add sample problem data (categories + 2 basic SELECT problems)"
```

---

## Chunk 2: Service Layer

### Task 4: ProblemService — YAML에서 문제 로드

**Files:**
- Create: `src/main/java/lemuel/com/database/service/ProblemService.java`
- Create: `src/test/java/lemuel/com/database/service/ProblemServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/service/ProblemServiceTest.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Problem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProblemServiceTest {

    @Autowired
    private ProblemService problemService;

    @Test
    void loadCategories() {
        List<Category> categories = problemService.getCategories();
        assertThat(categories).isNotEmpty();
        assertThat(categories.getFirst().id()).isEqualTo("basic");
    }

    @Test
    void loadAllProblems() {
        List<Problem> problems = problemService.getAllProblems();
        assertThat(problems).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findProblemById() {
        Problem problem = problemService.getProblemById("basic-001");
        assertThat(problem).isNotNull();
        assertThat(problem.title()).isEqualTo("모든 직원 조회");
        assertThat(problem.category()).isEqualTo("basic");
        assertThat(problem.expected()).isNotNull();
        assertThat(problem.expected().columns()).containsExactly("name", "department");
    }

    @Test
    void findProblemsByCategory() {
        List<Problem> problems = problemService.getProblemsByCategory("basic");
        assertThat(problems).hasSizeGreaterThanOrEqualTo(2);
        assertThat(problems).allMatch(p -> p.category().equals("basic"));
    }

    @Test
    void getProblemSchemaContent() {
        String schema = problemService.getSchemaContent("basic-001");
        assertThat(schema).contains("CREATE TABLE");
        assertThat(schema).contains("employees");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.ProblemServiceTest"`
Expected: FAIL — `ProblemService` 클래스 없음

- [ ] **Step 3: ProblemService 구현**

`src/main/java/lemuel/com/database/service/ProblemService.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
import lemuel.com.database.model.Validation;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProblemService {

    private final List<Category> categories;
    private final List<Problem> problems;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ProblemService() throws IOException {
        this.categories = loadCategories();
        this.problems = loadAllProblems();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Problem> getAllProblems() {
        return problems;
    }

    public Problem getProblemById(String id) {
        return problems.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Problem> getProblemsByCategory(String categoryId) {
        return problems.stream()
                .filter(p -> p.category().equals(categoryId))
                .toList();
    }

    public String getSchemaContent(String problemId) {
        Problem problem = getProblemById(problemId);
        if (problem == null) return null;
        try {
            Resource resource = resolver.getResource(
                    "classpath:problems/" + problem.category() + "/" + problem.schema());
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema for problem: " + problemId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Category> loadCategories() throws IOException {
        Yaml yaml = new Yaml();
        Resource resource = resolver.getResource("classpath:problems/categories.yaml");
        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> data = yaml.load(is);
            List<Map<String, Object>> catList = (List<Map<String, Object>>) data.get("categories");
            return catList.stream()
                    .map(m -> new Category(
                            (String) m.get("id"),
                            (String) m.get("name"),
                            (int) m.get("order")))
                    .toList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Problem> loadAllProblems() throws IOException {
        Yaml yaml = new Yaml();
        List<Problem> result = new ArrayList<>();
        Resource[] resources = resolver.getResources("classpath:problems/**/*.yaml");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || filename.equals("categories.yaml")) continue;
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> data = yaml.load(is);
                result.add(mapToProblem(data));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Problem mapToProblem(Map<String, Object> data) {
        Expected expected = null;
        if (data.containsKey("expected")) {
            Map<String, Object> exp = (Map<String, Object>) data.get("expected");
            expected = new Expected(
                    (List<String>) exp.get("columns"),
                    (List<List<Object>>) exp.get("rows"),
                    Boolean.TRUE.equals(exp.get("orderMatters")));
        }

        Validation validation = null;
        if (data.containsKey("validation")) {
            Map<String, Object> val = (Map<String, Object>) data.get("validation");
            validation = new Validation(
                    (String) val.get("type"),
                    (Map<String, String>) val.get("checkPerProfile"),
                    (int) val.get("expectedValue"));
        }

        return new Problem(
                (String) data.get("id"),
                (String) data.get("title"),
                (String) data.get("category"),
                (int) data.get("difficulty"),
                (String) data.get("description"),
                (String) data.get("type"),
                (String) data.get("schema"),
                expected,
                validation,
                (String) data.get("hint"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.ProblemServiceTest"`
Expected: ALL PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/lemuel/com/database/service/ProblemService.java src/test/java/lemuel/com/database/service/ProblemServiceTest.java
git commit -m "feat: add ProblemService to load problems from YAML files"
```

---

### Task 5: SchemaService — 문제별 스키마 초기화

**Files:**
- Create: `src/main/java/lemuel/com/database/service/SchemaService.java`
- Create: `src/test/java/lemuel/com/database/service/SchemaServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/service/SchemaServiceTest.java`:
```java
package lemuel.com.database.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaServiceTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void initializeSchemaCreatesTables() {
        schemaService.initializeSchema("basic-001");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM employees");
        assertThat(rows).hasSize(3);
        assertThat(rows.getFirst().get("NAME")).isEqualTo("김철수");
    }

    @Test
    void initializeSchemaCleansUpPreviousState() {
        schemaService.initializeSchema("basic-001");
        jdbcTemplate.update("DELETE FROM employees WHERE id = 1");

        // Re-initialize should restore all data
        schemaService.initializeSchema("basic-001");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM employees");
        assertThat(rows).hasSize(3);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SchemaServiceTest"`
Expected: FAIL — `SchemaService` 클래스 없음

- [ ] **Step 3: SchemaService 구현**

`src/main/java/lemuel/com/database/service/SchemaService.java`:
```java
package lemuel.com.database.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class SchemaService {

    private final DataSource dataSource;
    private final ProblemService problemService;

    public SchemaService(DataSource dataSource, ProblemService problemService) {
        this.dataSource = dataSource;
        this.problemService = problemService;
    }

    public void initializeSchema(String problemId) {
        String schemaContent = problemService.getSchemaContent(problemId);
        if (schemaContent == null) {
            throw new IllegalArgumentException("Problem not found: " + problemId);
        }
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn,
                    new ByteArrayResource(schemaContent.getBytes(StandardCharsets.UTF_8)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema for: " + problemId, e);
        }
    }
}
```

> Note: `ScriptUtils.executeSqlScript()`를 사용하여 문자열 내 세미콜론, 주석, 멀티라인 등을 올바르게 처리

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SchemaServiceTest"`
Expected: ALL PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/lemuel/com/database/service/SchemaService.java src/test/java/lemuel/com/database/service/SchemaServiceTest.java
git commit -m "feat: add SchemaService for per-problem schema initialization"
```

---

### Task 6: SqlExecuteService — SQL 실행 + 안전 검사

**Files:**
- Create: `src/main/java/lemuel/com/database/service/SqlExecuteService.java`
- Create: `src/test/java/lemuel/com/database/service/SqlExecuteServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/service/SqlExecuteServiceTest.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SqlExecuteServiceTest {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Autowired
    private SchemaService schemaService;

    @BeforeEach
    void setUp() {
        schemaService.initializeSchema("basic-001");
    }

    @Test
    void executeSelectQuery() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SELECT name, department FROM employees");
        assertThat(result.columns()).containsExactly("NAME", "DEPARTMENT");
        assertThat(result.rows()).hasSize(3);
        assertThat(result.message()).isEqualTo("OK");
        assertThat(result.executionTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void executeInvalidSql() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SELECT * FROM nonexistent");
        assertThat(result.columns()).isEmpty();
        assertThat(result.rows()).isEmpty();
        assertThat(result.message()).contains("Table \"NONEXISTENT\" not found");
    }

    @Test
    void blockDangerousKeyword() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SHUTDOWN");
        assertThat(result.columns()).isEmpty();
        assertThat(result.message()).contains("차단");
    }

    @Test
    void blockDropDatabase() {
        SqlResult result = sqlExecuteService.execute("basic-001", "DROP DATABASE sqltest");
        assertThat(result.columns()).isEmpty();
        assertThat(result.message()).contains("차단");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SqlExecuteServiceTest"`
Expected: FAIL — `SqlExecuteService` 클래스 없음

- [ ] **Step 3: SqlExecuteService 구현**

`src/main/java/lemuel/com/database/service/SqlExecuteService.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlExecuteService {

    private static final int MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 5;
    private static final Set<Pattern> BLOCKED_PATTERNS = Set.of(
            Pattern.compile("(?i)\\bSHUTDOWN\\b"),
            Pattern.compile("(?i)\\bDROP\\s+DATABASE\\b"),
            Pattern.compile("(?i)\\bALTER\\s+USER\\b"),
            Pattern.compile("(?i)\\bCALL\\s+"),
            Pattern.compile("(?i)\\bCREATE\\s+USER\\b"),
            Pattern.compile("(?i)\\bDROP\\s+USER\\b")
    );

    private final JdbcTemplate userJdbcTemplate;
    private final SchemaService schemaService;

    public SqlExecuteService(DataSource dataSource, SchemaService schemaService) {
        // 사용자 쿼리 전용 JdbcTemplate (타임아웃 적용, 공유 빈 영향 없음)
        this.userJdbcTemplate = new JdbcTemplate(dataSource);
        this.userJdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        this.schemaService = schemaService;
    }

    /**
     * SQL이 차단 대상인지 확인. 다른 서비스에서도 호출 가능.
     */
    public static String checkBlocked(String sql) {
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                return pattern.pattern();
            }
        }
        return null;
    }

    public SqlResult execute(String problemId, String sql) {
        String blocked = checkBlocked(sql);
        if (blocked != null) {
            return new SqlResult(List.of(), List.of(), "차단된 명령어입니다: " + blocked, 0);
        }

        schemaService.initializeSchema(problemId);

        long start = System.currentTimeMillis();
        try {
            String trimmed = sql.trim().toUpperCase();
            if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH") || trimmed.startsWith("SHOW")) {
                return executeQuery(sql, start);
            } else {
                return executeUpdate(sql, start);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new SqlResult(List.of(), List.of(), e.getMessage(), elapsed);
        }
    }

    private SqlResult executeQuery(String sql, long start) {
        return userJdbcTemplate.query(sql, rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            List<List<Object>> rows = new ArrayList<>();
            int rowCount = 0;
            while (rs.next() && rowCount < MAX_ROWS) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
                rowCount++;
            }

            long elapsed = System.currentTimeMillis() - start;
            String message = rowCount >= MAX_ROWS
                    ? "OK (최대 " + MAX_ROWS + "행까지 표시)"
                    : "OK";
            return new SqlResult(columns, rows, message, elapsed);
        });
    }

    private SqlResult executeUpdate(String sql, long start) {
        int affected = userJdbcTemplate.update(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new SqlResult(List.of(), List.of(),
                affected + "행이 영향을 받았습니다.", elapsed);
    }
}
```

> Note: `userJdbcTemplate`은 사용자 쿼리 전용. 공유 `JdbcTemplate` 빈에 타임아웃을 설정하지 않아 `SchemaService` 등에 영향 없음. `checkBlocked()`는 static으로 공개하여 `SqlValidationService`에서도 재사용.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SqlExecuteServiceTest"`
Expected: ALL PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/lemuel/com/database/service/SqlExecuteService.java src/test/java/lemuel/com/database/service/SqlExecuteServiceTest.java
git commit -m "feat: add SqlExecuteService with safety checks and query execution"
```

---

### Task 7: SqlValidationService — 결과 비교/채점

**Files:**
- Create: `src/main/java/lemuel/com/database/service/SqlValidationService.java`
- Create: `src/test/java/lemuel/com/database/service/SqlValidationServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/service/SqlValidationServiceTest.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SqlValidationServiceTest {

    @Autowired
    private SqlValidationService validationService;

    @Test
    void correctAnswerForSelectProblem() {
        SubmitResult result = validationService.validate(
                "basic-001", "SELECT name, department FROM employees");
        assertThat(result.correct()).isTrue();
        assertThat(result.feedback()).contains("정답");
    }

    @Test
    void wrongColumnsForSelectProblem() {
        SubmitResult result = validationService.validate(
                "basic-001", "SELECT name, salary FROM employees");
        assertThat(result.correct()).isFalse();
        assertThat(result.feedback()).contains("컬럼");
    }

    @Test
    void wrongRowCountForSelectProblem() {
        SubmitResult result = validationService.validate(
                "basic-001", "SELECT name, department FROM employees WHERE id = 1");
        assertThat(result.correct()).isFalse();
        assertThat(result.feedback()).contains("행");
    }

    @Test
    void invalidSqlReturnsError() {
        SubmitResult result = validationService.validate(
                "basic-001", "SELECT * FROM nonexistent");
        assertThat(result.correct()).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SqlValidationServiceTest"`
Expected: FAIL — `SqlValidationService` 클래스 없음

- [ ] **Step 3: SqlValidationService 구현**

`src/main/java/lemuel/com/database/service/SqlValidationService.java`:
```java
package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
import lemuel.com.database.model.Validation;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SqlValidationService {

    private final SqlExecuteService sqlExecuteService;
    private final ProblemService problemService;
    private final JdbcTemplate jdbcTemplate;
    private final SchemaService schemaService;
    private final String activeProfile;

    public SqlValidationService(SqlExecuteService sqlExecuteService,
                                 ProblemService problemService,
                                 JdbcTemplate jdbcTemplate,
                                 SchemaService schemaService,
                                 Environment environment) {
        this.sqlExecuteService = sqlExecuteService;
        this.problemService = problemService;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaService = schemaService;
        // Environment.getActiveProfiles()로 안정적으로 프로파일 확인
        String[] profiles = environment.getActiveProfiles();
        this.activeProfile = profiles.length > 0 ? profiles[0] : "h2";
    }

    public SubmitResult validate(String problemId, String sql) {
        Problem problem = problemService.getProblemById(problemId);
        if (problem == null) {
            return new SubmitResult(false, null, null, "문제를 찾을 수 없습니다: " + problemId);
        }

        SqlResult userResult = sqlExecuteService.execute(problemId, sql);

        if (!userResult.message().startsWith("OK") && !userResult.message().contains("영향")) {
            return new SubmitResult(false, userResult, null, "SQL 실행 오류: " + userResult.message());
        }

        if ("DDL".equals(problem.type())) {
            return validateDdl(problem, userResult, sql);
        }

        return validateSelect(problem, userResult);
    }

    private SubmitResult validateSelect(Problem problem, SqlResult userResult) {
        Expected expected = problem.expected();
        SqlResult expectedResult = new SqlResult(
                expected.columns(),
                expected.rows(),
                "OK",
                0);

        // 컬럼 비교 (case-insensitive)
        List<String> expectedCols = expected.columns().stream()
                .map(String::toUpperCase).toList();
        List<String> actualCols = userResult.columns().stream()
                .map(String::toUpperCase).toList();

        if (!expectedCols.equals(actualCols)) {
            return new SubmitResult(false, userResult, expectedResult,
                    "컬럼이 일치하지 않습니다. 기대: " + expected.columns() + ", 실제: " + userResult.columns());
        }

        // 행 수 비교
        if (expected.rows().size() != userResult.rows().size()) {
            return new SubmitResult(false, userResult, expectedResult,
                    "행 수가 다릅니다. 기대: " + expected.rows().size() + "행, 실제: " + userResult.rows().size() + "행");
        }

        // 행 데이터 비교 (toString으로 정규화하여 Integer/Long 등 타입 차이 흡수)
        List<List<String>> expectedRows = normalizeRows(expected.rows());
        List<List<String>> actualRows = normalizeRows(userResult.rows());

        if (!expected.orderMatters()) {
            expectedRows = new ArrayList<>(expectedRows);
            actualRows = new ArrayList<>(actualRows);
            Comparator<List<String>> rowComparator = (a, b) -> a.toString().compareTo(b.toString());
            expectedRows.sort(rowComparator);
            actualRows.sort(rowComparator);
        }

        if (!expectedRows.equals(actualRows)) {
            return new SubmitResult(false, userResult, expectedResult,
                    "데이터가 일치하지 않습니다.");
        }

        return new SubmitResult(true, userResult, expectedResult,
                "정답입니다! (" + userResult.executionTime() + "ms)");
    }

    private SubmitResult validateDdl(Problem problem, SqlResult userResult, String sql) {
        Validation validation = problem.validation();
        if (validation == null) {
            return new SubmitResult(false, userResult, null, "DDL 검증 정보가 없습니다.");
        }

        // DDL도 차단 키워드 검사 적용
        String blocked = SqlExecuteService.checkBlocked(sql);
        if (blocked != null) {
            return new SubmitResult(false, userResult, null, "차단된 명령어입니다: " + blocked);
        }

        // 스키마 초기화 후 사용자 DDL 실행
        schemaService.initializeSchema(problem.id());
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            return new SubmitResult(false, userResult, null, "DDL 실행 오류: " + e.getMessage());
        }

        String checkQuery = validation.checkPerProfile().getOrDefault(activeProfile,
                validation.checkPerProfile().values().iterator().next());

        Integer actualValue = jdbcTemplate.queryForObject(checkQuery, Integer.class);
        if (actualValue != null && actualValue == validation.expectedValue()) {
            return new SubmitResult(true, userResult, null,
                    "정답입니다! (" + userResult.executionTime() + "ms)");
        }

        return new SubmitResult(false, userResult, null, "DDL 검증 실패. 기대값: "
                + validation.expectedValue() + ", 실제값: " + actualValue);
    }

    private List<List<String>> normalizeRows(List<List<Object>> rows) {
        return rows.stream()
                .map(row -> row.stream()
                        .map(obj -> obj == null ? "null" : obj.toString())
                        .toList())
                .toList();
    }
}
```

> Note: (1) `Environment.getActiveProfiles()`로 안정적인 프로파일 확인. (2) DDL 검증 시에도 `checkBlocked()` 적용으로 안전 검사 우회 방지. (3) `normalizeRows`의 `toString()` 변환으로 Integer/Long 등 JDBC 드라이버 타입 차이 흡수.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.service.SqlValidationServiceTest"`
Expected: ALL PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/lemuel/com/database/service/SqlValidationService.java src/test/java/lemuel/com/database/service/SqlValidationServiceTest.java
git commit -m "feat: add SqlValidationService for answer validation and grading"
```

---

## Chunk 3: Controller + Web UI

### Task 8: ProblemController — 문제 목록/상세 페이지

**Files:**
- Create: `src/main/java/lemuel/com/database/controller/ProblemController.java`
- Create: `src/test/java/lemuel/com/database/controller/ProblemControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/controller/ProblemControllerTest.java`:
```java
package lemuel.com.database.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void problemListPage() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(view().name("problems"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("problemsByCategory"))
                .andExpect(content().string(containsString("SQL 코딩 테스트")))
                .andExpect(content().string(containsString("모든 직원 조회")));
    }

    @Test
    void problemDetailPage() throws Exception {
        mockMvc.perform(get("/problems/basic-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("problem-detail"))
                .andExpect(model().attributeExists("problem"))
                .andExpect(content().string(containsString("codemirror")))
                .andExpect(content().string(containsString("employees")));
    }

    @Test
    void problemNotFound() throws Exception {
        mockMvc.perform(get("/problems/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.controller.ProblemControllerTest"`
Expected: FAIL

- [ ] **Step 3: ProblemController 구현**

`src/main/java/lemuel/com/database/controller/ProblemController.java`:
```java
package lemuel.com.database.controller;

import lemuel.com.database.model.Category;
import lemuel.com.database.model.Problem;
import lemuel.com.database.service.ProblemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping("/problems")
    public String problemList(Model model) {
        List<Category> categories = problemService.getCategories();
        Map<String, List<Problem>> problemsByCategory = new LinkedHashMap<>();
        for (Category category : categories) {
            problemsByCategory.put(category.id(),
                    problemService.getProblemsByCategory(category.id()));
        }
        model.addAttribute("categories", categories);
        model.addAttribute("problemsByCategory", problemsByCategory);
        return "problems";
    }

    @GetMapping("/problems/{id}")
    public String problemDetail(@PathVariable String id, Model model) {
        Problem problem = problemService.getProblemById(id);
        if (problem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found: " + id);
        }
        String schemaContent = problemService.getSchemaContent(id);
        model.addAttribute("problem", problem);
        model.addAttribute("schemaContent", schemaContent);
        return "problem-detail";
    }
}
```

- [ ] **Step 4: Thymeleaf 템플릿 — problems.html**

`src/main/resources/templates/problems.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>SQL 코딩 테스트</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; padding: 2rem; }
        h1 { margin-bottom: 2rem; color: #333; }
        .category { background: white; border-radius: 8px; padding: 1.5rem; margin-bottom: 1.5rem; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .category h2 { color: #555; margin-bottom: 1rem; font-size: 1.2rem; }
        .problem-list { list-style: none; }
        .problem-list li { padding: 0.75rem 1rem; border-bottom: 1px solid #eee; }
        .problem-list li:last-child { border-bottom: none; }
        .problem-list a { text-decoration: none; color: #2196F3; font-weight: 500; }
        .problem-list a:hover { text-decoration: underline; }
        .difficulty { color: #FF9800; margin-left: 0.5rem; }
    </style>
</head>
<body>
    <h1>SQL 코딩 테스트 연습</h1>
    <div th:each="category : ${categories}" class="category">
        <h2 th:text="${category.name()}"></h2>
        <ul class="problem-list">
            <li th:each="problem : ${problemsByCategory[category.id()]}">
                <a th:href="@{/problems/{id}(id=${problem.id()})}" th:text="${problem.title()}"></a>
                <span class="difficulty"
                      th:with="stars=${'★'.repeat(problem.difficulty()) + '☆'.repeat(5 - problem.difficulty())}"
                      th:text="${stars}"></span>
            </li>
        </ul>
    </div>
</body>
</html>
```

- [ ] **Step 5: Thymeleaf 템플릿 — problem-detail.html**

`src/main/resources/templates/problem-detail.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${problem.title()}">문제</title>
    <link rel="stylesheet" th:href="@{/webjars/codemirror/lib/codemirror.css}">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f5f5f5; }
        .header { background: #333; color: white; padding: 1rem 2rem; display: flex; align-items: center; gap: 1rem; }
        .header a { color: #90CAF9; text-decoration: none; }
        .header h1 { font-size: 1.2rem; }
        .container { display: flex; height: calc(100vh - 56px); }
        .left-panel { width: 40%; padding: 1.5rem; overflow-y: auto; background: white; border-right: 1px solid #ddd; }
        .right-panel { width: 60%; display: flex; flex-direction: column; }
        .editor-area { flex: 1; display: flex; flex-direction: column; }
        .CodeMirror { flex: 1; font-size: 14px; }
        .buttons { padding: 0.5rem 1rem; background: #fafafa; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd; display: flex; gap: 0.5rem; }
        .buttons button { padding: 0.5rem 1.5rem; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
        .btn-execute { background: #4CAF50; color: white; }
        .btn-submit { background: #2196F3; color: white; }
        .btn-hint { background: #FF9800; color: white; }
        .result-area { height: 40%; overflow-y: auto; padding: 1rem; background: #fafafa; }
        .result-table { width: 100%; border-collapse: collapse; margin-top: 0.5rem; }
        .result-table th, .result-table td { border: 1px solid #ddd; padding: 0.5rem; text-align: left; font-size: 0.85rem; }
        .result-table th { background: #f0f0f0; }
        .correct { color: #4CAF50; font-weight: bold; }
        .wrong { color: #f44336; font-weight: bold; }
        .schema-box { background: #f8f8f8; padding: 1rem; border-radius: 4px; margin-top: 1rem; font-family: monospace; font-size: 0.85rem; white-space: pre-wrap; }
        .difficulty { color: #FF9800; }
        .hint-box { background: #FFF3E0; padding: 1rem; border-radius: 4px; margin-top: 1rem; display: none; }
    </style>
</head>
<body>
    <div class="header">
        <a th:href="@{/problems}">&larr; 문제 목록</a>
        <h1>
            <span th:text="'[' + ${problem.id()} + '] ' + ${problem.title()}"></span>
        </h1>
    </div>
    <div class="container">
        <div class="left-panel">
            <h3>문제 설명</h3>
            <p th:text="${problem.description()}" style="margin: 1rem 0; line-height: 1.6;"></p>

            <p>
                <strong>난이도: </strong>
                <span class="difficulty"
                      th:with="stars=${'★'.repeat(problem.difficulty()) + '☆'.repeat(5 - problem.difficulty())}"
                      th:text="${stars}"></span>
            </p>

            <h4 style="margin-top: 1.5rem;">테이블 구조</h4>
            <div class="schema-box" th:text="${schemaContent}"></div>

            <div id="hintBox" class="hint-box">
                <strong>힌트:</strong>
                <span th:text="${problem.hint()}"></span>
            </div>
        </div>
        <div class="right-panel">
            <div class="editor-area">
                <textarea id="sqlEditor">SELECT </textarea>
            </div>
            <div class="buttons">
                <button class="btn-execute" onclick="executeSql()">실행</button>
                <button class="btn-submit" onclick="submitSql()">제출</button>
                <button class="btn-hint" onclick="toggleHint()">힌트</button>
            </div>
            <div class="result-area" id="resultArea">
                <p style="color: #999;">SQL을 입력하고 실행 또는 제출 버튼을 클릭하세요.</p>
            </div>
        </div>
    </div>

    <script th:src="@{/webjars/codemirror/lib/codemirror.js}"></script>
    <script th:src="@{/webjars/codemirror/mode/sql/sql.js}"></script>
    <script th:inline="javascript">
        const problemId = [[${problem.id()}]];

        const editor = CodeMirror.fromTextArea(document.getElementById('sqlEditor'), {
            mode: 'text/x-sql',
            lineNumbers: true,
            theme: 'default',
            indentWithTabs: true,
            autofocus: true
        });

        function executeSql() {
            const sql = editor.getValue();
            fetch('/api/execute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ problemId: problemId, sql: sql })
            })
            .then(res => res.json())
            .then(data => renderResult(data, false))
            .catch(err => document.getElementById('resultArea').innerHTML = '<p class="wrong">오류: ' + err + '</p>');
        }

        function submitSql() {
            const sql = editor.getValue();
            fetch('/api/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ problemId: problemId, sql: sql })
            })
            .then(res => res.json())
            .then(data => renderSubmitResult(data))
            .catch(err => document.getElementById('resultArea').innerHTML = '<p class="wrong">오류: ' + err + '</p>');
        }

        function renderResult(result, showGrading) {
            const area = document.getElementById('resultArea');
            if (result.columns.length === 0 && result.rows.length === 0) {
                area.innerHTML = '<p>' + result.message + ' (' + result.executionTime + 'ms)</p>';
                return;
            }
            let html = '<p>' + result.message + ' (' + result.executionTime + 'ms)</p>';
            html += '<table class="result-table"><thead><tr>';
            result.columns.forEach(col => html += '<th>' + col + '</th>');
            html += '</tr></thead><tbody>';
            result.rows.forEach(row => {
                html += '<tr>';
                row.forEach(val => html += '<td>' + (val !== null ? val : 'NULL') + '</td>');
                html += '</tr>';
            });
            html += '</tbody></table>';
            area.innerHTML = html;
        }

        function renderSubmitResult(result) {
            const area = document.getElementById('resultArea');
            let html = '';
            if (result.correct) {
                html += '<p class="correct">' + result.feedback + '</p>';
            } else {
                html += '<p class="wrong">' + result.feedback + '</p>';
            }
            if (result.userResult) {
                html += '<h4 style="margin-top:1rem;">내 결과</h4>';
                area.innerHTML = html;
                renderResultInto(area, result.userResult);
            }
            if (result.expectedResult && !result.correct) {
                html = area.innerHTML;
                html += '<h4 style="margin-top:1rem;">기대 결과</h4>';
                area.innerHTML = html;
                renderResultInto(area, result.expectedResult);
            }
            if (!result.userResult) {
                area.innerHTML = html;
            }
        }

        function renderResultInto(area, result) {
            if (result.columns.length === 0) return;
            let html = '<table class="result-table"><thead><tr>';
            result.columns.forEach(col => html += '<th>' + col + '</th>');
            html += '</tr></thead><tbody>';
            result.rows.forEach(row => {
                html += '<tr>';
                row.forEach(val => html += '<td>' + (val !== null ? val : 'NULL') + '</td>');
                html += '</tr>';
            });
            html += '</tbody></table>';
            area.innerHTML += html;
        }

        function toggleHint() {
            const box = document.getElementById('hintBox');
            box.style.display = box.style.display === 'none' ? 'block' : 'none';
        }
    </script>
</body>
</html>
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.controller.ProblemControllerTest"`
Expected: ALL PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/lemuel/com/database/controller/ProblemController.java src/test/java/lemuel/com/database/controller/ProblemControllerTest.java src/main/resources/templates/
git commit -m "feat: add ProblemController and Thymeleaf templates for problem list and detail pages"
```

---

### Task 9: SqlExecuteController — SQL 실행/채점 API

**Files:**
- Create: `src/main/java/lemuel/com/database/controller/SqlExecuteController.java`
- Create: `src/test/java/lemuel/com/database/controller/SqlExecuteControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/lemuel/com/database/controller/SqlExecuteControllerTest.java`:
```java
package lemuel.com.database.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SqlExecuteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void executeReturnsResult() throws Exception {
        mockMvc.perform(post("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").isArray())
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void submitReturnsGradingResult() throws Exception {
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.feedback").exists());
    }

    @Test
    void submitWrongAnswer() throws Exception {
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "lemuel.com.database.controller.SqlExecuteControllerTest"`
Expected: FAIL — `SqlExecuteController` 없음

- [ ] **Step 3: SqlExecuteController 구현**

`src/main/java/lemuel/com/database/controller/SqlExecuteController.java`:
```java
package lemuel.com.database.controller;

import lemuel.com.database.dto.SqlRequest;
import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.service.SqlExecuteService;
import lemuel.com.database.service.SqlValidationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SqlExecuteController {

    private final SqlExecuteService sqlExecuteService;
    private final SqlValidationService sqlValidationService;

    public SqlExecuteController(SqlExecuteService sqlExecuteService,
                                 SqlValidationService sqlValidationService) {
        this.sqlExecuteService = sqlExecuteService;
        this.sqlValidationService = sqlValidationService;
    }

    @PostMapping("/execute")
    public SqlResult execute(@RequestBody SqlRequest request) {
        return sqlExecuteService.execute(request.problemId(), request.sql());
    }

    @PostMapping("/submit")
    public SubmitResult submit(@RequestBody SqlRequest request) {
        return sqlValidationService.validate(request.problemId(), request.sql());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "lemuel.com.database.controller.SqlExecuteControllerTest"`
Expected: ALL PASS

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/lemuel/com/database/controller/SqlExecuteController.java src/test/java/lemuel/com/database/controller/SqlExecuteControllerTest.java
git commit -m "feat: add SqlExecuteController for SQL execution and grading API"
```

---

### Task 10: 통합 테스트 작성

**Files:**
- Modify: `src/test/java/lemuel/com/database/DatabaseApplicationTests.java`

- [ ] **Step 1: 통합 테스트 작성**

`src/test/java/lemuel/com/database/DatabaseApplicationTests.java`:
```java
package lemuel.com.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DatabaseApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void fullFlowIntegrationTest() throws Exception {
        // 1. 문제 목록 조회
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("모든 직원 조회")));

        // 2. 문제 상세 조회
        mockMvc.perform(get("/problems/basic-001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("employees")));

        // 3. SQL 실행
        mockMvc.perform(post("/api/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.rows.length()").value(3));

        // 4. 정답 제출
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name, department FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        // 5. 오답 제출
        mockMvc.perform(post("/api/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"problemId": "basic-001", "sql": "SELECT name FROM employees"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));
    }
}
```

- [ ] **Step 2: 전체 테스트 실행**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 3: 커밋**

```bash
git add src/test/java/lemuel/com/database/DatabaseApplicationTests.java
git commit -m "test: add full-flow integration test"
```

---

### Task 11: 수동 검증 및 최종 확인

- [ ] **Step 1: 앱 시작**

Run: `./gradlew bootRun`
Expected: 앱이 정상 기동

- [ ] **Step 2: 브라우저 검증**

1. http://localhost:8080/problems → 문제 목록 표시 확인
2. 문제 클릭 → 상세 페이지 + SQL 에디터(CodeMirror) 표시 확인
3. `SELECT name, department FROM employees` 입력 → 실행 → 결과 테이블 확인
4. 제출 → 정답 표시 확인
5. 힌트 버튼 → 힌트 토글 확인
