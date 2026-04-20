package lemuel.com.database.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class SchemaServiceTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void initializeSchemaCreatesTables() {
        schemaService.initializeSchema("basic-001");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM employees");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("NAME")).isEqualTo("김철수");
    }

    @Test
    void initializeSchemaCleansUpPreviousState() {
        schemaService.initializeSchema("basic-001");
        jdbcTemplate.update("DELETE FROM employees WHERE id = 1");
        List<Map<String, Object>> afterDelete = jdbcTemplate.queryForList("SELECT * FROM employees");
        assertThat(afterDelete).hasSize(2);

        schemaService.initializeSchema("basic-001");
        List<Map<String, Object>> afterReInit = jdbcTemplate.queryForList("SELECT * FROM employees");
        assertThat(afterReInit).hasSize(3);
    }
}
