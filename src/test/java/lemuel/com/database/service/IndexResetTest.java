package lemuel.com.database.service;

import lemuel.com.database.dto.SubmitResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인덱스만 바꾼 뒤엔 schema.sql 을 다시 돌리지 않고 늘어난 인덱스만 지운다 (운영 MySQL 에서 14~27초였다).
 * 재생성을 건너뛰었는지는 서비스 밖에서 몰래 넣은 행이 살아남는지로 본다.
 */
@SpringBootTest
@ActiveProfiles("h2")
class IndexResetTest {

    @Autowired
    SqlExecuteService sqlExecuteService;
    @Autowired
    SqlValidationService validation;
    @Autowired
    SchemaService schemaService;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void reset() {
        schemaService.initializeSchema("tuning-001");
    }

    private long userIndexes() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'ORDERS' AND INDEX_NAME LIKE 'IDX_%'",
            Long.class);
    }

    private long orders() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
    }

    private void sneakInRow() {
        jdbcTemplate.update("INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) "
            + "VALUES (99999, 'x', 1, 'PAID', 1, TIMESTAMP '2026-01-01 00:00:00')");
    }

    @Test
    void indexSubmissionDropsOnlyAddedIndexes() {
        schemaService.initializeSchema("tuning-001");
        sneakInRow();
        SubmitResult r = validation.validate("tuning-001", "CREATE INDEX idx_orders_customer ON orders (customer_id)");
        assertThat(r.correct()).as(r.feedback()).isTrue();
        assertThat(userIndexes()).isEqualTo(1);

        validation.validate("tuning-001", "CREATE INDEX idx_orders_amount ON orders (amount)");
        assertThat(userIndexes()).as("앞 사람의 인덱스는 지워지고 새 것만 남는다").isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'ORDERS' AND INDEX_NAME LIKE 'IDX_%'",
            String.class)).isEqualToIgnoringCase("idx_orders_amount");
        assertThat(orders()).as("데이터는 다시 넣지 않는다").isEqualTo(30001);
    }

    @Test
    void executeWithIndexThenReadResetsIndexesOnly() {
        schemaService.initializeSchema("tuning-001");
        sneakInRow();
        sqlExecuteService.execute("tuning-001", "CREATE INDEX idx_orders_customer ON orders (customer_id)");
        assertThat(userIndexes()).isEqualTo(1);
        sqlExecuteService.execute("tuning-001", "SELECT 1");
        assertThat(userIndexes()).isZero();
        assertThat(orders()).isEqualTo(30001);
    }

    @Test
    void dataChangeStillRebuilds() {
        schemaService.initializeSchema("tuning-001");
        sqlExecuteService.execute("tuning-001", "CREATE INDEX idx_orders_customer ON orders (customer_id)");
        sqlExecuteService.execute("tuning-001", "DELETE FROM orders WHERE id = 1");
        sqlExecuteService.execute("tuning-001", "SELECT 1");
        assertThat(orders()).isEqualTo(30000);
        assertThat(userIndexes()).isZero();
    }

    @Test
    void otherProblemRebuilds() {
        schemaService.initializeSchema("tuning-001");
        sqlExecuteService.execute("tuning-001", "CREATE INDEX idx_orders_customer ON orders (customer_id)");
        sqlExecuteService.execute("basic-001", "SELECT 1");
        sqlExecuteService.execute("tuning-001", "SELECT 1");
        assertThat(userIndexes()).isZero();
    }
}
