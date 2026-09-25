package lemuel.com.database.model;

/**
 * @param target   튜닝 문제의 느린 쿼리 (TUNING 만)
 * @param tuning   튜닝 채점 기준 (TUNING 만)
 * @param solution 모범 답안. 화면에 내보내지 않고 문제 자체를 검증하는 테스트에서만 쓴다
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
