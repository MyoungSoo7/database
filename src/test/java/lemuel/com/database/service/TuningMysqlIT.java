package lemuel.com.database.service;

import lemuel.com.database.dto.SubmitResult;
import lemuel.com.database.model.Problem;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 튜닝 문제의 실행계획 기준은 MySQL 옵티마이저가 실제로 고르는 계획에 맞춰져 있어 H2 로는 검증이 안 된다.
 * 실제 MySQL 을 가리킬 때만 돈다:
 * SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:13306/sqltest SPRING_DATASOURCE_USERNAME=root
 * SPRING_DATASOURCE_PASSWORD=... ./gradlew test --tests '*TuningMysqlIT'
 */
@SpringBootTest
@ActiveProfiles("mysql")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:mysql:.*")
class TuningMysqlIT {

    @Autowired
    private SqlValidationService validation;

    static Stream<String> tuningProblems() {
        return new ProblemService().getProblemsByCategory("tuning").stream().map(Problem::id);
    }

    private Problem problem(String id) {
        return new ProblemService().getProblemById(id).orElseThrow();
    }

    @ParameterizedTest
    @MethodSource("tuningProblems")
    void solutionPasses(String id) {
        SubmitResult r = validation.validate(id, problem(id).solution());
        assertThat(r.correct()).as(r.feedback()).isTrue();
    }

    /** 튜닝 안 한 상태로는 떨어져야 문제가 된다. INDEX 는 쓸모없는 인덱스, REWRITE 는 원래 쿼리 그대로. */
    @ParameterizedTest
    @MethodSource("tuningProblems")
    void untunedFails(String id) {
        Problem p = problem(id);
        String sql = "INDEX".equals(p.tuning().mode())
            ? "CREATE INDEX idx_orders_amount ON orders (amount)"
            : p.target();
        SubmitResult r = validation.validate(id, sql);
        assertThat(r.correct()).as(r.feedback()).isFalse();
        assertThat(r.feedback()).contains("실행계획");
    }

    /** 흔한 반쪽짜리 답. 결과는 맞지만 계획 기준에서 떨어져야 한다. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "tuning-002|CREATE INDEX idx_x ON orders (created_at, status)",
        "tuning-003|CREATE INDEX idx_x ON orders (amount, customer_id)",
        "tuning-004|CREATE INDEX idx_x ON orders (created_at, customer_id)",
        "tuning-005|SELECT COUNT(*) AS cnt, SUM(amount) AS total FROM orders WHERE created_at LIKE '2026-03-15%'",
    })
    void halfAnswersFail(String id, String sql) {
        SubmitResult r = validation.validate(id, sql);
        assertThat(r.correct()).as(r.feedback()).isFalse();
    }
}
