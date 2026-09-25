package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCheckerTest {

    private static final List<String> COLS = List.of(
        "id", "select_type", "table", "partitions", "type", "possible_keys", "key", "key_len", "ref", "rows", "filtered", "Extra");

    private static SqlResult plan(String type, String key, long rows, String extra) {
        List<Object> row = Arrays.asList(1L, "SIMPLE", "orders", null, type, key, key, null, null, rows, 100.0, extra);
        return new SqlResult(COLS, List.of(row), "OK", 0);
    }

    private static Tuning tuning(List<String> forbid, List<String> require, String requireKey, int maxRows) {
        return new Tuning("INDEX", "orders", maxRows, forbid, require, requireKey);
    }

    @Test
    void fullScanIsRejected() {
        List<String> v = PlanChecker.checkMysql(plan("ALL", null, 31819, "Using where"),
            tuning(List.of("FULL_SCAN"), List.of(), null, 100));
        assertThat(v).hasSize(2).anyMatch(s -> s.contains("type=ALL")).anyMatch(s -> s.contains("31819"));
    }

    @Test
    void indexLookupPasses() {
        assertThat(PlanChecker.checkMysql(plan("ref", "idx_orders_customer", 15, null),
            tuning(List.of("FULL_SCAN"), List.of(), null, 100))).isEmpty();
    }

    @Test
    void indexConditionIsNotCovering() {
        Tuning t = tuning(List.of(), List.of("COVERING"), null, 0);
        assertThat(PlanChecker.checkMysql(plan("range", "idx_orders_customer", 75, "Using index condition"), t))
            .anyMatch(s -> s.contains("Using index 없음"));
        assertThat(PlanChecker.checkMysql(plan("range", "idx_orders_customer_amount", 75, "Using where; Using index"), t))
            .isEmpty();
    }

    @Test
    void fullIndexScanAndFilesortAreRejected() {
        Tuning t = tuning(List.of("FULL_INDEX_SCAN", "FILESORT", "TEMPORARY"), List.of(), null, 0);
        assertThat(PlanChecker.checkMysql(plan("index", "idx_x", 30000, "Using index; Using temporary; Using filesort"), t))
            .hasSize(3);
    }

    @Test
    void requireKeyRejectsOtherIndex() {
        Tuning t = tuning(List.of(), List.of(), "idx_orders_created", 0);
        assertThat(PlanChecker.checkMysql(plan("range", "PRIMARY", 10, null), t))
            .anyMatch(s -> s.contains("key=PRIMARY"));
        assertThat(PlanChecker.checkMysql(plan("range", "idx_orders_created", 164, null), t)).isEmpty();
    }

    @Test
    void missingTableIsReported() {
        SqlResult p = new SqlResult(COLS, List.of(Arrays.asList(1L, "SIMPLE", "tiny", null, "ALL", null, null, null, null, 1L, 100.0, null)), "OK", 0);
        assertThat(PlanChecker.checkMysql(p, tuning(List.of("FULL_SCAN"), List.of(), null, 100)))
            .anyMatch(s -> s.contains("orders 테이블이 없습니다"));
    }

    @Test
    void h2TableScanIsRejected() {
        SqlResult scan = new SqlResult(List.of("PLAN"),
            List.of(List.of("SELECT COUNT(*) FROM PUBLIC.ORDERS /* PUBLIC.ORDERS.tableScan */ WHERE CUSTOMER_ID = 777")), "OK", 0);
        SqlResult idx = new SqlResult(List.of("PLAN"),
            List.of(List.of("SELECT COUNT(*) FROM PUBLIC.ORDERS /* PUBLIC.IDX_ORDERS_CUSTOMER: CUSTOMER_ID = 777 */")), "OK", 0);
        Tuning t = tuning(List.of("FULL_SCAN"), List.of(), null, 100);
        assertThat(PlanChecker.checkH2(scan, t)).isNotEmpty();
        assertThat(PlanChecker.checkH2(idx, t)).isEmpty();
    }
}
