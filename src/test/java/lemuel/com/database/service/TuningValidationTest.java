package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class TuningValidationTest {

    @Autowired
    private SqlValidationService validation;

    @Autowired
    private SqlExecuteService execute;

    @Test
    void selectProblemRejectsMultipleStatements() {
        SubmitResult r = validation.validate("basic-001",
            "DELETE FROM employees WHERE id > 1; SELECT name, department FROM employees");
        assertThat(r.correct()).isFalse();
        assertThat(r.feedback()).contains("한 문장");
    }

    @Test
    void indexProblemRejectsNonIndexDdl() {
        SubmitResult r = validation.validate("tuning-001",
            "DROP TABLE orders; CREATE TABLE orders (id INT PRIMARY KEY)");
        assertThat(r.correct()).isFalse();
        assertThat(r.feedback()).contains("CREATE INDEX");
    }

    @Test
    void rewriteProblemRejectsDml() {
        SubmitResult r = validation.validate("tuning-005", "DELETE FROM orders");
        assertThat(r.correct()).isFalse();
    }

    @Test
    void executeRunsStatementsInOrderAndReturnsLast() {
        SqlResult r = execute.execute("tuning-001",
            "CREATE INDEX idx_orders_customer ON orders (customer_id); SELECT COUNT(*) AS cnt FROM orders WHERE customer_id = 777");
        assertThat(r.message()).startsWith("OK");
        assertThat(r.rows().get(0).get(0).toString()).isEqualTo("15");
    }

    @Test
    void indexSolutionPassesOnH2AndIgnoresTrailingExplain() {
        SubmitResult r = validation.validate("tuning-001",
            "-- 풀이\nCREATE INDEX idx_orders_customer ON orders (customer_id);\nEXPLAIN SELECT 1");
        assertThat(r.correct()).as(r.feedback()).isTrue();
        assertThat(r.plan()).isNotNull();
    }

    @Test
    void untunedTargetFailsOnPlan() {
        SubmitResult r = validation.validate("tuning-001", "CREATE INDEX idx_orders_status ON orders (status)");
        assertThat(r.correct()).isFalse();
        assertThat(r.feedback()).contains("실행계획");
    }
}
