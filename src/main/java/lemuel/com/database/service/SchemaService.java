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

    /**
     * 지금 sqltest 스키마가 schema.sql 그대로인 문제 id. 사용자가 데이터·구조를 바꿀 수 있는 SQL 을
     * 실행하면 null 로 되돌린다. MySQL 에서 DROP/CREATE 가 문 하나에 1~2초라 매번 다시 만들면
     * 요청마다 3~5초가 걸렸다. 호출은 컨트롤러 락으로 직렬화되지만 여기서도 synchronized 로 지킨다.
     */
    private String pristineProblemId;

    public SchemaService(DataSource dataSource, ProblemService problemService) {
        this.dataSource = dataSource;
        this.problemService = problemService;
    }

    /** 스키마가 이미 그 문제의 초기 상태면 건너뛰고, 아니면 다시 만든다. */
    public synchronized void ensureSchema(String problemId) {
        if (problemId != null && problemId.equals(pristineProblemId)) {
            return;
        }
        initializeSchema(problemId);
    }

    /** 데이터나 구조를 바꿀 수 있는 SQL 을 실행하기 직전에 부른다. 실패해도 바뀌었다고 본다. */
    public synchronized void markDirty() {
        pristineProblemId = null;
    }

    /** 무조건 schema.sql 을 다시 실행한다. */
    public synchronized void initializeSchema(String problemId) {
        pristineProblemId = null;
        String schemaContent = problemService.getSchemaContent(problemId);
        if (schemaContent == null || schemaContent.isBlank()) {
            return;
        }
        byte[] bytes = schemaContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, resource);
            pristineProblemId = problemId;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema for problem: " + problemId, e);
        }
    }
}
