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
        // H2 uppercases column names
        assertThat(result.columns()).containsExactly("NAME", "DEPARTMENT");
        assertThat(result.rows()).hasSize(3);
        assertThat(result.message()).isEqualTo("OK");
    }

    @Test
    void executeInvalidSql() {
        SqlResult result = sqlExecuteService.execute("basic-001", "SELECT * FROM nonexistent_table_xyz");
        assertThat(result.columns()).isEmpty();
        assertThat(result.message()).isNotBlank();
        assertThat(result.message()).isNotEqualTo("OK");
    }

    @Test
    void blockDangerousKeyword() {
        SqlResult result = SqlExecuteService.checkBlocked("SHUTDOWN");
        assertThat(result).isNotNull();
        assertThat(result.message()).contains("차단");
    }

    @Test
    void blockDropDatabase() {
        SqlResult result = SqlExecuteService.checkBlocked("DROP DATABASE sqltest");
        assertThat(result).isNotNull();
        assertThat(result.message()).contains("차단");
    }
}
