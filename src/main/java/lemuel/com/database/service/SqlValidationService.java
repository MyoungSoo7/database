package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
import lemuel.com.database.model.Tuning;
import lemuel.com.database.model.Validation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SqlValidationService {

    private final SqlExecuteService sqlExecuteService;
    private final ProblemService problemService;
    private final JdbcTemplate jdbcTemplate;
    private final SchemaService schemaService;
    private volatile String dialect;

    public SqlValidationService(SqlExecuteService sqlExecuteService,
                                ProblemService problemService,
                                JdbcTemplate jdbcTemplate,
                                SchemaService schemaService) {
        this.sqlExecuteService = sqlExecuteService;
        this.problemService = problemService;
        this.jdbcTemplate = jdbcTemplate;
        this.schemaService = schemaService;
    }

    public SubmitResult validate(String problemId, String sql) {
        Optional<Problem> problemOpt = problemService.getProblemById(problemId);
        if (problemOpt.isEmpty()) {
            return new SubmitResult(false, null, null, "문제를 찾을 수 없습니다: " + problemId);
        }

        Problem problem = problemOpt.get();

        if ("TUNING".equals(problem.type())) {
            return validateTuning(problem, sql);
        }

        // 여러 문장을 허용하면 DELETE 로 데이터를 정답 모양으로 깎은 뒤 SELECT * 로 맞힐 수 있다.
        if (SqlExecuteService.splitStatements(sql).size() > 1) {
            return new SubmitResult(false, null, null, "제출은 SQL 한 문장만 됩니다. (실행 버튼은 여러 문장 가능)");
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

        List<String> expectedCols = expected.columns().stream()
            .map(String::toUpperCase).toList();
        List<String> actualCols = userResult.columns().stream()
            .map(String::toUpperCase).toList();

        if (!expectedCols.equals(actualCols)) {
            return new SubmitResult(false, userResult, expectedResult,
                "컬럼이 일치하지 않습니다. 기대: " + expected.columns() + ", 실제: " + userResult.columns());
        }

        if (expected.rows().size() != userResult.rows().size()) {
            return new SubmitResult(false, userResult, expectedResult,
                "행 수가 다릅니다. 기대: " + expected.rows().size() + "행, 실제: " + userResult.rows().size() + "행");
        }

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

        String blocked = SqlExecuteService.checkBlocked(sql);
        if (blocked != null) {
            return new SubmitResult(false, userResult, null, "차단된 명령어입니다: " + blocked);
        }

        schemaService.initializeSchema(problem.id());
        schemaService.markDirty();
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            return new SubmitResult(false, userResult, null, "DDL 실행 오류: " + e.getMessage());
        }

        String checkQuery = validation.checkPerProfile().getOrDefault(dialect(),
            validation.checkPerProfile().values().iterator().next());

        Integer actualValue = jdbcTemplate.queryForObject(checkQuery, Integer.class);
        if (actualValue != null && actualValue == validation.expectedValue()) {
            return new SubmitResult(true, userResult, null,
                "정답입니다! (" + userResult.executionTime() + "ms)");
        }

        return new SubmitResult(false, userResult, null, "DDL 검증 실패. 기대값: "
            + validation.expectedValue() + ", 실제값: " + actualValue);
    }

    /**
     * 튜닝 문제. INDEX 는 인덱스 DDL 만 받아 초기 스키마에 적용한 뒤 target 쿼리를 채점하고,
     * REWRITE 는 제출한 SELECT 를 채점한다. 둘 다 결과가 정답과 같아야 하고 실행계획이 기준을 통과해야 한다.
     * 테이블을 다시 만들 수 있으면 정답 행만 담은 작은 테이블로 rows 기준을 속일 수 있어 DDL 종류를 좁힌다.
     */
    private SubmitResult validateTuning(Problem problem, String sql) {
        Tuning tuning = problem.tuning();
        if (tuning == null) {
            return new SubmitResult(false, null, null, "튜닝 채점 정보가 없습니다.");
        }
        String blocked = SqlExecuteService.checkBlocked(sql);
        if (blocked != null) {
            return new SubmitResult(false, null, null, "차단된 명령어입니다: " + blocked);
        }
        List<String> statements = SqlExecuteService.splitStatements(sql);

        String query;
        if ("INDEX".equals(tuning.mode())) {
            // 실행 버튼용으로 같이 적어 둔 EXPLAIN·SELECT 는 무시한다 (읽기만 하니 채점에 영향이 없다).
            statements = statements.stream().filter(st -> !SqlExecuteService.isReadOnly(st)).toList();
            if (statements.isEmpty()) {
                return new SubmitResult(false, null, null, "CREATE INDEX 문을 제출하세요.");
            }
            for (String stmt : statements) {
                if (!SqlExecuteService.isIndexDdl(stmt)) {
                    return new SubmitResult(false, null, null,
                        "인덱스 문제는 CREATE INDEX / DROP INDEX 만 제출할 수 있습니다: " + firstLine(stmt));
                }
            }
            // 3만 행을 다시 넣지 않고, 초기에 없던 인덱스만 지운 뒤 제출한 인덱스를 얹는다.
            schemaService.ensureSchema(problem.id());
            schemaService.markIndexChanged();
            for (String stmt : statements) {
                try {
                    jdbcTemplate.execute(stmt);
                } catch (Exception e) {
                    return new SubmitResult(false, null, null, "인덱스 생성 오류: " + e.getMessage());
                }
            }
            query = problem.target();
        } else {
            if (statements.size() != 1 || !SqlExecuteService.isReadOnly(statements.get(0))
                || SqlExecuteService.isExplain(statements.get(0))) {
                return new SubmitResult(false, null, null, "고친 SELECT 문 한 개를 제출하세요.");
            }
            schemaService.ensureSchema(problem.id());
            query = statements.get(0);
        }

        SqlResult userResult;
        try {
            userResult = sqlExecuteService.runOne(query);
        } catch (Exception e) {
            return new SubmitResult(false, null, null, "SQL 실행 오류: " + e.getMessage());
        }
        SubmitResult resultCheck = validateSelect(problem, userResult);
        if (!resultCheck.correct()) {
            return resultCheck;
        }

        SqlResult plan;
        try {
            plan = sqlExecuteService.runOne("EXPLAIN " + query);
        } catch (Exception e) {
            return new SubmitResult(false, userResult, null, "EXPLAIN 실행 오류: " + e.getMessage());
        }
        List<String> violations = "mysql".equals(dialect())
            ? PlanChecker.checkMysql(plan, tuning)
            : PlanChecker.checkH2(plan, tuning);
        if (!violations.isEmpty()) {
            return new SubmitResult(false, userResult, null,
                "결과는 맞지만 실행계획이 기준을 못 넘었습니다.\n- " + String.join("\n- ", violations), plan);
        }
        return new SubmitResult(true, userResult, null,
            "정답입니다! 실행계획 기준 통과 (" + userResult.executionTime() + "ms)", plan);
    }

    private static String firstLine(String stmt) {
        String line = stmt.strip().lines().findFirst().orElse("");
        return line.length() > 80 ? line.substring(0, 80) + "…" : line;
    }

    /**
     * checkPerProfile 의 키(h2 / mysql / postgres)를 활성 프로필 이름이 아니라 실제 접속한 DB 로 고른다.
     * 운영은 프로필이 "prod" 라 이름으로 찾으면 아무 키에도 안 걸리고 첫 번째 쿼리로 떨어졌다.
     */
    String dialect() {
        String d = dialect;
        if (d == null) {
            d = detectDialect();
            dialect = d;
        }
        return d;
    }

    private String detectDialect() {
        try {
            String product = JdbcUtils.extractDatabaseMetaData(jdbcTemplate.getDataSource(),
                DatabaseMetaData::getDatabaseProductName);
            return dialectOf(product);
        } catch (MetaDataAccessException e) {
            return "h2";
        }
    }

    static String dialectOf(String productName) {
        String p = productName == null ? "" : productName.toLowerCase();
        if (p.contains("mysql") || p.contains("mariadb")) return "mysql";
        if (p.contains("postgres")) return "postgres";
        return "h2";
    }

    private List<List<String>> normalizeRows(List<List<Object>> rows) {
        return rows.stream()
            .map(row -> row.stream()
                .map(obj -> obj == null ? "null" : obj.toString())
                .toList())
            .toList();
    }
}
