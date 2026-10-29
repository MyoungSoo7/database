package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.model.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 문제 작성 도구. expected 가 없는 SELECT 문제의 정답 예시를 실제 MySQL 에서 돌려 결과를
 * build/gen/<id>.yaml 로 적는다. 채점과 같은 경로(SqlExecuteService)로 돌리므로 값의 문자열 표현
 * (DECIMAL 의 소수 자릿수, DATE 형식)이 채점 때와 똑같다. 적힌 결과는 사람이 문제와 대조해 본 뒤 붙인다.
 * GEN_EXPECTED=1 SPRING_DATASOURCE_URL=jdbc:mysql://... ./gradlew test --tests '*ExpectedGenerator'
 */
@SpringBootTest
@ActiveProfiles("mysql")
@EnabledIfEnvironmentVariable(named = "GEN_EXPECTED", matches = "1")
class ExpectedGenerator {

    @Autowired
    private SqlExecuteService execute;
    @Autowired
    private ProblemService problems;
    @Autowired
    private SchemaService schema;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void generate() throws IOException {
        Path dir = Path.of("build/gen");
        Files.createDirectories(dir);
        for (Problem p : problems.getAllProblems()) {
            if ("DDL".equals(p.type()) && p.validation() != null && p.solution() != null) {
                Files.writeString(dir.resolve(p.id() + ".ddl.txt"), ddl(p));
                continue;
            }
            if (!"SELECT".equals(p.type()) || p.expected() != null || p.solution() == null) {
                continue;
            }
            SqlResult r = execute.execute(p.id(), p.solution());
            StringBuilder sb = new StringBuilder("expected:\n");
            if (!r.message().startsWith("OK")) {
                sb.append("  # ERROR ").append(r.message().replace('\n', ' ')).append('\n');
            } else {
                sb.append("  columns: [").append(r.columns().stream().map(ExpectedGenerator::q)
                    .collect(Collectors.joining(", "))).append("]\n");
                sb.append("  rows:\n");
                for (List<Object> row : r.rows()) {
                    sb.append("    - [").append(row.stream().map(ExpectedGenerator::v)
                        .collect(Collectors.joining(", "))).append("]\n");
                }
            }
            Files.writeString(dir.resolve(p.id() + ".yaml"), sb.toString());
        }
    }

    /** 검증 쿼리 값을 초기 상태와 정답 예시 실행 뒤 두 번 잰다. 초기 상태가 이미 기대값이면 문제가 아니다. */
    private String ddl(Problem p) {
        String check = p.validation().checkPerProfile().getOrDefault("mysql",
            p.validation().checkPerProfile().values().iterator().next());
        StringBuilder sb = new StringBuilder();
        try {
            schema.initializeSchema(p.id());
            sb.append("pristine=").append(jdbc.queryForObject(check, Integer.class)).append('\n');
            schema.initializeSchema(p.id());
            schema.markDirty();
            jdbc.execute(p.solution().strip().replaceAll(";\\s*$", ""));
            sb.append("solved=").append(jdbc.queryForObject(check, Integer.class)).append('\n');
        } catch (Exception e) {
            sb.append("ERROR ").append(e.getMessage().replace('\n', ' ')).append('\n');
        } finally {
            schema.markDirty();
        }
        sb.append("expectedValue=").append(p.validation().expectedValue()).append('\n');
        return sb.toString();
    }

    /** 채점은 toString 비교다. 정수는 그대로, 그 밖(DECIMAL·DOUBLE·날짜·문자열)은 toString 을 따옴표로. */
    private static String v(Object o) {
        if (o == null) return "null";
        if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte
            || o instanceof BigInteger) {
            return o.toString();
        }
        return q(o.toString());
    }

    private static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
