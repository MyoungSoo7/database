package lemuel.com.database.model;

import java.util.List;

/**
 * 튜닝 문제의 채점 기준. 결과가 정답과 같은지 본 다음 EXPLAIN 으로 실행계획을 본다.
 *
 * @param mode       INDEX = 인덱스 DDL 을 제출하고 target 쿼리를 채점 / REWRITE = 같은 결과를 내는 쿼리를 제출
 * @param table      실행계획에서 볼 테이블
 * @param maxRows    그 테이블에 대한 EXPLAIN rows 추정치 상한 (0 이면 안 봄)
 * @param forbid     FULL_SCAN(type=ALL) · FULL_INDEX_SCAN(type=index) · FILESORT · TEMPORARY
 * @param require    COVERING(Extra 에 Using index)
 * @param requireKey 반드시 써야 하는 인덱스 이름 (없으면 null) — PK 로 우회해 답을 맞히는 걸 막는다
 */
public record Tuning(
    String mode,
    String table,
    int maxRows,
    List<String> forbid,
    List<String> require,
    String requireKey
) {}
