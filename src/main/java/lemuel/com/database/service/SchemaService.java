package lemuel.com.database.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * 데이터는 그대로고 인덱스만 바뀌었을 수 있는 문제 id. 튜닝 문제는 3만 행이라 schema.sql 을 다시 돌리면
     * 운영 MySQL 에서 14~27초가 걸렸다. 이 상태면 초기 인덱스 목록(baseline)과 비교해 늘어난 것만 지운다.
     */
    private String indexOnlyProblemId;

    /** schema.sql 직후의 테이블별 인덱스. 키는 인덱스 이름, 값은 "UNIQUE|컬럼1,컬럼2" 꼴의 정의. */
    private Map<String, Map<String, String>> baselineIndexes = Map.of();

    public SchemaService(DataSource dataSource, ProblemService problemService) {
        this.dataSource = dataSource;
        this.problemService = problemService;
    }

    /** 스키마가 이미 그 문제의 초기 상태면 건너뛰고, 아니면 다시 만든다. */
    public synchronized void ensureSchema(String problemId) {
        if (problemId != null && problemId.equals(pristineProblemId)) {
            return;
        }
        if (problemId != null && problemId.equals(indexOnlyProblemId) && restoreIndexes()) {
            pristineProblemId = problemId;
            indexOnlyProblemId = null;
            return;
        }
        initializeSchema(problemId);
    }

    /** 데이터나 구조를 바꿀 수 있는 SQL 을 실행하기 직전에 부른다. 실패해도 바뀌었다고 본다. */
    public synchronized void markDirty() {
        pristineProblemId = null;
        indexOnlyProblemId = null;
    }

    /**
     * CREATE / DROP INDEX 만 실행하기 직전에 부른다. 초기 상태였다면 "인덱스만 바뀜" 으로 내려가고,
     * 이미 더럽혀졌다면 그대로 둔다 (다음 번엔 통째로 다시 만든다).
     */
    public synchronized void markIndexChanged() {
        if (pristineProblemId != null) {
            indexOnlyProblemId = pristineProblemId;
            pristineProblemId = null;
        }
    }

    /**
     * 초기에 없던 인덱스를 지운다. 초기 인덱스가 없어졌거나 정의가 달라졌으면(지우고 같은 이름으로 다시 만든 경우)
     * 되살리지 않고 false 를 돌려 통째 재생성으로 넘긴다. 실패해도 false.
     */
    private boolean restoreIndexes() {
        try (Connection conn = dataSource.getConnection()) {
            boolean mysql = isMysql(conn);
            for (Map.Entry<String, Map<String, String>> e : baselineIndexes.entrySet()) {
                String table = e.getKey();
                Map<String, String> current = readIndexes(conn, table);
                for (Map.Entry<String, String> base : e.getValue().entrySet()) {
                    if (!base.getValue().equals(current.get(base.getKey()))) {
                        return false;
                    }
                }
                for (String name : current.keySet()) {
                    if (e.getValue().containsKey(name)) continue;
                    try (Statement st = conn.createStatement()) {
                        st.execute(mysql
                            ? "DROP INDEX " + quote(name, true) + " ON " + quote(table, true)
                            : "DROP INDEX " + quote(name, false));
                    }
                }
                if (!readIndexes(conn, table).equals(e.getValue())) {
                    return false;
                }
            }
            return true;
        } catch (SQLException | RuntimeException ex) {
            return false;
        }
    }

    /** 무조건 schema.sql 을 다시 실행한다. */
    public synchronized void initializeSchema(String problemId) {
        pristineProblemId = null;
        indexOnlyProblemId = null;
        baselineIndexes = Map.of();
        String schemaContent = problemService.getSchemaContent(problemId);
        if (schemaContent == null || schemaContent.isBlank()) {
            return;
        }
        byte[] bytes = schemaContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, resource);
            baselineIndexes = readAllIndexes(conn);
            pristineProblemId = problemId;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema for problem: " + problemId, e);
        }
    }

    private static Map<String, Map<String, String>> readAllIndexes(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), conn.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        Map<String, Map<String, String>> out = new HashMap<>();
        for (String table : tables) {
            out.put(table, readIndexes(conn, table));
        }
        return out;
    }

    /** 인덱스 이름 → "UNIQUE|a,b" / "NONUNIQUE|a,b". 컬럼은 ORDINAL_POSITION 순. */
    private static Map<String, String> readIndexes(Connection conn, String table) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        Map<String, TreeMap<Integer, String>> cols = new TreeMap<>();
        Map<String, Boolean> unique = new HashMap<>();
        try (ResultSet rs = md.getIndexInfo(conn.getCatalog(), conn.getSchema(), table, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null) continue;
                cols.computeIfAbsent(name, k -> new TreeMap<>())
                    .put(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME"));
                unique.put(name, !rs.getBoolean("NON_UNIQUE"));
            }
        }
        Map<String, String> out = new TreeMap<>();
        cols.forEach((name, c) -> out.put(name,
            (unique.get(name) ? "UNIQUE|" : "NONUNIQUE|") + String.join(",", c.values())));
        return out;
    }

    private static boolean isMysql(Connection conn) throws SQLException {
        String p = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        return p.contains("mysql") || p.contains("mariadb");
    }

    private static String quote(String ident, boolean mysql) {
        return mysql ? "`" + ident.replace("`", "``") + "`" : "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
