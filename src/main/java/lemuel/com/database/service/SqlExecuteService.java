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

    private static final Pattern DML = Pattern.compile("(?i)\\b(INSERT|UPDATE|DELETE|REPLACE)\\b");

    /**
     * 스키마를 바꾸지 않는 문인가. 아니면 다음 실행 전에 schema.sql 을 다시 돌린다.
     * MySQL 8 은 WITH ... UPDATE/DELETE 가 되므로 WITH 는 DML 키워드가 없을 때만 읽기로 본다.
     * SELECT ... FOR UPDATE 는 잠금만 걸고 데이터는 안 바꾼다.
     */
    static boolean isReadOnly(String sql) {
        String trimmed = stripLeadingComments(sql).toUpperCase();
        if (trimmed.startsWith("SELECT") || trimmed.startsWith("SHOW") || trimmed.startsWith("DESC")) {
            return true;
        }
        return (trimmed.startsWith("WITH") || trimmed.startsWith("EXPLAIN")) && !DML.matcher(sql).find();
    }

    static boolean isExplain(String sql) {
        String trimmed = stripLeadingComments(sql).toUpperCase();
        return trimmed.startsWith("EXPLAIN") || trimmed.startsWith("DESC");
    }

    static boolean isQuery(String sql) {
        String trimmed = stripLeadingComments(sql).toUpperCase();
        return trimmed.startsWith("SELECT") || trimmed.startsWith("WITH")
            || trimmed.startsWith("SHOW") || trimmed.startsWith("EXPLAIN") || trimmed.startsWith("DESC");
    }

    /** 앞에 붙은 주석(-- , 블록)과 공백을 떼어낸다. 문장 종류를 첫 단어로 가르기 위해. */
    static String stripLeadingComments(String sql) {
        String s = sql.strip();
        while (true) {
            if (s.startsWith("--")) {
                int nl = s.indexOf('\n');
                s = nl < 0 ? "" : s.substring(nl + 1).strip();
            } else if (s.startsWith("/*") && !s.startsWith("/*+")) {
                int end = s.indexOf("*/", 2);
                s = end < 0 ? "" : s.substring(end + 2).strip();
            } else {
                return s;
            }
        }
    }

    /**
     * 세미콜론으로 문장을 나눈다. 따옴표('' " `) 안과 주석(-- , 블록) 안의 세미콜론은 구분자가 아니다.
     * 주석·공백뿐인 조각은 버린다.
     */
    static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean meaningful = false;
        int n = sql.length();
        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"' || c == '`') {
                int j = i + 1;
                while (j < n) {
                    char d = sql.charAt(j);
                    if (d == '\\' && c != '`' && j + 1 < n) { j += 2; continue; }
                    if (d == c) {
                        if (j + 1 < n && sql.charAt(j + 1) == c) { j += 2; continue; }
                        break;
                    }
                    j++;
                }
                int end = Math.min(j + 1, n);
                cur.append(sql, i, end);
                meaningful = true;
                i = end - 1;
            } else if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int j = sql.indexOf('\n', i);
                int end = j < 0 ? n : j;
                cur.append(sql, i, end);
                i = end - 1;
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int j = sql.indexOf("*/", i + 2);
                int end = j < 0 ? n : j + 2;
                cur.append(sql, i, end);
                i = end - 1;
            } else if (c == ';') {
                if (meaningful) out.add(cur.toString().trim());
                cur.setLength(0);
                meaningful = false;
            } else {
                cur.append(c);
                if (!Character.isWhitespace(c)) meaningful = true;
            }
        }
        if (meaningful) out.add(cur.toString().trim());
        return out;
    }

    /**
     * 문제의 초기 스키마 위에서 SQL 을 실행한다. 세미콜론으로 여러 문장을 주면 차례로 실행하고
     * 마지막 문장의 결과를 돌려준다 — 튜닝 문제에서 CREATE INDEX 후 EXPLAIN 을 한 번에 보려고.
     */
    public SqlResult execute(String problemId, String sql) {
        String blocked = checkBlocked(sql);
        if (blocked != null) {
            return new SqlResult(List.of(), List.of(), "차단된 명령어입니다: " + blocked, 0);
        }
        List<String> statements = splitStatements(sql);
        if (statements.isEmpty()) {
            return new SqlResult(List.of(), List.of(), "실행할 SQL 이 없습니다.", 0);
        }

        schemaService.ensureSchema(problemId);

        long start = System.currentTimeMillis();
        SqlResult last = null;
        for (int i = 0; i < statements.size(); i++) {
            String stmt = statements.get(i);
            if (!isReadOnly(stmt)) {
                schemaService.markDirty();
            }
            try {
                last = runOne(stmt, start);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                String where = statements.size() > 1 ? (i + 1) + "번째 문장: " : "";
                return new SqlResult(List.of(), List.of(), where + e.getMessage(), elapsed);
            }
        }
        return last;
    }

    /** 스키마를 건드리지 않고 한 문장을 실행한다. 튜닝 채점이 방금 만든 인덱스를 지우지 않도록. */
    public SqlResult runOne(String sql) {
        return runOne(sql, System.currentTimeMillis());
    }

    private SqlResult runOne(String sql, long start) {
        return isQuery(sql) ? executeQuery(sql, start) : executeUpdate(sql, start);
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
