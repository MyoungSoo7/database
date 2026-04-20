package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.model.Expected;
import lemuel.com.database.model.Problem;
import lemuel.com.database.model.Validation;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class SqlValidationService {

    private final ProblemService problemService;
    private final SqlExecuteService sqlExecuteService;
    private final SchemaService schemaService;
    private final Environment environment;

    public SqlValidationService(ProblemService problemService,
                                SqlExecuteService sqlExecuteService,
                                SchemaService schemaService,
                                Environment environment) {
        this.problemService = problemService;
        this.sqlExecuteService = sqlExecuteService;
        this.schemaService = schemaService;
        this.environment = environment;
    }

    public SubmitResult validate(String problemId, String sql) {
        Optional<Problem> problemOpt = problemService.getProblemById(problemId);
        if (problemOpt.isEmpty()) {
            return new SubmitResult(false, emptySqlResult(), emptySqlResult(), "문제를 찾을 수 없습니다: " + problemId);
        }

        Problem problem = problemOpt.get();
        String type = problem.type() != null ? problem.type().toUpperCase() : "SELECT";

        if ("SELECT".equals(type)) {
            return validateSelect(problem, sql);
        } else {
            return validateDdl(problem, sql);
        }
    }

    private SubmitResult validateSelect(Problem problem, String sql) {
        // Safety check first
        SqlResult blocked = SqlExecuteService.checkBlocked(sql);
        if (blocked != null) {
            return new SubmitResult(false, blocked, emptySqlResult(), "차단된 SQL입니다.");
        }

        SqlResult userResult = sqlExecuteService.execute(problem.id(), sql);
        if (!"OK".equals(userResult.message())) {
            return new SubmitResult(false, userResult, emptySqlResult(), "SQL 실행 오류: " + userResult.message());
        }

        Expected expected = problem.expected();
        if (expected == null) {
            return new SubmitResult(false, userResult, emptySqlResult(), "expected 정보가 없습니다.");
        }

        // Build expected SqlResult for display
        SqlResult expectedResult = new SqlResult(expected.columns(), expected.rows(), "OK", 0L);

        // Compare columns (case-insensitive)
        List<String> userCols = userResult.columns().stream().map(String::toLowerCase).toList();
        List<String> expCols = expected.columns().stream().map(String::toLowerCase).toList();
        if (!userCols.equals(expCols)) {
            return new SubmitResult(false, userResult, expectedResult,
                "컬럼이 일치하지 않습니다. 기대: " + expected.columns() + ", 실제: " + userResult.columns());
        }

        // Compare row count
        if (userResult.rows().size() != expected.rows().size()) {
            return new SubmitResult(false, userResult, expectedResult,
                "행 수가 일치하지 않습니다. 기대: " + expected.rows().size() + "행, 실제: " + userResult.rows().size() + "행");
        }

        // Compare row data
        List<List<String>> userNorm = normalizeRows(userResult.rows());
        List<List<String>> expNorm = normalizeRows(expected.rows());

        boolean rowsMatch;
        if (expected.orderMatters()) {
            rowsMatch = userNorm.equals(expNorm);
        } else {
            rowsMatch = userNorm.size() == expNorm.size()
                && userNorm.containsAll(expNorm)
                && expNorm.containsAll(userNorm);
        }

        if (!rowsMatch) {
            return new SubmitResult(false, userResult, expectedResult, "데이터가 일치하지 않습니다.");
        }

        return new SubmitResult(true, userResult, expectedResult, "정답입니다!");
    }

    private SubmitResult validateDdl(Problem problem, String sql) {
        // Safety check
        SqlResult blocked = SqlExecuteService.checkBlocked(sql);
        if (blocked != null) {
            return new SubmitResult(false, blocked, emptySqlResult(), "차단된 SQL입니다.");
        }

        // Re-init schema, then execute DDL
        schemaService.initializeSchema(problem.id());
        SqlResult ddlResult = sqlExecuteService.execute(problem.id(), sql);
        if (!"OK".equals(ddlResult.message()) && !ddlResult.message().contains("행이 영향")) {
            return new SubmitResult(false, ddlResult, emptySqlResult(), "DDL 실행 오류: " + ddlResult.message());
        }

        // Run validation query per profile
        Validation validation = problem.validation();
        if (validation == null) {
            return new SubmitResult(false, ddlResult, emptySqlResult(), "validation 정보가 없습니다.");
        }

        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "h2";
        String checkQuery = validation.checkPerProfile() != null
            ? validation.checkPerProfile().getOrDefault(profile, validation.checkPerProfile().get("h2"))
            : null;

        if (checkQuery == null) {
            return new SubmitResult(false, ddlResult, emptySqlResult(), "프로파일에 맞는 검증 쿼리가 없습니다: " + profile);
        }

        SqlResult checkResult = sqlExecuteService.execute(problem.id(), checkQuery);
        if (!"OK".equals(checkResult.message())) {
            return new SubmitResult(false, checkResult, emptySqlResult(), "검증 쿼리 실행 오류: " + checkResult.message());
        }

        // Validate expected value
        if (!checkResult.rows().isEmpty() && !checkResult.rows().get(0).isEmpty()) {
            Object val = checkResult.rows().get(0).get(0);
            int actualValue = val instanceof Number n ? n.intValue() : Integer.parseInt(val.toString());
            if (actualValue == validation.expectedValue()) {
                return new SubmitResult(true, ddlResult, checkResult, "정답입니다!");
            } else {
                return new SubmitResult(false, ddlResult, checkResult,
                    "검증 값이 일치하지 않습니다. 기대: " + validation.expectedValue() + ", 실제: " + actualValue);
            }
        }

        return new SubmitResult(false, ddlResult, emptySqlResult(), "검증 결과를 확인할 수 없습니다.");
    }

    private List<List<String>> normalizeRows(List<List<Object>> rows) {
        List<List<String>> result = new ArrayList<>();
        for (List<Object> row : rows) {
            List<String> normalized = new ArrayList<>();
            for (Object cell : row) {
                normalized.add(cell == null ? "null" : cell.toString());
            }
            result.add(normalized);
        }
        return result;
    }

    private SqlResult emptySqlResult() {
        return new SqlResult(List.of(), List.of(), "", 0L);
    }
}
