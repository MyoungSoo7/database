package lemuel.com.database.service;

import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class SqlValidationServiceTest {

    @Autowired
    private SqlValidationService sqlValidationService;

    @Test
    void correctAnswerForSelectProblem() {
        SubmitResult result = sqlValidationService.validate("basic-001",
            "SELECT name, department FROM employees");
        assertThat(result.correct()).isTrue();
        assertThat(result.feedback()).contains("정답");
    }

    @Test
    void wrongColumnsForSelectProblem() {
        SubmitResult result = sqlValidationService.validate("basic-001",
            "SELECT id, name FROM employees");
        assertThat(result.correct()).isFalse();
        assertThat(result.feedback()).contains("컬럼");
    }

    @Test
    void wrongRowCountForSelectProblem() {
        SubmitResult result = sqlValidationService.validate("basic-001",
            "SELECT name, department FROM employees WHERE department = '개발팀'");
        assertThat(result.correct()).isFalse();
        assertThat(result.feedback()).contains("행");
    }

    @Test
    void invalidSqlReturnsError() {
        SubmitResult result = sqlValidationService.validate("basic-001",
            "SELECT * FROM nonexistent_table_xyz");
        assertThat(result.correct()).isFalse();
    }

    @Test
    void numbersCompareByValueNotByType() {
        assertThat(SqlValidationService.normalizeValue(new java.math.BigDecimal("5250.0000")))
            .isEqualTo(SqlValidationService.normalizeValue(5250L))
            .isEqualTo(SqlValidationService.normalizeValue(5250.0))
            .isEqualTo(SqlValidationService.normalizeValue("5250.0000"));
        assertThat(SqlValidationService.normalizeValue(new java.math.BigDecimal("0.00"))).isEqualTo("0");
        assertThat(SqlValidationService.normalizeValue(146.67))
            .isNotEqualTo(SqlValidationService.normalizeValue(new java.math.BigDecimal("146.7")));
        assertThat(SqlValidationService.normalizeValue("2025-03-01")).isEqualTo("2025-03-01");
        assertThat(SqlValidationService.normalizeValue("개발팀")).isEqualTo("개발팀");
    }
}
