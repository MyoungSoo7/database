package lemuel.com.database.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 읽기만 하는 실행은 schema.sql 을 다시 돌리지 않는다 (MySQL 에서 요청당 3~5초였다).
 * 재생성을 건너뛰었는지는 서비스 밖에서 몰래 넣은 행이 살아남는지로 본다.
 */
@SpringBootTest
@ActiveProfiles("h2")
class SchemaReuseTest {

    @Autowired
    SqlExecuteService sqlExecuteService;
    @Autowired
    SchemaService schemaService;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void reset() {
        schemaService.initializeSchema("basic-001");
    }

    private long count() {
        Object v = sqlExecuteService.execute("basic-001", "SELECT COUNT(*) FROM employees").rows().get(0).get(0);
        return ((Number) v).longValue();
    }

    private void sneakInRow() {
        jdbcTemplate.update("INSERT INTO employees (id, name, department, salary) VALUES (999, 'x', 'x', 1)");
    }

    @Test
    void readOnlyQueryReusesPristineSchema() {
        assertThat(count()).isEqualTo(3);
        sneakInRow();
        assertThat(count()).as("SELECT 는 재생성하지 않는다").isEqualTo(4);
    }

    @Test
    void writeMakesNextRunRebuild() {
        assertThat(count()).isEqualTo(3);
        sqlExecuteService.execute("basic-001", "DELETE FROM employees");
        assertThat(count()).as("DELETE 뒤엔 다시 만든다").isEqualTo(3);
    }

    @Test
    void withDeleteCountsAsWrite() {
        assertThat(count()).isEqualTo(3);
        sneakInRow();
        schemaService.markDirty();
        assertThat(count()).isEqualTo(3);
        assertThat(SqlExecuteService.isReadOnly("WITH x AS (SELECT 1) DELETE FROM employees")).isFalse();
        assertThat(SqlExecuteService.isReadOnly("WITH x AS (SELECT 1) SELECT * FROM x")).isTrue();
        assertThat(SqlExecuteService.isReadOnly("SELECT * FROM employees FOR UPDATE")).isTrue();
        assertThat(SqlExecuteService.isReadOnly("  show tables")).isTrue();
        assertThat(SqlExecuteService.isReadOnly("/* c */ DELETE FROM employees")).isFalse();
    }

    @Test
    void failedWriteStillCountsAsDirty() {
        assertThat(count()).isEqualTo(3);
        sneakInRow();
        sqlExecuteService.execute("basic-001", "UPDATE no_such_table SET a = 1");
        assertThat(count()).isEqualTo(3);
    }
}
