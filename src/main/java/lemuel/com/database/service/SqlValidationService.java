package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
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
