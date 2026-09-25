package lemuel.com.database.service;

import lemuel.com.database.dto.SqlResult;
import lemuel.com.database.model.Tuning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * EXPLAIN 결과를 튜닝 기준과 대조해 위반 사항을 돌려준다. 빈 목록이면 통과.
 * MySQL 은 전통 표 형식(type·key·rows·Extra)을 본다. H2 는 계획이 문장 하나라 풀스캔·인덱스 이름만 본다.
 */
final class PlanChecker {

    private PlanChecker() {}

    static List<String> checkMysql(SqlResult plan, Tuning tuning) {
        List<String> cols = plan.columns().stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
        int tableIdx = cols.indexOf("table");
        int typeIdx = cols.indexOf("type");
        int keyIdx = cols.indexOf("key");
        int rowsIdx = cols.indexOf("rows");
        int extraIdx = cols.indexOf("extra");
        if (tableIdx < 0 || typeIdx < 0 || keyIdx < 0 || rowsIdx < 0 || extraIdx < 0) {
            return List.of("실행계획 형식을 읽을 수 없습니다: " + plan.columns());
        }

        List<String> violations = new ArrayList<>();
        boolean found = false;
        for (List<Object> row : plan.rows()) {
            if (!tuning.table().equalsIgnoreCase(str(row.get(tableIdx)))) {
                continue;
            }
            found = true;
            String type = str(row.get(typeIdx));
            String key = str(row.get(keyIdx));
            List<String> extra = extraTokens(str(row.get(extraIdx)));

            if (tuning.forbid().contains("FULL_SCAN") && "ALL".equalsIgnoreCase(type)) {
                violations.add(tuning.table() + " 을 풀스캔합니다 (type=ALL).");
            }
            if (tuning.forbid().contains("FULL_INDEX_SCAN") && "index".equalsIgnoreCase(type)) {
                violations.add(tuning.table() + " 의 인덱스를 처음부터 끝까지 훑습니다 (type=index).");
            }
            if (tuning.forbid().contains("FILESORT") && extra.contains("using filesort")) {
                violations.add("정렬을 인덱스로 못 하고 따로 합니다 (Using filesort).");
            }
            if (tuning.forbid().contains("TEMPORARY") && extra.contains("using temporary")) {
                violations.add("임시 테이블을 만듭니다 (Using temporary).");
            }
            if (tuning.require().contains("COVERING") && !extra.contains("using index")) {
                violations.add("인덱스만으로 끝나지 않고 테이블을 다시 읽습니다 (Extra 에 Using index 없음).");
            }
            if (tuning.requireKey() != null && !tuning.requireKey().equalsIgnoreCase(key)) {
                violations.add(tuning.requireKey() + " 인덱스를 써야 합니다 (지금 key=" + (key.isEmpty() ? "NULL" : key) + ").");
            }
            long rows = parseRows(row.get(rowsIdx));
            if (tuning.maxRows() > 0 && rows > tuning.maxRows()) {
                violations.add("예상 읽기 행 수가 " + rows + " 행입니다 (기준 " + tuning.maxRows() + " 행 이하).");
            }
        }
        if (!found) {
            violations.add("실행계획에 " + tuning.table() + " 테이블이 없습니다.");
        }
        return violations;
    }

    static List<String> checkH2(SqlResult plan, Tuning tuning) {
        StringBuilder sb = new StringBuilder();
        for (List<Object> row : plan.rows()) {
            for (Object cell : row) {
                sb.append(str(cell)).append('\n');
            }
        }
        String text = sb.toString().toUpperCase(Locale.ROOT);
        String table = tuning.table().toUpperCase(Locale.ROOT);

        List<String> violations = new ArrayList<>();
        if (!text.contains(table)) {
            violations.add("실행계획에 " + tuning.table() + " 테이블이 없습니다.");
        }
        if (tuning.forbid().contains("FULL_SCAN") && text.contains(table + ".TABLESCAN")) {
            violations.add(tuning.table() + " 을 풀스캔합니다 (tableScan).");
        }
        if (tuning.requireKey() != null && !text.contains(tuning.requireKey().toUpperCase(Locale.ROOT))) {
            violations.add(tuning.requireKey() + " 인덱스를 써야 합니다.");
        }
        return violations;
    }

    /** "Using where; Using index" → [using where, using index]. 부분 문자열로 보면 "Using index condition" 도 걸린다. */
    private static List<String> extraTokens(String extra) {
        return Arrays.stream(extra.split(";"))
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static long parseRows(Object v) {
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(str(v));
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : v.toString();
    }
}
