package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class SqlExecuteServiceTest {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Test
    void executeSelectQuery() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SELECT name, department FROM employees");
        assertThat(result.columns()).containsExactly("NAME", "DEPARTMENT");
        assertThat(result.rows()).hasSize(3);
        assertThat(result.message()).isEqualTo("OK");
        assertThat(result.executionTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void executeInvalidSql() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SELECT * FROM nonexistent_table_xyz");
        assertThat(result.columns()).isEmpty();
        assertThat(result.rows()).isEmpty();
        assertThat(result.message()).isNotBlank();
        assertThat(result.message()).isNotEqualTo("OK");
    }

    @Test
    void blockDangerousKeyword() {
        String blocked = SqlExecuteService.checkBlocked("SHUTDOWN");
        assertThat(blocked).isNotNull();

        SqlResult result = sqlExecuteService.execute("basic-001", "SHUTDOWN");
        assertThat(result.columns()).isEmpty();
        assertThat(result.message()).contains("차단");
    }

    @Test
    void blockDropDatabase() {
        String blocked = SqlExecuteService.checkBlocked("DROP DATABASE sqltest");
        assertThat(blocked).isNotNull();

        SqlResult result = sqlExecuteService.execute("basic-001", "DROP DATABASE sqltest");
        assertThat(result.columns()).isEmpty();
        assertThat(result.message()).contains("차단");
    }
}
