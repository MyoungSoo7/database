package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlExecuteService {

    private static final int MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 5;
    private static final Set<Pattern> BLOCKED_PATTERNS = Set.of(
        Pattern.compile("(?i)\\bSHUTDOWN\\b"),
        Pattern.compile("(?i)\\bDROP\\s+DATABASE\\b"),
        Pattern.compile("(?i)\\bALTER\\s+USER\\b"),
        Pattern.compile("(?i)\\bCALL\\s+"),
        Pattern.compile("(?i)\\bCREATE\\s+USER\\b"),
        Pattern.compile("(?i)\\bDROP\\s+USER\\b")
    );

    private final JdbcTemplate userJdbcTemplate;
    private final SchemaService schemaService;

    public SqlExecuteService(DataSource dataSource, SchemaService schemaService) {
        this.userJdbcTemplate = new JdbcTemplate(dataSource);
        this.userJdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        this.schemaService = schemaService;
    }

    public static String checkBlocked(String sql) {
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                return pattern.pattern();
            }
        }
        return null;
    }

    public SqlResult execute(String problemId, String sql) {
        String blocked = checkBlocked(sql);
        if (blocked != null) {
            return new SqlResult(List.of(), List.of(), "차단된 명령어입니다: " + blocked, 0);
        }

        schemaService.initializeSchema(problemId);

        long start = System.currentTimeMillis();
        try {
            String trimmed = sql.trim().toUpperCase();
            if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH") || trimmed.startsWith("SHOW")) {
                return executeQuery(sql, start);
            } else {
                return executeUpdate(sql, start);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new SqlResult(List.of(), List.of(), e.getMessage(), elapsed);
        }
    }

    private SqlResult executeQuery(String sql, long start) {
        return userJdbcTemplate.query(sql, rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            List<List<Object>> rows = new ArrayList<>();
            int rowCount = 0;
            while (rs.next() && rowCount < MAX_ROWS) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
                rowCount++;
            }

            long elapsed = System.currentTimeMillis() - start;
            String message = rowCount >= MAX_ROWS
                ? "OK (최대 " + MAX_ROWS + "행까지 표시)"
                : "OK";
            return new SqlResult(columns, rows, message, elapsed);
        });
    }

    private SqlResult executeUpdate(String sql, long start) {
        int affected = userJdbcTemplate.update(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new SqlResult(List.of(), List.of(),
            affected + "행이 영향을 받았습니다.", elapsed);
    }
}
