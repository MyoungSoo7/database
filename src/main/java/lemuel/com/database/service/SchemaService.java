package lemuel.com.database.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class SchemaService {

    private final DataSource dataSource;
    private final ProblemService problemService;

    public SchemaService(DataSource dataSource, ProblemService problemService) {
        this.dataSource = dataSource;
        this.problemService = problemService;
    }

    public void initializeSchema(String problemId) {
        String schemaContent = problemService.getSchemaContent(problemId);
        if (schemaContent == null || schemaContent.isBlank()) {
            return;
        }
        byte[] bytes = schemaContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, resource);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema for problem: " + problemId, e);
        }
    }
}
