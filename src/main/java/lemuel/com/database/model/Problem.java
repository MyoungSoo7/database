package lemuel.com.database.model;

/**
 * @param target   튜닝 문제의 느린 쿼리 (TUNING 만)
 * @param tuning   튜닝 채점 기준 (TUNING 만)
 * @param solution 모범 답안. 문제 화면엔 내보내지 않는다 — 오늘의 문제 풀이(/api/daily/.../solution)와 검증 테스트에서 쓴다
 */
public record Problem(
    String id,
    String title,
    String category,
    int difficulty,
    String description,
    String type,
    String schema,
    Expected expected,
    Validation validation,
    String hint,
    String target,
    Tuning tuning,
    String solution
) {}
