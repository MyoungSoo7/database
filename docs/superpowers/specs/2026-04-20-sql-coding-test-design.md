# SQL 코딩 테스트 연습 플랫폼 설계

## 목적

SQL 코딩 테스트(프로그래머스, LeetCode 스타일)를 연습/학습하기 위한 로컬 웹 환경.
SELECT, JOIN, 서브쿼리, 윈도우 함수, CTE, DDL, DML 등 전 영역을 커버한다.

**단일 사용자 로컬 환경**을 전제로 한다. 동시 요청(멀티탭 등)은 지원하지 않는다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 프레임워크 | Spring Boot 4.0.5 / Java 25 |
| SQL 실행 | JdbcTemplate |
| 템플릿 엔진 | Thymeleaf |
| SQL 에디터 | CodeMirror (webjars) |
| 문제 관리 | YAML 파일 (Spring 내장 SnakeYAML) |
| DB (기본) | H2 인메모리 |
| DB (선택) | MySQL 8.0 / PostgreSQL 16 (Docker) |
| 컨테이너 | Docker Compose |

## 아키텍처

```
[Browser - Thymeleaf UI]
        │
        ▼
[Spring Boot Controller]
        │
        ├── ProblemController  — 문제 목록/상세 조회
        ├── SqlExecuteController — SQL 실행 + 결과 비교
        │
        ▼
[Service Layer]
        │
        ├── ProblemService — YAML에서 문제 로드/관리
        ├── SqlExecuteService — JdbcTemplate으로 SQL 실행
        ├── SqlValidationService — 결과 비교/채점
        ├── SchemaService — 문제별 스키마 초기화
        │
        ▼
[Database]
        ├── H2 (기본, 인메모리)
        ├── MySQL (Docker, 프로파일 전환)
        └── PostgreSQL (Docker, 프로파일 전환)
```

### 핵심 흐름

1. 앱 시작 → 문제별 초기 데이터(스키마 + 데이터) 로드
2. 사용자가 문제 선택 → 문제 설명 + SQL 에디터 표시
3. SQL 입력 → 실행 → 결과를 기대값과 비교 → 정답/오답 표시

## 문제 데이터 구조

### 디렉토리 구조

```
src/main/resources/problems/
├── categories.yaml          # 카테고리 정의
├── basic/
│   ├── 001-select-all.yaml  # 문제 정의
│   ├── 001-schema.sql       # 테이블 생성 + 데이터 삽입
│   ├── 002-where-filter.yaml
│   └── 002-schema.sql
├── join/
│   ├── 001-inner-join.yaml
│   └── 001-schema.sql
├── advanced/
│   └── ...
└── ddl/
    └── ...
```

### 카테고리 정의 (categories.yaml)

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

### SELECT/DML 문제 YAML 예시

```yaml
id: "basic-001"
title: "모든 직원 조회"
category: "basic"
difficulty: 1
description: |
  employees 테이블에서 모든 직원의 이름과 부서를 조회하세요.
type: SELECT
schema: "001-schema.sql"
expected:
  columns: ["name", "department"]
  rows:
    - ["김철수", "개발팀"]
    - ["이영희", "기획팀"]
    - ["박민수", "개발팀"]
  orderMatters: false
hint: "SELECT 컬럼명 FROM 테이블명"
```

### DDL 문제 YAML 예시

```yaml
id: "ddl-001"
title: "인덱스 생성"
category: "ddl"
difficulty: 3
description: |
  orders 테이블의 customer_id 컬럼에 인덱스를 생성하세요.
type: DDL
schema: "001-schema.sql"
validation:
  type: DDL_CHECK
  # DB별로 다른 검증 쿼리 지원
  checkPerProfile:
    h2: "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME='ORDERS' AND COLUMN_NAME='CUSTOMER_ID'"
    mysql: "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME='orders' AND COLUMN_NAME='customer_id'"
    postgres: "SELECT COUNT(*) FROM pg_indexes WHERE tablename='orders' AND indexdef LIKE '%customer_id%'"
  expectedValue: 1
```

## 컴포넌트 설계

### Controller

| Controller | 역할 | 주요 엔드포인트 |
|---|---|---|
| `ProblemController` | 문제 목록/상세 | `GET /problems` — 카테고리별 목록<br>`GET /problems/{id}` — 문제 상세 + SQL 에디터 |
| `SqlExecuteController` | SQL 실행/채점 | `POST /api/execute` — SQL 실행 후 결과 반환<br>`POST /api/submit` — SQL 실행 + 기대값 비교 채점 |

### Service

| Service | 역할 |
|---|---|
| `ProblemService` | YAML 파일에서 문제 로드, 카테고리별 조회, 문제 검색 |
| `SqlExecuteService` | `JdbcTemplate`으로 사용자 SQL 실행, SELECT/DML/DDL 구분 처리 |
| `SqlValidationService` | 실행 결과와 기대값 비교 (컬럼명, 행 데이터, 정렬 여부), DDL 검증 |
| `SchemaService` | 문제별 스키마 SQL 실행하여 테이블/데이터 초기화 (매 실행마다 리셋) |

### DTO

```java
// SQL 실행 요청
record SqlRequest(String problemId, String sql) {}

// SQL 실행 결과
record SqlResult(
    List<String> columns,
    List<List<Object>> rows,
    String message,
    long executionTime
) {}

// 채점 결과
record SubmitResult(
    boolean correct,
    SqlResult userResult,
    SqlResult expectedResult,
    String feedback
) {}
```

### 스키마 초기화 전략

매 실행/제출 요청 시:

1. `SchemaService`가 기존 테이블 DROP
2. 해당 문제의 `schema.sql` 실행 (CREATE TABLE + INSERT)
3. 사용자 SQL 실행

DML 문제에서 데이터를 수정해도 다음 실행에 영향을 주지 않는다.

### 에러 처리 및 제한

- **쿼리 타임아웃**: 5초 (`JdbcTemplate.setQueryTimeout(5)`)
- **최대 결과 행**: 1000행 초과 시 잘라서 반환 + 경고 메시지
- **에러 응답**: SQL 에러 발생 시 `SqlResult`의 `columns`/`rows`는 빈 리스트, `message`에 에러 메시지 포함, HTTP 200 반환 (UI에서 에러 표시)
- **차단 키워드**: `SHUTDOWN`, `DROP DATABASE`, `ALTER USER`, `CALL` 등 위험한 명령어는 실행 전 차단 (로컬 환경이지만 실수 방지 목적)

### Model 정의

```java
record Problem(
    String id,
    String title,
    String category,
    int difficulty,
    String description,
    String type,          // SELECT | DML | DDL
    String schema,        // schema SQL 파일명
    Expected expected,    // SELECT/DML용 기대 결과
    Validation validation,// DDL용 검증 정보
    String hint
) {}

record Expected(
    List<String> columns,
    List<List<Object>> rows,
    boolean orderMatters
) {}

record Validation(
    String type,                    // DDL_CHECK
    Map<String, String> checkPerProfile, // 프로파일별 검증 쿼리
    int expectedValue
) {}

record Category(
    String id,
    String name,
    int order
) {}
```

## 웹 UI 구성

### 페이지

| 페이지 | 경로 | 설명 |
|---|---|---|
| 문제 목록 | `/problems` | 카테고리별 문제 목록, 난이도 표시 |
| 문제 풀이 | `/problems/{id}` | 문제 설명 + SQL 에디터 + 결과 영역 |

### 문제 풀이 페이지 레이아웃

```
┌─────────────────────────────────────────────┐
│  ◀ 문제 목록    [basic-001] 모든 직원 조회    │
├─────────────────────┬───────────────────────┤
│                     │                       │
│  문제 설명           │  SQL 에디터            │
│                     │                       │
│  employees 테이블에서 │  SELECT ...           │
│  모든 직원의 이름과   │                       │
│  부서를 조회하세요.   │                       │
│                     │  [실행]  [제출]  [힌트] │
│  ── 테이블 구조 ──   ├───────────────────────┤
│                     │                       │
│  employees          │  실행 결과              │
│  ├ id (INT, PK)     │  ┌──────┬────────┐    │
│  ├ name (VARCHAR)   │  │ name │ dept   │    │
│  └ department       │  │ 김철수│ 개발팀  │    │
│                     │  │ 이영희│ 기획팀  │    │
│  난이도: ★☆☆☆☆      │  └──────┴────────┘    │
│                     │  정답! (12ms)          │
└─────────────────────┴───────────────────────┘
```

### 주요 인터랙션

- **실행** — SQL 실행만 하고 결과 표시 (채점 안 함)
- **제출** — SQL 실행 + 기대값 비교 → 정답/오답 표시
- **힌트** — 문제의 hint 필드 표시
- SQL 에디터는 CodeMirror(webjars) 적용

## DB 프로파일 전환

### application.yaml

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

### 실행 방법

```bash
# H2 (기본, 설치 불필요)
./gradlew bootRun --args='--spring.profiles.active=h2'

# MySQL
docker-compose up mysql -d
./gradlew bootRun --args='--spring.profiles.active=mysql'

# PostgreSQL
docker-compose up postgres -d
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

### docker-compose.yaml

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: sqltest

  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: sqltest
```

## 의존성 (build.gradle 추가분)

```groovy
// DB
implementation 'org.springframework.boot:spring-boot-starter-jdbc'
runtimeOnly 'com.h2database:h2'
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'org.postgresql:postgresql'

// UI
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'org.webjars.npm:codemirror:5.65.18'
implementation 'org.webjars:webjars-locator-core'

// YAML 파싱: Spring Boot 내장 SnakeYAML 사용 (별도 의존성 불필요)
```

## 프로젝트 루트 파일

```
database/
├── build.gradle
├── settings.gradle
├── docker-compose.yaml
├── src/
│   └── ...
└── docs/
    └── ...
```

## 패키지 구조

```
lemuel.com.database
├── DatabaseApplication.java
├── controller/
│   ├── ProblemController.java
│   └── SqlExecuteController.java
├── service/
│   ├── ProblemService.java
│   ├── SqlExecuteService.java
│   ├── SqlValidationService.java
│   └── SchemaService.java
├── dto/
│   ├── SqlRequest.java
│   ├── SqlResult.java
│   └── SubmitResult.java
└── model/
    ├── Problem.java
    ├── Expected.java
    ├── Validation.java
    └── Category.java
```

## 비고

- Lombok은 기존 build.gradle에 포함되어 있으나, 이 프로젝트에서는 Java record를 사용하므로 Lombok이 불필요하다. 기존 의존성은 유지하되 신규 클래스에는 사용하지 않는다.
- 기존 build.gradle의 `spring-boot-starter-webmvc-test`는 `spring-boot-starter-test`로 수정한다.
