package lemuel.com.database.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlSplitTest {

    @Test
    void splitsOnSemicolonsOutsideQuotesAndComments() {
        String sql = """
            -- 첫 줄 주석; 여기서 안 끊김
            CREATE INDEX idx_a ON orders (customer_id);
            SELECT ';' AS s, `a;b`, "x;y" /* ; */ FROM orders WHERE note = 'it''s; ok';
            ;
            """;
        assertThat(SqlExecuteService.splitStatements(sql)).hasSize(2);
        assertThat(SqlExecuteService.splitStatements(sql).get(1)).contains("'it''s; ok'");
    }

    @Test
    void commentOnlyInputIsEmpty() {
        assertThat(SqlExecuteService.splitStatements("-- 아무것도 없음\n /* ; */ ;")).isEmpty();
    }

    @Test
    void leadingCommentsDoNotHideStatementKind() {
        assertThat(SqlExecuteService.isReadOnly("-- 설명\nEXPLAIN SELECT 1")).isTrue();
        assertThat(SqlExecuteService.isExplain("/* x */ EXPLAIN SELECT 1")).isTrue();
        assertThat(SqlExecuteService.isReadOnly("-- 설명\nCREATE INDEX i ON t (c)")).isFalse();
        assertThat(SqlExecuteService.isReadOnly("EXPLAIN DELETE FROM t")).isFalse();
    }
}
