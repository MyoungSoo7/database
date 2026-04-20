package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SqlExecuteService {

    private static final int MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 5;

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        Pattern.compile("(?i)\\bSHUTDOWN\\b"),
        Pattern.compile("(?i)\\bDROP\\s+DATABASE\\b"),
        Pattern.compile("(?i)\\bALTER\\s+USER\\b"),
        Pattern.compile("(?i)\\bCALL\\b"),
        Pattern.compile("(?i)\\bCREATE\\s+USER\\b"),
        Pattern.compile("(?i)\\bDROP\\s+USER\\b")
    );

    private final DataSource dataSource;
    private final SchemaService schemaService;

    public SqlExecuteService(DataSource dataSource, SchemaService schemaService) {
        this.dataSource = dataSource;
        this.schemaService = schemaService;
    }

    public static SqlResult checkBlocked(String sql) {
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                return new SqlResult(List.of(), List.of(), "차단된 키워드가 포함되어 있습니다: " + sql, 0L);
            }
        }
        return null;
    }

    public SqlResult execute(String problemId, String sql) {
        SqlResult blocked = checkBlocked(sql);
        if (blocked != null) {
            return blocked;
        }

        schemaService.initializeSchema(problemId);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

        long start = System.currentTimeMillis();
        try {
            String trimmed = sql.strip();
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("SELECT") || upper.startsWith("WITH") || upper.startsWith("SHOW")) {
                return executeQuery(jdbc, sql, start);
            } else {
                return executeUpdate(jdbc, sql, start);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new SqlResult(List.of(), List.of(), e.getMessage(), elapsed);
        }
    }

    private SqlResult executeQuery(JdbcTemplate jdbc, String sql, long start) {
        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();

        jdbc.query(sql, rs -> {
            if (columns.isEmpty()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                for (int i = 1; i <= count; i++) {
                    columns.add(meta.getColumnName(i));
                }
            }
            if (rows.size() < MAX_ROWS) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columns.size(); i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
        });

        long elapsed = System.currentTimeMillis() - start;
        return new SqlResult(columns, rows, "OK", elapsed);
    }

    private SqlResult executeUpdate(JdbcTemplate jdbc, String sql, long start) {
        int affected = jdbc.update(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new SqlResult(List.of(), List.of(), affected + "행이 영향을 받았습니다.", elapsed);
    }
}
